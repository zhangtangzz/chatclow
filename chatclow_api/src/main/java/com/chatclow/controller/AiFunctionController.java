package com.chatclow.controller;

import com.chatclow.common.R;
import com.chatclow.entity.AiFunction;
import com.chatclow.service.AiFunctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/function")
public class AiFunctionController {

    @Autowired
    private AiFunctionService aiFunctionService;

    /**
     * 新增工具
     * POST /api/function/add
     */
    @PostMapping("/add")
    public R<Object> add(@RequestBody AiFunction aiFunction) {
        aiFunctionService.add(aiFunction);
        return R.ok(aiFunction.getId());
    }

    /**
     * 修改工具
     * PUT /api/function/update
     */
    @PutMapping("/update")
    public R<String> update(@RequestBody AiFunction aiFunction) {
        aiFunctionService.update(aiFunction);
        return R.ok("修改成功");
    }

    /**
     * 删除工具
     * DELETE /api/function/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public R<String> delete(@PathVariable Long id) {
        aiFunctionService.delete(id);
        return R.ok("删除成功");
    }

    /**
     * 工具详情
     * GET /api/function/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public R<AiFunction> detail(@PathVariable Long id) {
        AiFunction func = aiFunctionService.getById(id);
        return R.ok(func);
    }

    /**
     * 按智能体查询工具列表 ⭐ 最常用！
     * GET /api/function/list?agentId=1
     */
    @GetMapping("/list")
    public R<List<AiFunction>> list(@RequestParam Long agentId) {
        List<AiFunction> list = aiFunctionService.listByAgentId(agentId);
        return R.ok(list);
    }
}
