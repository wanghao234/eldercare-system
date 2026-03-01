package com.wanghao.eldercare.eldercaresystem.activity;

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
import java.util.Locale;
import java.util.Set;

@Service
public class ActivityService {

    private static final Set<String> PARTICIPANT_STATUSES = Set.of("signed", "checked_in", "cancelled");

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

    private Activity getActivityOrThrow(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("活动不存在"));
    }

    private ActivityParticipant getParticipantOrThrow(Long activityId, Long elderId) {
        return activityParticipantRepository.findByActivityIdAndElderId(activityId, elderId)
                .orElseThrow(() -> new NotFoundException("报名记录不存在"));
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

