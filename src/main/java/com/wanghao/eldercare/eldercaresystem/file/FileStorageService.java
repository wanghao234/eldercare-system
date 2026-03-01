package com.wanghao.eldercare.eldercaresystem.file;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private final FileStorageProperties properties;

    public FileStorageService(FileStorageProperties properties) {
        this.properties = properties;
    }

    public FileUploadResponse store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("文件不能为空");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw badRequest("文件大小超过限制");
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        Set<String> allowed = properties.getAllowedExtensions().stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowed.contains(extension)) {
            throw badRequest("文件类型不支持");
        }

        String fileId = UUID.randomUUID().toString().replace("-", "");
        String storedFileName = fileId + "." + extension;
        Path storagePath = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
        Path destination = storagePath.resolve(storedFileName).normalize();

        if (!destination.startsWith(storagePath)) {
            throw badRequest("非法文件路径");
        }

        try {
            Files.createDirectories(storagePath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(fileId);
        response.setFileName(originalName);
        response.setUrl("/uploads/" + storedFileName);
        response.setSize(file.getSize());
        response.setContentType(file.getContentType());
        return response;
    }

    public Path getStorageAbsolutePath() {
        return Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
    }

    private String extractExtension(String fileName) {
        if (fileName == null) {
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

