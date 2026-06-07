package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.UserDocument;
import com.chatclow.service.UserDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/user-doc")
public class UserDocumentController {

    @Autowired
    private UserDocumentService userDocumentService;

    /**
     * 上传文档
     * POST /api/user-doc/upload
     */
    @PostMapping("/upload")
    public R<Object> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long docId = userDocumentService.upload(file, userId);
        return R.ok(docId);
    }

    /**
     * 我的文档列表
     * GET /api/user-doc/list
     */
    @GetMapping("/list")
    public R<List<UserDocument>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserDocument> list = userDocumentService.listByUserId(userId);
        return R.ok(list);
    }

    /**
     * 删除文档
     * DELETE /api/user-doc/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userDocumentService.delete(id, userId);
        return R.ok("删除成功");
    }

    /**
     * 检索个人 RAG
     * GET /api/user-doc/search?query=xxx&topK=3
     */
    @GetMapping("/search")
    public R<List<String>> search(@RequestParam String query,
                                  @RequestParam(defaultValue = "3") int topK,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> results = userDocumentService.search(userId, query, topK);
        return R.ok(results);
    }

    /**
     * 获取文档数量（用于前端显示红点/标识）
     * GET /api/user-doc/count
     */
    @GetMapping("/count")
    public R<Integer> count(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserDocument> list = userDocumentService.listByUserId(userId);
        return R.ok(list.size());
    }
}
