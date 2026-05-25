package com.wanghao.eldercare.eldercaresystem.service.digitaltwin;

import com.wanghao.eldercare.eldercaresystem.dto.digitaltwin.DigitalTwinAlarmPointVO;
import com.wanghao.eldercare.eldercaresystem.dto.digitaltwin.DigitalTwinCameraPointVO;
import com.wanghao.eldercare.eldercaresystem.dto.digitaltwin.DigitalTwinMapResponse;
import com.wanghao.eldercare.eldercaresystem.entity.admission.Room;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.Alarm;
import com.wanghao.eldercare.eldercaresystem.entity.alarm.AlarmActionLog;
import com.wanghao.eldercare.eldercaresystem.entity.digitaltwin.DigitalTwinMap;
import com.wanghao.eldercare.eldercaresystem.entity.facility.CameraDevice;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.RoomRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmActionLogRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.alarm.AlarmRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.digitaltwin.DigitalTwinMapRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.CameraDeviceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalTwinService {

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinService.class);

    private static final Set<String> ACTIVE_ALARM_STATUS = Set.of("created", "accepted", "on_site", "handling");
    private static final Set<String> VALID_ALARM_TYPE = Set.of("call_emergency", "call_normal", "fall", "vital_abnormal", "wander", "other");
    private static final Set<String> VALID_SEVERITY = Set.of("info", "warning", "critical");
    private static final Set<String> VALID_ALARM_STATUS = Set.of("created", "accepted", "on_site", "handling", "closed", "cancelled");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DigitalTwinMapRepository digitalTwinMapRepository;
    private final CameraDeviceRepository cameraDeviceRepository;
    private final AlarmRepository alarmRepository;
    private final AlarmActionLogRepository alarmActionLogRepository;
    private final RoomRepository roomRepository;

    public DigitalTwinService(DigitalTwinMapRepository digitalTwinMapRepository,
                              CameraDeviceRepository cameraDeviceRepository,
                              AlarmRepository alarmRepository,
                              AlarmActionLogRepository alarmActionLogRepository,
                              RoomRepository roomRepository) {
        this.digitalTwinMapRepository = digitalTwinMapRepository;
        this.cameraDeviceRepository = cameraDeviceRepository;
        this.alarmRepository = alarmRepository;
        this.alarmActionLogRepository = alarmActionLogRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public DigitalTwinMapResponse getCurrentMap() {
        DigitalTwinMap map = loadCurrentMap();

        List<CameraDevice> cameras = loadMapCameras(map);
        Map<Long, CameraDevice> cameraById = cameras.stream()
                .collect(Collectors.toMap(CameraDevice::getCameraId, camera -> camera, (left, right) -> left, LinkedHashMap::new));

        List<Alarm> alarms = loadActiveAlarms(map, cameraById.values());
        Map<Long, String> alarmNoteMap = loadAlarmNoteMap(alarms);

        DigitalTwinMapResponse response = new DigitalTwinMapResponse();
        response.setMapId(map.getMapId());
        response.setMapName(defaultIfBlank(map.getMapName(), "默认监控地图"));
        response.setBuildingName(defaultIfBlank(map.getBuildingName(), "默认楼栋"));
        response.setFloorNo(map.getFloorNo() == null ? 1 : map.getFloorNo());
        response.setMapImage(defaultIfBlank(map.getMapImage(), "/maps/floor1.png"));
        response.setWidth(map.getWidth() == null ? 1000 : map.getWidth());
        response.setHeight(map.getHeight() == null ? 600 : map.getHeight());
        response.setCameras(cameras.stream().map(this::toCameraPoint).toList());
        response.setActiveAlarms(alarms.stream()
                .map(alarm -> toAlarmPoint(alarm, cameraById.get(alarm.getCameraId()), alarmNoteMap.get(alarm.getAlarmId())))
                .sorted(Comparator.comparing(DigitalTwinAlarmPointVO::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
        return response;
    }

    private DigitalTwinMap loadCurrentMap() {
        try {
            return digitalTwinMapRepository.findFirstByStatusIgnoreCaseOrderByMapIdAsc("enabled")
                    .orElseGet(this::buildFallbackMap);
        } catch (DataAccessException ex) {
            log.warn("Failed to load digital twin map configuration, using fallback map", ex);
            return buildFallbackMap();
        }
    }

    private List<CameraDevice> loadMapCameras(DigitalTwinMap map) {
        List<CameraDevice> allCameras = cameraDeviceRepository.findAll().stream()
                .filter(camera -> camera.getMapX() != null && camera.getMapY() != null)
                .sorted(Comparator.comparing(CameraDevice::getCameraId))
                .toList();
        if (map.getFloorId() == null) {
            return allCameras;
        }

        Map<Long, Room> roomById = roomRepository.findAllById(extractNonNullRoomIds(allCameras)).stream()
                .collect(Collectors.toMap(Room::getRoomId, room -> room));

        List<CameraDevice> scoped = allCameras.stream()
                .filter(camera -> camera.getRoomId() != null)
                .filter(camera -> {
                    Room room = roomById.get(camera.getRoomId());
                    return room != null && Objects.equals(room.getFloorId(), map.getFloorId());
                })
                .toList();
        return scoped.isEmpty() ? allCameras : scoped;
    }

    private List<Alarm> loadActiveAlarms(DigitalTwinMap map, Collection<CameraDevice> cameras) {
        Set<Long> scopedCameraIds = cameras.stream()
                .map(CameraDevice::getCameraId)
                .collect(Collectors.toSet());

        Specification<Alarm> spec = (root, query, cb) -> root.get("status").in(ACTIVE_ALARM_STATUS);
        if (map.getFloorId() != null && !scopedCameraIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.or(
                    root.get("cameraId").in(scopedCameraIds),
                    cb.and(cb.isNull(root.get("cameraId")), cb.isNotNull(root.get("mapX")), cb.isNotNull(root.get("mapY")))
            ));
        }
        return alarmRepository.findAll(spec).stream()
                .sorted(Comparator.comparing(Alarm::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Map<Long, String> loadAlarmNoteMap(List<Alarm> alarms) {
        if (alarms.isEmpty()) {
            return Map.of();
        }

        List<Long> alarmIds = alarms.stream().map(Alarm::getAlarmId).toList();
        List<AlarmActionLog> logs = alarmActionLogRepository.findByAlarmIdInOrderByActionTimeDescLogIdDesc(alarmIds);

        Map<Long, String> latestNonBlankNote = new HashMap<>();
        Map<Long, String> createNote = new HashMap<>();
        for (AlarmActionLog log : logs) {
            if (log.getNote() == null || log.getNote().isBlank()) {
                continue;
            }
            latestNonBlankNote.putIfAbsent(log.getAlarmId(), log.getNote());
            if ("create".equalsIgnoreCase(log.getAction())) {
                createNote.putIfAbsent(log.getAlarmId(), log.getNote());
            }
        }

        Map<Long, String> result = new HashMap<>();
        for (Long alarmId : alarmIds) {
            result.put(alarmId, createNote.getOrDefault(alarmId, latestNonBlankNote.get(alarmId)));
        }
        return result;
    }

    private DigitalTwinCameraPointVO toCameraPoint(CameraDevice camera) {
        DigitalTwinCameraPointVO vo = new DigitalTwinCameraPointVO();
        vo.setCameraId(camera.getCameraId());
        vo.setCameraName(camera.getCameraName());
        vo.setLocationText(camera.getLocationText());
        vo.setMapX(camera.getMapX());
        vo.setMapY(camera.getMapY());
        vo.setStatus(normalizeCameraStatus(camera.getStatus()));
        return vo;
    }

    private DigitalTwinAlarmPointVO toAlarmPoint(Alarm alarm, CameraDevice camera, String note) {
        DigitalTwinAlarmPointVO vo = new DigitalTwinAlarmPointVO();
        vo.setAlarmId(alarm.getAlarmId());
        vo.setCameraId(alarm.getCameraId());
        vo.setAlarmType(normalizeAlarmType(alarm.getAlarmType()));
        vo.setSeverity(normalizeSeverity(alarm.getSeverity()));
        vo.setStatus(normalizeAlarmStatus(alarm.getStatus()));
        vo.setLocationText(firstNonBlank(alarm.getLocationText(), camera == null ? null : camera.getLocationText()));
        vo.setMapX(firstNonNull(alarm.getMapX(), camera == null ? null : camera.getMapX()));
        vo.setMapY(firstNonNull(alarm.getMapY(), camera == null ? null : camera.getMapY()));
        vo.setSnapshotUrl(alarm.getSnapshotUrl());
        vo.setCreatedAt(formatTime(alarm.getCreatedAt()));
        vo.setNote(note);
        return vo;
    }

    private DigitalTwinMap buildFallbackMap() {
        DigitalTwinMap map = new DigitalTwinMap();
        map.setMapId(0L);
        map.setMapName("默认监控地图");
        map.setBuildingName("默认楼栋");
        map.setFloorNo(1);
        map.setMapImage("/maps/floor1.png");
        map.setWidth(1000);
        map.setHeight(600);
        map.setStatus("enabled");
        return map;
    }

    private List<Long> extractNonNullRoomIds(List<CameraDevice> cameras) {
        List<Long> roomIds = new ArrayList<>();
        for (CameraDevice camera : cameras) {
            if (camera.getRoomId() != null) {
                roomIds.add(camera.getRoomId());
            }
        }
        return roomIds;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private String normalizeAlarmType(String value) {
        String normalized = normalizeLower(value);
        return VALID_ALARM_TYPE.contains(normalized) ? normalized : "other";
    }

    private String normalizeSeverity(String value) {
        String normalized = normalizeLower(value);
        return VALID_SEVERITY.contains(normalized) ? normalized : "warning";
    }

    private String normalizeAlarmStatus(String value) {
        String normalized = normalizeLower(value);
        return VALID_ALARM_STATUS.contains(normalized) ? normalized : "created";
    }

    private String normalizeCameraStatus(String value) {
        String normalized = normalizeLower(value);
        if ("error".equals(normalized)) {
            return "fault";
        }
        if ("online".equals(normalized) || "offline".equals(normalized)
                || "fault".equals(normalized) || "disabled".equals(normalized)) {
            return normalized;
        }
        return "offline";
    }

    private String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }
}
