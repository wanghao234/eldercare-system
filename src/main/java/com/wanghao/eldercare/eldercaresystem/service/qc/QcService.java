package com.wanghao.eldercare.eldercaresystem.service.qc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.qc.*;
import com.wanghao.eldercare.eldercaresystem.dto.qc.*;
import com.wanghao.eldercare.eldercaresystem.entity.qc.*;
import com.wanghao.eldercare.eldercaresystem.entity.rectification.Rectification;
import com.wanghao.eldercare.eldercaresystem.mapper.qc.*;
import com.wanghao.eldercare.eldercaresystem.mapper.rectification.RectificationRepository;
import java.time.LocalDateTime;
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
public class QcService {

    private final QcAuditRepository qcAuditRepository;
    private final QcAuditItemRepository qcAuditItemRepository;
    private final QcIssueRepository qcIssueRepository;
    private final RectificationRepository rectificationRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    public QcService(QcAuditRepository qcAuditRepository,
                     QcAuditItemRepository qcAuditItemRepository,
                     QcIssueRepository qcIssueRepository,
                     RectificationRepository rectificationRepository,
                     ObjectMapper objectMapper,
                     JdbcTemplate jdbcTemplate) {
        this.qcAuditRepository = qcAuditRepository;
        this.qcAuditItemRepository = qcAuditItemRepository;
        this.qcIssueRepository = qcIssueRepository;
        this.rectificationRepository = rectificationRepository;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public IdResponse createAudit(CurrentUser currentUser, CreateQcAuditRequest request) {
        ensureAdminOrLeader(currentUser);

        QcAudit audit = new QcAudit();
        audit.setElderId(request.getElderId());
        audit.setTitle(request.getTitle());
        audit.setStatus("running");
        audit.setCreatedBy(currentUser.getUserId());
        audit.setCreatedAt(LocalDateTime.now());
        audit.setUpdatedAt(LocalDateTime.now());

        QcAudit saved = qcAuditRepository.save(audit);
        return new IdResponse(saved.getAuditId());
    }

    public QcAuditListResponse listAudits(CurrentUser currentUser, int page, int size) {
        ensureAdminOrLeader(currentUser);

        Page<QcAudit> result;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        try {
            result = qcAuditRepository.findAll(pageable);
        } catch (RuntimeException ex) {
            if (isSchemaMismatch(ex)) {
                return listAuditsByJdbc(page, size);
            }
            throw ex;
        }

        QcAuditListResponse response = new QcAuditListResponse();
        response.setContent(result.getContent().stream().map(QcAuditDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public QcAuditDTO getAuditDetail(CurrentUser currentUser, Long id) {
        ensureAdminOrLeader(currentUser);

        QcAudit audit = qcAuditRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("抽查记录不存在"));

        QcAuditDTO dto = QcAuditDTO.from(audit);
        dto.setItems(qcAuditItemRepository.findByAuditIdOrderByItemIdAsc(id)
                .stream().map(QcAuditItemDTO::from).toList());
        return dto;
    }

    @Transactional
    public QcAuditItemDTO checkItem(CurrentUser currentUser,
                                    Long auditId,
                                    Long itemId,
                                    CheckQcItemRequest request) {
        ensureAdminOrLeader(currentUser);

        QcAudit audit = qcAuditRepository.findById(auditId)
                .orElseThrow(() -> new NotFoundException("抽查记录不存在"));
        if (audit == null) {
            throw new NotFoundException("抽查记录不存在");
        }

        QcAuditItem item = qcAuditItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("抽查项不存在"));
        if (!auditId.equals(item.getAuditId())) {
            throw badRequest("item 不属于该 audit");
        }

        String result = normalizeResult(request.getResult());
        item.setResult(result);
        item.setIssues(request.getIssues());
        item.setEvidenceJson(toJson(request.getEvidenceJson()));
        item.setCheckedBy(currentUser.getUserId());
        item.setCheckedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());

        return QcAuditItemDTO.from(qcAuditItemRepository.save(item));
    }

    @Transactional
    public QcIssueDTO createIssue(CurrentUser currentUser, CreateQcIssueRequest request) {
        ensureAdminOrLeader(currentUser);

        QcAuditItem item = qcAuditItemRepository.findById(request.getQcItemId())
                .orElseThrow(() -> new NotFoundException("抽查项不存在"));
        if (!"fail".equalsIgnoreCase(item.getResult())) {
            throw badRequest("仅 fail 抽查项可创建问题单");
        }

        QcAudit audit = qcAuditRepository.findById(item.getAuditId())
                .orElseThrow(() -> new NotFoundException("抽查记录不存在"));

        LocalDateTime now = LocalDateTime.now();
        QcIssue issue = new QcIssue();
        issue.setQcItemId(item.getItemId());
        issue.setAuditId(item.getAuditId());
        issue.setElderId(audit.getElderId());
        issue.setLevel(request.getLevel().toLowerCase(Locale.ROOT));
        issue.setDescription(request.getDescription());
        issue.setResponsibleId(request.getResponsibleId());
        issue.setStatus("open");
        issue.setCreatedBy(currentUser.getUserId());
        issue.setCreatedAt(now);
        issue.setUpdatedAt(now);

        QcIssue savedIssue = qcIssueRepository.save(issue);

        Rectification rectification = new Rectification();
        rectification.setSourceType("qc");
        rectification.setSourceId(savedIssue.getIssueId());
        rectification.setTitle("质控问题整改");
        rectification.setDescription(savedIssue.getDescription());
        rectification.setLevel(savedIssue.getLevel());
        rectification.setOwnerId(savedIssue.getResponsibleId());
        rectification.setDueAt(now.plusDays(7));
        rectification.setStatus("open");
        rectification.setCreatedBy(currentUser.getUserId());
        rectification.setCreatedAt(now);
        rectification.setUpdatedAt(now);

        Rectification savedRectification = rectificationRepository.save(rectification);

        savedIssue.setRectificationId(savedRectification.getRectificationId());
        savedIssue.setStatus("rectifying");
        savedIssue.setUpdatedAt(LocalDateTime.now());
        return QcIssueDTO.from(qcIssueRepository.save(savedIssue));
    }

    public List<QcIssueDTO> listIssues(CurrentUser currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<QcIssue> result;
        try {
            if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
                result = qcIssueRepository.findAll(pageable);
            } else {
                Specification<QcIssue> spec = (root, query, cb) -> cb.equal(root.get("responsibleId"), currentUser.getUserId());
                result = qcIssueRepository.findAll(spec, pageable);
            }
        } catch (RuntimeException ex) {
            if (isSchemaMismatch(ex)) {
                return listIssuesByJdbc(currentUser, page, size);
            }
            throw ex;
        }

        return result.getContent().stream().map(QcIssueDTO::from).toList();
    }

