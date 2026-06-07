package com.chatclow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chatclow.entity.UserChunk;
import com.chatclow.entity.UserDocument;
import com.chatclow.mapper.UserChunkMapper;
import com.chatclow.mapper.UserDocumentMapper;
import com.chatclow.service.EmbeddingService;
import com.chatclow.service.UserDocumentService;
import com.chatclow.util.ChunkSplitUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
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
import java.util.stream.Collectors;

@Service
public class UserDocumentServiceImpl implements UserDocumentService {

    private static final Logger log = LoggerFactory.getLogger(UserDocumentServiceImpl.class);

    @Autowired
    private UserDocumentMapper userDocumentMapper;

    @Autowired
    private UserChunkMapper userChunkMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    @Qualifier("sseExecutor")
    private Executor sseExecutor;

    @Override
    @Transactional
    public Long upload(MultipartFile file, Long userId) {
        try {
            // 1. 保存文件到 uploads/user/{userId}/
            String projectDir = System.getProperty("user.dir");
            String uploadDir = projectDir + "/uploads/user/" + userId + "/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + fileName;
            Files.copy(file.getInputStream(), Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);

            // 2. 提取文本
            String content = extractText(filePath, file.getOriginalFilename());

            // 3. 创建文档记录
            UserDocument doc = new UserDocument();
            doc.setUserId(userId);
            doc.setFileName(file.getOriginalFilename());
            doc.setFileType(getFileExtension(file.getOriginalFilename()));
            doc.setFileSize(file.getSize());
            doc.setFilePath(filePath);
            doc.setContent(content);
            doc.setStatus(1);
            doc.setChunkCount(0);
            userDocumentMapper.insert(doc);

            Long docId = doc.getId();

            // 4. 事务提交后再异步处理切片+向量化
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sseExecutor.execute(() -> processUserDoc(docId, userId));
                }
            });

            return docId;

        } catch (Exception e) {
            throw new RuntimeException("上传失败：" + e.getMessage(), e);
        }
    }

    /**
     * 异步处理：切片 → 去重 → 向量化 → 入库
     */
    private void processUserDoc(Long docId, Long userId) {
        try {
            // 更新状态为"处理中"
            UserDocument doc = userDocumentMapper.selectById(docId);
            doc.setStatus(2);
            userDocumentMapper.updateById(doc);

            String content = doc.getContent();
            if (content == null || content.isEmpty()) {
                throw new RuntimeException("文档内容为空");
            }

            // 切片
            List<String> textChunks = ChunkSplitUtil.split(content);
            if (textChunks.isEmpty()) {
                throw new RuntimeException("切片结果为空");
            }

            // 去重（查 user_chunk 表，按 user_id + content_hash）
            List<String> uniqueChunks = new ArrayList<>();
            List<String> uniqueHashes = new ArrayList<>();
            int skippedCount = 0;

            for (String chunkContent : textChunks) {
                String hash = sha256(chunkContent);

                LambdaQueryWrapper<UserChunk> dupCheck = new LambdaQueryWrapper<>();
                dupCheck.eq(UserChunk::getUserId, userId);
                dupCheck.eq(UserChunk::getContentHash, hash);
                if (userChunkMapper.selectCount(dupCheck) > 0) {
                    skippedCount++;
                    continue;
                }

                uniqueChunks.add(chunkContent);
                uniqueHashes.add(hash);
            }

            if (!uniqueChunks.isEmpty()) {
                // 批量向量化
                List<float[]> embeddings = embeddingService.batchEmbed(uniqueChunks);

                // 组装并入库
                List<UserChunk> chunks = new ArrayList<>();
                for (int i = 0; i < uniqueChunks.size(); i++) {
                    UserChunk chunk = new UserChunk();
                    chunk.setUserId(userId);
                    chunk.setDocId(docId);
                    chunk.setChunkIndex(i);
                    chunk.setContent(uniqueChunks.get(i));
                    chunk.setTokenCount(uniqueChunks.get(i).length());
                    chunk.setContentHash(uniqueHashes.get(i));
                    chunk.setVectorData(floatArrayToString(embeddings.get(i)));
                    chunks.add(chunk);
                }

                for (UserChunk chunk : chunks) {
                    userChunkMapper.insert(chunk);
                }

                doc.setChunkCount(chunks.size());
            }

            doc.setStatus(3); // 完成
            userDocumentMapper.updateById(doc);

            log.info("[UserDoc] 处理完成！文档 {}，入库 {} 条切片（跳过重复 {} 条）", doc.getFileName(), doc.getChunkCount(), skippedCount);

        } catch (Exception e) {
            UserDocument doc = userDocumentMapper.selectById(docId);
            if (doc != null) {
                doc.setStatus(4);
                doc.setErrorMsg(e.getMessage());
                userDocumentMapper.updateById(doc);
            }
            log.warn("[UserDoc] 处理失败: {}", e.getMessage());
        }
    }

    @Override
    public List<UserDocument> listByUserId(Long userId) {
        LambdaQueryWrapper<UserDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDocument::getUserId, userId);
        wrapper.orderByDesc(UserDocument::getCreatedDt);
        return userDocumentMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        UserDocument doc = userDocumentMapper.selectById(id);
        if (doc == null || !doc.getUserId().equals(userId)) {
            throw new RuntimeException("文档不存在或无权删除");
        }

        // 删除切片
        LambdaQueryWrapper<UserChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserChunk::getDocId, id);
        userChunkMapper.delete(wrapper);

        // 删除文档记录
        userDocumentMapper.deleteById(id);

        // 删除文件
        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()));
        } catch (IOException ignored) {}
    }

    @Override
    public List<String> search(Long userId, String query, int topK) {
        // 1. 向量化问题
        float[] questionVector = embeddingService.embed(query);

        // 2. 查出该用户所有有向量的切片
        LambdaQueryWrapper<UserChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserChunk::getUserId, userId);
        wrapper.isNotNull(UserChunk::getVectorData);
        List<UserChunk> allChunks = userChunkMapper.selectList(wrapper);

        if (allChunks.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 余弦相似度计算
        List<ScoredChunk> scored = new ArrayList<>();
        for (UserChunk chunk : allChunks) {
            float[] chunkVector = parseVector(chunk.getVectorData());
            if (chunkVector == null || chunkVector.length != questionVector.length) continue;

            float similarity = cosineSimilarity(questionVector, chunkVector);
            if (similarity >= 0.5f) { // 默认阈值
                scored.add(new ScoredChunk(chunk, similarity));
            }
        }

        // 4. 取 topK
        return scored.stream()
                .sorted((a, b) -> Float.compare(b.score, a.score))
                .limit(topK)
                .map(sc -> sc.chunk.getContent())
                .collect(Collectors.toList());
    }

    // ===== 文本提取（复用 RagDocumentServiceImpl 同样的逻辑） =====

    private String extractText(String filePath, String fileName) throws IOException {
        String ext = getFileExtension(fileName).toLowerCase();
        if ("txt".equals(ext) || "md".equals(ext)) {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (containsGarbled(text)) {
                text = new String(bytes, java.nio.charset.Charset.forName("GBK"));
            }
            return text;
        } else if ("pdf".equals(ext)) {
            try (PDDocument document = PDDocument.load(new File(filePath))) {
                if (document.isEncrypted()) {
                    throw new RuntimeException("PDF 文件已加密: " + fileName);
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);
                return text != null ? text : "";
            }
        } else if ("docx".equals(ext)) {
            try (FileInputStream fis = new FileInputStream(filePath);
                 XWPFDocument document = new XWPFDocument(fis);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                String text = extractor.getText();
                return text != null ? text : "";
            }
        } else {
            throw new RuntimeException("暂不支持的文件类型: ." + ext);
        }
    }

    private boolean containsGarbled(String text) {
        int garbledCount = 0;
        for (int i = 0; i < Math.min(text.length(), 200); i++) {
            char c = text.charAt(i);
            if (c == '�' || (c >= 0x0000 && c <= 0x001F && c != '\n' && c != '\r' && c != '\t')) {
                garbledCount++;
            }
        }
        return garbledCount > 5;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }

    // ===== 向量工具 =====

    private String floatArrayToString(float[] vector) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    private float[] parseVector(String vectorData) {
        if (vectorData == null || vectorData.isEmpty()) return null;
        String[] parts = vectorData.split(",");
        float[] vector = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vector[i] = Float.parseFloat(parts[i].trim());
        }
        return vector;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0f, normA = 0f, normB = 0f;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0f || normB == 0f) return 0f;
        return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 计算失败", e);
        }
    }

    private static class ScoredChunk {
        UserChunk chunk;
        float score;
        ScoredChunk(UserChunk chunk, float score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
