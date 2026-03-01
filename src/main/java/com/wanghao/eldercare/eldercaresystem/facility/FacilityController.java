package com.wanghao.eldercare.eldercaresystem.facility;

import com.wanghao.eldercare.eldercaresystem.audit.AuditAction;
import com.wanghao.eldercare.eldercaresystem.audit.Audited;
import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facility")
@PreAuthorize("hasAnyAuthority(T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_ADMIN,"
        + "T(com.wanghao.eldercare.eldercaresystem.security.Role).ROLE_NURSE_LEADER)")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @GetMapping("/buildings")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "facility_buildings")
    public ApiResponse<FacilityPageResponse<FacilityBuilding>> listBuildings(@RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "20") int size,
                                                                             @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(facilityService.listBuildings(keyword, page, size));
    }

    @PostMapping("/buildings")
    @Audited(action = AuditAction.CREATE, entityType = "facility_buildings", responseIdPath = "buildingId", requestFields = {"buildingName"})
    public ApiResponse<FacilityBuilding> createBuilding(@Valid @RequestBody BuildingUpsertRequest request) {
        return ApiResponse.ok(facilityService.createBuilding(request));
    }

    @PutMapping("/buildings/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "facility_buildings", entityIdArg = "id", requestFields = {"buildingName"})
    public ApiResponse<FacilityBuilding> updateBuilding(@PathVariable Long id,
                                                        @Valid @RequestBody BuildingUpsertRequest request) {
        return ApiResponse.ok(facilityService.updateBuilding(id, request));
    }

    @DeleteMapping("/buildings/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "facility_buildings", entityIdArg = "id")
    public ApiResponse<Void> deleteBuilding(@PathVariable Long id) {
        facilityService.deleteBuilding(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/floors")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "facility_floors")
    public ApiResponse<FacilityPageResponse<FacilityFloor>> listFloors(@RequestParam(required = false) Long buildingId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(facilityService.listFloors(buildingId, page, size));
    }

    @PostMapping("/floors")
    @Audited(action = AuditAction.CREATE, entityType = "facility_floors", responseIdPath = "floorId",
            requestFields = {"buildingId", "floorNo", "floorName"})
    public ApiResponse<FacilityFloor> createFloor(@Valid @RequestBody FloorCreateRequest request) {
        return ApiResponse.ok(facilityService.createFloor(request));
    }

    @PutMapping("/floors/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "facility_floors", entityIdArg = "id", requestFields = {"floorNo", "floorName"})
    public ApiResponse<FacilityFloor> updateFloor(@PathVariable Long id,
                                                  @Valid @RequestBody FloorUpdateRequest request) {
        return ApiResponse.ok(facilityService.updateFloor(id, request));
    }

    @DeleteMapping("/floors/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "facility_floors", entityIdArg = "id")
    public ApiResponse<Void> deleteFloor(@PathVariable Long id) {
        facilityService.deleteFloor(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/rooms")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "facility_rooms")
    public ApiResponse<FacilityPageResponse<FacilityRoom>> listRooms(@RequestParam(required = false) Long floorId,
                                                                     @RequestParam(required = false, defaultValue = "active") String status,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(facilityService.listRooms(floorId, status, page, size));
    }

    @PostMapping("/rooms")
    @Audited(action = AuditAction.CREATE, entityType = "facility_rooms", responseIdPath = "roomId",
            requestFields = {"floorId", "roomNumber", "roomType", "note"})
    public ApiResponse<FacilityRoom> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        return ApiResponse.ok(facilityService.createRoom(request));
    }

    @PutMapping("/rooms/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "facility_rooms", entityIdArg = "id",
            requestFields = {"roomNumber", "roomType", "note", "status"})
    public ApiResponse<FacilityRoom> updateRoom(@PathVariable Long id,
                                                @Valid @RequestBody RoomUpdateRequest request) {
        return ApiResponse.ok(facilityService.updateRoom(id, request));
    }

    @PostMapping("/rooms/{id}/status")
    @Audited(action = AuditAction.TRANSITION, entityType = "facility_rooms", entityIdArg = "id", fromField = "from", toField = "to")
    public ApiResponse<FacilityRoom> transitionRoomStatus(@PathVariable Long id,
                                                           @Valid @RequestBody StatusTransitionRequest request) {
        return ApiResponse.ok(facilityService.transitionRoomStatus(id, request));
    }

    @DeleteMapping("/rooms/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "facility_rooms", entityIdArg = "id")
    public ApiResponse<FacilityRoom> deleteRoom(@PathVariable Long id) {
        return ApiResponse.ok(facilityService.softDeleteRoom(id));
    }

    @GetMapping("/beds")
    @Audited(action = AuditAction.VIEW_SENSITIVE, entityType = "facility_beds")
    public ApiResponse<FacilityPageResponse<FacilityBed>> listBeds(@RequestParam(required = false) Long roomId,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(facilityService.listBeds(roomId, status, page, size));
    }

    @PostMapping("/beds")
    @Audited(action = AuditAction.CREATE, entityType = "facility_beds", responseIdPath = "bedId", requestFields = {"roomId", "bedCode"})
    public ApiResponse<FacilityBed> createBed(@Valid @RequestBody BedCreateRequest request) {
        return ApiResponse.ok(facilityService.createBed(request));
    }

    @PutMapping("/beds/{id}")
    @Audited(action = AuditAction.UPDATE, entityType = "facility_beds", entityIdArg = "id", requestFields = {"bedCode", "status"})
    public ApiResponse<FacilityBed> updateBed(@PathVariable Long id,
                                              @Valid @RequestBody BedUpdateRequest request) {
        return ApiResponse.ok(facilityService.updateBed(id, request));
    }

    @PostMapping("/beds/{id}/status")
    @Audited(action = AuditAction.TRANSITION, entityType = "facility_beds", entityIdArg = "id", fromField = "from", toField = "to")
    public ApiResponse<FacilityBed> transitionBedStatus(@PathVariable Long id,
                                                         @Valid @RequestBody StatusTransitionRequest request) {
        return ApiResponse.ok(facilityService.transitionBedStatus(id, request));
    }

    @DeleteMapping("/beds/{id}")
    @Audited(action = AuditAction.DELETE, entityType = "facility_beds", entityIdArg = "id")
    public ApiResponse<Void> deleteBed(@PathVariable Long id) {
        facilityService.deleteBed(id);
        return ApiResponse.ok(null);
    }
}
