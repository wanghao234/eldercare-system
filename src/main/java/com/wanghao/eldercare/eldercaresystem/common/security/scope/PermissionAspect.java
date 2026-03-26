package com.wanghao.eldercare.eldercaresystem.common.security.scope;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUserUtils;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.medication.MedicationPlan;
import com.wanghao.eldercare.eldercaresystem.entity.rectification.Rectification;
import com.wanghao.eldercare.eldercaresystem.entity.task.Task;
import com.wanghao.eldercare.eldercaresystem.entity.visit.VisitRequest;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.medication.MedicationPlanRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.rectification.RectificationRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.task.TaskRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.visit.VisitRequestRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private final PermissionService permissionService;
    private final CurrentUserUtils currentUserUtils;
    private final AlarmRepository alarmRepository;
    private final VisitRequestRepository visitRequestRepository;
    private final TaskRepository taskRepository;
    private final RectificationRepository rectificationRepository;
    private final MedicationPlanRepository medicationPlanRepository;

    public PermissionAspect(PermissionService permissionService,
                            CurrentUserUtils currentUserUtils,
                            AlarmRepository alarmRepository,
                            VisitRequestRepository visitRequestRepository,
                            TaskRepository taskRepository,
                            RectificationRepository rectificationRepository,
                            MedicationPlanRepository medicationPlanRepository) {
        this.permissionService = permissionService;
        this.currentUserUtils = currentUserUtils;
        this.alarmRepository = alarmRepository;
        this.visitRequestRepository = visitRequestRepository;
        this.taskRepository = taskRepository;
        this.rectificationRepository = rectificationRepository;
        this.medicationPlanRepository = medicationPlanRepository;
    }

    @Around("@annotation(elderScoped)")
    public Object checkElderScoped(ProceedingJoinPoint joinPoint, ElderScoped elderScoped) throws Throwable {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        Long elderId = resolveIdArg(joinPoint, elderScoped.elderIdParam());
        permissionService.assertCanAccessElder(currentUser, elderId);
        return joinPoint.proceed();
    }

    @Around("@annotation(bizScoped)")
    public Object checkBizScoped(ProceedingJoinPoint joinPoint, BizScoped bizScoped) throws Throwable {
        CurrentUser currentUser = currentUserUtils.getCurrentUser();
        Long bizId = resolveIdArg(joinPoint, bizScoped.idParam());
        Long elderId = resolveElderIdByBiz(currentUser, bizScoped.type(), bizId);
        if (elderId != null) {
            permissionService.assertCanAccessElder(currentUser, elderId);
        }
        return joinPoint.proceed();
    }

    private Long resolveElderIdByBiz(CurrentUser currentUser, String type, Long bizId) {
        String normalizedType = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return switch (normalizedType) {
            case "alarm" -> getAlarmOrThrow(bizId).getElderId();
            case "visit" -> getVisitOrThrow(bizId).getElderId();
            case "task" -> {
                Task task = getTaskOrThrow(bizId);
                if (task.getElderId() == null) {
                    throw new AccessDeniedException("任务未关联老人，无法校验访问权限");
                }
                yield task.getElderId();
            }
            case "med_plan", "medication_plan" -> getMedicationPlanOrThrow(bizId).getElderId();
            case "rectification" -> resolveRectificationScope(currentUser, bizId);
            default -> throw new IllegalStateException("不支持的 BizScoped type: " + type);
        };
    }

    private Long resolveRectificationScope(CurrentUser currentUser, Long rectificationId) {
        Rectification rectification = rectificationRepository.findById(rectificationId)
                .orElseThrow(() -> new NotFoundException("整改记录不存在"));
        String sourceType = rectification.getSourceType() == null ? "" : rectification.getSourceType().toLowerCase(Locale.ROOT);
        if ("alarm".equals(sourceType)) {
            return getAlarmOrThrow(rectification.getSourceId()).getElderId();
        }
        if ("visit".equals(sourceType)) {
            return getVisitOrThrow(rectification.getSourceId()).getElderId();
        }
        if ("task".equals(sourceType)) {
            Task task = getTaskOrThrow(rectification.getSourceId());
            if (task.getElderId() != null) {
                return task.getElderId();
            }
        }
        if ("elder".equals(sourceType)) {
            return rectification.getSourceId();
        }
        if (Objects.equals(rectification.getOwnerId(), currentUser.getUserId())) {
            return null;
        }
        throw new AccessDeniedException("整改记录未关联可访问的老人数据");
    }

    private Alarm getAlarmOrThrow(Long alarmId) {
        return alarmRepository.findById(alarmId).orElseThrow(() -> new NotFoundException("报警不存在"));
    }

    private VisitRequest getVisitOrThrow(Long visitId) {
        return visitRequestRepository.findById(visitId).orElseThrow(() -> new NotFoundException("探视申请不存在"));
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("任务不存在"));
    }

    private MedicationPlan getMedicationPlanOrThrow(Long planId) {
        return medicationPlanRepository.findById(planId).orElseThrow(() -> new NotFoundException("用药计划不存在"));
    }

    private Long resolveIdArg(ProceedingJoinPoint joinPoint, String idParam) {
        Object raw = resolveParamValue(joinPoint, idParam);
        if (raw == null) {
            throw new IllegalStateException("未找到参数: " + idParam);
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String value && !value.isBlank()) {
            return Long.parseLong(value);
        }
        throw new IllegalStateException("参数 " + idParam + " 不是可识别的 ID 类型");
    }

    private Object resolveParamValue(ProceedingJoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (paramName.equals(parameterNames[i])) {
                    return args[i];
                }
            }
        }

        for (Object arg : args) {
            Object nested = readProperty(arg, paramName);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private Object readProperty(Object target, String name) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        try {
            Method getter = type.getMethod("get" + capitalize(name));
            return getter.invoke(target);
        } catch (Exception ignored) {
        }
        try {
            Method getter = type.getMethod("is" + capitalize(name));
            return getter.invoke(target);
        } catch (Exception ignored) {
        }

        Class<?> searchType = type;
        while (searchType != null && searchType != Object.class) {
            try {
                Field field = searchType.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (Exception ignored) {
                searchType = searchType.getSuperclass();
            }
        }
        return null;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
