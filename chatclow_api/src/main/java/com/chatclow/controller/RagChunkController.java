package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.RagChunk;
import com.chatclow.service.RagChunkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chunk")
public class RagChunkController {

    @Autowired
    private RagChunkService ragChunkService;

    /**
     * 按文档ID查切片列表
     * GET /api/chunk/list?documentId=1
     */
    @GetMapping("/list")
    public R<List<RagChunk>> listByDocumentId(@RequestParam Long documentId) {
        List<RagChunk> list = ragChunkService.listByDocumentId(documentId);
        return R.ok(list);
    }

    /**
     * 按知识库ID查所有切片
     * GET /api/chunk/listByKb?kbId=1
     */
    @GetMapping("/listByKb")
    public R<List<RagChunk>> listByKbId(@RequestParam Long kbId) {
        List<RagChunk> list = ragChunkService.listByKbId(kbId);
        return R.ok(list);
    }

    /**
     * 按ID查询单条
     * GET /api/chunk/{id}
     */
    @GetMapping("/{id}")
    public R<RagChunk> getById(@PathVariable Long id) {
        RagChunk chunk = ragChunkService.getById(id);
        return R.ok(chunk);
    }
}
