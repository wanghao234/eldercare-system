package com.wanghao.eldercare.eldercaresystem.health;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.security.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class VitalSignService {
    private static final Set<String> SOURCE_VALUES = Set.of("manual", "device", "system");

    private final VitalSignRecordRepository vitalRepo;
    private final PermissionService permissionService;

    public VitalSignService(VitalSignRecordRepository vitalRepo, PermissionService permissionService) {
        this.vitalRepo = vitalRepo;
        this.permissionService = permissionService;
    }

    @Transactional
    public VitalSignRecord create(CurrentUser user, CreateVitalSignRequest request) {
        permissionService.assertCanAccessElder(user, request.getElderId());
        if (!(user.hasRole("admin") || user.hasRole("nurse_leader") || user.hasRole("nurse") || user.hasRole("caregiver"))) {
            throw new AccessDeniedException("当前角色无权录入体征");
        }
        validateRequest(request);

        VitalSignRecord entity = new VitalSignRecord();
        entity.setElderId(request.getElderId());
        entity.setRecordTime(request.getRecordTime() == null ? LocalDateTime.now() : request.getRecordTime());
        entity.setHeartRate(request.getHeartRate());
        entity.setSystolicBp(request.getSystolicBp());
        entity.setDiastolicBp(request.getDiastolicBp());
        entity.setSpo2(request.getSpo2());
        entity.setTemperature(request.getTemperature());
        entity.setBloodGlucose(request.getBloodGlucose());
        entity.setSource(request.getSource() == null ? "manual" : normalize(request.getSource()));
        entity.setRecordedBy(user.getUserId());
        entity.setNote(request.getNote());
        entity.setCreatedAt(LocalDateTime.now());
        return vitalRepo.save(entity);
    }

    @Transactional(readOnly = true)
    public List<VitalSignRecord> listByRange(CurrentUser user, Long elderId, LocalDateTime from, LocalDateTime to) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return vitalRepo.findByElderIdAndRecordTimeBetweenOrderByRecordTimeAsc(elderId, from, to);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return vitalRepo.findByRecordTimeBetweenOrderByRecordTimeAsc(from, to);
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return vitalRepo.findByElderIdInAndRecordTimeBetweenOrderByRecordTimeAsc(visibleElderIds, from, to);
    }

    private void validateRequest(CreateVitalSignRequest request) {
        if (request.getSource() != null && !SOURCE_VALUES.contains(normalize(request.getSource()))) {
            throw badRequest("source仅支持 manual/device/system");
        }
        if (request.getSpo2() != null && (request.getSpo2() < 0 || request.getSpo2() > 100)) {
            throw badRequest("spo2需在0~100之间");
        }
        if (request.getTemperature() != null && (request.getTemperature() < 34 || request.getTemperature() > 43)) {
            throw badRequest("temperature需在34~43之间");
        }
        if (request.getHeartRate() != null && (request.getHeartRate() < 20 || request.getHeartRate() > 260)) {
            throw badRequest("heartRate需在20~260之间");
        }
        if (request.getSystolicBp() != null && (request.getSystolicBp() < 60 || request.getSystolicBp() > 260)) {
            throw badRequest("systolicBp需在60~260之间");
        }
        if (request.getDiastolicBp() != null && (request.getDiastolicBp() < 30 || request.getDiastolicBp() > 180)) {
            throw badRequest("diastolicBp需在30~180之间");
        }
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message, HttpStatus.BAD_REQUEST);
    }
}
