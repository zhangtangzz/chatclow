package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.RagKnowledgeBase;
import com.chatclow.service.RagKnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kb")
public class RagKnowledgeBaseController {

    @Autowired
    private RagKnowledgeBaseService ragKnowledgeBaseService;

    /**
     * 新建知识库
     * POST /api/kb/add
     */
    @PostMapping("/add")
    public R<String> add(@RequestBody RagKnowledgeBase kb) {
        ragKnowledgeBaseService.add(kb);
        return R.ok("知识库创建成功");
    }

    /**
     * 查询所有知识库
     * GET /api/kb/list
     */
    @GetMapping("/list")
    public R<List<RagKnowledgeBase>> list() {
        List<RagKnowledgeBase> list = ragKnowledgeBaseService.list();
        return R.ok(list);
    }

    /**
     * 按ID查询
     * GET /api/kb/{id}
     */
    @GetMapping("/{id}")
    public R<RagKnowledgeBase> getById(@PathVariable Long id) {
        RagKnowledgeBase kb = ragKnowledgeBaseService.getById(id);
        return R.ok(kb);
    }

    /**
     * 更新知识库
     * PUT /api/kb/update
     */
    @PutMapping("/update")
    public R<String> update(@RequestBody RagKnowledgeBase kb) {
        ragKnowledgeBaseService.update(kb);
        return R.ok("更新成功");
    }

    /**
     * 删除知识库
     * DELETE /api/kb/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        ragKnowledgeBaseService.deleteById(id);
        return R.ok("删除成功");
    }

    /**
     * 切换启用/禁用状态
     * PUT /api/kb/status/{id}
     */
    @PutMapping("/status/{id}")
    public R<Integer> toggleStatus(@PathVariable Long id) {
        Integer newStatus = ragKnowledgeBaseService.toggleStatus(id);
        return R.ok(newStatus);
    }
}
