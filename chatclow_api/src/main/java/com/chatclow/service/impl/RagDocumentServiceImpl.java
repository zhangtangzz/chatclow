package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagChunk;
import com.chatclow.entity.RagDocument;
import com.chatclow.mapper.RagDocumentMapper;
import com.chatclow.service.EmbeddingService;
import com.chatclow.service.RagChunkService;
import com.chatclow.service.RagDocumentService;
import com.chatclow.storage.vector.ChatClowVectorStore;
import com.chatclow.storage.vector.VectorStoreFactory;
import com.chatclow.util.ChunkSplitUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 文档上传-处理-查重-保存
 *
 */

@Service
public class RagDocumentServiceImpl implements RagDocumentService {

    private static final Logger log = LoggerFactory.getLogger(RagDocumentServiceImpl.class);

    @Autowired
    private RagDocumentMapper ragDocumentMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreFactory vectorStoreFactory;

    @Autowired
    private RagChunkService ragChunkService;

    @Autowired
    @Qualifier("sseExecutor")
    private Executor sseExecutor;

    @Override
    public boolean add(RagDocument document) {
        return ragDocumentMapper.insert(document) > 0;
    }

    @Override
    public List<RagDocument> listByKbId(Long kbId) {
        LambdaQueryWrapper<RagDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RagDocument::getKbId, kbId);
        wrapper.orderByDesc(RagDocument::getCreatedDt);  // 最新的在前面
        return ragDocumentMapper.selectList(wrapper);
    }

    @Override
    public RagDocument getById(Long id) {
        return ragDocumentMapper.selectById(id);
    }

    @Override
    public boolean update(RagDocument document) {
        return ragDocumentMapper.updateById(document) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        // 1. 查出文档，获取知识库ID
        RagDocument doc = ragDocumentMapper.selectById(id);
        if (doc == null) return false;

        // 2. 清理 MySQL chatclow_rag_chunk 中的切片（MySQL 模式）
        ragChunkService.deleteByDocumentId(id);

        // 3. 清理向量存储中的向量（PG / MongoDB 模式）
        try {
            ChatClowVectorStore vectorStore = vectorStoreFactory.getVectorStore(doc.getKbId());
            vectorStore.deleteByDocument(id);
        } catch (Exception e) {
            log.warn("[RAG-Delete] 清理向量存储失败: {}", e.getMessage());
        }

        // 4. 删除文档记录
        return ragDocumentMapper.deleteById(id) > 0;
    }

    /**
     * 文档处理全链路：切片 → 去重 → 向量化 → 存储
     * 存储：通过 VectorStoreFactory 路由到对应后端（MySQL / PG / MongoDB）
     */
    @Override
    public void processDocument(Long documentId) {
        // 1. 查出文档
        RagDocument document = ragDocumentMapper.selectById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        if (document.getContent() == null || document.getContent().isEmpty()) {
            throw new RuntimeException("文档内容为空");
        }

        // 2. 切片
        List<String> textChunks = ChunkSplitUtil.split(document.getContent());
        if (textChunks.isEmpty()) {
            throw new RuntimeException("切片结果为空");
        }
        log.info("[RAG] 文档切成 " + textChunks.size() + " 个片段");

        // 3. ★ 通过 VectorStoreFactory 获取该知识库对应的向量存储后端
        Long kbId = document.getKbId();
        ChatClowVectorStore vectorStore = vectorStoreFactory.getVectorStore(kbId);
        log.info("[RAG] 知识库 " + kbId + " 使用向量存储: " + vectorStore.getClass().getSimpleName());

        // 4. ★ 去重过滤：通过 SPI 接口查询后端是否已有该内容
        List<String> uniqueChunks = new ArrayList<>();
        List<String> uniqueHashes = new ArrayList<>();
        int skippedCount = 0;

        for (String chunkContent : textChunks) {
            String hash = sha256(chunkContent);

            // 通过 SPI 接口查询去重（MySQL 查 chatclow_rag_chunk，PG 查 rag_vectors）
            if (vectorStore.existsByHash(kbId, hash)) {
                skippedCount++;
                log.info("[RAG-Dedup] 跳过重复切片，hash=" + hash.substring(0, 8) + "...");
                continue;
            }

            uniqueChunks.add(chunkContent);
            uniqueHashes.add(hash);
        }
        log.info("[RAG-Dedup] 去重完成：跳过 " + skippedCount + " 个，剩余 " + uniqueChunks.size() + " 个待向量化");

        if (uniqueChunks.isEmpty()) {
            document.setChunkCount(0);
            document.setStatus(3);
            ragDocumentMapper.updateById(document);
            log.info("[RAG-Dedup] 所有切片都是重复的，无需处理");
            return;
        }

        // 5. 批量向量化
        List<float[]> embeddings = embeddingService.batchEmbed(uniqueChunks);

        // 6. 组装 Chunk 对象
        List<RagChunk> chunks = new ArrayList<>();
        for (int i = 0; i < uniqueChunks.size(); i++) {
            RagChunk chunk = new RagChunk();
            chunk.setKbId(kbId);
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(uniqueChunks.get(i));
            chunk.setTokenCount(uniqueChunks.get(i).length());
            chunk.setContentHash(uniqueHashes.get(i));

            String vectorStr = arrayToString(embeddings.get(i));
            chunk.setVectorData(vectorStr);

            chunks.add(chunk);
        }

        // 7. ★ 通过 SPI 接口写入对应后端（MySQL / PG / MongoDB）
        boolean storeResult = vectorStore.addBatch(chunks);
        if (!storeResult) {
            throw new RuntimeException("向量写入失败，后端: " + vectorStore.getClass().getSimpleName());
        }
        log.info("[RAG] 向量已写入 " + vectorStore.getClass().getSimpleName() + "，共 " + chunks.size() + " 条");

        // 8. 更新文档的切片数量和状态
        document.setChunkCount(chunks.size());
        document.setStatus(3);  // 处理完成
        ragDocumentMapper.updateById(document);

        log.info("[RAG] 文档处理完成！去重后入库 " + chunks.size() + " 条切片（跳过重复 " + skippedCount + " 条）");
    }

    /**
     * 将 float[] 向量转为逗号分隔的字符串
     */
    private String arrayToString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.toString();
    }
    @Override
    public Long uploadAndProcess(MultipartFile file, Long kbId) {
        try {
            // 1. 保存文件到 uploads/ 目录（用绝对路径，避免 transferTo 路径解析问题）
            String projectDir = System.getProperty("user.dir");
            String uploadDir = projectDir + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + fileName;
            // 用 Files.copy 代替 transferTo，路径更可控
            Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);

            // 2. 根据文件类型提取文本
            String content = extractText(filePath, file.getOriginalFilename());

            // 3. 创建 RagDocument 记录（status=1 解析中）
            RagDocument document = new RagDocument();
            document.setKbId(kbId);
            document.setName(file.getOriginalFilename());
            document.setFileType(getFileExtension(file.getOriginalFilename()));
            document.setContent(content);
            document.setStatus(1); // 解析中
            document.setChunkCount(0);
            ragDocumentMapper.insert(document); // 插入后 ID 自动回填

            // 4. 立即返回 documentId，后台异步处理
            Long documentId = document.getId();
            sseExecutor.execute(() -> asyncProcessDocument(documentId));

            return documentId;

        } catch (Exception e) {
            throw new RuntimeException("文件上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 后台异步处理文档（切片+向量化）
     */
    private void asyncProcessDocument(Long documentId) {
        try {
            // 更新状态为"处理中"
            RagDocument doc = ragDocumentMapper.selectById(documentId);
            doc.setStatus(2);
            ragDocumentMapper.updateById(doc);

            // 执行处理（复用已有的 processDocument 逻辑）
            processDocument(documentId);

            // 处理完成，状态已在 processDocument 里更新为 3

        } catch (Exception e) {
            // 处理失败，更新状态为 4
            RagDocument doc = ragDocumentMapper.selectById(documentId);
            if (doc != null) {
                doc.setStatus(4);
                doc.setErrorMsg(e.getMessage());
                ragDocumentMapper.updateById(doc);
            }
            log.warn("[RAG-Upload] 处理失败: {}", e.getMessage());
        }
    }

    /**
     * 根据文件类型提取文本内容
     * 支持：txt, md（自动检测编码）| pdf（PDFBox）| docx（Apache POI）
     */
    private String extractText(String filePath, String fileName) throws IOException {
        String ext = getFileExtension(fileName).toLowerCase();
        if ("txt".equals(ext) || "md".equals(ext)) {
            return extractFromTextFile(filePath, fileName);
        } else if ("pdf".equals(ext)) {
            return extractFromPdf(filePath, fileName);
        } else if ("docx".equals(ext)) {
            return extractFromDocx(filePath, fileName);
        } else {
            throw new RuntimeException("暂不支持的文件类型: ." + ext + "，目前支持 txt、md、pdf、docx");
        }
    }

    /**
     * 从纯文本文件提取（txt/md），自动检测 UTF-8 / GBK 编码
     */
    private String extractFromTextFile(String filePath, String fileName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (containsGarbled(text)) {
            text = new String(bytes, java.nio.charset.Charset.forName("GBK"));
            log.info("[RAG-Upload] 文件使用 GBK 编码读取: " + fileName);
        } else {
            log.info("[RAG-Upload] 文件使用 UTF-8 编码读取: " + fileName);
        }
        return text;
    }

    /**
     * 从 PDF 文件提取文本（Apache PDFBox）
     */
    private String extractFromPdf(String filePath, String fileName) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            if (document.isEncrypted()) {
                throw new RuntimeException("PDF 文件已加密，无法读取: " + fileName);
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 按阅读顺序排序
            String text = stripper.getText(document);
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("PDF 文件无可提取的文字（可能为扫描件）: " + fileName);
            }
            log.info("[RAG-Upload] PDF 解析成功: " + fileName
                    + ", 页数=" + document.getNumberOfPages()
                    + ", 字数=" + text.length());
            return text;
        }
    }

    /**
     * 从 Word 文件提取文本（Apache POI XWPF）
     */
    private String extractFromDocx(String filePath, String fileName) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            if (text == null || text.trim().isEmpty()) {
                throw new RuntimeException("Word 文件内容为空: " + fileName);
            }
            log.info("[RAG-Upload] Word 解析成功: " + fileName
                    + ", 字数=" + text.length());
            return text;
        }
    }

    /**
     * 检测文本是否包含 GBK 转 UTF-8 后的典型乱码特征
     */
    private boolean containsGarbled(String text) {
        // 统计常见乱码字符（�、空格符、控制字符等）
        int garbledCount = 0;
        for (int i = 0; i < Math.min(text.length(), 200); i++) { // 只检查前200字符
            char c = text.charAt(i);
            if (c == '�' || (c >= 0x0000 && c <= 0x001F && c != '\n' && c != '\r' && c != '\t')) {
                garbledCount++;
            }
        }
        return garbledCount > 5; // 超过5个乱码字符则判定为编码错误
    }

    /**
     * 获取文件扩展名（不含点号）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }

    /**
     * 计算文本的 SHA-256 哈希值
     * 作用：给一段文字算一个"指纹"，相同文字的指纹一定相同
     */
    private String sha256(String text) {
        try {
            // 第一步：拿到一个 SHA-256 的"计算器"
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // 第二步：把文字转成字节数组，喂给计算器
            //         UTF-8 是文字编码方式，保证中文也能正确处理
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));

            // 第三步：把字节数组转成 16 进制字符串
            //         （因为字节是 -128~127 的数字，人看不懂，转成 00~ff 才好存数据库）
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                // 0xff & b 的作用：把 Java 的有符号 byte 转成无符号的 0~255
                String hex = Integer.toHexString(0xff & b);
                // 如果只有一位（比如 0x0f），前面补个 0 变成 "0f"，保持对齐
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();  // 最终得到 64 位长的 16 进制字符串

        } catch (Exception e) {
            // SHA-256 是 JDK 自带的，正常情况不会进这个 catch
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }


}
