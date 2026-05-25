package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.RagDocument;
import com.chatclow.service.RagDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/document")
public class RagDocumentController {

    @Autowired
    private RagDocumentService ragDocumentService;

    /**
     * 新增文档记录
     * POST /api/document/add
     */
    @PostMapping("/add")
    public R<Object> add(@RequestBody RagDocument document) {
        ragDocumentService.add(document);

        return R.ok(document.getId());  // MyBatis-Plus insert后会回填自增ID到对象里
    }

    /**
     * 按知识库ID查文档列表
     * GET /api/document/list?kbId=1
     */
    @GetMapping("/list")
    public R<List<RagDocument>> listByKbId(@RequestParam Long kbId) {
        List<RagDocument> list = ragDocumentService.listByKbId(kbId);
        return R.ok(list);
    }

    /**
     * 按ID查询
     * GET /api/document/{id}
     */
    @GetMapping("/{id}")
    public R<RagDocument> getById(@PathVariable Long id) {
        RagDocument doc = ragDocumentService.getById(id);
        return R.ok(doc);
    }

    /**
     * 更新文档信息
     * PUT /api/document/update
     */
    @PutMapping("/update")
    public R<String> update(@RequestBody RagDocument document) {
        ragDocumentService.update(document);
        return R.ok("更新成功");
    }

    /**
     * 删除文档
     * DELETE /api/document/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        ragDocumentService.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 处理文档：执行 RAG 全链路（切片+向量化+存储）
     * POST /api/document/process/{id}
     */
    @PostMapping("/process/{id}")
    public R<Map<String, Object>> process(@PathVariable Long id) {
        ragDocumentService.processDocument(id);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "文档处理成功");
        result.put("documentId", id);
        return R.ok(result);
    }

    /**
     * 上传文件并自动触发处理（异步）
     * POST /api/document/upload
     * 参数：file（文件）+ kbId（知识库ID）
     */
    @PostMapping("/upload")
    public R<Object> upload(@RequestPart("file") MultipartFile file,
                            @RequestParam("kbId") Long kbId) {
        Long documentId = ragDocumentService.uploadAndProcess(file, kbId);
        return R.ok(documentId);
    }

}
