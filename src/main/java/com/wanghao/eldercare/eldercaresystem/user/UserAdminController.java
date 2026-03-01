package com.wanghao.eldercare.eldercaresystem.user;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
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
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ApiResponse<UserAdminPageResponse> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String role,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userAdminService.list(keyword, role, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserAdminDTO> detail(@PathVariable("id") Long id) {
        return ApiResponse.ok(userAdminService.getById(id));
    }

    @PostMapping
    @Audited(action = AuditAction.CREATE, entityType = "users", responseIdPath = "userId",
            requestFields = {"username", "role", "status", "realName", "phone", "email", "avatarUrl"})
    public ApiResponse<UserAdminDTO> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "users", entityIdArg = "id",
            requestFields = {"username", "role", "status", "realName", "phone", "email", "avatarUrl", "lastLoginAt"})
    public ApiResponse<UserAdminDTO> update(@PathVariable("id") Long id,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "users", entityIdArg = "id")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        userAdminService.delete(id);
        return ApiResponse.ok(null);
    }
}
