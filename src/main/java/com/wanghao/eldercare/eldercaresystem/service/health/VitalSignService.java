package com.wanghao.eldercare.eldercaresystem.service.health;

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
import com.wanghao.eldercare.eldercaresystem.controller.health.*;
import com.wanghao.eldercare.eldercaresystem.dto.health.*;
import com.wanghao.eldercare.eldercaresystem.entity.health.*;
import com.wanghao.eldercare.eldercaresystem.mapper.health.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VitalSignService {
    private static final String APPLE_WATCH_DEVICE_TYPE = "apple_watch";
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
        assertCanRecord(user);
        validateRequest(request);

        VitalSignRecord entity = new VitalSignRecord();
        entity.setElderId(request.getElderId());
        entity.setRecordTime(request.getRecordTime() == null ? LocalDateTime.now() : request.getRecordTime());
        applyMeasurements(entity, request.getHeartRate(), request.getSystolicBp(), request.getDiastolicBp(),
                request.getSpo2(), request.getTemperature(), request.getBloodGlucose());
        entity.setSource(request.getSource() == null ? "manual" : normalize(request.getSource()));
        entity.setRecordedBy(user.getUserId());
        entity.setNote(request.getNote());
        entity.setCreatedAt(LocalDateTime.now());
        return vitalRepo.save(entity);
    }

    @Transactional
    public VitalSignRecord createFromAppleWatch(CurrentUser user, CreateAppleWatchVitalSignRequest request) {
        assertAppleWatchUploader(user, request.getElderId());
        validateRequest(request);

        VitalSignRecord entity = new VitalSignRecord();
        entity.setElderId(request.getElderId());
        entity.setRecordTime(request.getRecordTime() == null ? LocalDateTime.now() : request.getRecordTime());
        applyMeasurements(entity, request.getHeartRate(), request.getSystolicBp(), request.getDiastolicBp(),
                request.getSpo2(), request.getTemperature(), request.getBloodGlucose());
        entity.setSource("device");
        entity.setDeviceType(APPLE_WATCH_DEVICE_TYPE);
        entity.setDeviceId(normalize(request.getDeviceId()));
        entity.setDeviceName(trimToNull(request.getDeviceName()));
        entity.setRecordedBy(user.getUserId());
        entity.setNote(request.getNote());
        entity.setCreatedAt(LocalDateTime.now());
        return vitalRepo.save(entity);
    }

    @Transactional
    public VitalSignRecord update(CurrentUser user, Long vitalId, CreateVitalSignRequest request) {
        VitalSignRecord entity = vitalRepo.findById(vitalId).orElseThrow(() -> new NotFoundException("体征记录不存在"));
        permissionService.assertCanAccessElder(user, entity.getElderId());
        assertCanRecord(user);
        assertElderUnchanged(entity.getElderId(), request.getElderId());
        validateRequest(request);

        entity.setRecordTime(request.getRecordTime() == null ? entity.getRecordTime() : request.getRecordTime());
        applyMeasurements(entity, request.getHeartRate(), request.getSystolicBp(), request.getDiastolicBp(),
                request.getSpo2(), request.getTemperature(), request.getBloodGlucose());
        entity.setSource(request.getSource() == null ? "manual" : normalize(request.getSource()));
        entity.setNote(request.getNote());
        return vitalRepo.save(entity);
    }

    @Transactional
    public void delete(CurrentUser user, Long vitalId) {
        VitalSignRecord entity = vitalRepo.findById(vitalId).orElseThrow(() -> new NotFoundException("体征记录不存在"));
        permissionService.assertCanAccessElder(user, entity.getElderId());
        assertCanRecord(user);
        vitalRepo.delete(entity);
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

    @Transactional(readOnly = true)
    public List<VitalSignRecord> listByDate(CurrentUser user, Long elderId, LocalDate date) {
        return listByRange(user, elderId, date.atStartOfDay(), date.atTime(LocalTime.MAX));
    }

    @Transactional(readOnly = true)
    public List<VitalSignRecord> listAll(CurrentUser user, Long elderId) {
        if (elderId != null) {
            permissionService.assertCanAccessElder(user, elderId);
            return vitalRepo.findByElderIdOrderByRecordTimeAsc(elderId);
        }
        List<Long> visibleElderIds = permissionService.getVisibleElderIds(user);
        if (visibleElderIds == null) {
            return vitalRepo.findAll(Sort.by(Sort.Direction.ASC, "recordTime"));
        }
        if (visibleElderIds.isEmpty()) {
            return List.of();
        }
        return vitalRepo.findByElderIdInOrderByRecordTimeAsc(visibleElderIds);
    }

    private void assertCanRecord(CurrentUser user) {
        if (user.hasRole("admin") || user.hasRole("nurse_leader") || user.hasRole("nurse") || user.hasRole("caregiver")) {
            return;
        }
        throw new AccessDeniedException("当前角色无权录入体征");
    }

    private void assertAppleWatchUploader(CurrentUser user, Long elderId) {
        if (!user.hasRole("elder")) {
            throw new AccessDeniedException("当前角色无权通过Apple Watch上传体征");
        }
        permissionService.assertCanAccessElder(user, elderId);
        if (!user.getUserId().equals(elderId)) {
            throw new AccessDeniedException("Apple Watch仅允许上传当前老人账号本人的体征");
        }
    }

    private void assertElderUnchanged(Long currentElderId, Long requestElderId) {
        if (!currentElderId.equals(requestElderId)) {
            throw badRequest("elderId不允许修改");
        }
    }

    private void validateRequest(CreateVitalSignRequest request) {
        if (request.getSource() != null && !SOURCE_VALUES.contains(normalize(request.getSource()))) {
            throw badRequest("source仅支持 manual/device/system");
        }
        validateMeasurements(request.getSpo2(), request.getTemperature(), request.getHeartRate(),
                request.getSystolicBp(), request.getDiastolicBp());
    }

    private void validateRequest(CreateAppleWatchVitalSignRequest request) {
        if (trimToNull(request.getDeviceId()) == null) {
            throw badRequest("deviceId不能为空");
        }
        validateMeasurements(request.getSpo2(), request.getTemperature(), request.getHeartRate(),
                request.getSystolicBp(), request.getDiastolicBp());
    }

    private void validateMeasurements(Integer spo2,
                                      Double temperature,
                                      Integer heartRate,
                                      Integer systolicBp,
                                      Integer diastolicBp) {
        if (spo2 != null && (spo2 < 0 || spo2 > 100)) {
            throw badRequest("spo2需在0~100之间");
        }
        if (temperature != null && (temperature < 34 || temperature > 43)) {
            throw badRequest("temperature需在34~43之间");
        }
        if (heartRate != null && (heartRate < 20 || heartRate > 260)) {
            throw badRequest("heartRate需在20~260之间");
        }
        if (systolicBp != null && (systolicBp < 60 || systolicBp > 260)) {
            throw badRequest("systolicBp需在60~260之间");
        }
        if (diastolicBp != null && (diastolicBp < 30 || diastolicBp > 180)) {
            throw badRequest("diastolicBp需在30~180之间");
        }
    }

    private void applyMeasurements(VitalSignRecord entity,
                                   Integer heartRate,
                                   Integer systolicBp,
                                   Integer diastolicBp,
                                   Integer spo2,
                                   Double temperature,
                                   Double bloodGlucose) {
        entity.setHeartRate(heartRate);
        entity.setSystolicBp(systolicBp);
        entity.setDiastolicBp(diastolicBp);
        entity.setSpo2(spo2);
        entity.setTemperature(temperature);
        entity.setBloodGlucose(bloodGlucose);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
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
