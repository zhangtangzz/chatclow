package com.chatclow.service.impl;

import com.chatclow.service.FunctionExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 工具函数执行器
 *
 * <p>内置工具：get_weather（模拟天气）、get_current_time（当前时间）、calculate（四则运算）。
 * 新增工具只需：1. 写一个 private 方法  2. 在 init() 中注册到 registry。
 * 无需修改 execute() 的分发逻辑。</p>
 */
@Service
public class FunctionExecutorImpl implements FunctionExecutor {

    private static final Logger log = LoggerFactory.getLogger(FunctionExecutorImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, Function<Map<String, Object>, Map<String, Object>>> registry = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        registry.put("get_weather", this::getWeather);
        registry.put("get_current_time", this::getCurrentTime);
        registry.put("calculate", this::calculate);
    }

    @Override
    public String execute(String functionName, Map<String, Object> args) {
        Function<Map<String, Object>, Map<String, Object>> fn = registry.get(functionName);
        if (fn == null) {
            return toJson(Map.of("error", "未知工具: " + functionName));
        }
        return toJson(fn.apply(args));
    }

    // ──── 工具方法 ────

    private Map<String, Object> getWeather(Map<String, Object> args) {
        String city = (String) args.get("city");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("city", city);
        switch (city != null ? city : "") {
            case "北京": result.put("weather", "晴");  result.put("temperature", 25); break;
            case "上海": result.put("weather", "多云"); result.put("temperature", 28); break;
            case "广州": result.put("weather", "阴");  result.put("temperature", 32); break;
            case "深圳": result.put("weather", "小雨"); result.put("temperature", 29); break;
            default:     result.put("weather", "晴");  result.put("temperature", 22); break;
        }
        result.put("unit", "摄氏度");
        return result;
    }

    private Map<String, Object> getCurrentTime(Map<String, Object> args) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("datetime", now.format(formatter));
        result.put("date", now.toLocalDate().toString());
        result.put("time", now.toLocalTime().toString());
        return result;
    }

    private Map<String, Object> calculate(Map<String, Object> args) {
        String expression = (String) args.get("expression");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expression", expression);
        try {
            result.put("result", evaluate(expression));
        } catch (Exception e) {
            result.put("error", "计算错误: " + e.getMessage());
        }
        return result;
    }

    // ──── 简单算术表达式求值（仅支持 +-* /）────

    private double evaluate(String expr) {
        expr = expr.replaceAll("\\s+", "");
        java.util.regex.Pattern mdPattern =
                java.util.regex.Pattern.compile("(\\d+\\.?\\d*)([*/])(\\d+\\.?\\d*)");
        java.util.regex.Matcher mdMatcher = mdPattern.matcher(expr);
        if (mdMatcher.find()) {
            double left = Double.parseDouble(mdMatcher.group(1));
            char op = mdMatcher.group(2).charAt(0);
            double right = Double.parseDouble(mdMatcher.group(3));
            double subResult = (op == '*') ? left * right : left / right;
            return evaluate(mdMatcher.replaceFirst(String.valueOf(subResult)));
        }
        java.util.regex.Pattern asPattern =
                java.util.regex.Pattern.compile("(\\d+\\.?\\d*)([+-])(\\d+\\.?\\d*)");
        java.util.regex.Matcher asMatcher = asPattern.matcher(expr);
        if (asMatcher.find()) {
            double left = Double.parseDouble(asMatcher.group(1));
            char op = asMatcher.group(2).charAt(0);
            double right = Double.parseDouble(asMatcher.group(3));
            double subResult = (op == '+') ? left + right : left - right;
            return evaluate(asMatcher.replaceFirst(String.valueOf(subResult)));
        }
        return Double.parseDouble(expr);
    }

    private static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            return "{}";
        }
    }
}
