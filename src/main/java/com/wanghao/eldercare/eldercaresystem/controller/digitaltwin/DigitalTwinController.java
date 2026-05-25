package com.wanghao.eldercare.eldercaresystem.controller.digitaltwin;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.dto.digitaltwin.DigitalTwinMapResponse;
import com.wanghao.eldercare.eldercaresystem.service.digitaltwin.DigitalTwinService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/digital-twin")
@PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN)")
public class DigitalTwinController {

    private final DigitalTwinService digitalTwinService;

    public DigitalTwinController(DigitalTwinService digitalTwinService) {
        this.digitalTwinService = digitalTwinService;
    }

    @GetMapping("/map")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "digital_twin_map")
    public ApiResponse<DigitalTwinMapResponse> getMap() {
        return ApiResponse.ok(digitalTwinService.getCurrentMap());
    }
}
