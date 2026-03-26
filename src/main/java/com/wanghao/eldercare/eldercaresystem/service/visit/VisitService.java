package com.wanghao.eldercare.eldercaresystem.service.visit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.wanghao.eldercare.eldercaresystem.controller.visit.*;
import com.wanghao.eldercare.eldercaresystem.dto.visit.*;
import com.wanghao.eldercare.eldercaresystem.entity.visit.*;
import com.wanghao.eldercare.eldercaresystem.mapper.visit.*;
import com.wanghao.eldercare.eldercaresystem.service.task.TaskIntegrationService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
public class VisitService {

    private final VisitRequestRepository visitRequestRepository;
    private final VisitRequestLogRepository visitRequestLogRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final TaskIntegrationService taskIntegrationService;
    private final JdbcTemplate jdbcTemplate;

    public VisitService(VisitRequestRepository visitRequestRepository,
                        VisitRequestLogRepository visitRequestLogRepository,
                        PermissionService permissionService,
                        ObjectMapper objectMapper,
                        TaskIntegrationService taskIntegrationService,
                        JdbcTemplate jdbcTemplate) {
        this.visitRequestRepository = visitRequestRepository;
        this.visitRequestLogRepository = visitRequestLogRepository;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
        this.taskIntegrationService = taskIntegrationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public VisitCreateResponse create(CurrentUser currentUser, CreateVisitRequest request) {
        if (!currentUser.hasRole("family")) {
            throw new AccessDeniedException("仅家属可发起探视/外出申请");
        }

        permissionService.assertCanAccessElder(currentUser, request.getElderId());

        LocalDateTime now = LocalDateTime.now();
        VisitRequest visitRequest = new VisitRequest();
        visitRequest.setElderId(request.getElderId());
        visitRequest.setFamilyId(currentUser.getUserId());
        visitRequest.setRequestType(normalize(request.getRequestType()));
        visitRequest.setPlannedStartAt(request.getPlannedStartAt());
        visitRequest.setPlannedEndAt(request.getPlannedEndAt());
        visitRequest.setDestination(request.getDestination());
        visitRequest.setReason(request.getReason());
        visitRequest.setCompanionCount(request.getCompanionCount());
        visitRequest.setStatus("pending");
        visitRequest.setExtraJson(toJson(request.getExtraJson()));
        visitRequest.setCreatedAt(now);
        visitRequest.setUpdatedAt(now);

        VisitRequest saved = visitRequestRepository.save(visitRequest);
        writeLog(saved.getRequestId(), "create", currentUser.getUserId(), "发起申请", visitRequest.getExtraJson());
        return new VisitCreateResponse(saved.getRequestId());
    }

    public VisitListResponse list(CurrentUser currentUser,
                                  Long elderId,
                                  String status,
                                  String requestType,
                                  LocalDateTime from,
                                  LocalDateTime to,
                                  int page,
                                  int size) {
        Specification<VisitRequest> spec = Specification.where(null);

        if (elderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalize(status)));
        }
        if (requestType != null && !requestType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("requestType"), normalize(requestType)));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            // no permission filter
        } else if (currentUser.hasRole("family")) {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("familyId"), currentUser.getUserId()));
            if (visibleElderIds != null && visibleElderIds.isEmpty()) {
                return emptyPage(page, size);
            }
            if (visibleElderIds != null) {
                spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
            }
        } else if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds != null && visibleElderIds.isEmpty()) {
                return emptyPage(page, size);
            }
            if (visibleElderIds != null) {
                spec = spec.and((root, query, cb) -> root.get("elderId").in(visibleElderIds));
            }
        } else {
            throw new AccessDeniedException("当前角色无权限访问探视模块");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VisitRequest> result;
        try {
            result = visitRequestRepository.findAll(spec, pageable);
        } catch (RuntimeException ex) {
            if (isVisitSchemaMismatch(ex)) {
                return listByJdbcCompatibility(currentUser, elderId, status, requestType, from, to, page, size);
            }
            throw ex;
        }

        VisitListResponse response = new VisitListResponse();
        response.setContent(result.getContent().stream().map(VisitListItemDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public VisitDetailDTO detail(CurrentUser currentUser, Long id) {
        VisitRequest visitRequest = getOrThrow(id);
        assertCanAccess(currentUser, visitRequest);

        VisitDetailDTO detail = VisitDetailDTO.from(visitRequest);
        detail.setLogs(visitRequestLogRepository.findByRequestIdOrderByActionTimeAsc(id)
                .stream().map(VisitLogDTO::from).toList());
        return detail;
    }

    @Transactional
    public VisitDetailDTO confirm(CurrentUser currentUser, Long id, VisitActionRequest request) {
        requireAnyRole(currentUser, "nurse", "caregiver", "admin", "nurse_leader");
        VisitRequest visitRequest = getOrThrow(id);
        assertCanAccess(currentUser, visitRequest);

        LocalDateTime now = LocalDateTime.now();
        int updated = visitRequestRepository.confirmIfPending(id, now, currentUser.getUserId());
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending -> confirmed");
        }

        writeLog(id, "confirm", currentUser.getUserId(), request == null ? null : request.getComment(),
                request == null ? null : toJson(request.getExtraJson()));
        return detail(currentUser, id);
    }

    @Transactional
    public VisitDetailDTO approve(CurrentUser currentUser, Long id, VisitActionRequest request) {
        requireAnyRole(currentUser, "admin", "nurse_leader");
        VisitRequest visitRequest = getOrThrow(id);
        assertCanAccess(currentUser, visitRequest);

        LocalDateTime now = LocalDateTime.now();
        int updated = visitRequestRepository.approveIfConfirmed(id, now, currentUser.getUserId());
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 confirmed -> approved");
        }

        taskIntegrationService.createVisitHandoverTask(getOrThrow(id), currentUser.getUserId());
        writeLog(id, "approve", currentUser.getUserId(), request == null ? null : request.getComment(),
                request == null ? null : toJson(request.getExtraJson()));
        writeLog(id, "task_create", currentUser.getUserId(), "生成交接任务: 物品/药物清单核对", "{\"task\":\"visit_handover\"}");
        writeLog(id, "task_create", currentUser.getUserId(), "生成交接任务: 归院确认", "{\"task\":\"visit_return_confirm\"}");
        return detail(currentUser, id);
    }

    @Transactional
    public VisitDetailDTO reject(CurrentUser currentUser, Long id, VisitActionRequest request) {
        requireAnyRole(currentUser, "nurse", "caregiver", "admin", "nurse_leader");
        VisitRequest visitRequest = getOrThrow(id);
        assertCanAccess(currentUser, visitRequest);

        LocalDateTime now = LocalDateTime.now();
        String reason = request == null ? null : request.getReason();
        int updated = visitRequestRepository.rejectIfPendingOrConfirmed(id, now, currentUser.getUserId(), reason);
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending/confirmed -> rejected");
        }

        writeLog(id, "reject", currentUser.getUserId(), request == null ? null : request.getComment(),
                request == null ? null : toJson(request.getExtraJson()));
        return detail(currentUser, id);
    }

    @Transactional
    public VisitDetailDTO checkOut(CurrentUser currentUser, Long id, VisitActionRequest request) {
        requireAnyRole(currentUser, "nurse", "caregiver", "admin", "nurse_leader");
        VisitRequest visitRequest = getOrThrow(id);
        assertCanAccess(currentUser, visitRequest);

        LocalDateTime now = LocalDateTime.now();
        int updated = visitRequestRepository.checkOutIfApproved(id, now, currentUser.getUserId());
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 approved -> in_progress");
        }

        writeLog(id, "check_out", currentUser.getUserId(), request == null ? null : request.getComment(),
                request == null ? null : toJson(request.getExtraJson()));
        return detail(currentUser, id);
    }

    @Transactional
    public VisitDetailDTO checkIn(CurrentUser currentUser, Long id, VisitActionRequest request) {
        requireAnyRole(currentUser, "nurse", "caregiver", "admin", "nurse_leader");
        VisitRequest visitRequest = getOrThrow(id);
        assertCanAccess(currentUser, visitRequest);

        LocalDateTime now = LocalDateTime.now();
        int updated = visitRequestRepository.checkInIfInProgress(id, now, currentUser.getUserId());
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 in_progress -> completed");
        }

        writeLog(id, "check_in", currentUser.getUserId(), request == null ? null : request.getComment(),
                request == null ? null : toJson(request.getExtraJson()));
        return detail(currentUser, id);
    }

    @Transactional
    public VisitDetailDTO cancel(CurrentUser currentUser, Long id, VisitActionRequest request) {
        VisitRequest visitRequest = getOrThrow(id);
        assertCanAccess(currentUser, visitRequest);

        if (currentUser.hasRole("family") && !visitRequest.getFamilyId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("仅申请家属可取消申请");
        }

        if (!(currentUser.hasRole("family") || currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")
                || currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            throw new AccessDeniedException("当前角色无权限取消申请");
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = request == null ? null : request.getReason();
        int updated = visitRequestRepository.cancelIfPendingConfirmedApproved(id, now, currentUser.getUserId(), reason);
        if (updated == 0) {
            throw badRequest("状态不匹配，仅允许 pending/confirmed/approved -> cancelled");
        }

        writeLog(id, "cancel", currentUser.getUserId(), request == null ? null : request.getComment(),
                request == null ? null : toJson(request.getExtraJson()));
        return detail(currentUser, id);
    }

    private VisitRequest getOrThrow(Long id) {
        return visitRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("探视/外出申请不存在"));
    }

    private void assertCanAccess(CurrentUser currentUser, VisitRequest visitRequest) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            return;
        }

        if (currentUser.hasRole("family")) {
            if (!visitRequest.getFamilyId().equals(currentUser.getUserId())) {
                throw new AccessDeniedException("无权访问其他家属申请");
            }
            permissionService.assertCanAccessElder(currentUser, visitRequest.getElderId());
            return;
        }

        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            permissionService.assertCanAccessElder(currentUser, visitRequest.getElderId());
            return;
        }

        throw new AccessDeniedException("当前角色无权限访问探视模块");
    }

    private void writeLog(Long requestId, String action, Long actorId, String comment, String extraJson) {
        VisitRequestLog log = new VisitRequestLog();
        log.setRequestId(requestId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setActionTime(LocalDateTime.now());
        log.setComment(comment);
        log.setExtraJson(extraJson);
        visitRequestLogRepository.save(log);
    }

    private String normalize(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw badRequest("JSON 序列化失败");
        }
    }

    private void requireAnyRole(CurrentUser currentUser, String... roles) {
        for (String role : roles) {
            if (currentUser.hasRole(role)) {
                return;
            }
        }
        throw new AccessDeniedException("当前角色无权限执行该动作");
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private VisitListResponse emptyPage(int page, int size) {
        VisitListResponse response = new VisitListResponse();
        response.setContent(List.of());
        response.setTotalElements(0);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private boolean isVisitSchemaMismatch(Throwable ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && message.contains("Unknown column")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private VisitListResponse listByJdbcCompatibility(CurrentUser currentUser,
                                                      Long elderId,
                                                      String status,
                                                      String requestType,
                                                      LocalDateTime from,
                                                      LocalDateTime to,
                                                      int page,
                                                      int size) {
        String startColumn = resolvePreferredColumn("planned_start_at", "start_time");
        String endColumn = resolvePreferredColumn("planned_end_at", "end_time");
        String destinationSelect = hasColumn("destination") ? "destination" : "NULL";

        StringBuilder where = new StringBuilder(" where 1=1 ");
        List<Object> params = new ArrayList<>();

        if (elderId != null) {
            where.append(" and elder_id = ? ");
            params.add(elderId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" and status = ? ");
            params.add(normalize(status));
        }
        if (requestType != null && !requestType.isBlank()) {
            where.append(" and request_type = ? ");
            params.add(normalize(requestType));
        }
        if (from != null) {
            where.append(" and created_at >= ? ");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            where.append(" and created_at <= ? ");
            params.add(Timestamp.valueOf(to));
        }

        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            // full access
        } else if (currentUser.hasRole("family")) {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            where.append(" and family_id = ? ");
            params.add(currentUser.getUserId());
            if (visibleElderIds != null && visibleElderIds.isEmpty()) {
                return emptyPage(page, size);
            }
            appendInClause(where, params, "elder_id", visibleElderIds);
        } else if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
            if (visibleElderIds != null && visibleElderIds.isEmpty()) {
                return emptyPage(page, size);
            }
            appendInClause(where, params, "elder_id", visibleElderIds);
        } else {
            throw new AccessDeniedException("当前角色无权限访问探视模块");
        }

        String countSql = "select count(*) from visit_requests " + where;
        Long total = jdbcTemplate.queryForObject(countSql, params.toArray(), Long.class);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;

        String dataSql = "select request_id, elder_id, family_id, request_type, status, created_at, "
                + startColumn + " as planned_start_at, "
                + endColumn + " as planned_end_at, "
                + destinationSelect + " as destination "
                + "from visit_requests "
                + where
                + " order by created_at desc limit ? offset ?";

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(safeSize);
        dataParams.add(offset);

        List<VisitListItemDTO> items = jdbcTemplate.query(dataSql, dataParams.toArray(), (rs, rowNum) -> {
            VisitListItemDTO dto = new VisitListItemDTO();
            dto.setRequestId(rs.getLong("request_id"));
            dto.setElderId(rs.getLong("elder_id"));
            dto.setFamilyId(rs.getLong("family_id"));
            dto.setRequestType(rs.getString("request_type"));
            dto.setStatus(rs.getString("status"));

            Timestamp plannedStart = rs.getTimestamp("planned_start_at");
            if (plannedStart != null) {
                dto.setPlannedStartAt(plannedStart.toLocalDateTime());
            }
            Timestamp plannedEnd = rs.getTimestamp("planned_end_at");
            if (plannedEnd != null) {
                dto.setPlannedEndAt(plannedEnd.toLocalDateTime());
            }
            dto.setDestination(rs.getString("destination"));

            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                dto.setCreatedAt(createdAt.toLocalDateTime());
            }
            return dto;
        });

        VisitListResponse response = new VisitListResponse();
        response.setContent(items);
        response.setTotalElements(total == null ? 0 : total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private String resolvePreferredColumn(String preferred, String fallback) {
        return hasColumn(preferred) ? preferred : fallback;
    }

    private boolean hasColumn(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'visit_requests' and column_name = ?",
                Integer.class,
                columnName
        );
        return count != null && count > 0;
    }

    private void appendInClause(StringBuilder where, List<Object> params, String column, List<Long> values) {
        if (values == null) {
            return;
        }
        if (values.isEmpty()) {
            where.append(" and 1 = 0 ");
            return;
        }
        where.append(" and ").append(column).append(" in (")
                .append(String.join(",", Collections.nCopies(values.size(), "?")))
                .append(") ");
        params.addAll(values);
    }
}
