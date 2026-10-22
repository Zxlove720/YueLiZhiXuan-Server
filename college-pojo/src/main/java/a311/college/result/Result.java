package a311.college.result;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一返回结果Result
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "统一返回结果")
public class Result<T> implements Serializable {

    @Schema(description = "请求状态(HTTP请求通用状态码)")
    private String status;

    @Schema(description = "编码：1.成功；0和其他数字都是失败")
    private Integer code;

    @Schema(description = "返回信息")
    private String msg;

    @Schema(description = "返回数据")
    private T responseData;

    /**
     * 统一的不带数据的成功响应
     * @return Result响应结果
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "successful";
        return result;
    }

    /**
     * 统一的带数据的成功响应
     * @param data 响应数据
     * @return Result响应结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "successful";
        result.responseData = data;
        return result;
    }

    /**
     * 统一的不带数据的失败响应
     * <p>
     * 兼容历史错误常量携带的 "-数字" 后缀（如 "密码错误-500"）：
     * 下发的 msg 统一去除该后缀，前端无需再做清洗。
     * </p>
     * @param msg 错误信息
     * @return Result响应结果
     */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.status = extractStatus(msg);
        result.code = 0;
        result.msg = cleanMessage(msg);
        return result;
    }

    /**
     * 统一的带数据的失败响应
     * @param msg 错误信息
     * @param data 响应数据
     * @return Result响应结果
     */
    public static <T> Result<T> error(String msg, T data) {
        Result<T> result = new Result<>();
        result.status = extractStatus(msg);
        result.code = 0;
        result.msg = cleanMessage(msg);
        result.responseData = data;
        return result;
    }

    /** 去掉错误文案尾部的 "-数字" 后缀 */
    private static String cleanMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "未知错误";
        }
        return msg.replaceAll("-\\s*\\d+\\s*$", "").trim();
    }

    /**
     * 从错误信息中提取状态码
     * <p>
     * 兼容形如 "错误描述-500" 的消息格式；消息中不携带状态码时默认返回 "500"，
     * 避免 split("-")[1] 在无分隔符时抛出数组越界异常。
     * </p>
     */
    private static String extractStatus(String msg) {
        if (msg == null || msg.isBlank()) {
            return "500";
        }
        String[] parts = msg.split("-");
        return parts.length > 1 ? parts[parts.length - 1] : "500";
    }
}
