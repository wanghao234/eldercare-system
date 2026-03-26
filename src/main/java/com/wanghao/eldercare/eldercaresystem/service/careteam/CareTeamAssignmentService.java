package com.wanghao.eldercare.eldercaresystem.service.careteam;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.common.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careteam.*;
import com.wanghao.eldercare.eldercaresystem.dto.careteam.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareTeamAssignmentService {

    private static final Set<String> NURSE_ROLES = Set.of("nurse", "caregiver");

    private final CareTeamAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public CareTeamAssignmentService(CareTeamAssignmentRepository assignmentRepository, UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CareTeamAssignmentPageResponse list(Long elderId,
                                               Long nurseId,
                                               Long familyId,
                                               Integer isActive,
                                               int page,
                                               int size) {
        Specification<CareTeamAssignment> spec = Specification.where(null);
        if (elderId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("elderId"), elderId));
        }
        if (nurseId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("nurseId"), nurseId));
        }
        if (familyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("familyId"), familyId));
        }
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), isActive));
        }

        Page<CareTeamAssignment> result = assignmentRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt", "assignmentId"))
        );

        CareTeamAssignmentPageResponse response = new CareTeamAssignmentPageResponse();
        response.setContent(result.getContent().stream().map(CareTeamAssignmentDTO::from).toList());
        response.setTotalElements(result.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public CareTeamAssignmentDTO detail(Long id) {
        return CareTeamAssignmentDTO.from(getOrThrow(id));
    }

    @Transactional
    public CareTeamAssignmentDTO create(CreateCareTeamAssignmentRequest request) {
        validateIds(request.getElderId(), request.getNurseId(), request.getFamilyId());
        Integer isActive = normalizeIsActiveOrDefault(request.getIsActive(), 1);
        ensureSingleActiveFamily(request.getElderId(), request.getFamilyId(), isActive, null);

        CareTeamAssignment entity = new CareTeamAssignment();
        entity.setElderId(request.getElderId());
        entity.setNurseId(request.getNurseId());
        entity.setFamilyId(request.getFamilyId());
        entity.setIsActive(isActive);
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            return CareTeamAssignmentDTO.from(assignmentRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "绑定关系写入失败，请检查唯一约束或外键约束", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public CareTeamAssignmentDTO update(Long id, UpdateCareTeamAssignmentRequest request) {
        CareTeamAssignment entity = getOrThrow(id);
        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(request.getUnbindNurse())) {
            if (entity.getNurseId() == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "当前绑定记录不存在护士可解绑", HttpStatus.BAD_REQUEST);
            }
            entity.setIsActive(0);
            entity.setUpdatedAt(now);
            try {
                return CareTeamAssignmentDTO.from(assignmentRepository.saveAndFlush(entity));
            } catch (DataIntegrityViolationException ex) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "绑定关系更新失败，请检查唯一约束或外键约束", HttpStatus.BAD_REQUEST);
            }
        }

        Long elderId = request.getElderId() == null ? entity.getElderId() : request.getElderId();
        Long nurseId = request.getNurseId() == null ? entity.getNurseId() : request.getNurseId();
        Long familyId = request.getFamilyId() == null ? entity.getFamilyId() : request.getFamilyId();
        validateIds(elderId, nurseId, familyId);

        if (request.getElderId() != null) {
            entity.setElderId(request.getElderId());
        }
        if (request.getNurseId() != null) {
            entity.setNurseId(request.getNurseId());
        }
        if (request.getFamilyId() != null) {
            entity.setFamilyId(request.getFamilyId());
        }
        if (request.getIsActive() != null) {
            entity.setIsActive(normalizeIsActive(request.getIsActive()));
        }
        ensureSingleActiveFamily(entity.getElderId(), entity.getFamilyId(), entity.getIsActive(), entity.getAssignmentId());
        entity.setUpdatedAt(now);
        try {
            return CareTeamAssignmentDTO.from(assignmentRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "绑定关系更新失败，请检查唯一约束或外键约束", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public void delete(Long id) {
        int changed = assignmentRepository.deactivateById(id, LocalDateTime.now());
        if (changed == 0) {
            throw new NotFoundException("绑定关系不存在");
        }
    }

    @Audited(action = AuditAction.UPDATE, entityType = "care_team_assignment", responseIdPath = "assignmentId")
    @Transactional
    public CareTeamAssignmentDTO upsertNurseByElderId(Long elderId, Long nurseId) {
        validateUserRole(elderId, "elder");
        User nurse = findUserOrThrow(nurseId);
        String role = normalizeRole(nurse.getRole());
        if (!NURSE_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "nurseId 对应用户角色必须是 nurse/caregiver", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        CareTeamAssignment assignment = assignmentRepository
                .findFirstByElderIdAndIsActiveOrderByAssignmentIdAsc(elderId, 1)
                .orElseGet(() -> {
                    CareTeamAssignment created = new CareTeamAssignment();
                    created.setElderId(elderId);
                    created.setIsActive(1);
                    created.setCreatedAt(now);
                    return created;
                });
        assignment.setNurseId(nurseId);
        assignment.setUpdatedAt(now);
        return CareTeamAssignmentDTO.from(assignmentRepository.saveAndFlush(assignment));
    }

    @Audited(action = AuditAction.UPDATE, entityType = "care_team_assignment")
    @Transactional
    public List<CareTeamAssignmentDTO> upsertNursesByElderId(Long elderId, List<Long> nurseIds) {
        validateUserRole(elderId, "elder");
        List<Long> normalizedNurseIds = normalizeUserIds(nurseIds);
        if (normalizedNurseIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "nurseIds 不能为空", HttpStatus.BAD_REQUEST);
        }
        for (Long nurseId : normalizedNurseIds) {
            User nurse = findUserOrThrow(nurseId);
            String role = normalizeRole(nurse.getRole());
            if (!NURSE_ROLES.contains(role)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "nurseId 对应用户角色必须是 nurse/caregiver", HttpStatus.BAD_REQUEST);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<CareTeamAssignment> activeAssignments = assignmentRepository
                .findAllByElderIdAndIsActiveAndNurseIdIsNotNullOrderByAssignmentIdAsc(elderId, 1);
        Map<Long, CareTeamAssignment> activeByNurseId = activeAssignments.stream()
                .collect(Collectors.toMap(CareTeamAssignment::getNurseId, v -> v, (a, b) -> a));
        Set<Long> targetNurseSet = new LinkedHashSet<>(normalizedNurseIds);
        List<CareTeamAssignment> changed = new ArrayList<>();

        for (CareTeamAssignment assignment : activeAssignments) {
            if (!targetNurseSet.contains(assignment.getNurseId())) {
                assignment.setIsActive(0);
                assignment.setUpdatedAt(now);
                changed.add(assignment);
            }
        }

        for (Long nurseId : normalizedNurseIds) {
            CareTeamAssignment existing = activeByNurseId.get(nurseId);
            if (existing != null) {
                existing.setUpdatedAt(now);
                changed.add(existing);
                continue;
            }
            CareTeamAssignment created = new CareTeamAssignment();
            created.setElderId(elderId);
            created.setNurseId(nurseId);
            created.setIsActive(1);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            changed.add(created);
        }

        if (!changed.isEmpty()) {
            assignmentRepository.saveAll(changed);
            assignmentRepository.flush();
        }

        return assignmentRepository.findAllByElderIdAndIsActiveAndNurseIdIsNotNullOrderByAssignmentIdAsc(elderId, 1)
                .stream()
                .map(CareTeamAssignmentDTO::from)
                .toList();
    }

    @Audited(action = AuditAction.UPDATE, entityType = "care_team_assignment")
    @Transactional
    public List<CareTeamAssignmentDTO> upsertFamiliesByElderId(Long elderId, List<Long> familyIds) {
        validateUserRole(elderId, "elder");
        List<Long> normalizedFamilyIds = normalizeUserIds(familyIds);
        if (normalizedFamilyIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "familyIds 不能为空", HttpStatus.BAD_REQUEST);
        }
        if (normalizedFamilyIds.size() > 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "一个老人只能绑定一个家属", HttpStatus.BAD_REQUEST);
        }
        for (Long familyId : normalizedFamilyIds) {
            validateUserRole(familyId, "family");
        }

        LocalDateTime now = LocalDateTime.now();
        List<CareTeamAssignment> activeAssignments = assignmentRepository
                .findAllByElderIdAndIsActiveAndFamilyIdIsNotNullOrderByAssignmentIdAsc(elderId, 1);
        Map<Long, CareTeamAssignment> activeByFamilyId = activeAssignments.stream()
                .collect(Collectors.toMap(CareTeamAssignment::getFamilyId, v -> v, (a, b) -> a));
        Set<Long> targetFamilySet = new LinkedHashSet<>(normalizedFamilyIds);
        List<CareTeamAssignment> changed = new ArrayList<>();

        for (CareTeamAssignment assignment : activeAssignments) {
            if (!targetFamilySet.contains(assignment.getFamilyId())) {
                assignment.setIsActive(0);
                assignment.setUpdatedAt(now);
                changed.add(assignment);
            }
        }

        for (Long familyId : normalizedFamilyIds) {
            CareTeamAssignment existing = activeByFamilyId.get(familyId);
            if (existing != null) {
                existing.setUpdatedAt(now);
                changed.add(existing);
                continue;
            }
            CareTeamAssignment created = new CareTeamAssignment();
            created.setElderId(elderId);
            created.setFamilyId(familyId);
            created.setIsActive(1);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            changed.add(created);
        }

        if (!changed.isEmpty()) {
            assignmentRepository.saveAll(changed);
            assignmentRepository.flush();
        }

        return assignmentRepository.findAllByElderIdAndIsActiveAndFamilyIdIsNotNullOrderByAssignmentIdAsc(elderId, 1)
                .stream()
                .map(CareTeamAssignmentDTO::from)
                .toList();
    }

    private void ensureSingleActiveFamily(Long elderId, Long familyId, Integer isActive, Long excludeAssignmentId) {
        if (elderId == null || familyId == null || isActive == null || isActive != 1) {
            return;
        }
        boolean existsOther = excludeAssignmentId == null
                ? assignmentRepository.existsActiveFamilyByElderId(elderId)
                : assignmentRepository.existsActiveFamilyByElderIdAndAssignmentIdNot(elderId, excludeAssignmentId);
        if (existsOther) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "一个老人只能绑定一个家属", HttpStatus.BAD_REQUEST);
        }
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> deduplicated = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                deduplicated.add(userId);
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private void validateIds(Long elderId, Long nurseId, Long familyId) {
        if (nurseId == null && familyId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "nurseId 和 familyId 不能同时为空", HttpStatus.BAD_REQUEST);
        }
        validateUserRole(elderId, "elder");
        if (nurseId != null) {
            User nurse = findUserOrThrow(nurseId);
            String role = normalizeRole(nurse.getRole());
            if (!NURSE_ROLES.contains(role)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "nurseId 对应用户角色必须是 nurse/caregiver", HttpStatus.BAD_REQUEST);
            }
        }
        if (familyId != null) {
            validateUserRole(familyId, "family");
        }
    }

    private void validateUserRole(Long userId, String expectedRole) {
        User user = findUserOrThrow(userId);
        if (!expectedRole.equals(normalizeRole(user.getRole()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户角色不匹配: 期望 " + expectedRole, HttpStatus.BAD_REQUEST);
        }
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    private CareTeamAssignment getOrThrow(Long id) {
        return assignmentRepository.findByAssignmentId(id)
                .orElseThrow(() -> new NotFoundException("绑定关系不存在"));
    }

    private int normalizeIsActiveOrDefault(Integer isActive, int defaultValue) {
        return isActive == null ? defaultValue : normalizeIsActive(isActive);
    }

    private int normalizeIsActive(Integer isActive) {
        if (isActive == null || (isActive != 0 && isActive != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "isActive 仅支持 0/1", HttpStatus.BAD_REQUEST);
        }
        return isActive;
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.toLowerCase(Locale.ROOT);
    }
}
