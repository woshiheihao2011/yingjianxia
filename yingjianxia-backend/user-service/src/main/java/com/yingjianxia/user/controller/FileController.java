package com.yingjianxia.user.controller;

import com.yingjianxia.common.core.context.UserContext;
import com.yingjianxia.common.core.exception.BusinessException;
import com.yingjianxia.common.core.result.ApiResponse;
import com.yingjianxia.common.core.result.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 通用文件上传 Controller
 * <p>
 * 提供商品图片、用户头像等通用文件上传能力，复用 user-service 的静态资源映射。
 * 上传目录：{@code ./uploads/file/}，访问路径：{@code /uploads/file/xxx.jpg}
 *
 * @author 硬件侠后端团队
 */
@Slf4j
@Tag(name = "文件上传模块", description = "通用图片/文件上传")
@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    /** 单文件最大 10MB（与 user-service multipart 配置一致） */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    @Value("${yingjianxia.upload.file-dir:./uploads/file}")
    private String fileUploadDir;

    @Value("${yingjianxia.upload.file-url-prefix:/uploads/file}")
    private String fileUrlPrefix;

    @Operation(summary = "上传文件/图片", description = "接收 MultipartFile，返回可访问 URL；需登录")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        // 1. 必须登录
        Long userId = UserContext.requiredUserId();

        // 2. 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件过大，最大 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ResultCode.UPLOAD_FILE_TYPE_NOT_ALLOWED);
        }

        // 3. 生成文件名：{userId}_{timestamp}_{random}.{ext}
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String filename = "u" + userId + "_" + System.currentTimeMillis()
                + "_" + new Random().nextInt(10000) + ext;

        // 4. 保存文件（绝对路径，避免 Tomcat 临时目录解析问题）
        File dir = new File(fileUploadDir).getAbsoluteFile();
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("[文件上传] 创建目录失败: {}", dir.getAbsolutePath());
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "上传目录创建失败");
        }
        File dest = new File(dir, filename);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("[文件上传] 保存文件失败: {}", filename, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文件保存失败");
        }

        // 5. 返回可访问 URL
        String url = fileUrlPrefix + "/" + filename;
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        data.put("filename", filename);
        log.info("[文件上传] userId={}, file={}, url={}", userId, filename, url);
        return ApiResponse.success(data);
    }
}
