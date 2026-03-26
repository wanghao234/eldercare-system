package com.wanghao.eldercare.eldercaresystem.controller.inventory;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.dto.inventory.*;
import com.wanghao.eldercare.eldercaresystem.entity.inventory.*;
import com.wanghao.eldercare.eldercaresystem.mapper.inventory.*;
import com.wanghao.eldercare.eldercaresystem.service.inventory.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final CurrentUserUtils currentUserUtils;

    public InventoryController(InventoryService inventoryService, CurrentUserUtils currentUserUtils) {
        this.inventoryService = inventoryService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    public ApiResponse<InventoryPageResponse<SupplyItemDTO>> listItems(@RequestParam(required = false) String keyword,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inventoryService.listItems(keyword, page, size));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "supply_items", responseIdPath = "supplyItemId",
            requestFields = {"itemName", "category", "unit"})
    public ApiResponse<SupplyItemDTO> createItem(@Valid @RequestBody SupplyItemUpsertRequest request) {
        return ApiResponse.ok(inventoryService.createItem(request));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.UPDATE, entityType = "supply_items", entityIdArg = "id",
            requestFields = {"itemName", "category", "unit"})
    public ApiResponse<SupplyItemDTO> updateItem(@PathVariable Long id,
                                                 @Valid @RequestBody SupplyItemUpsertRequest request) {
        return ApiResponse.ok(inventoryService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.DELETE, entityType = "supply_items", entityIdArg = "id")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        inventoryService.deleteItem(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/stocks")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    public ApiResponse<InventoryPageResponse<SupplyStockDTO>> listStocks(@RequestParam(required = false) Long itemId,
                                                                          @RequestParam(required = false) String location,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inventoryService.listStocks(itemId, location, page, size));
    }

    @PostMapping("/stocks")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.CREATE, entityType = "supply_stocks", responseIdPath = "stockId",
            requestFields = {"supplyItemId", "location", "quantity", "minThreshold"})
    public ApiResponse<SupplyStockDTO> createStock(@Valid @RequestBody SupplyStockCreateRequest request) {
        return ApiResponse.ok(inventoryService.createStock(request));
    }

    @PutMapping("/stocks/{id}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.UPDATE, entityType = "supply_stocks", entityIdArg = "id",
            requestFields = {"quantity", "minThreshold"})
    public ApiResponse<SupplyStockDTO> updateStock(@PathVariable Long id,
                                                   @Valid @RequestBody SupplyStockUpdateRequest request) {
        return ApiResponse.ok(inventoryService.updateStock(id, request));
    }

    @PostMapping("/stocks/{id}/adjust")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER)")
    @Audited(action = AuditAction.UPDATE, entityType = "supply_stocks", entityIdArg = "id",
            requestFields = {"delta", "reason"})
    public ApiResponse<SupplyStockDTO> adjustStock(@PathVariable Long id,
                                                   @Valid @RequestBody SupplyStockAdjustRequest request) {
        return ApiResponse.ok(inventoryService.adjustStock(id, request));
    }

    @PostMapping("/issues")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    @Audited(action = "ISSUE", entityType = "supply_issue_records", responseIdPath = "issueId",
            requestFields = {"supplyItemId", "location", "quantity", "note", "relatedTaskId"})
    public ApiResponse<SupplyIssueDTO> createIssue(@Valid @RequestBody SupplyIssueCreateRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(inventoryService.createIssue(currentUser, request));
    }

    @GetMapping("/issues")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_NURSE,"
            + "T(com.wanghao.eldercare.eldercaresystem.common.security.Role).ROLE_CAREGIVER)")
    public ApiResponse<InventoryPageResponse<SupplyIssueDTO>> listIssues(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(inventoryService.listIssues(currentUser, from, to, itemId, page, size));
    }
}
