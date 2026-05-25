package com.chatclow.exception;

import com.chatclow.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * 作用：所有 Controller 抛出的异常，都会被这里拦截
 *       统一返回 {code, msg, data} 格式
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 1. 参数校验失败异常（@Valid 校验不通过时触发）
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e){
        String message = e.getBindingResult().getFieldError()!=null
                ?e.getBindingResult().getFieldError().getDefaultMessage()
                :"参数校验失败";
        log.warn("参数校验失败:{}",message);
        return R.error(400,message);
    }

    /**
     * 2. 参数绑定异常（类型转换错误等）
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBind(BindException e) {
        String message = e.getFieldError() != null
                ? e.getFieldError().getDefaultMessage()
                : "参数格式错误";
        log.warn("参数绑定错误: {}", message);
        return R.error(400, message);
    }

    /**
     * 3. 空指针异常（代码有 bug 时触发）
     */
    @ExceptionHandler(NullPointerException.class)
    public R<Void> handleNullPoint(NullPointerException e) {
        log.error("空指针异常: ", e);
        return R.error("系统繁忙，请稍后重试");
    }

    /**
     * 4. 非法参数异常（传了不该传的东西）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArg(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return R.error(400, e.getMessage());
    }

    /**
     * 5. 兜底：捕获所有其他未处理的异常
     * 这是最后一道防线！任何没被上面接住的异常都会到这里
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return R.error("服务器内部错误");
    }


}

