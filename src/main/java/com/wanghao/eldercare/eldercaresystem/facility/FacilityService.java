package com.wanghao.eldercare.eldercaresystem.facility;

import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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
                ? buildingRepository.findAll(pageable)
                : buildingRepository.findByBuildingNameContainingIgnoreCase(keyword.trim(), pageable);
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
        if (floorRepository.existsByBuildingId(id)) {
            throw badRequest("该楼栋下已存在楼层，禁止删除");
        }
        buildingRepository.delete(building);
    }

    @Transactional(readOnly = true)
    public FacilityPageResponse<FacilityFloor> listFloors(Long buildingId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "floorId"));
        Page<FacilityFloor> result = buildingId == null
                ? floorRepository.findAll(pageable)
                : floorRepository.findByBuildingId(buildingId, pageable);
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
        if (roomRepository.existsByFloorId(id)) {
            throw badRequest("该楼层下已存在房间，禁止删除");
        }
        floorRepository.delete(floor);
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
                    ? bedRepository.findAll(pageable)
                    : bedRepository.findByStatus(normalizedStatus, pageable);
        } else if (normalizedStatus == null) {
            result = bedRepository.findByRoomId(roomId, pageable);
        } else {
            result = bedRepository.findByRoomIdAndStatus(roomId, normalizedStatus, pageable);
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
        bed.setBedCode(bedCode);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            bed.setStatus(normalizeBedStatus(request.getStatus()));
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
        if ("occupied".equalsIgnoreCase(bed.getStatus())) {
            throw badRequest("已占用床位不可删除");
        }
        bedRepository.delete(bed);
    }

    private FacilityBuilding getBuilding(Long id) {
        return buildingRepository.findById(id).orElseThrow(() -> new NotFoundException("楼栋不存在"));
    }

    private FacilityFloor getFloor(Long id) {
        return floorRepository.findById(id).orElseThrow(() -> new NotFoundException("楼层不存在"));
    }

    private FacilityRoom getRoom(Long id) {
        return roomRepository.findById(id).orElseThrow(() -> new NotFoundException("房间不存在"));
    }

    private FacilityBed getBed(Long id) {
        return bedRepository.findById(id).orElseThrow(() -> new NotFoundException("床位不存在"));
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
