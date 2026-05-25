package com.chatclow.common;

import lombok.Data;

/**
 * 统一 API 响应封装类
 * 所有接口返回格式统一为：{ code, msg, data }
 */

@Data
public class R<T> {

    /** 状态码：200=成功，其他=失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 返回数据 */
    private T data;

    // ===== 私有构造，强制用静态方法创建 =====

    private R() {}

    private R(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ===== 成功响应 =====

    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(200, msg, data);
    }

    // ===== 失败响应 =====

    public static <T> R<T> error() {
        return new R<>(500, "操作失败", null);
    }

    public static <T> R<T> error(String msg) {
        return new R<>(500, msg, null);
    }

    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg, null);
    }
}
