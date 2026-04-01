package com.wanghao.eldercare.eldercaresystem.service.admission;

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
import com.wanghao.eldercare.eldercaresystem.controller.admission.*;
import com.wanghao.eldercare.eldercaresystem.dto.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.admission.*;
import com.wanghao.eldercare.eldercaresystem.entity.facility.FacilityBed;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfInstance;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfTask;
import com.wanghao.eldercare.eldercaresystem.entity.workflow.WfTaskAction;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.*;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.FacilityBedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfInstanceRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskActionRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.workflow.WfTaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdmissionService {

    private static final long FIRST_TASK_DUE_HOURS = 2L;

    private final AdmissionRecordRepository admissionRecordRepository;
    private final DischargeRecordRepository dischargeRecordRepository;
    private final BedRepository bedRepository;
    private final FacilityBedRepository facilityBedRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final JdbcTemplate jdbcTemplate;
    private final WfInstanceRepository wfInstanceRepository;
    private final WfTaskRepository wfTaskRepository;
    private final WfTaskActionRepository wfTaskActionRepository;

    public AdmissionService(AdmissionRecordRepository admissionRecordRepository,
                            DischargeRecordRepository dischargeRecordRepository,
                            BedRepository bedRepository,
                            FacilityBedRepository facilityBedRepository,
                            UserRepository userRepository,
                            PermissionService permissionService,
                            JdbcTemplate jdbcTemplate,
                            WfInstanceRepository wfInstanceRepository,
                            WfTaskRepository wfTaskRepository,
                            WfTaskActionRepository wfTaskActionRepository) {
        this.admissionRecordRepository = admissionRecordRepository;
        this.dischargeRecordRepository = dischargeRecordRepository;
        this.bedRepository = bedRepository;
        this.facilityBedRepository = facilityBedRepository;
        this.userRepository = userRepository;
        this.permissionService = permissionService;
        this.jdbcTemplate = jdbcTemplate;
        this.wfInstanceRepository = wfInstanceRepository;
        this.wfTaskRepository = wfTaskRepository;
        this.wfTaskActionRepository = wfTaskActionRepository;
    }

    @Transactional
    public IdResponse createAdmission(CurrentUser currentUser, CreateAdmissionRequest request) {
        requireWriteRole(currentUser);
        Long elderId = resolveElderId(request);
        Long bedId = resolveBedId(request);
        permissionService.assertCanAccessElder(currentUser, elderId);
        if (admissionRecordRepository.existsByElderIdAndEndDateIsNull(elderId)) {
            throw badRequest("老人存在未退住的入住记录，不允许重复入住");
        }
        if (admissionRecordRepository.existsByBedIdAndEndDateIsNull(bedId)) {
            throw badRequest("床位已有未退住的入住记录，不允许重复分配");
        }

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new NotFoundException("床位不存在"));
        if (!"available".equalsIgnoreCase(bed.getStatus())) {
            throw badRequest("床位不可用，当前状态=" + bed.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        AdmissionRecord admission = new AdmissionRecord();
        admission.setElderId(elderId);
        admission.setBedId(bedId);
        admission.setContractNo(trimToNull(request.getContractNo()));
        admission.setPackageName(trimToNull(request.getPackageName()));
        admission.setStatus("pending");
        admission.setStartDate(request.getStartDate() == null ? LocalDate.now() : request.getStartDate());
        admission.setDepositAmount(request.getDepositAmount() == null ? java.math.BigDecimal.ZERO : request.getDepositAmount());
        admission.setProcessInstanceId(null);
        admission.setCreatedBy(currentUser.getUserId());
        admission.setCreatedAt(now);
        admission.setUpdatedAt(now);

        int bedUpdated;
        try {
            bedUpdated = bedRepository.reserveIfAvailable(bedId);
        } catch (DataAccessException ex) {
            throw badRequest("床位状态更新失败，请检查 beds.status 是否支持 reserved");
        }
        if (bedUpdated == 0) {
            throw badRequest("床位已被占用，请刷新后重试");
        }
        AdmissionRecord saved = admissionRecordRepository.save(admission);
        Long instanceId = createAdmissionWorkflow(saved, currentUser.getUserId(), now);
        admissionRecordRepository.bindProcessInstance(saved.getAdmissionId(), instanceId, now);
        return new IdResponse(saved.getAdmissionId());
    }

    private Long createAdmissionWorkflow(AdmissionRecord admission, Long startedBy, LocalDateTime now) {
        WfInstance instance = new WfInstance();
        instance.setProcessKey("admission");
        instance.setBizType("admission");
        instance.setBizId(admission.getAdmissionId());
        instance.setStatus("running");
        instance.setStartedBy(startedBy);
        instance.setStartedAt(now);
        instance.setCreatedAt(now);
        WfInstance savedInstance = wfInstanceRepository.save(instance);
        trySetDefVersion(savedInstance.getInstanceId(), 1);

        WfTask firstTask = new WfTask();
        firstTask.setInstanceId(savedInstance.getInstanceId());
        firstTask.setNodeKey("assign_nurse");
        firstTask.setTaskName("绑定护理员");
        firstTask.setCandidateRole("nurse_leader");
        firstTask.setStatus("pending");
        firstTask.setDueAt(now.plusHours(FIRST_TASK_DUE_HOURS));
        firstTask.setCreatedAt(now);
        WfTask savedTask = wfTaskRepository.save(firstTask);

        WfTaskAction action = new WfTaskAction();
        action.setWfTaskId(savedTask.getWfTaskId());
        action.setAction("create");
        action.setActorId(startedBy);
        action.setActionTime(now);
        action.setComment("创建入住流程并生成首节点");
        wfTaskActionRepository.save(action);
        return savedInstance.getInstanceId();
    }

    private void trySetDefVersion(Long instanceId, int defVersion) {
        try {
            jdbcTemplate.update("update wf_instances set def_version=? where instance_id=?", defVersion, instanceId);
        } catch (DataAccessException ex) {
            if (!isUnknownColumn(ex)) {
                throw ex;
            }
        }
    }

    public AdmissionListResponse listAdmissions(CurrentUser currentUser,
                                                Long elderId,
                                                String status,
                                                int page,
                                                int size) {
        Specification<AdmissionRecord> spec = Specification.where(null);

        if (elderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.toLowerCase(Locale.ROOT)));
        }

        List<Long> visibleElderIds = applyPermissionFilter(currentUser);
        if (visibleElderIds != null && visibleElderIds.isEmpty()) {
            return emptyAdmissionPage(page, size);
        }
        if (visibleElderIds != null) {
            spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdmissionRecord> pageResult;
        try {
            pageResult = admissionRecordRepository.findAll(spec, pageable);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex)) {
                return listAdmissionsViaJdbc(elderId, status, page, size, visibleElderIds);
            }
            throw ex;
        }

        AdmissionListResponse response = new AdmissionListResponse();
        response.setContent(toAdmissionListItems(pageResult.getContent()));
        response.setTotalElements(pageResult.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public AdmissionRecord getAdmissionDetail(CurrentUser currentUser, Long id) {
        AdmissionRecord record = admissionRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("入住记录不存在"));
        if (!currentUser.hasRole("doctor")) {
            permissionService.assertCanAccessElder(currentUser, record.getElderId());
        }
        return record;
    }

    @Transactional
    public IdResponse createDischarge(CurrentUser currentUser, DischargeCreateRequest request) {
        requireWriteRole(currentUser);

        AdmissionRecord admission = admissionRecordRepository.findById(request.getAdmissionId())
                .orElseThrow(() -> new NotFoundException("入住记录不存在"));
        permissionService.assertCanAccessElder(currentUser, admission.getElderId());

        DischargeRecord discharge = new DischargeRecord();
        discharge.setAdmissionId(admission.getAdmissionId());
        discharge.setElderId(admission.getElderId());
        discharge.setBedId(admission.getBedId());
        discharge.setStatus("pending");
        discharge.setReason(request.getReason());
        discharge.setRequestedDate(request.getRequestedDate() == null ? LocalDate.now() : request.getRequestedDate());
        discharge.setCreatedBy(currentUser.getUserId());
        discharge.setCreatedAt(LocalDateTime.now());
        discharge.setUpdatedAt(LocalDateTime.now());

        DischargeRecord saved = dischargeRecordRepository.save(discharge);
        return new IdResponse(saved.getDischargeId());
    }

    @Transactional
    public DischargeRecord settlement(CurrentUser currentUser, Long id, SettlementRequest request) {
        requireWriteRole(currentUser);

        DischargeRecord discharge = getDischargeOrThrow(id);
        permissionService.assertCanAccessElder(currentUser, discharge.getElderId());

        int updated = dischargeRecordRepository.settlementIfPending(id, request.getSettlementAmount(), request.getRefundAmount(), LocalDateTime.now());
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending -> settling");
        }
        return getDischargeOrThrow(id);
    }

    @Transactional
    public DischargeRecord completeDischarge(CurrentUser currentUser, Long id, CompleteDischargeRequest request) {
        requireWriteRole(currentUser);

        DischargeRecord discharge = getDischargeOrThrow(id);
        permissionService.assertCanAccessElder(currentUser, discharge.getElderId());

        LocalDateTime now = LocalDateTime.now();
        int updatedDischarge = dischargeRecordRepository.completeIfSettlingOrApproved(id, request.getActualDate(), now);
        if (updatedDischarge == 0) {
            throw badRequest("状态不匹配，仅允许 settling/approved -> completed");
        }

        int bedUpdated = bedRepository.releaseAsAvailable(discharge.getBedId());
        if (bedUpdated == 0) {
            throw badRequest("床位更新失败");
        }

        int admissionUpdated = admissionRecordRepository.markEnded(discharge.getAdmissionId(), request.getActualDate(), now);
        if (admissionUpdated == 0) {
            throw badRequest("入住记录状态更新失败");
        }

        return getDischargeOrThrow(id);
    }

    public DischargeListResponse listDischarges(CurrentUser currentUser,
                                                Long admissionId,
                                                String status,
                                                int page,
                                                int size) {
        Specification<DischargeRecord> spec = Specification.where(null);

        if (admissionId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("admissionId"), admissionId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.toLowerCase(Locale.ROOT)));
        }

        List<Long> visibleElderIds = applyPermissionFilter(currentUser);
        if (visibleElderIds != null && visibleElderIds.isEmpty()) {
            return emptyDischargePage(page, size);
        }
        if (visibleElderIds != null) {
            spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DischargeRecord> pageResult;
        try {
            pageResult = dischargeRecordRepository.findAll(spec, pageable);
        } catch (DataAccessException ex) {
            if (isUnknownColumn(ex)) {
                return listDischargesViaJdbc(admissionId, status, page, size, visibleElderIds);
            }
            throw ex;
        }

        DischargeListResponse response = new DischargeListResponse();
        response.setContent(pageResult.getContent().stream().map(DischargeListItemDTO::from).toList());
        response.setTotalElements(pageResult.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private DischargeListResponse listDischargesViaJdbc(Long admissionId,
                                                        String status,
                                                        int page,
                                                        int size,
                                                        List<Long> visibleElderIds) {
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<>();

        if (admissionId != null) {
            where.append(" and d.admission_id=?");
            args.add(admissionId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and d.status=?");
            args.add(status.toLowerCase(Locale.ROOT));
        }
        if (visibleElderIds != null) {
            where.append(" and a.elder_id in (");
            where.append("?,".repeat(Math.max(0, visibleElderIds.size())));
            where.setLength(where.length() - 1);
            where.append(")");
            args.addAll(visibleElderIds);
        }

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from discharge_records d join admission_records a on a.admission_id=d.admission_id" + where,
                Long.class,
                args.toArray()
        );
        if (total == null || total == 0L) {
            return emptyDischargePage(page, size);
        }

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(Math.max(page, 0) * size);
        List<DischargeListItemDTO> content = jdbcTemplate.query(
                "select d.discharge_id, d.admission_id, a.elder_id, a.bed_id, d.status, "
                        + "date(d.request_date) as requested_date, d.actual_date, "
                        + "d.settlement_amount, d.refund_amount, d.created_at "
                        + "from discharge_records d join admission_records a on a.admission_id=d.admission_id"
                        + where
                        + " order by d.created_at desc limit ? offset ?",
                (rs, rowNum) -> {
                    DischargeListItemDTO dto = new DischargeListItemDTO();
                    dto.setDischargeId(rs.getLong("discharge_id"));
                    dto.setAdmissionId(rs.getLong("admission_id"));
                    dto.setElderId(rs.getLong("elder_id"));
                    dto.setBedId(rs.getLong("bed_id"));
                    dto.setStatus(rs.getString("status"));
                    dto.setRequestedDate(rs.getObject("requested_date", LocalDate.class));
                    dto.setActualDate(rs.getObject("actual_date", LocalDate.class));
                    dto.setSettlementAmount(rs.getBigDecimal("settlement_amount"));
                    dto.setRefundAmount(rs.getBigDecimal("refund_amount"));
                    dto.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    return dto;
                },
                pageArgs.toArray()
        );

        DischargeListResponse response = new DischargeListResponse();
        response.setContent(content);
        response.setTotalElements(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public DischargeRecord getDischargeDetail(CurrentUser currentUser, Long id) {
        DischargeRecord record = getDischargeOrThrow(id);
        if (!currentUser.hasRole("doctor")) {
            permissionService.assertCanAccessElder(currentUser, record.getElderId());
        }
        return record;
    }

    private DischargeRecord getDischargeOrThrow(Long id) {
        return dischargeRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("退住记录不存在"));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long resolveElderId(CreateAdmissionRequest request) {
        if (request.getElderId() != null) {
            return request.getElderId();
        }
        String elderName = trimToNull(request.getElderName());
        if (elderName == null) {
            throw badRequest("elderId 或 elderName 不能为空");
        }
        List<User> elders = userRepository.findByRoleIgnoreCaseAndRealNameAndDeletedAtIsNull("elder", elderName);
        if (elders.isEmpty()) {
            throw new NotFoundException("老人不存在");
        }
        if (elders.size() > 1) {
            throw badRequest("老人姓名不唯一，请使用 elderId");
        }
        return elders.get(0).getUserId();
    }

    private Long resolveBedId(CreateAdmissionRequest request) {
        if (request.getBedId() != null) {
            return request.getBedId();
        }
        String bedCode = trimToNull(request.getBedCode());
        if (bedCode == null) {
            throw badRequest("bedId 或 bedCode 不能为空");
        }
        return bedRepository.findByBedNoIgnoreCase(bedCode)
                .map(Bed::getBedId)
                .orElseThrow(() -> new NotFoundException("床位不存在"));
    }

    private void requireWriteRole(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            return;
        }
        throw new AccessDeniedException("当前角色无写权限");
    }

    private List<Long> applyPermissionFilter(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")
                || currentUser.hasRole("doctor")
                || currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")
                || currentUser.hasRole("family")) {
            if (currentUser.hasRole("doctor")) {
                return null;
            }
            return permissionService.getVisibleElderIds(currentUser);
        }
        throw new AccessDeniedException("当前角色无权限访问");
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private boolean isUnknownColumn(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("unknown column")
                        || (lower.contains("column") && lower.contains("not found"))) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private AdmissionListResponse listAdmissionsViaJdbc(Long elderId,
                                                        String status,
                                                        int page,
                                                        int size,
                                                        List<Long> visibleElderIds) {
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<>();

        if (elderId != null) {
            where.append(" and elder_id=?");
            args.add(elderId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and status=?");
            args.add(status.toLowerCase(Locale.ROOT));
        }
        if (visibleElderIds != null) {
            where.append(" and elder_id in (");
            where.append("?,".repeat(Math.max(0, visibleElderIds.size())));
            where.setLength(where.length() - 1);
            where.append(")");
            args.addAll(visibleElderIds);
        }

        Long total = jdbcTemplate.queryForObject(
                "select count(*) from admission_records" + where,
                Long.class,
                args.toArray()
        );
        if (total == null || total == 0L) {
            return emptyAdmissionPage(page, size);
        }

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add(Math.max(page, 0) * size);
        List<AdmissionRecord> content = jdbcTemplate.query(
                "select admission_id, elder_id, bed_id, status, start_date, end_date, deposit_amount, "
                        + "created_by, created_at, updated_at "
                        + "from admission_records"
                        + where
                        + " order by created_at desc limit ? offset ?",
                (rs, rowNum) -> {
                    AdmissionRecord record = new AdmissionRecord();
                    record.setAdmissionId(rs.getLong("admission_id"));
                    record.setElderId(rs.getLong("elder_id"));
                    record.setBedId(rs.getLong("bed_id"));
                    record.setStatus(rs.getString("status"));
                    record.setStartDate(rs.getObject("start_date", LocalDate.class));
                    record.setEndDate(rs.getObject("end_date", LocalDate.class));
                    record.setDepositAmount(rs.getBigDecimal("deposit_amount"));
                    record.setCreatedBy(rs.getObject("created_by", Long.class));
                    record.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
                    record.setUpdatedAt(rs.getObject("updated_at", LocalDateTime.class));
                    return record;
                },
                pageArgs.toArray()
        );

        AdmissionListResponse response = new AdmissionListResponse();
        response.setContent(toAdmissionListItems(content));
        response.setTotalElements(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private List<AdmissionListItemDTO> toAdmissionListItems(List<AdmissionRecord> records) {
        List<AdmissionListItemDTO> items = records.stream().map(AdmissionListItemDTO::from).toList();
        if (items.isEmpty()) {
            return items;
        }

        Map<Long, User> elderMap = loadElderMap(records);
        items.forEach(item -> {
            item.setBedCode(loadBedCode(item.getBedId()));
            User elder = elderMap.get(item.getElderId());
            if (elder != null) {
                item.setElderUsername(trimToNull(elder.getUsername()));
                item.setElderName(trimToNull(elder.getRealName()));
            }
        });
        return items;
    }

    private Map<Long, User> loadElderMap(List<AdmissionRecord> records) {
        List<Long> elderIds = records.stream().map(AdmissionRecord::getElderId).filter(id -> id != null).distinct().toList();
        Map<Long, User> elderMap = new LinkedHashMap<>();
        for (User user : userRepository.findAllById(elderIds)) {
            elderMap.put(user.getUserId(), user);
        }
        return elderMap;
    }

    private String loadBedCode(Long bedId) {
        if (bedId == null) {
            return null;
        }
        FacilityBed facilityBed = facilityBedRepository.findByBedIdAndDeletedAtIsNull(bedId).orElse(null);
        if (facilityBed != null && trimToNull(facilityBed.getBedCode()) != null) {
            return trimToNull(facilityBed.getBedCode());
        }
        return bedRepository.findById(bedId)
                .map(Bed::getBedNo)
                .map(this::trimToNull)
                .orElse(null);
    }

    private AdmissionListResponse emptyAdmissionPage(int page, int size) {
        AdmissionListResponse response = new AdmissionListResponse();
        response.setContent(List.of());
        response.setTotalElements(0);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private DischargeListResponse emptyDischargePage(int page, int size) {
        DischargeListResponse response = new DischargeListResponse();
        response.setContent(List.of());
        response.setTotalElements(0);
        response.setPage(page);
        response.setSize(size);
        return response;
    }
}
