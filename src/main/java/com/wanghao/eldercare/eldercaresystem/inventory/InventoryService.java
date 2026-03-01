package com.wanghao.eldercare.eldercaresystem.inventory;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {

    private final SupplyItemRepository supplyItemRepository;
    private final SupplyStockRepository supplyStockRepository;
    private final SupplyIssueRecordRepository supplyIssueRecordRepository;

    public InventoryService(SupplyItemRepository supplyItemRepository,
                            SupplyStockRepository supplyStockRepository,
                            SupplyIssueRecordRepository supplyIssueRecordRepository) {
        this.supplyItemRepository = supplyItemRepository;
        this.supplyStockRepository = supplyStockRepository;
        this.supplyIssueRecordRepository = supplyIssueRecordRepository;
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse<SupplyItemDTO> listItems(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "supplyItemId"));
        String q = trimToNull(keyword);
        Page<SupplyItem> result = supplyItemRepository.search(q, pageable);
        return toPage(result.map(SupplyItemDTO::from), page, size);
    }

    @Transactional
    public SupplyItemDTO createItem(SupplyItemUpsertRequest request) {
        SupplyItem item = new SupplyItem();
        item.setItemName(request.getItemName().trim());
        item.setCategory(request.getCategory().trim());
        item.setUnit(trimToNull(request.getUnit()));
        item.setIsActive(1);
        item.setCreatedAt(LocalDateTime.now());
        return saveItem(item);
    }

    @Transactional
    public SupplyItemDTO updateItem(Long id, SupplyItemUpsertRequest request) {
        SupplyItem item = supplyItemRepository.findById(id).orElseThrow(() -> new NotFoundException("物资不存在"));
        item.setItemName(request.getItemName().trim());
        item.setCategory(request.getCategory().trim());
        item.setUnit(trimToNull(request.getUnit()));
        return saveItem(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        SupplyItem item = supplyItemRepository.findById(id).orElseThrow(() -> new NotFoundException("物资不存在"));
        if (supplyStockRepository.existsBySupplyItemId(id)) {
            throw badRequest("物资已被库存引用，禁止删除");
        }
        supplyItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse<SupplyStockDTO> listStocks(Long itemId, String location, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "stockId"));
        String locationLike = trimToNull(location);
        Page<SupplyStock> result = supplyStockRepository.search(itemId, locationLike, pageable);

        Map<Long, String> itemNameMap = loadItemNameMap(result.getContent().stream().map(SupplyStock::getSupplyItemId).distinct().toList());
        Page<SupplyStockDTO> dtoPage = result.map(stock -> toStockDTO(stock, itemNameMap.get(stock.getSupplyItemId())));
        return toPage(dtoPage, page, size);
    }

    @Transactional
    public SupplyStockDTO createStock(SupplyStockCreateRequest request) {
        SupplyItem item = supplyItemRepository.findById(request.getSupplyItemId())
                .orElseThrow(() -> new NotFoundException("物资不存在"));

        SupplyStock stock = new SupplyStock();
        stock.setSupplyItemId(request.getSupplyItemId());
        stock.setLocation(trimToNull(request.getLocation()));
        stock.setQuantity(normalizeNonNegative(request.getQuantity(), "quantity"));
        stock.setMinThreshold(normalizeNonNegative(request.getMinThreshold(), "minThreshold"));
        stock.setUpdatedAt(LocalDateTime.now());
        SupplyStock saved = supplyStockRepository.save(stock);
        return toStockDTO(saved, item.getItemName());
    }

    @Transactional
    public SupplyStockDTO updateStock(Long id, SupplyStockUpdateRequest request) {
        SupplyStock stock = supplyStockRepository.findById(id).orElseThrow(() -> new NotFoundException("库存不存在"));
        stock.setQuantity(normalizeNonNegative(request.getQuantity(), "quantity"));
        stock.setMinThreshold(normalizeNonNegative(request.getMinThreshold(), "minThreshold"));
        stock.setUpdatedAt(LocalDateTime.now());
        SupplyStock saved = supplyStockRepository.save(stock);
        String itemName = findItemName(stock.getSupplyItemId());
        return toStockDTO(saved, itemName);
    }

    @Transactional
    public SupplyStockDTO adjustStock(Long id, SupplyStockAdjustRequest request) {
        SupplyStock stock = supplyStockRepository.findById(id).orElseThrow(() -> new NotFoundException("库存不存在"));
        BigDecimal delta = request.getDelta().setScale(2, RoundingMode.HALF_UP);
        int updated;
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            updated = supplyStockRepository.addQuantity(id, delta, LocalDateTime.now());
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            updated = supplyStockRepository.deductIfEnough(id, delta.abs(), LocalDateTime.now());
        } else {
            updated = 1;
        }

        if (updated == 0) {
            throw badRequest("库存不足，无法扣减");
        }

        SupplyStock latest = supplyStockRepository.findById(id).orElseThrow(() -> new NotFoundException("库存不存在"));
        return toStockDTO(latest, findItemName(latest.getSupplyItemId()));
    }

    @Transactional
    public SupplyIssueDTO createIssue(CurrentUser currentUser, SupplyIssueCreateRequest request) {
        SupplyItem item = supplyItemRepository.findById(request.getSupplyItemId())
                .orElseThrow(() -> new NotFoundException("物资不存在"));
        String location = request.getLocation().trim();
        BigDecimal quantity = request.getQuantity().setScale(2, RoundingMode.HALF_UP);

        SupplyStock stock = supplyStockRepository.findTopBySupplyItemIdAndLocationOrderByStockIdDesc(request.getSupplyItemId(), location)
                .orElseThrow(() -> new NotFoundException("库存不存在"));

        int updated = supplyStockRepository.deductIfEnough(stock.getStockId(), quantity, LocalDateTime.now());
        if (updated == 0) {
            throw badRequest("库存不足");
        }

        SupplyIssueRecord issue = new SupplyIssueRecord();
        issue.setSupplyItemId(request.getSupplyItemId());
        issue.setQuantity(quantity);
        issue.setIssuedTo(currentUser.getUserId());
        issue.setIssuedBy(currentUser.getUserId());
        issue.setIssueTime(LocalDateTime.now());
        issue.setNote(trimToNull(request.getNote()));
        issue.setRelatedTaskId(request.getRelatedTaskId());
        SupplyIssueRecord saved = supplyIssueRecordRepository.save(issue);
        return toIssueDTO(saved, item.getItemName());
    }

    @Transactional(readOnly = true)
    public InventoryPageResponse<SupplyIssueDTO> listIssues(CurrentUser currentUser,
                                                            LocalDateTime from,
                                                            LocalDateTime to,
                                                            Long itemId,
                                                            int page,
                                                            int size) {
        Specification<SupplyIssueRecord> spec = Specification.where(null);

        if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("issuedTo"), currentUser.getUserId()));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issueTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("issueTime"), to));
        }
        if (itemId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("supplyItemId"), itemId));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issueTime"));
        Page<SupplyIssueRecord> result = supplyIssueRecordRepository.findAll(spec, pageable);
        Map<Long, String> itemNameMap = loadItemNameMap(result.getContent().stream().map(SupplyIssueRecord::getSupplyItemId).distinct().toList());

        Page<SupplyIssueDTO> dtoPage = result.map(issue -> toIssueDTO(issue, itemNameMap.get(issue.getSupplyItemId())));
        return toPage(dtoPage, page, size);
    }

    private String findItemName(Long supplyItemId) {
        return supplyItemRepository.findById(supplyItemId)
                .map(SupplyItem::getItemName)
                .orElse(null);
    }

    private Map<Long, String> loadItemNameMap(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>();
        supplyItemRepository.findAllById(ids).forEach(item -> map.put(item.getSupplyItemId(), item.getItemName()));
        return map;
    }

    private SupplyStockDTO toStockDTO(SupplyStock stock, String itemName) {
        SupplyStockDTO dto = new SupplyStockDTO();
        dto.setStockId(stock.getStockId());
        dto.setSupplyItemId(stock.getSupplyItemId());
        dto.setItemName(itemName);
        dto.setQuantity(stock.getQuantity());
        dto.setMinThreshold(stock.getMinThreshold());
        dto.setLocation(stock.getLocation());
        dto.setUpdatedAt(stock.getUpdatedAt());
        return dto;
    }

    private SupplyIssueDTO toIssueDTO(SupplyIssueRecord issue, String itemName) {
        SupplyIssueDTO dto = new SupplyIssueDTO();
        dto.setIssueId(issue.getIssueId());
        dto.setSupplyItemId(issue.getSupplyItemId());
        dto.setItemName(itemName);
        dto.setQuantity(issue.getQuantity());
        dto.setIssuedTo(issue.getIssuedTo());
        dto.setIssuedBy(issue.getIssuedBy());
        dto.setIssueTime(issue.getIssueTime());
        dto.setNote(issue.getNote());
        dto.setRelatedTaskId(issue.getRelatedTaskId());
        return dto;
    }

    private SupplyItemDTO saveItem(SupplyItem item) {
        try {
            return SupplyItemDTO.from(supplyItemRepository.save(item));
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("物资名称冲突");
        }
    }

    private BigDecimal normalizeNonNegative(BigDecimal value, String field) {
        if (value == null) {
            throw badRequest(field + "不能为空");
        }
        BigDecimal normalized = value.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest(field + "不能为负数");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private <T> InventoryPageResponse<T> toPage(Page<T> pageData, int page, int size) {
        InventoryPageResponse<T> response = new InventoryPageResponse<>();
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
