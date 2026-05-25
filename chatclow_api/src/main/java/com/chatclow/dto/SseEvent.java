package com.chatclow.dto;

/**
 * 结构化 SSE 事件
 * 前端收到后 JSON.parse，按 type 分发处理
 *
 * type 枚举：
 *   conv_id    - 会话ID，data=String
 *   content    - AI 文字内容，data=String
 *   tool_call  - 正在调用工具，data={name, args}
 *   tool_result - 工具执行结果，data={name, result}
 *   done       - 流结束，data=""
 *   error      - 出错，data=String
 */
public class SseEvent {

    private String type;
    private Object data;

    public SseEvent() {}

    public SseEvent(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    // --- 静态工厂方法，调用更简洁 ---
    public static SseEvent convId(String conversationId) {
        return new SseEvent("conv_id", conversationId);
    }

    public static SseEvent content(String text) {
        return new SseEvent("content", text);
    }

    public static SseEvent toolCall(String name, String args) {
        return new SseEvent("tool_call", new ToolCallData(name, args));
    }

    public static SseEvent toolResult(String name, String result) {
        return new SseEvent("tool_result", new ToolResultData(name, result));
    }

    public static SseEvent done() {
        return new SseEvent("done", "");
    }

    public static SseEvent error(String message) {
        return new SseEvent("error", message);
    }

    // --- getter/setter ---
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    // --- 内嵌数据类 ---
    public static class ToolCallData {
        private String name;
        private String args;
        public ToolCallData(String name, String args) {
            this.name = name;
            this.args = args;
        }
        public String getName() { return name; }
        public String getArgs() { return args; }
    }

    public static class ToolResultData {
        private String name;
        private String result;
        public ToolResultData(String name, String result) {
            this.name = name;
            this.result = result;
        }
        public String getName() { return name; }
        public String getResult() { return result; }
    }
}