    private void ensureAdminOrLeader(CurrentUser currentUser) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            return;
        }
        throw new AccessDeniedException("当前用户无质控权限");
    }

    private String normalizeResult(String result) {
        String value = result.toLowerCase(Locale.ROOT);
        if (!"pass".equals(value) && !"fail".equals(value)) {
            throw badRequest("result 仅支持 pass/fail");
        }
        return value;
    }

    private String toJson(Object node) {
        if (node == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw badRequest("evidenceJson 序列化失败");
        }
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }

    private boolean isSchemaMismatch(Throwable ex) {
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

    private QcAuditListResponse listAuditsByJdbc(int page, int size) {
        String idColumn = hasColumn("qc_audit_id") ? "qc_audit_id" : "audit_id";
        String titleColumn = hasColumn("audit_type") ? "audit_type" : "title";
        String elderColumn = hasColumn("elder_id") ? "elder_id" : "NULL";

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;

        Long total = jdbcTemplate.queryForObject("select count(*) from qc_audits", Long.class);
        String sql = "select "
                + idColumn + " as audit_id, "
                + elderColumn + " as elder_id, "
                + titleColumn + " as title, "
                + "status, created_by, created_at "
                + "from qc_audits order by created_at desc limit ? offset ?";

        var items = jdbcTemplate.query(sql, new Object[]{safeSize, offset}, (rs, rowNum) -> {
            QcAuditDTO dto = new QcAuditDTO();
            dto.setAuditId(rs.getLong("audit_id"));
            Object elderVal = rs.getObject("elder_id");
            dto.setElderId(elderVal == null ? null : ((Number) elderVal).longValue());
            dto.setTitle(rs.getString("title"));
            dto.setStatus(rs.getString("status"));
            dto.setCreatedBy(rs.getLong("created_by"));
            var createdAt = rs.getTimestamp("created_at");
            dto.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return dto;
        });

        QcAuditListResponse response = new QcAuditListResponse();
        response.setContent(items);
        response.setTotalElements(total == null ? 0 : total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private boolean hasColumn(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'qc_audits' and column_name = ?",
                Integer.class,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean hasIssueColumn(String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns "
                        + "where table_schema = database() and table_name = 'qc_issues' and column_name = ?",
                Integer.class,
                columnName
        );
        return count != null && count > 0;
    }

    private List<QcIssueDTO> listIssuesByJdbc(CurrentUser currentUser, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int offset = safePage * safeSize;

        String auditColumn = hasIssueColumn("audit_id") ? "audit_id" : "NULL";
        String elderColumn = hasIssueColumn("elder_id") ? "elder_id" : "NULL";
        String createdByColumn = hasIssueColumn("created_by") ? "created_by" : "NULL";
        String updatedColumn = hasIssueColumn("updated_at") ? "updated_at" : "created_at";

        StringBuilder sql = new StringBuilder("select issue_id, qc_item_id, ")
                .append(auditColumn).append(" as audit_id, ")
                .append(elderColumn).append(" as elder_id, ")
                .append("level, description, responsible_id, status, rectification_id, ")
                .append(createdByColumn).append(" as created_by, ")
                .append("created_at, ").append(updatedColumn).append(" as updated_at ")
                .append("from qc_issues ");

        Object[] args;
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            sql.append("order by created_at desc limit ? offset ?");
            args = new Object[]{safeSize, offset};
        } else {
            sql.append("where responsible_id = ? order by created_at desc limit ? offset ?");
            args = new Object[]{currentUser.getUserId(), safeSize, offset};
        }

        return jdbcTemplate.query(sql.toString(), args, (rs, rowNum) -> {
            QcIssueDTO dto = new QcIssueDTO();
            dto.setIssueId(rs.getLong("issue_id"));
            dto.setQcItemId(rs.getLong("qc_item_id"));

            Object auditVal = rs.getObject("audit_id");
            dto.setAuditId(auditVal == null ? null : ((Number) auditVal).longValue());
            Object elderVal = rs.getObject("elder_id");
            dto.setElderId(elderVal == null ? null : ((Number) elderVal).longValue());

            dto.setLevel(rs.getString("level"));
            dto.setDescription(rs.getString("description"));
            dto.setResponsibleId(rs.getLong("responsible_id"));
            dto.setStatus(rs.getString("status"));

            Object rectVal = rs.getObject("rectification_id");
            dto.setRectificationId(rectVal == null ? null : ((Number) rectVal).longValue());
            Object createdByVal = rs.getObject("created_by");
            dto.setCreatedBy(createdByVal == null ? null : ((Number) createdByVal).longValue());

            var createdAt = rs.getTimestamp("created_at");
            dto.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return dto;
        });
    }
}
