package com.chatclow.service.impl;

import com.chatclow.service.FunctionExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 工具函数执行器 实现类
 *
 * 内置3个示例工具：
 *   1. get_weather    — 查天气（模拟数据）
 *   2. get_current_time — 获取当前时间
 *   3. calculate      — 数学计算器
 */
@Service
public class FunctionExecutorImpl implements FunctionExecutor {

    @Override
    public String execute(String functionName, Map<String, Object> args) {
        // 根据工具名路由到对应的处理方法
        switch (functionName) {
            case "get_weather":
                return getWeather(args);
            case "get_current_time":
                return getCurrentTime(args);
            case "calculate":
                return calculate(args);
            default:
                return "{\"error\": \"未知工具: " + functionName + "\"}";
        }
    }

    // ==================== 具体工具方法 ====================

    /**
     * 工具1: 查询天气（模拟数据）
     * 参数: city — 城市名称
     */
    private String getWeather(Map<String, Object> args) {
        String city = (String) args.get("city");

        // 模拟天气数据（实际项目中对接真实天气API）
        String weather;
        int temperature;
        switch (city) {
            case "北京":
                weather = "晴";
                temperature = 25;
                break;
            case "上海":
                weather = "多云";
                temperature = 28;
                break;
            case "广州":
                weather = "阴";
                temperature = 32;
                break;
            case "深圳":
                weather = "小雨";
                temperature = 29;
                break;
            default:
                weather = "晴";
                temperature = 22;
                break;
        }

        // 返回JSON格式的结果（AI 能读懂 JSON）
        return "{"
                + "\"city\": \"" + city + "\", "
                + "\"weather\": \"" + weather + "\", "
                + "\"temperature\": " + temperature + ", "
                + "\"unit\": \"摄氏度\""
                + "}";
    }

    /**
     * 工具2: 获取当前日期和时间
     * 参数: 无（args 为空）
     */
    private String getCurrentTime(Map<String, Object> args) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "{"
                + "\"datetime\": \"" + now.format(formatter) + "\", "
                + "\"date\": \"" + now.toLocalDate().toString() + "\", "
                + "\"time\": \"" + now.toLocalTime().toString() + "\""
                + "}";
    }

    /**
     * 工具3: 数学计算器（简单四则运算）
     * parameter: expression — 算术表达式（如 "123+456"）
     */
    private String calculate(Map<String, Object> args) {
        String expression = (String) args.get("expression");
        try {
            // 注意：生产环境不要用这种方式！这里只是示例
            // 安全做法是用脚本引擎或自己解析表达式
            double result = evaluate(expression);
            return "{"
                    + "\"expression\": \"" + expression + "\", "
                    + "\"result\": " + result
                    + "}";
        } catch (Exception e) {
            return "{\"error\": \"计算错误: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 简单算术表达式求值（仅支持 +-*/
    private double evaluate(String expr) {
        // 去掉所有空格
        expr = expr.replaceAll("\\s+", "");

        // 先算乘除
        if (expr.contains("*") || expr.contains("/")) {
            // 用正则匹配 a*b 或 a/b 的模式
            java.util.regex.Pattern pattern =
                    java.util.regex.Pattern.compile("(\\d+\\.?\\d*)([*/])(\\d+\\.?\\d*)");
            java.util.regex.Matcher matcher = pattern.matcher(expr);
            if (matcher.find()) {
                double left = Double.parseDouble(matcher.group(1));
                double op = matcher.group(2).charAt(0);
                double right = Double.parseDouble(matcher.group(3));
                double subResult = (op == '*') ? left * right : left / right;
                String replaced = matcher.replaceFirst(String.valueOf(subResult));
                return evaluate(replaced); // 递归继续算
            }
        }

        // 再算加减
        if (expr.contains("+") || expr.contains("-")) {
            java.util.regex.Pattern pattern =
                    java.util.regex.Pattern.compile("(\\d+\\.?\\d*)([+-])(\\d+\\.?\\d*)");
            java.util.regex.Matcher matcher = pattern.matcher(expr);
            if (matcher.find()) {
                double left = Double.parseDouble(matcher.group(1));
                char op = matcher.group(2).charAt(0);
                double right = Double.parseDouble(matcher.group(3));
                double subResult = (op == '+') ? left + right : left - right;
                String replaced = matcher.replaceFirst(String.valueOf(subResult));
                return evaluate(replaced); // 递归继续算
            }
        }

        // 纯数字，直接返回
        return Double.parseDouble(expr);
    }
}
