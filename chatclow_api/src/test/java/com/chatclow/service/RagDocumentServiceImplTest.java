package com.chatclow.service;

import com.chatclow.entity.RagDocument;
import com.chatclow.entity.RagKnowledgeBase;
import com.chatclow.mapper.RagDocumentMapper;
import com.chatclow.mapper.RagKnowledgeBaseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("RagDocumentServiceImpl 集成测试")
class RagDocumentServiceImplTest {

    @Autowired private RagDocumentService service;
    @Autowired private RagDocumentMapper mapper;
    @Autowired private RagKnowledgeBaseMapper kbMapper;

    private Long kbId;

    @BeforeEach
    void setUp() {
        RagKnowledgeBase kb = new RagKnowledgeBase();
        kb.setName("测试知识库");
        kb.setStoreInstanceId(1L);
        kb.setStatus(1);
        kbMapper.insert(kb);
        kbId = kb.getId();
    }

    @Test
    @DisplayName("新增文档记录")
    void shouldAddDocument() {
        RagDocument doc = new RagDocument();
        doc.setKbId(kbId);
        doc.setName("test.txt");
        doc.setFileType("txt");
        doc.setContent("测试内容");
        doc.setStatus(0);

        service.add(doc);

        assertNotNull(doc.getId());
        RagDocument saved = mapper.selectById(doc.getId());
        assertEquals("test.txt", saved.getName());
        assertEquals("测试内容", saved.getContent());
    }

    @Test
    @DisplayName("按知识库ID查文档列表")
    void shouldListByKbId() {
        RagDocument doc1 = new RagDocument();
        doc1.setKbId(kbId);
        doc1.setName("doc1.txt");
        doc1.setFileType("txt");
        doc1.setContent("内容1");
        service.add(doc1);

        RagDocument doc2 = new RagDocument();
        doc2.setKbId(kbId);
        doc2.setName("doc2.txt");
        doc2.setFileType("txt");
        doc2.setContent("内容2");
        service.add(doc2);

        List<RagDocument> docs = service.listByKbId(kbId);
        assertEquals(2, docs.size());
    }

    @Test
    @DisplayName("按ID查询文档")
    void shouldGetById() {
        RagDocument doc = new RagDocument();
        doc.setKbId(kbId);
        doc.setName("test.md");
        doc.setFileType("md");
        doc.setContent("markdown内容");
        service.add(doc);

        RagDocument found = service.getById(doc.getId());
        assertNotNull(found);
        assertEquals("test.md", found.getName());
    }

    @Test
    @DisplayName("更新文档信息")
    void shouldUpdateDocument() {
        RagDocument doc = new RagDocument();
        doc.setKbId(kbId);
        doc.setName("原始名.txt");
        doc.setContent("原始内容");
        service.add(doc);

        doc.setName("修改后.txt");
        doc.setContent("修改后内容");
        service.update(doc);

        RagDocument updated = mapper.selectById(doc.getId());
        assertEquals("修改后.txt", updated.getName());
    }

    @Test
    @DisplayName("删除文档")
    void shouldDeleteDocument() {
        RagDocument doc = new RagDocument();
        doc.setKbId(kbId);
        doc.setName("待删除.txt");
        doc.setContent("内容");
        service.add(doc);

        service.deleteById(doc.getId());

        assertNull(mapper.selectById(doc.getId()));
    }
}
