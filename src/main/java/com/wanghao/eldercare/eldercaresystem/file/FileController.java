package com.wanghao.eldercare.eldercaresystem.file;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    @Audited(action = AuditAction.UPLOAD, entityType = "files", responseIdPath = "fileId")
    public ApiResponse<FileUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        FileUploadResponse response = fileStorageService.store(file);
        String absoluteUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(response.getUrl())
                .toUriString();
        response.setUrl(absoluteUrl);
        return ApiResponse.ok(response);
    }
}
