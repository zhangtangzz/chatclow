package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.RagChunk;
import com.chatclow.entity.RagDocument;
import com.chatclow.mapper.RagDocumentMapper;
import com.chatclow.service.EmbeddingService;
import com.chatclow.service.RagChunkService;
import com.chatclow.service.RagDocumentService;
import com.chatclow.util.ChunkSplitUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagDocumentServiceImpl implements RagDocumentService {

    @Autowired
    private RagDocumentMapper ragDocumentMapper;

    @Autowired
    private RagChunkService ragChunkService;

    @Autowired
    private EmbeddingService embeddingService;

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
        return ragDocumentMapper.deleteById(id) > 0;
    }

    /**
     * 文档处理全链路：切片 → 向量化 → 存储
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

        System.out.println("[RAG] 文档切成 " + textChunks.size() + " 个片段");

        // 3. 批量向量化（一次 API 调用把所有切片都转成向量）
        List<float[]> embeddings = embeddingService.batchEmbed(textChunks);

        // 4. 组装 Chunk 对象并存入数据库
        List<RagChunk> chunks = new ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            RagChunk chunk = new RagChunk();
            chunk.setKbId(document.getKbId());
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(textChunks.get(i));
            chunk.setTokenCount(textChunks.get(i).length());

            // 把 float[] 转成字符串存储（逗号分隔）
            String vectorStr = arrayToString(embeddings.get(i));
            chunk.setVectorData(vectorStr);

            chunks.add(chunk);
        }

        // 5. 批量插入 chunk 表
        ragChunkService.batchInsert(chunks);

        // 6. 更新文档的切片数量和状态
        document.setChunkCount(chunks.size());
        document.setStatus(3);  // 处理完成（3=完成，1=解析中）
        ragDocumentMapper.updateById(document);

        System.out.println("[RAG] 文档处理完成！共生成 " + chunks.size() + " 条切片记录");
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
            new Thread(() -> asyncProcessDocument(documentId)).start();

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
            System.err.println("[RAG-Upload] 处理失败: " + e.getMessage());
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
            System.out.println("[RAG-Upload] 文件使用 GBK 编码读取: " + fileName);
        } else {
            System.out.println("[RAG-Upload] 文件使用 UTF-8 编码读取: " + fileName);
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
            System.out.println("[RAG-Upload] PDF 解析成功: " + fileName
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
            System.out.println("[RAG-Upload] Word 解析成功: " + fileName
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

}
