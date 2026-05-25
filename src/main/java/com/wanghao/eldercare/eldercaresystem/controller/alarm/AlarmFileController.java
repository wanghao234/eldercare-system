package com.wanghao.eldercare.eldercaresystem.controller.alarm;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/alarm-files")
public class AlarmFileController {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Path ALARM_UPLOAD_DIR = Paths.get("uploads", "alarm").toAbsolutePath().normalize();

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestPart("file") MultipartFile file) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String suffix = UUID.randomUUID().toString().replace("-", "");
        suffix = suffix.substring(Math.max(0, suffix.length() - 6));
        String fileName = "alarm_" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + "_" + suffix + "." + extension;
        Path destination = ALARM_UPLOAD_DIR.resolve(fileName).normalize();
        if (!destination.startsWith(ALARM_UPLOAD_DIR)) {
            throw badRequest("非法文件路径");
        }

        try {
            Files.createDirectories(ALARM_UPLOAD_DIR);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "报警截图保存失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ApiResponse.success(Map.of("url", "/uploads/alarm/" + fileName));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("file 不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw badRequest("文件大小不能超过 5MB");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw badRequest("仅支持 jpg、jpeg、png 图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw badRequest("仅允许上传图片文件");
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw badRequest("文件名非法");
        }
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0 || idx == fileName.length() - 1) {
            throw badRequest("文件扩展名缺失");
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
