package com.wanghao.eldercare.eldercaresystem.service.billing;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.billing.*;
import com.wanghao.eldercare.eldercaresystem.dto.billing.*;
import com.wanghao.eldercare.eldercaresystem.entity.billing.*;
import com.wanghao.eldercare.eldercaresystem.mapper.billing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final Set<String> BILL_STATUS = Set.of("unpaid", "paid", "cancelled");
    private static final Set<String> PAYMENT_METHODS = Set.of("offline", "wechat", "alipay", "bank");

    private final FeeItemRepository feeItemRepository;
    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;
    private final PaymentRepository paymentRepository;
    private final PermissionService permissionService;

    public BillingService(FeeItemRepository feeItemRepository,
                          BillRepository billRepository,
                          BillItemRepository billItemRepository,
                          PaymentRepository paymentRepository,
                          PermissionService permissionService) {
        this.feeItemRepository = feeItemRepository;
        this.billRepository = billRepository;
        this.billItemRepository = billItemRepository;
        this.paymentRepository = paymentRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public BillingPageResponse<FeeItemDTO> listFeeItems(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "feeItemId"));
        String q = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<FeeItem> result = feeItemRepository.search(q, pageable);
        return toPage(result.map(FeeItemDTO::from), page, size);
    }

    @Transactional
    public FeeItemDTO createFeeItem(FeeItemUpsertRequest request) {
        FeeItem feeItem = new FeeItem();
        feeItem.setItemName(request.getItemName().trim());
        feeItem.setCategory(request.getCategory().trim());
        feeItem.setUnit(trimToNull(request.getUnit()));
        feeItem.setUnitPrice(normalizeMoney(request.getUnitPrice()));
        feeItem.setIsActive(1);
        feeItem.setCreatedAt(LocalDateTime.now());
        return saveFeeItem(feeItem);
    }

    @Transactional
    public FeeItemDTO updateFeeItem(Long id, FeeItemUpsertRequest request) {
        FeeItem feeItem = feeItemRepository.findById(id).orElseThrow(() -> new NotFoundException("收费项目不存在"));
        feeItem.setItemName(request.getItemName().trim());
        feeItem.setCategory(request.getCategory().trim());
        feeItem.setUnit(trimToNull(request.getUnit()));
        feeItem.setUnitPrice(normalizeMoney(request.getUnitPrice()));
        return saveFeeItem(feeItem);
    }

    @Transactional
    public void deleteFeeItem(Long id) {
        FeeItem feeItem = feeItemRepository.findById(id).orElseThrow(() -> new NotFoundException("收费项目不存在"));
        if (billItemRepository.existsByFeeItemId(id)) {
            throw badRequest("收费项目已被账单引用，禁止删除");
        }
        feeItemRepository.delete(feeItem);
    }

    @Transactional(readOnly = true)
    public BillingPageResponse<BillSummaryDTO> listBills(CurrentUser currentUser,
                                                         Long elderId,
                                                         String status,
                                                         LocalDate periodStart,
                                                         LocalDate periodEnd,
                                                         int page,
                                                         int size) {
        Specification<Bill> spec = Specification.where(null);

        Long fixedElderId = elderId;
        List<Long> visibleElderIds = null;
        if (isAdminOrLeader(currentUser)) {
            // full access
        } else {
            visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds == null) {
                throw new AccessDeniedException("当前角色无账单权限");
            }
            if (fixedElderId != null && !visibleElderIds.contains(fixedElderId)) {
                throw new AccessDeniedException("无权限访问该老人账单");
            }
            if (fixedElderId == null && visibleElderIds.isEmpty()) {
                return emptyPage(page, size);
            }
        }

        if (fixedElderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), fixedElderId));
        } else if (visibleElderIds != null) {
            List<Long> scopedElderIds = visibleElderIds;
            spec = spec.and((root, query, cb) -> root.get("elderId").in(scopedElderIds));
        }

        if (status != null && !status.isBlank()) {
            String normalizedStatus = normalizeStatus(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalizedStatus));
        }

        if (periodStart != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("periodStart"), periodStart));
        }

        if (periodEnd != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("periodEnd"), periodEnd));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"));
        Page<Bill> result = billRepository.findAll(spec, pageable);
        return toPage(result.map(BillSummaryDTO::from), page, size);
    }

    @Transactional(readOnly = true)
    public BillDetailDTO getBillDetail(CurrentUser currentUser, Long billId) {
        Bill bill = billRepository.findById(billId).orElseThrow(() -> new NotFoundException("账单不存在"));
        if (!isAdminOrLeader(currentUser)) {
            permissionService.assertCanAccessElder(currentUser, bill.getElderId());
        }
        return toBillDetail(bill);
    }

    @Transactional
    public BillDetailDTO generateBill(CurrentUser currentUser, BillGenerateRequest request) {
        ensureAdmin(currentUser);
        if (request.getPeriodStart().isAfter(request.getPeriodEnd())) {
            throw badRequest("periodStart 不能晚于 periodEnd");
        }

        Map<Long, FeeItem> feeItemMap = loadFeeItems(request.getItems());
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BillItem> billItems = new ArrayList<>();

        for (BillGenerateItemRequest item : request.getItems()) {
            BigDecimal quantity = item.getQuantity().setScale(2, RoundingMode.HALF_UP);
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw badRequest("quantity 必须大于0");
            }
            FeeItem feeItem = feeItemMap.get(item.getFeeItemId());
            if (feeItem == null) {
                throw new NotFoundException("收费项目不存在: " + item.getFeeItemId());
            }

            BigDecimal unitPrice = normalizeMoney(feeItem.getUnitPrice());
            BigDecimal amount = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(amount);

            BillItem billItem = new BillItem();
            billItem.setFeeItemId(feeItem.getFeeItemId());
            billItem.setQuantity(quantity);
            billItem.setUnitPrice(unitPrice);
            billItem.setAmount(amount);
            billItems.add(billItem);
        }

        Bill bill = new Bill();
        bill.setElderId(request.getElderId());
        bill.setPeriodStart(request.getPeriodStart());
        bill.setPeriodEnd(request.getPeriodEnd());
        bill.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        bill.setStatus("unpaid");
        bill.setGeneratedAt(LocalDateTime.now());
        bill.setCreatedBy(currentUser.getUserId());
        Bill savedBill = billRepository.save(bill);

        for (BillItem billItem : billItems) {
            billItem.setBillId(savedBill.getBillId());
        }
        billItemRepository.saveAll(billItems);

        return toBillDetail(savedBill);
    }

    @Transactional
    public BillDetailDTO registerPayment(CurrentUser currentUser, Long billId, BillPayRequest request) {
        ensureAdmin(currentUser);

        Bill bill = billRepository.findById(billId).orElseThrow(() -> new NotFoundException("账单不存在"));
        String method = normalizePaymentMethod(request.getMethod());
        BigDecimal amount = normalizeMoney(request.getAmount());

        if (amount.compareTo(bill.getTotalAmount()) != 0) {
            throw badRequest("支付金额需等于账单总额");
        }

        Payment payment = new Payment();
        payment.setBillId(billId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setTransactionNo(trimToNull(request.getTransactionNo()));
        payment.setStatus("paid");
        payment.setPaidAt(LocalDateTime.now());
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        int updated = billRepository.updateStatusIfMatch(billId, "unpaid", "paid");
        if (updated == 0) {
            throw badRequest("账单状态不匹配，仅允许 unpaid -> paid");
        }

        Bill latest = billRepository.findById(billId).orElseThrow(() -> new NotFoundException("账单不存在"));
        return toBillDetail(latest);
    }

    private BillDetailDTO toBillDetail(Bill bill) {
        List<BillItem> itemEntities = billItemRepository.findByBillId(bill.getBillId());
        List<Payment> paymentEntities = paymentRepository.findByBillIdOrderByPaidAtDesc(bill.getBillId());

        Map<Long, String> itemNameMap = new HashMap<>();
        if (!itemEntities.isEmpty()) {
            List<Long> feeItemIds = itemEntities.stream().map(BillItem::getFeeItemId).distinct().toList();
            feeItemRepository.findAllById(feeItemIds)
                    .forEach(feeItem -> itemNameMap.put(feeItem.getFeeItemId(), feeItem.getItemName()));
        }

        List<BillItemDTO> itemDtos = itemEntities.stream().map(item -> {
            BillItemDTO dto = new BillItemDTO();
            dto.setBillItemId(item.getBillItemId());
            dto.setFeeItemId(item.getFeeItemId());
            dto.setItemName(itemNameMap.get(item.getFeeItemId()));
            dto.setQuantity(item.getQuantity());
            dto.setUnitPrice(item.getUnitPrice());
            dto.setAmount(item.getAmount());
            dto.setNote(item.getNote());
            return dto;
        }).toList();

        BillDetailDTO detail = new BillDetailDTO();
        detail.setBillId(bill.getBillId());
        detail.setElderId(bill.getElderId());
        detail.setPeriodStart(bill.getPeriodStart());
        detail.setPeriodEnd(bill.getPeriodEnd());
        detail.setTotalAmount(bill.getTotalAmount());
        detail.setStatus(bill.getStatus());
        detail.setGeneratedAt(bill.getGeneratedAt());
        detail.setDueDate(bill.getDueDate());
        detail.setItems(itemDtos);
        detail.setPayments(paymentEntities.stream().map(PaymentDTO::from).toList());
        return detail;
    }

    private Map<Long, FeeItem> loadFeeItems(List<BillGenerateItemRequest> items) {
        List<Long> feeItemIds = items.stream().map(BillGenerateItemRequest::getFeeItemId).distinct().toList();
        List<FeeItem> feeItems = feeItemRepository.findAllById(feeItemIds);
        Map<Long, FeeItem> result = new HashMap<>();
        for (FeeItem feeItem : feeItems) {
            result.put(feeItem.getFeeItemId(), feeItem);
        }
        return result;
    }

    private BillingPageResponse<BillSummaryDTO> emptyPage(int page, int size) {
        BillingPageResponse<BillSummaryDTO> response = new BillingPageResponse<>();
        response.setItems(List.of());
        response.setTotal(0);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private FeeItemDTO saveFeeItem(FeeItem feeItem) {
        try {
            return FeeItemDTO.from(feeItemRepository.save(feeItem));
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("收费项目名称冲突");
        }
    }

    private String normalizeStatus(String status) {
        String value = status.toLowerCase(Locale.ROOT);
        if (!BILL_STATUS.contains(value)) {
            throw badRequest("账单状态非法");
        }
        return value;
    }

    private String normalizePaymentMethod(String method) {
        String value = method.toLowerCase(Locale.ROOT);
        if (!PAYMENT_METHODS.contains(value)) {
            throw badRequest("支付方式非法");
        }
        return value;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            throw badRequest("金额不能为空");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isAdminOrLeader(CurrentUser currentUser) {
        return currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader");
    }

    private void ensureAdmin(CurrentUser currentUser) {
        if (!currentUser.hasRole("admin")) {
            throw new AccessDeniedException("当前角色无权限执行该操作");
        }
    }

    private <T> BillingPageResponse<T> toPage(Page<T> pageData, int page, int size) {
        BillingPageResponse<T> response = new BillingPageResponse<>();
        response.setItems(pageData.getContent());
        response.setTotal(pageData.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
