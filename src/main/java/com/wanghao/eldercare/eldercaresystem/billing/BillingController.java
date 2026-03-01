package com.wanghao.eldercare.eldercaresystem.billing;

import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUserUtils;
import jakarta.validation.Valid;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final CurrentUserUtils currentUserUtils;

    public BillingController(BillingService billingService, CurrentUserUtils currentUserUtils) {
        this.billingService = billingService;
        this.currentUserUtils = currentUserUtils;
    }

    @GetMapping("/fee-items")
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    @Audited(action = "VIEW_SENSITIVE", entityType = "fee_items", sensitive = true)
    public ApiResponse<BillingPageResponse<FeeItemDTO>> listFeeItems(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(billingService.listFeeItems(keyword, page, size));
    }

    @PostMapping("/fee-items")
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    @Audited(action = "CREATE", entityType = "fee_items", responseIdPath = "feeItemId",
            requestFields = {"itemName", "category", "unit", "unitPrice"})
    public ApiResponse<FeeItemDTO> createFeeItem(@Valid @RequestBody FeeItemUpsertRequest request) {
        return ApiResponse.ok(billingService.createFeeItem(request));
    }

    @PutMapping("/fee-items/{id}")
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    @Audited(action = "UPDATE", entityType = "fee_items", entityIdArg = "id",
            requestFields = {"itemName", "category", "unit", "unitPrice"})
    public ApiResponse<FeeItemDTO> updateFeeItem(@PathVariable Long id,
                                                 @Valid @RequestBody FeeItemUpsertRequest request) {
        return ApiResponse.ok(billingService.updateFeeItem(id, request));
    }

    @DeleteMapping("/fee-items/{id}")
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    @Audited(action = "DELETE", entityType = "fee_items", entityIdArg = "id")
    public ApiResponse<Void> deleteFeeItem(@PathVariable Long id) {
        billingService.deleteFeeItem(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/bills")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ELDER)")
    @Audited(action = "VIEW_SENSITIVE", entityType = "bills", sensitive = true)
    public ApiResponse<BillingPageResponse<BillSummaryDTO>> listBills(@RequestParam(required = false) Long elderId,
                                                                       @RequestParam(required = false) String status,
                                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
                                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(billingService.listBills(currentUser, elderId, status, periodStart, periodEnd, page, size));
    }

    @GetMapping("/bills/{billId}")
    @PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_FAMILY,"
            + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ELDER)")
    @Audited(action = "VIEW_SENSITIVE", entityType = "bills", entityIdArg = "billId", sensitive = true)
    public ApiResponse<BillDetailDTO> getBill(@PathVariable Long billId) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(billingService.getBillDetail(currentUser, billId));
    }

    @PostMapping("/bills/generate")
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    @Audited(action = "CREATE", entityType = "bills", responseIdPath = "billId",
            requestFields = {"elderId", "periodStart", "periodEnd", "items"})
    public ApiResponse<BillDetailDTO> generate(@Valid @RequestBody BillGenerateRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(billingService.generateBill(currentUser, request));
    }

    @PostMapping("/bills/{billId}/pay")
    @PreAuthorize("hasAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN)")
    @Audited(action = "PAYMENT", entityType = "payments", entityIdArg = "billId",
            requestFields = {"amount", "method", "transactionNo"})
    public ApiResponse<BillDetailDTO> pay(@PathVariable Long billId,
                                          @Valid @RequestBody BillPayRequest request) {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        return ApiResponse.ok(billingService.registerPayment(currentUser, billId, request));
    }
}
