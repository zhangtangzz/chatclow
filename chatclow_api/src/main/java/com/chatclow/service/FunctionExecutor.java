package com.chatclow.service;

import java.util.Map;

/**
 * 工具函数执行器 接口
 *
 * AI 返回 tool_calls 时，由这个执行器负责调用具体的工具方法
 */
public interface FunctionExecutor {

    /**
     * 执行指定工具
     * @param functionName 工具名称（如 get_weather）
     * @param args         AI 传过来的参数（如 {"city": "北京"}）
     * @return             工具执行结果字符串（会喂回给 AI）
     */
    String execute(String functionName, Map<String, Object> args);
}
