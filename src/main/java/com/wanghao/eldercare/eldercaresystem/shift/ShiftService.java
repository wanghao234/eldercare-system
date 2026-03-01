package com.wanghao.eldercare.eldercaresystem.shift;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.PermissionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ShiftService {

    private static final Set<String> SHIFT_TYPES = Set.of("morning", "afternoon", "night");
    private static final Set<String> SHIFT_STATUSES = Set.of("open", "closed");

    private final ShiftRepository shiftRepository;
    private final HandoverNoteRepository handoverNoteRepository;
    private final HandoverFocusElderRepository handoverFocusElderRepository;
    private final PermissionService permissionService;

    public ShiftService(ShiftRepository shiftRepository,
                        HandoverNoteRepository handoverNoteRepository,
                        HandoverFocusElderRepository handoverFocusElderRepository,
                        PermissionService permissionService) {
        this.shiftRepository = shiftRepository;
        this.handoverNoteRepository = handoverNoteRepository;
        this.handoverFocusElderRepository = handoverFocusElderRepository;
        this.permissionService = permissionService;
    }

    @Transactional
    public Shift createShift(CreateShiftRequest request) {
        String shiftType = normalizeShiftType(request.getShiftType());
        if (shiftRepository.existsByShiftDateAndShiftType(request.getShiftDate(), shiftType)) {
            throw badRequest("班次已存在(shift_date + shift_type)");
        }

        Shift shift = new Shift();
        shift.setShiftDate(request.getShiftDate());
        shift.setShiftType(shiftType);
        shift.setLeaderId(request.getLeaderId());
        shift.setStatus("open");
        shift.setCreatedAt(LocalDateTime.now());

        try {
            return shiftRepository.save(shift);
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("班次冲突(shift_date + shift_type)");
        }
    }

    @Transactional(readOnly = true)
    public ShiftPageResponse<Shift> listShifts(java.time.LocalDate from,
                                               java.time.LocalDate to,
                                               String shiftType,
                                               String status,
                                               int page,
                                               int size) {
        Specification<Shift> spec = Specification.where(null);
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("shiftDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("shiftDate"), to));
        }
        if (shiftType != null && !shiftType.isBlank()) {
            String normalizedType = normalizeShiftType(shiftType);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("shiftType"), normalizedType));
        }
        if (status != null && !status.isBlank()) {
            String normalizedStatus = normalizeShiftStatus(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalizedStatus));
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("shiftDate"), Sort.Order.desc("shiftId")));
        Page<Shift> result = shiftRepository.findAll(spec, pageable);
        ShiftPageResponse<Shift> response = new ShiftPageResponse<>();
        response.setItems(result.getContent());
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public Shift getShift(Long shiftId) {
        return getShiftOrThrow(shiftId);
    }

    @Transactional
    public Shift closeShift(Long shiftId, CloseShiftRequest request) {
        Shift shift = getShiftOrThrow(shiftId);
        String from = normalizeShiftStatus(request.getFrom());
        String to = normalizeShiftStatus(request.getTo());

        if (!"open".equals(from) || !"closed".equals(to)) {
            throw badRequest("仅支持 open -> closed");
        }

        if (to.equals(shift.getStatus())) {
            return shift;
        }

        int updated = shiftRepository.transitionStatus(shiftId, from, to);
        if (updated == 0) {
            Shift latest = getShiftOrThrow(shiftId);
            if (to.equals(latest.getStatus())) {
                return latest;
            }
            throw badRequest("状态不匹配，期望 from=" + from + "，实际=" + latest.getStatus());
        }
        return getShiftOrThrow(shiftId);
    }

    @Transactional
    public HandoverNote createHandoverNote(CurrentUser currentUser, Long shiftId, CreateHandoverNoteRequest request) {
        Shift shift = getShiftOrThrow(shiftId);
        if ("closed".equals(shift.getStatus())) {
            throw badRequest("班次已关闭，禁止新增交接班记录");
        }

        HandoverNote note = new HandoverNote();
        note.setShiftId(shiftId);
        note.setCreatedBy(currentUser.getUserId());
        note.setContent(request.getContent().trim());
        note.setCreatedAt(LocalDateTime.now());
        return handoverNoteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public ShiftPageResponse<HandoverNote> listHandoverNotes(Long shiftId, int page, int size) {
        getShiftOrThrow(shiftId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "noteId"));
        Page<HandoverNote> result = handoverNoteRepository.findByShiftId(shiftId, pageable);

        ShiftPageResponse<HandoverNote> response = new ShiftPageResponse<>();
        response.setItems(result.getContent());
        response.setTotal(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional
    public HandoverFocusElder addFocusElder(CurrentUser currentUser, Long shiftId, CreateFocusElderRequest request) {
        getShiftOrThrow(shiftId);
        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        HandoverFocusElder focus = handoverFocusElderRepository.findByShiftIdAndElderId(shiftId, request.getElderId())
                .orElseGet(HandoverFocusElder::new);
        if (focus.getId() == null) {
            focus.setShiftId(shiftId);
            focus.setElderId(request.getElderId());
            focus.setCreatedAt(LocalDateTime.now());
        }
        focus.setNote(trimToNull(request.getNote()));
        return handoverFocusElderRepository.save(focus);
    }

    @Transactional(readOnly = true)
    public List<HandoverFocusElder> listFocusElders(CurrentUser currentUser, Long shiftId) {
        getShiftOrThrow(shiftId);
        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds != null && visibleElderIds.isEmpty()) {
                return List.of();
            }
            if (visibleElderIds != null) {
                return handoverFocusElderRepository.findByShiftIdAndElderIdInOrderByCreatedAtDesc(shiftId, visibleElderIds);
            }
        }
        return handoverFocusElderRepository.findByShiftIdOrderByCreatedAtDesc(shiftId);
    }

    @Transactional
    public void deleteFocusElder(Long shiftId, Long elderId) {
        getShiftOrThrow(shiftId);
        handoverFocusElderRepository.deleteByShiftIdAndElderId(shiftId, elderId);
    }

    private Shift getShiftOrThrow(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new NotFoundException("班次不存在"));
    }

    private String normalizeShiftType(String shiftType) {
        String value = shiftType.toLowerCase(Locale.ROOT);
        if (!SHIFT_TYPES.contains(value)) {
            throw badRequest("shiftType 非法");
        }
        return value;
    }

    private String normalizeShiftStatus(String status) {
        String value = status.toLowerCase(Locale.ROOT);
        if (!SHIFT_STATUSES.contains(value)) {
            throw badRequest("status 非法");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
