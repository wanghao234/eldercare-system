package com.wanghao.eldercare.eldercaresystem.service.facility;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.facility.*;
import com.wanghao.eldercare.eldercaresystem.dto.facility.*;
import com.wanghao.eldercare.eldercaresystem.entity.facility.*;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityService {

    private static final Set<String> ROOM_STATUSES = Set.of("active", "maintenance", "deleted");
    private static final Set<String> BED_STATUSES = Set.of("available", "occupied", "reserved", "maintenance");

    private final FacilityBuildingRepository buildingRepository;
    private final FacilityFloorRepository floorRepository;
    private final FacilityRoomRepository roomRepository;
    private final FacilityBedRepository bedRepository;

    public FacilityService(FacilityBuildingRepository buildingRepository,
                           FacilityFloorRepository floorRepository,
                           FacilityRoomRepository roomRepository,
                           FacilityBedRepository bedRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
    }

    @Transactional(readOnly = true)
    public FacilityPageResponse<FacilityBuilding> listBuildings(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "buildingId"));
        Page<FacilityBuilding> result = (keyword == null || keyword.isBlank())
                ? buildingRepository.findByDeletedAtIsNull(pageable)
                : buildingRepository.findByBuildingNameContainingIgnoreCaseAndDeletedAtIsNull(keyword.trim(), pageable);
        return toPage(result, page, size);
    }

    @Transactional
    public FacilityBuilding createBuilding(BuildingUpsertRequest request) {
        FacilityBuilding building = new FacilityBuilding();
        building.setBuildingName(request.getBuildingName().trim());
        return saveBuilding(building);
    }

    @Transactional
    public FacilityBuilding updateBuilding(Long id, BuildingUpsertRequest request) {
        FacilityBuilding building = getBuilding(id);
        building.setBuildingName(request.getBuildingName().trim());
        return saveBuilding(building);
    }

    @Transactional
    public void deleteBuilding(Long id) {
        FacilityBuilding building = getBuilding(id);
        List<FacilityFloor> floors = floorRepository.findAllByBuildingIdAndDeletedAtIsNull(id);
        if (!floors.isEmpty()) {
            List<Long> floorIds = floors.stream().map(FacilityFloor::getFloorId).toList();
            List<FacilityRoom> rooms = roomRepository.findAllByFloorIdIn(floorIds);
            if (!rooms.isEmpty()) {
                List<Long> roomIds = rooms.stream().map(FacilityRoom::getRoomId).toList();
                if (bedRepository.existsByRoomIdInAndStatusNotAndDeletedAtIsNull(roomIds, "available")) {
                    throw badRequest("该楼栋存在非可用床位，禁止删除");
                }
                List<FacilityBed> beds = bedRepository.findAllByRoomIdInAndDeletedAtIsNull(roomIds);
                if (!beds.isEmpty()) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    for (FacilityBed bed : beds) {
                        bed.setBedCode(buildDeletedBedCode(bed));
                        bed.setDeletedAt(now);
                    }
                    bedRepository.saveAll(beds);
                }
                for (FacilityRoom room : rooms) {
                    room.setRoomNumber(buildDeletedRoomNumber(room));
                    room.setStatus("deleted");
                }
                roomRepository.saveAll(rooms);
            }
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            for (FacilityFloor floor : floors) {
                floor.setFloorNo(buildDeletedFloorNo(floor));
                floor.setFloorName(buildDeletedFloorName(floor));
                floor.setDeletedAt(now);
            }
            floorRepository.saveAll(floors);
        }
        building.setBuildingName(buildDeletedBuildingName(building));
        building.setDeletedAt(java.time.LocalDateTime.now());
        buildingRepository.save(building);
    }

    @Transactional(readOnly = true)
    public FacilityPageResponse<FacilityFloor> listFloors(Long buildingId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "floorId"));
        Page<FacilityFloor> result = buildingId == null
                ? floorRepository.findByDeletedAtIsNull(pageable)
                : floorRepository.findByBuildingIdAndDeletedAtIsNull(buildingId, pageable);
        return toPage(result, page, size);
    }

    @Transactional
    public FacilityFloor createFloor(FloorCreateRequest request) {
        getBuilding(request.getBuildingId());
        FacilityFloor floor = new FacilityFloor();
        floor.setBuildingId(request.getBuildingId());
        floor.setFloorNo(parseFloorNo(request.getFloorNo()));
        floor.setFloorName(trimToNull(request.getFloorName()));
        return saveFloor(floor);
    }

    @Transactional
    public FacilityFloor updateFloor(Long id, FloorUpdateRequest request) {
        FacilityFloor floor = getFloor(id);
        floor.setFloorNo(parseFloorNo(request.getFloorNo()));
        floor.setFloorName(trimToNull(request.getFloorName()));
        return saveFloor(floor);
    }

    @Transactional
    public void deleteFloor(Long id) {
        FacilityFloor floor = getFloor(id);
        List<FacilityRoom> rooms = roomRepository.findAllByFloorId(id);
        if (!rooms.isEmpty()) {
            List<Long> roomIds = rooms.stream().map(FacilityRoom::getRoomId).toList();
            if (bedRepository.existsByRoomIdInAndStatusNotAndDeletedAtIsNull(roomIds, "available")) {
                throw badRequest("该楼层存在非可用床位，禁止删除");
            }
            List<FacilityBed> beds = bedRepository.findAllByRoomIdInAndDeletedAtIsNull(roomIds);
            if (!beds.isEmpty()) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                for (FacilityBed bed : beds) {
                    bed.setBedCode(buildDeletedBedCode(bed));
                    bed.setDeletedAt(now);
                }
                bedRepository.saveAll(beds);
            }
            for (FacilityRoom room : rooms) {
                room.setRoomNumber(buildDeletedRoomNumber(room));
                room.setStatus("deleted");
            }
            roomRepository.saveAll(rooms);
        }
        floor.setFloorName(buildDeletedFloorName(floor));
        floor.setFloorNo(buildDeletedFloorNo(floor));
        floor.setDeletedAt(java.time.LocalDateTime.now());
        floorRepository.save(floor);
    }

    @Transactional(readOnly = true)
    public FacilityPageResponse<FacilityRoom> listRooms(Long floorId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "roomId"));
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus != null) {
            normalizedStatus = normalizeRoomStatus(normalizedStatus);
        }
        Page<FacilityRoom> result;
        if (floorId == null) {
            result = normalizedStatus == null
                    ? roomRepository.findAll(pageable)
                    : roomRepository.findByStatus(normalizedStatus, pageable);
        } else if (normalizedStatus == null) {
            result = roomRepository.findByFloorId(floorId, pageable);
        } else {
            result = roomRepository.findByFloorIdAndStatus(floorId, normalizedStatus, pageable);
        }
        return toPage(result, page, size);
    }

    @Transactional
    public FacilityRoom createRoom(RoomCreateRequest request) {
        getFloor(request.getFloorId());
        FacilityRoom room = new FacilityRoom();
        room.setFloorId(request.getFloorId());
        room.setRoomNumber(request.getRoomNumber().trim());
        room.setRoomType(trimToNull(request.getRoomType()));
        room.setNote(trimToNull(request.getNote()));
        room.setStatus("active");
        return saveRoom(room);
    }

    @Transactional
    public FacilityRoom updateRoom(Long id, RoomUpdateRequest request) {
        FacilityRoom room = getRoom(id);
        room.setRoomNumber(request.getRoomNumber().trim());
        room.setRoomType(trimToNull(request.getRoomType()));
        room.setNote(trimToNull(request.getNote()));
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            room.setStatus(normalizeRoomStatus(request.getStatus()));
        }
        return saveRoom(room);
    }

    @Transactional
    public FacilityRoom transitionRoomStatus(Long id, StatusTransitionRequest request) {
        FacilityRoom room = getRoom(id);
        String from = normalizeRoomStatus(request.getFrom());
        String to = normalizeRoomStatus(request.getTo());
        if (from.equals(to)) {
            return room;
        }
        int updated = roomRepository.transitionStatus(id, from, to);
        if (updated == 0) {
            FacilityRoom latest = getRoom(id);
            if (to.equals(latest.getStatus())) {
                return latest;
            }
            throw badRequest("状态不匹配，期望 from=" + from + "，实际=" + latest.getStatus());
        }
        return getRoom(id);
    }

    @Transactional
    public FacilityRoom softDeleteRoom(Long id) {
        FacilityRoom room = getRoom(id);
        if ("deleted".equals(room.getStatus())) {
            return room;
        }
        if (bedRepository.existsByRoomIdAndStatusAndDeletedAtIsNull(id, "occupied")) {
            throw badRequest("房间存在已占用床位，禁止删除");
        }
        List<FacilityBed> beds = bedRepository.findAllByRoomIdAndDeletedAtIsNull(id);
        if (!beds.isEmpty()) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            for (FacilityBed bed : beds) {
                bed.setBedCode(buildDeletedBedCode(bed));
                bed.setDeletedAt(now);
            }
            bedRepository.saveAll(beds);
        }
        room.setRoomNumber(buildDeletedRoomNumber(room));
        saveRoom(room);
        int updated = roomRepository.transitionStatus(id, room.getStatus(), "deleted");
        if (updated == 0) {
            return getRoom(id);
        }
        return getRoom(id);
    }

    @Transactional(readOnly = true)
    public FacilityPageResponse<FacilityBed> listBeds(Long roomId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "bedId"));
        String normalizedStatus = trimToNull(status);
        if ("all".equalsIgnoreCase(normalizedStatus)) {
            normalizedStatus = null;
        } else if (normalizedStatus != null) {
            normalizedStatus = normalizeBedStatus(normalizedStatus);
        }
        Page<FacilityBed> result;
        if (roomId == null) {
            result = normalizedStatus == null
                    ? bedRepository.findByDeletedAtIsNull(pageable)
                    : bedRepository.findByStatusAndDeletedAtIsNull(normalizedStatus, pageable);
        } else if (normalizedStatus == null) {
            result = bedRepository.findByRoomIdAndDeletedAtIsNull(roomId, pageable);
        } else {
            result = bedRepository.findByRoomIdAndStatusAndDeletedAtIsNull(roomId, normalizedStatus, pageable);
        }
        if (!result.getContent().isEmpty()) {
            List<Long> roomIds = result.getContent().stream().map(FacilityBed::getRoomId).distinct().toList();
            Map<Long, String> roomNumberMap = roomRepository.findAllByRoomIdIn(roomIds).stream()
                    .filter(room -> room.getRoomId() != null)
                    .collect(Collectors.toMap(FacilityRoom::getRoomId, FacilityRoom::getRoomNumber, (a, b) -> a));
            result.getContent().forEach(bed -> bed.setRoomNumber(roomNumberMap.get(bed.getRoomId())));
        }
        return toPage(result, page, size);
    }

    @Transactional
    public FacilityBed createBed(BedCreateRequest request) {
        getRoom(request.getRoomId());
        FacilityBed bed = new FacilityBed();
        bed.setRoomId(request.getRoomId());
        String bedCode = trimToNull(request.resolveBedCode());
        if (bedCode == null) {
            throw badRequest("bedCode不能为空");
        }
        ensureBedCodeUnique(bedCode, null);
        bed.setBedCode(bedCode);
        bed.setStatus("available");
        return saveBed(bed);
    }

    @Transactional
    public FacilityBed updateBed(Long id, BedUpdateRequest request) {
        FacilityBed bed = getBed(id);
        String bedCode = trimToNull(request.resolveBedCode());
        if (bedCode == null) {
            throw badRequest("bedCode不能为空");
        }
        ensureBedCodeUnique(bedCode, bed.getBedId());
        bed.setBedCode(bedCode);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String targetStatus = normalizeBedStatus(request.getStatus());
            ensureBedStatusMutable(bed.getStatus(), targetStatus);
            bed.setStatus(targetStatus);
        }
        return saveBed(bed);
    }

    @Transactional
    public FacilityBed transitionBedStatus(Long id, StatusTransitionRequest request) {
        FacilityBed bed = getBed(id);
        String from = normalizeBedStatus(request.getFrom());
        String to = normalizeBedStatus(request.getTo());
        if (from.equals(to)) {
            return bed;
        }
        ensureBedStatusMutable(bed.getStatus(), to);
        int updated = bedRepository.transitionStatus(id, from, to);
        if (updated == 0) {
            FacilityBed latest = getBed(id);
            if (to.equals(latest.getStatus())) {
                return latest;
            }
            throw badRequest("状态不匹配，期望 from=" + from + "，实际=" + latest.getStatus());
        }
        return getBed(id);
    }

    @Transactional
    public void deleteBed(Long id) {
        FacilityBed bed = getBed(id);
        if ("occupied".equalsIgnoreCase(bed.getStatus())
                || "reserved".equalsIgnoreCase(bed.getStatus())
                || "maintenance".equalsIgnoreCase(bed.getStatus())) {
            throw badRequest("已占用、预留或维修中床位不可删除");
        }
        if (bed.getDeletedAt() == null) {
            bed.setBedCode(buildDeletedBedCode(bed));
            bed.setDeletedAt(java.time.LocalDateTime.now());
            bedRepository.save(bed);
        }
    }

    private FacilityBuilding getBuilding(Long id) {
        return buildingRepository.findByBuildingIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("楼栋不存在"));
    }

    private FacilityFloor getFloor(Long id) {
        return floorRepository.findByFloorIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("楼层不存在"));
    }

    private FacilityRoom getRoom(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("房间不存在"));
    }

    private FacilityBed getBed(Long id) {
        return bedRepository.findByBedIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("床位不存在"));
    }

    private FacilityBuilding saveBuilding(FacilityBuilding building) {
        try {
            return buildingRepository.save(building);
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("楼栋名称冲突");
        }
    }

    private FacilityFloor saveFloor(FacilityFloor floor) {
        try {
            return floorRepository.save(floor);
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("楼层信息冲突");
        }
    }

    private FacilityRoom saveRoom(FacilityRoom room) {
        try {
            return roomRepository.save(room);
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("房间号冲突(uk_floor_room_number)");
        }
    }

    private FacilityBed saveBed(FacilityBed bed) {
        try {
            return bedRepository.save(bed);
        } catch (DataIntegrityViolationException ex) {
            throw badRequest("床位编码冲突(uk_room_bed_code)");
        }
    }

    private void ensureBedCodeUnique(String bedCode, Long excludeBedId) {
        boolean exists = excludeBedId == null
                ? bedRepository.existsActiveByBedCodeIgnoreCase(bedCode)
                : bedRepository.existsActiveByBedCodeIgnoreCaseAndBedIdNot(bedCode, excludeBedId);
        if (exists) {
            throw badRequest("床位编码已存在");
        }
    }

    private void ensureBedStatusMutable(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null || currentStatus.equalsIgnoreCase(targetStatus)) {
            return;
        }
        String current = currentStatus.toLowerCase(Locale.ROOT);
        if ("occupied".equals(current) || "reserved".equals(current)) {
            throw badRequest("已占用或预留床位不可直接修改状态");
        }
    }

    private String normalizeRoomStatus(String status) {
        String value = status.toLowerCase(Locale.ROOT);
        if (!ROOM_STATUSES.contains(value)) {
            throw badRequest("room status 非法");
        }
        return value;
    }

    private String normalizeBedStatus(String status) {
        String value = status.toLowerCase(Locale.ROOT);
        if (!BED_STATUSES.contains(value)) {
            throw badRequest("bed status 非法");
        }
        return value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildDeletedBedCode(FacilityBed bed) {
        String original = trimToNull(bed.getBedCode());
        String suffix = "#D" + bed.getBedId();
        if (original == null) {
            return "DELETED" + suffix;
        }
        int maxPrefixLen = Math.max(1, 64 - suffix.length());
        String prefix = original.length() > maxPrefixLen ? original.substring(0, maxPrefixLen) : original;
        return prefix + suffix;
    }

    private String buildDeletedRoomNumber(FacilityRoom room) {
        String original = trimToNull(room.getRoomNumber());
        String suffix = "#D" + room.getRoomId();
        if (original == null) {
            return "DELETED" + suffix;
        }
        if (original.endsWith(suffix)) {
            return original;
        }
        int maxPrefixLen = Math.max(1, 64 - suffix.length());
        String prefix = original.length() > maxPrefixLen ? original.substring(0, maxPrefixLen) : original;
        return prefix + suffix;
    }

    private String buildDeletedFloorName(FacilityFloor floor) {
        String original = trimToNull(floor.getFloorName());
        String suffix = "#D" + floor.getFloorId();
        if (original == null) {
            return "DELETED" + suffix;
        }
        if (original.endsWith(suffix)) {
            return original;
        }
        int maxPrefixLen = Math.max(1, 64 - suffix.length());
        String prefix = original.length() > maxPrefixLen ? original.substring(0, maxPrefixLen) : original;
        return prefix + suffix;
    }

    private Integer buildDeletedFloorNo(FacilityFloor floor) {
        if (floor.getFloorNo() != null && floor.getFloorNo() < 0) {
            return floor.getFloorNo();
        }
        Long floorId = floor.getFloorId();
        if (floorId == null) {
            return -1;
        }
        if (floorId <= Integer.MAX_VALUE) {
            return -floorId.intValue();
        }
        int hashed = Math.floorMod(Long.hashCode(floorId), Integer.MAX_VALUE - 1) + 1;
        return -hashed;
    }

    private String buildDeletedBuildingName(FacilityBuilding building) {
        String original = trimToNull(building.getBuildingName());
        String suffix = "#D" + building.getBuildingId();
        if (original == null) {
            return "DELETED" + suffix;
        }
        if (original.endsWith(suffix)) {
            return original;
        }
        int maxPrefixLen = Math.max(1, 128 - suffix.length());
        String prefix = original.length() > maxPrefixLen ? original.substring(0, maxPrefixLen) : original;
        return prefix + suffix;
    }

    private Integer parseFloorNo(String floorNo) {
        String raw = trimToNull(floorNo);
        if (raw == null) {
            throw badRequest("floorNo不能为空");
        }
        String normalized = raw.toUpperCase(Locale.ROOT).replace("F", "").trim();
        if (!normalized.matches("\\d+")) {
            throw badRequest("floorNo格式错误，支持 1/F1/1F");
        }
        try {
            return Integer.valueOf(normalized);
        } catch (NumberFormatException ex) {
            throw badRequest("floorNo超出范围");
        }
    }

    private <T> FacilityPageResponse<T> toPage(Page<T> pageData, int page, int size) {
        FacilityPageResponse<T> response = new FacilityPageResponse<>();
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
