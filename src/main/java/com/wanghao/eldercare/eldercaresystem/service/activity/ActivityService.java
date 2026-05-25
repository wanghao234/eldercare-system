package com.wanghao.eldercare.eldercaresystem.service.activity;

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
import com.wanghao.eldercare.eldercaresystem.controller.activity.*;
import com.wanghao.eldercare.eldercaresystem.dto.activity.*;
import com.wanghao.eldercare.eldercaresystem.entity.activity.*;
import com.wanghao.eldercare.eldercaresystem.mapper.activity.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private static final Set<String> PARTICIPANT_STATUSES = Set.of("signed", "checked_in", "cancelled");
    private static final DateTimeFormatter AI_ACTIVITY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ActivityRepository activityRepository;
    private final ActivityParticipantRepository activityParticipantRepository;
    private final PermissionService permissionService;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityParticipantRepository activityParticipantRepository,
                           PermissionService permissionService) {
        this.activityRepository = activityRepository;
        this.activityParticipantRepository = activityParticipantRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public ActivityPageResponse<ActivityDTO> listActivities(LocalDateTime from,
                                                            LocalDateTime to,
                                                            int page,
                                                            int size) {
        Specification<Activity> spec = Specification.where(null);
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("activityTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("activityTime"), to));
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("activityTime"), Sort.Order.desc("activityId")));
        Page<ActivityDTO> dtoPage = activityRepository.findAll(spec, pageable).map(ActivityDTO::from);
        return toPage(dtoPage, page, size);
    }

    @Transactional
    public ActivityDTO createActivity(CurrentUser currentUser, ActivityUpsertRequest request) {
        Activity activity = new Activity();
        applyUpsert(activity, request);
        activity.setCreatedBy(currentUser.getUserId());
        activity.setCreatedAt(LocalDateTime.now());
        return ActivityDTO.from(activityRepository.save(activity));
    }

    @Transactional
    public int confirmBatchActivities(Integer operatorId, String operatorRole, List<AiActivityFormVO> activityForms) {
        CurrentUser currentUser = new CurrentUser(operatorId.longValue(), "ai_voice", operatorRole);
        for (int i = 0; i < activityForms.size(); i++) {
            AiActivityFormVO form = activityForms.get(i);
            validateAiActivityForm(form, i);

            ActivityUpsertRequest request = new ActivityUpsertRequest();
            request.setTitle(form.getActivityName().trim());
            request.setActivityTime(parseAiActivityTime(form.getActivityTime().trim(), i));
            request.setLocation(trimToNull(form.getActivityLocation()));
            request.setDescription(trimToNull(form.getActivityDescription()));
            createActivity(currentUser, request);
        }
        return activityForms.size();
    }

    @Transactional
    public ActivityDTO updateActivity(Long id, ActivityUpsertRequest request) {
        Activity activity = getActivityOrThrow(id);
        applyUpsert(activity, request);
        return ActivityDTO.from(activityRepository.save(activity));
    }

    @Transactional
    public void deleteActivity(Long id) {
        Activity activity = getActivityOrThrow(id);
        activityRepository.delete(activity);
    }

    @Transactional
    public ActivityParticipantDTO signup(CurrentUser currentUser, Long activityId, Long elderId) {
        getActivityOrThrow(activityId);
        permissionService.assertCanAccessElder(currentUser, elderId);

        ActivityParticipant existing = activityParticipantRepository.findByActivityIdAndElderId(activityId, elderId).orElse(null);
        if (existing != null) {
            if ("cancelled".equals(existing.getStatus())) {
                existing.setStatus("signed");
                return ActivityParticipantDTO.from(activityParticipantRepository.save(existing));
            }
            throw badRequest("该老人已报名活动");
        }

        ActivityParticipant participant = new ActivityParticipant();
        participant.setActivityId(activityId);
        participant.setElderId(elderId);
        participant.setStatus("signed");
        participant.setCreatedAt(LocalDateTime.now());
        try {
            return ActivityParticipantDTO.from(activityParticipantRepository.save(participant));
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("报名冲突(activity_id, elder_id)");
        }
    }

    @Transactional
    public ActivityBatchSignupResponse signupBatch(CurrentUser currentUser, Long activityId, List<Long> elderIds) {
        getActivityOrThrow(activityId);

        List<Long> effectiveElderIds = resolveBatchElderIds(currentUser, elderIds);
        ActivityBatchSignupResponse response = new ActivityBatchSignupResponse();
        List<ActivityBatchSignupResponse.ActivityBatchSignupResultItem> items = new ArrayList<>();

        int successCount = 0;
        int failCount = 0;
        for (Long elderId : effectiveElderIds) {
            ActivityBatchSignupResponse.ActivityBatchSignupResultItem item = new ActivityBatchSignupResponse.ActivityBatchSignupResultItem();
            item.setElderId(elderId);
            try {
                ActivityParticipantDTO participant = signup(currentUser, activityId, elderId);
                item.setSuccess(true);
                item.setMessage("报名成功");
                item.setParticipant(participant);
                successCount++;
            } catch (Exception ex) {
                item.setSuccess(false);
                item.setMessage(ex.getMessage());
                failCount++;
            }
            items.add(item);
        }
        response.setSuccessCount(successCount);
        response.setFailCount(failCount);
        response.setItems(items);
        return response;
    }

    @Transactional
    public ActivityParticipantDTO checkIn(CurrentUser currentUser, Long activityId, Long elderId) {
        getActivityOrThrow(activityId);
        permissionService.assertCanAccessElder(currentUser, elderId);
        ActivityParticipant participant = getParticipantOrThrow(activityId, elderId);

        String currentStatus = normalizeStatus(participant.getStatus());
        if ("checked_in".equals(currentStatus)) {
            return ActivityParticipantDTO.from(participant);
        }
        if (!"signed".equals(currentStatus)) {
            throw badRequest("当前状态不允许签到: " + currentStatus);
        }
        participant.setStatus("checked_in");
        return ActivityParticipantDTO.from(activityParticipantRepository.save(participant));
    }

    @Transactional
    public ActivityParticipantDTO cancel(CurrentUser currentUser, Long activityId, Long elderId) {
        getActivityOrThrow(activityId);
        permissionService.assertCanAccessElder(currentUser, elderId);
        ActivityParticipant participant = getParticipantOrThrow(activityId, elderId);

        if ("cancelled".equals(normalizeStatus(participant.getStatus()))) {
            return ActivityParticipantDTO.from(participant);
        }
        participant.setStatus("cancelled");
        return ActivityParticipantDTO.from(activityParticipantRepository.save(participant));
    }

    @Transactional(readOnly = true)
    public ActivityPageResponse<ActivityParticipantDTO> listParticipants(Long activityId, int page, int size) {
        getActivityOrThrow(activityId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<ActivityParticipantDTO> dtoPage = activityParticipantRepository.findByActivityId(activityId, pageable)
                .map(ActivityParticipantDTO::from);
        return toPage(dtoPage, page, size);
    }

    @Transactional(readOnly = true)
    public ActivityParticipantStatsResponse participantStats(Long activityId) {
        getActivityOrThrow(activityId);

        long signedCount = activityParticipantRepository.countByActivityIdAndStatus(activityId, "signed");
        long checkedInCount = activityParticipantRepository.countByActivityIdAndStatus(activityId, "checked_in");
        long cancelledCount = activityParticipantRepository.countByActivityIdAndStatus(activityId, "cancelled");
        long totalCount = activityParticipantRepository.countByActivityId(activityId);

        ActivityParticipantStatsResponse response = new ActivityParticipantStatsResponse();
        response.setSignedCount(signedCount);
        response.setCheckedInCount(checkedInCount);
        response.setCancelledCount(cancelledCount);
        response.setTotalCount(totalCount);
        response.setParticipantCount(signedCount + checkedInCount);
        return response;
    }

    private Activity getActivityOrThrow(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("活动不存在"));
    }

    private ActivityParticipant getParticipantOrThrow(Long activityId, Long elderId) {
        return activityParticipantRepository.findByActivityIdAndElderId(activityId, elderId)
                .orElseThrow(() -> new NotFoundException("报名记录不存在"));
    }

    private List<Long> resolveBatchElderIds(CurrentUser currentUser, List<Long> elderIds) {
        List<Long> normalized = normalizeElderIds(elderIds);
        if (!normalized.isEmpty()) {
            return normalized;
        }

        List<Long> visibleElderIds = permissionService.getVisibleElderIds(currentUser);
        if (visibleElderIds == null) {
            throw badRequest("elderIds 不能为空");
        }
        if (visibleElderIds.isEmpty()) {
            throw badRequest("当前账号未绑定任何老人");
        }
        return visibleElderIds;
    }

    private List<Long> normalizeElderIds(List<Long> elderIds) {
        if (elderIds == null || elderIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(elderIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    private void applyUpsert(Activity activity, ActivityUpsertRequest request) {
        activity.setTitle(request.getTitle().trim());
        activity.setDescription(trimToNull(request.getDescription()));
        activity.setActivityTime(request.getActivityTime());
        activity.setLocation(trimToNull(request.getLocation()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!PARTICIPANT_STATUSES.contains(normalized)) {
            throw badRequest("活动报名状态非法");
        }
        return normalized;
    }

    private void validateAiActivityForm(AiActivityFormVO form, int index) {
        int rowNumber = index + 1;
        if (form == null) {
            throw badRequest("第" + rowNumber + "条活动数据不能为空");
        }
        if (form.getActivityName() == null || form.getActivityName().isBlank()) {
            throw badRequest("第" + rowNumber + "条活动名称不能为空");
        }
        if (form.getActivityTime() == null || form.getActivityTime().isBlank()) {
            throw badRequest("第" + rowNumber + "条活动时间不能为空");
        }
    }

    private LocalDateTime parseAiActivityTime(String value, int index) {
        try {
            return LocalDateTime.parse(value, AI_ACTIVITY_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw badRequest("第" + (index + 1) + "条活动时间格式错误，要求 yyyy-MM-dd HH:mm:ss");
        }
    }

    private <T> ActivityPageResponse<T> toPage(Page<T> pageData, int page, int size) {
        ActivityPageResponse<T> response = new ActivityPageResponse<>();
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
