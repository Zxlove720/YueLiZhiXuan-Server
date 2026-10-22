package a311.college.controller.common;

import a311.college.ailiyun.AliOssUtil;
import a311.college.constant.API.APIConstant;
import a311.college.exception.BaseException;
import a311.college.result.Result;
import cn.hutool.core.lang.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * 通用控制器
 */
@Slf4j
@RestController
@RequestMapping("/upload")
@Tag(name = APIConstant.COMMON_SERVICE)
public class CommonController {

    /** 允许上传的图片扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** 单文件大小上限：5MB */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final AliOssUtil aliOssUtil;

    @Autowired
    public CommonController(AliOssUtil aliOssUtil) {
        this.aliOssUtil = aliOssUtil;
    }

    /**
     * 文件上传（当前仅用于头像等图片）
     * <p>
     * 服务端校验扩展名与大小：接口在游客白名单内，不校验会被匿名滥用。
     * </p>
     *
     * @param file 要上传的文件
     * @return filePath 文件路径
     */
    @PostMapping
    @Operation(summary = "文件上传")
    public Result<String> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BaseException("图片不能超过 5MB");
        }
        String originalFileName = file.getOriginalFilename();
        String extension = extractExtension(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BaseException("仅支持 jpg / png / webp / gif 图片");
        }
        try {
            // 构建新文件名（UUID 防覆盖，保留原始扩展名）
            String objectName = UUID.randomUUID() + "." + extension;
            String filePath = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", originalFileName, e);
            return Result.error("文件上传失败");
        }
    }

    /**
     * 提取小写扩展名；无扩展名或扩展名不合法时返回空串（调用方判定拒绝）
     */
    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        // 扩展名只允许字母数字，防止路径拼接类注入
        return ext.matches("[a-z0-9]+") ? ext : "";
    }

}
