package com.wanghao.eldercare.eldercaresystem.controller.facility;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.dto.facility.CameraDeviceUpsertRequest;
import com.wanghao.eldercare.eldercaresystem.dto.facility.FacilityPageResponse;
import com.wanghao.eldercare.eldercaresystem.entity.facility.CameraDevice;
import com.wanghao.eldercare.eldercaresystem.service.facility.CameraDeviceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cameras")
@PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN)")
public class CameraDeviceController {

    private final CameraDeviceService cameraDeviceService;

    public CameraDeviceController(CameraDeviceService cameraDeviceService) {
        this.cameraDeviceService = cameraDeviceService;
    }

    @GetMapping
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "camera_device")
    public ApiResponse<FacilityPageResponse<CameraDevice>> list(@RequestParam(required = false) String keyword,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(cameraDeviceService.list(keyword, status, page, size));
    }

    @GetMapping("/{cameraId}")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "camera_device", entityIdArg = "cameraId")
    public ApiResponse<CameraDevice> detail(@PathVariable Long cameraId) {
        return ApiResponse.ok(cameraDeviceService.getById(cameraId));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "camera_device", responseIdPath = "cameraId",
            requestFields = {"cameraName", "cameraCode", "cameraType", "elderId", "roomId", "bedId", "locationText"})
    public ApiResponse<CameraDevice> create(@Valid @RequestBody CameraDeviceUpsertRequest request) {
        return ApiResponse.ok(cameraDeviceService.create(request));
    }

    @PutMapping("/{cameraId}")
    @Audited(action = AuditAction.UPDATE, entityType = "camera_device", entityIdArg = "cameraId",
            requestFields = {"cameraName", "cameraCode", "cameraType", "elderId", "roomId", "bedId", "locationText", "status"})
    public ApiResponse<CameraDevice> update(@PathVariable Long cameraId,
                                            @Valid @RequestBody CameraDeviceUpsertRequest request) {
        return ApiResponse.ok(cameraDeviceService.update(cameraId, request));
    }

    @DeleteMapping("/{cameraId}")
    @Audited(action = AuditAction.DELETE, entityType = "camera_device", entityIdArg = "cameraId")
    public ApiResponse<Void> delete(@PathVariable Long cameraId) {
        cameraDeviceService.delete(cameraId);
        return ApiResponse.ok(null);
    }
}
