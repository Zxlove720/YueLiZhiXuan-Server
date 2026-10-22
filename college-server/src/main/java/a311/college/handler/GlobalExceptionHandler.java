package a311.college.handler;


import a311.college.constant.user.UserErrorConstant;
import a311.college.exception.BaseException;
import a311.college.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 路径不存在：返回 HTTP 404（此前会落到兜底处理器返回 200 + "未知错误"）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<String>> noResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(404).body(Result.error("请求的资源不存在-404"));
    }

    /**
     * 请求方法不支持：返回 HTTP 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<String>> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405).body(Result.error("请求方法不支持-405"));
    }

    /**
     * 缺少必填参数：返回 HTTP 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<String>> missingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity.status(400)
                .body(Result.error("缺少必填参数：" + exception.getParameterName() + "-400"));
    }

    /**
     * 捕获业务异常
     * @param exception 业务异常
     * @return Result<String>
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException exception) {
        log.info("异常信息：{}", exception.getMessage());
        return Result.error(exception.getMessage());
    }

    /**
     * 处理SQL异常
     * @param exception SQL异常
     * @return Result<Void>
     */
    @ExceptionHandler
    public Result<Void> exceptionHandler(SQLIntegrityConstraintViolationException exception) {
        // 记录SQL约束异常，避免静默失败难以排查
        log.warn("SQL约束异常：{}", exception.getMessage());
        // 获得报错信息
        String message = exception.getMessage();
        // 解决重复添加问题
        if (message.contains("Duplicate entry")) {
            // 按照空格对报错信息进行切片处理，获取到重复添加的值
            String[] split = message.split(" ");
            // 获取重复添加的用户名
            String username = split[2];
            // 拼接报错信息
            String errorMessage = username + UserErrorConstant.ALREADY_EXISTS;
            return Result.error(errorMessage);
        } else if (message.contains("cannot be null")) {
            // 非空约束冲突：提示调用方补全必填字段
            return Result.error("部分必填信息未填写");
        } else {
            // 并非重复添加报错
            // TODO日后可以根据需求添加更多的SQL异常处理
            return Result.error("未知错误");
        }
    }

    /**
     * 兜底捕获未预期异常：记录完整堆栈并返回统一错误结构
     * @param exception 未预期异常
     * @return Result<Void>
     */
    @ExceptionHandler
    public Result<Void> exceptionHandler(Exception exception) {
        log.error("未预期异常：", exception);
        return Result.error("未知错误");
    }

}
