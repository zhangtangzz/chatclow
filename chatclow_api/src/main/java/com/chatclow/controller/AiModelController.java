package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.AiModel;
import com.chatclow.service.AiModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI模型配置控制器
 */

@RestController
@RequestMapping("/api/model")
public class AiModelController {

    @Autowired
    private AiModelService aiModelService;

    /**
     * 新增模型
     * POST /api/model/add
     */
    @PostMapping("/add")
    public R<Void> add(@RequestBody @Valid AiModel aiModel) {
        boolean success = aiModelService.add(aiModel);
        if (success) {
            return R.ok("模型添加成功!", null);
        }
        return R.error("添加失败");
    }

    /**
     * 查询所有启用的模型
     * GET /api/model/list
     */
    @GetMapping("/list")
    public R<List<AiModel>> list() {
        List<AiModel> list = aiModelService.listEnabled();
        return R.ok("查询成功", list);
    }

    /**
     * 管理员查询所有模型（含禁用）
     * GET /api/model/admin/list
     */
    @GetMapping("/admin/list")
    public R<List<AiModel>> adminList() {
        List<AiModel> list = aiModelService.listAll();
        return R.ok("查询成功", list);
    }

    /**
     * 根据ID查询
     * GET /api/model/{id}
     */
    @GetMapping("/{id}")
    public R<AiModel> getById(@PathVariable Long id) {
        AiModel model = aiModelService.getById(id);
        if (model != null) {
            return R.ok("查询成功", model);
        }
        return R.error("模型不存在");
    }

    /**
     * 更新模型
     * PUT /api/model/update
     */
    @PutMapping("/update")
    public R<Void> update(@RequestBody @Valid AiModel aiModel) {
        boolean success = aiModelService.update(aiModel);
        if (success) {
            return R.ok("更新成功!", null);
        }
        return R.error("更新失败");
    }

    /**
     * 删除模型
     * DELETE /api/model/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        boolean success = aiModelService.deleteById(id);
        if (success) {
            return R.ok("删除成功!", null);
        }
        return R.error("删除失败");
    }

    /**
     * 切换启用/禁用状态
     * PUT /api/model/status/{id}
     */
    @PutMapping("/status/{id}")
    public R<Map<String, Object>> toggleStatus(@PathVariable Long id) {
        Integer newStatus = aiModelService.toggleStatus(id);
        Map<String, Object> data = new HashMap<>();
        data.put("newStatus", newStatus);
        data.put("statusText", newStatus == 1 ? "已启用" : "已禁用");
        return R.ok("状态切换成功", data);
    }
}
