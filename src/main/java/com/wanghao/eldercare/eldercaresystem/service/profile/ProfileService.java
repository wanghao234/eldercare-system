package com.wanghao.eldercare.eldercaresystem.service.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.BusinessException;
import com.wanghao.eldercare.eldercaresystem.common.ErrorCode;
import com.wanghao.eldercare.eldercaresystem.common.NotFoundException;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.profile.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.CurrentUser;
import com.wanghao.eldercare.eldercaresystem.common.security.PermissionService;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.profile.*;
import com.wanghao.eldercare.eldercaresystem.dto.profile.*;
import com.wanghao.eldercare.eldercaresystem.dto.profile.ElderProfileDTO;
import com.wanghao.eldercare.eldercaresystem.dto.profile.ElderProfileListItemDTO;
import com.wanghao.eldercare.eldercaresystem.dto.profile.ElderProfileUpdateRequest;
import com.wanghao.eldercare.eldercaresystem.dto.profile.ProfilePageResponse;
import com.wanghao.eldercare.eldercaresystem.dto.profile.StaffListItemDTO;
import com.wanghao.eldercare.eldercaresystem.dto.profile.StaffProfileDTO;
import com.wanghao.eldercare.eldercaresystem.dto.profile.StaffProfileUpdateRequest;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.facility.FacilityBed;
import com.wanghao.eldercare.eldercaresystem.entity.facility.FacilityBuilding;
import com.wanghao.eldercare.eldercaresystem.entity.facility.FacilityFloor;
import com.wanghao.eldercare.eldercaresystem.entity.facility.FacilityRoom;
import com.wanghao.eldercare.eldercaresystem.entity.profile.*;
import com.wanghao.eldercare.eldercaresystem.entity.profile.ElderProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.profile.StaffProfileEntity;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.FacilityBedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.FacilityBuildingRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.FacilityFloorRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.FacilityRoomRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.*;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.ElderProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.profile.StaffProfileRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private static final Set<String> STAFF_ROLES = Set.of("nurse", "caregiver", "doctor", "admin", "nurse_leader");
    private static final Set<String> NURSING_ROLES = Set.of("nurse", "caregiver");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final UserRepository userRepository;
    private final ElderProfileRepository elderProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AdmissionRecordRepository admissionRecordRepository;
    private final FacilityBedRepository facilityBedRepository;
    private final FacilityRoomRepository facilityRoomRepository;
    private final FacilityFloorRepository facilityFloorRepository;
    private final FacilityBuildingRepository facilityBuildingRepository;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    public ProfileService(UserRepository userRepository,
                          ElderProfileRepository elderProfileRepository,
                          StaffProfileRepository staffProfileRepository,
                          AdmissionRecordRepository admissionRecordRepository,
                          FacilityBedRepository facilityBedRepository,
                          FacilityRoomRepository facilityRoomRepository,
                          FacilityFloorRepository facilityFloorRepository,
                          FacilityBuildingRepository facilityBuildingRepository,
                          PermissionService permissionService,
                          ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.elderProfileRepository = elderProfileRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.admissionRecordRepository = admissionRecordRepository;
        this.facilityBedRepository = facilityBedRepository;
        this.facilityRoomRepository = facilityRoomRepository;
        this.facilityFloorRepository = facilityFloorRepository;
        this.facilityBuildingRepository = facilityBuildingRepository;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ElderProfileDTO getElderProfile(CurrentUser currentUser, Long elderId, boolean includeSensitive) {
        User elderUser = getUserOrThrow(elderId);
        requireElderRole(elderUser);
        checkCanViewElderProfile(currentUser, elderId);

        ElderProfileEntity profile = elderProfileRepository.findById(elderId).orElse(null);
        return toElderProfileDTO(currentUser, elderUser, profile, includeSensitive);
    }

    @Transactional
    public ElderProfileDTO updateElderProfile(CurrentUser currentUser, Long elderId, ElderProfileUpdateRequest request) {
        User elderUser = getUserOrThrow(elderId);
        requireElderRole(elderUser);
        assertValidGender(request.getGender());
        assertValidDate(request.getBirthday(), "birthday");

        ElderProfileEntity profile = elderProfileRepository.findById(elderId).orElseGet(() -> {
            ElderProfileEntity created = new ElderProfileEntity();
            created.setElderId(elderId);
            LocalDateTime now = LocalDateTime.now();
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            return created;
        });

        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            applyElderProfileAllFields(profile, request);
            applyUserBasicFields(elderUser, request.getRealName(), request.getPhone(), request.getEmail(), request.getAvatarUrl());
        } else if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            permissionService.assertCanAccessElder(currentUser, elderId);
            applyElderProfileNursingFields(profile, request);
        } else if (currentUser.hasRole("elder")) {
            if (!elderId.equals(currentUser.getUserId())) {
                throw new AccessDeniedException("仅可修改本人档案");
            }
            applyElderProfileSelfFields(profile, request);
            applyUserBasicFields(elderUser, request.getRealName(), request.getPhone(), request.getEmail(), request.getAvatarUrl());
        } else if (currentUser.hasRole("family")) {
            throw new AccessDeniedException("family 角色无权修改老人档案");
        } else {
            throw new AccessDeniedException("当前角色无权修改老人档案");
        }

        LocalDateTime now = LocalDateTime.now();
        profile.setUpdatedAt(now);
        elderUser.setUpdatedAt(now);

        userRepository.saveAndFlush(elderUser);
        elderProfileRepository.saveAndFlush(profile);
        boolean includeSensitiveInResponse = currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader");
        return toElderProfileDTO(currentUser, elderUser, profile, includeSensitiveInResponse);
    }

    @Transactional(readOnly = true)
    public ProfilePageResponse<ElderProfileListItemDTO> listElderProfiles(CurrentUser currentUser,
                                                                           String keyword,
                                                                           String careLevel,
                                                                           String status,
                                                                           int page,
                                                                           int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt", "userId"));
        List<Long> scopeIds = null;
        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver")) {
            scopeIds = permissionService.getVisibleElderIds(currentUser);
        } else if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader") || currentUser.hasRole("doctor"))) {
            throw new AccessDeniedException("当前角色无权访问老人档案列表");
        }

        Page<User> userPage = searchUsersByRoleWithScope("elder", scopeIds, keyword, status, careLevel, pageable);
        List<Long> elderIds = userPage.getContent().stream().map(User::getUserId).toList();
        Map<Long, ElderProfileEntity> profileMap = mapElderProfiles(elderIds);
        Map<Long, AdmissionRecord> admissionMap = mapActiveAdmissions(elderIds);
        Map<Long, FacilityBed> bedMap = mapBedsById(admissionMap.values().stream().map(AdmissionRecord::getBedId).toList());
        Map<Long, FacilityRoom> roomMap = mapRoomsById(bedMap.values().stream().map(FacilityBed::getRoomId).toList());
        Map<Long, FacilityFloor> floorMap = mapFloorsById(roomMap.values().stream().map(FacilityRoom::getFloorId).toList());
        Map<Long, FacilityBuilding> buildingMap = mapBuildingsById(floorMap.values().stream().map(FacilityFloor::getBuildingId).toList());
        List<ElderProfileListItemDTO> content = userPage.getContent().stream()
                .map(u -> toElderListItemDTO(u,
                        profileMap.get(u.getUserId()),
                        admissionMap.get(u.getUserId()),
                        bedMap,
                        roomMap,
                        floorMap,
                        buildingMap))
                .toList();

        ProfilePageResponse<ElderProfileListItemDTO> response = new ProfilePageResponse<>();
        response.setContent(content);
        response.setTotalElements(userPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Transactional(readOnly = true)
    public StaffProfileDTO getStaffProfile(CurrentUser currentUser, Long staffId, boolean includeSensitive) {
        User staffUser = getUserOrThrow(staffId);
        requireStaffRole(staffUser);
        checkCanViewStaffProfile(currentUser, staffUser);

        StaffProfileEntity profile = staffProfileRepository.findById(staffId).orElse(null);
        return toStaffProfileDTO(currentUser, staffUser, profile, includeSensitive);
    }

    @Transactional
    public StaffProfileDTO updateStaffProfile(CurrentUser currentUser, Long staffId, StaffProfileUpdateRequest request) {
        User staffUser = getUserOrThrow(staffId);
        requireStaffRole(staffUser);
        assertValidDate(request.getHireDate(), "hireDate");

        StaffProfileEntity profile = staffProfileRepository.findById(staffId).orElseGet(() -> {
            StaffProfileEntity created = new StaffProfileEntity();
            created.setStaffId(staffId);
            LocalDateTime now = LocalDateTime.now();
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            return created;
        });

        if (currentUser.hasRole("admin")) {
            applyStaffFullFields(profile, request);
            applyStaffUserSelfFields(staffUser, request);
        } else if (currentUser.hasRole("nurse_leader")) {
            if (staffId.equals(currentUser.getUserId())) {
                applyStaffUserSelfFields(staffUser, request);
                applyStaffSelfFields(profile, request);
            } else {
                String targetRole = normalize(staffUser.getRole());
                if (!NURSING_ROLES.contains(targetRole)) {
                    throw new AccessDeniedException("护士长仅可编辑护理人员档案");
                }
                applyStaffLeaderFields(profile, request);
            }
        } else if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver") || currentUser.hasRole("doctor")) {
            if (!staffId.equals(currentUser.getUserId())) {
                throw new AccessDeniedException("仅可修改本人员工档案");
            }
            applyStaffUserSelfFields(staffUser, request);
            applyStaffSelfFields(profile, request);
        } else {
            throw new AccessDeniedException("当前角色无权修改员工档案");
        }

        LocalDateTime now = LocalDateTime.now();
        profile.setUpdatedAt(now);
        staffUser.setUpdatedAt(now);
        userRepository.saveAndFlush(staffUser);
        staffProfileRepository.saveAndFlush(profile);
        return toStaffProfileDTO(currentUser, staffUser, profile, false);
    }

    @Transactional(readOnly = true)
    public ProfilePageResponse<StaffListItemDTO> listStaffProfiles(CurrentUser currentUser,
                                                                   String role,
                                                                   String keyword,
                                                                   String status,
                                                                   int page,
                                                                   int size) {
        if (!(currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"))) {
            throw new AccessDeniedException("当前角色无权访问员工档案列表");
        }

        Set<String> roles = new LinkedHashSet<>();
        if (currentUser.hasRole("admin")) {
            if (role == null || role.isBlank()) {
                roles.addAll(STAFF_ROLES);
            } else {
                String normalized = normalize(role);
                if (!STAFF_ROLES.contains(normalized)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "role 不合法", HttpStatus.BAD_REQUEST);
                }
                roles.add(normalized);
            }
        } else {
            if (role == null || role.isBlank()) {
                roles.addAll(NURSING_ROLES);
            } else {
                String normalized = normalize(role);
                if (!NURSING_ROLES.contains(normalized)) {
                    return emptyStaffPage(page, size);
                }
                roles.add(normalized);
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt", "userId"));
        Page<User> userPage = userRepository.searchByRoles(roles, normalizeNullable(keyword), normalizeNullable(status), pageable);
        Map<Long, StaffProfileEntity> profileMap = mapStaffProfiles(userPage.getContent().stream().map(User::getUserId).toList());

        List<StaffListItemDTO> content = userPage.getContent().stream()
                .map(u -> toStaffListItemDTO(u, profileMap.get(u.getUserId())))
                .toList();

        ProfilePageResponse<StaffListItemDTO> response = new ProfilePageResponse<>();
        response.setContent(content);
        response.setTotalElements(userPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private void checkCanViewElderProfile(CurrentUser currentUser, Long elderId) {
        if (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader")) {
            return;
        }
        if (currentUser.hasRole("elder")) {
            if (!elderId.equals(currentUser.getUserId())) {
                throw new AccessDeniedException("仅可查看本人档案");
            }
            return;
        }
        if (currentUser.hasRole("nurse") || currentUser.hasRole("caregiver") || currentUser.hasRole("family")) {
            permissionService.assertCanAccessElder(currentUser, elderId);
            return;
        }
        throw new AccessDeniedException("当前角色无权访问老人档案");
    }

    private void checkCanViewStaffProfile(CurrentUser currentUser, User target) {
        if (currentUser.hasRole("family") || currentUser.hasRole("elder")) {
            throw new AccessDeniedException("family/elder 无权访问员工档案");
        }
        if (currentUser.hasRole("admin")) {
            return;
        }
        if (currentUser.hasRole("nurse_leader")) {
            String role = normalize(target.getRole());
            if (NURSING_ROLES.contains(role) || target.getUserId().equals(currentUser.getUserId())) {
                return;
            }
            throw new AccessDeniedException("护士长仅可查看护理人员档案");
        }
        if ((currentUser.hasRole("nurse") || currentUser.hasRole("caregiver") || currentUser.hasRole("doctor"))
                && target.getUserId().equals(currentUser.getUserId())) {
            return;
        }
        throw new AccessDeniedException("仅可查看本人员工档案");
    }

    private ElderProfileDTO toElderProfileDTO(CurrentUser currentUser, User user, ElderProfileEntity profile, boolean includeSensitive) {
        ElderProfileDTO dto = new ElderProfileDTO();
        dto.setElderId(user.getUserId());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());

        if (profile != null) {
            dto.setGender(profile.getGender());
            dto.setBirthday(profile.getBirthday());
            dto.setAddress(profile.getAddress());
            dto.setEmergencyContactName(profile.getEmergencyContactName());
            dto.setCareLevel(profile.getCareLevel());
            dto.setAllergies(profile.getAllergies());
            dto.setChronicConditions(profile.getChronicConditions());
            dto.setDietTaboo(profile.getDietTaboo());
            dto.setNotes(profile.getNotes());

            boolean canViewSensitive = includeSensitive
                    && (currentUser.hasRole("admin") || currentUser.hasRole("nurse_leader"));
            dto.setIdNumber(canViewSensitive ? profile.getIdNumber() : null);
            dto.setEmergencyContactPhone(
                    canViewSensitive ? profile.getEmergencyContactPhone() : MaskUtil.maskPhone(profile.getEmergencyContactPhone())
            );
        }
        return dto;
    }

    private ElderProfileListItemDTO toElderListItemDTO(User user,
                                                       ElderProfileEntity profile,
                                                       AdmissionRecord admission,
                                                       Map<Long, FacilityBed> bedMap,
                                                       Map<Long, FacilityRoom> roomMap,
                                                       Map<Long, FacilityFloor> floorMap,
                                                       Map<Long, FacilityBuilding> buildingMap) {
        ElderProfileListItemDTO dto = new ElderProfileListItemDTO();
        dto.setElderId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        if (profile != null) {
            dto.setGender(profile.getGender());
            dto.setBirthday(profile.getBirthday());
            dto.setCareLevel(profile.getCareLevel());
        }
        if (admission != null) {
            dto.setAdmissionId(admission.getAdmissionId());
            dto.setBedId(admission.getBedId());
            FacilityBed bed = bedMap.get(admission.getBedId());
            if (bed != null) {
                dto.setBedCode(bed.getBedCode());
                dto.setRoomId(bed.getRoomId());
                FacilityRoom room = roomMap.get(bed.getRoomId());
                if (room != null && !"deleted".equals(normalize(room.getStatus()))) {
                    dto.setRoomNumber(room.getRoomNumber());
                    dto.setFloorId(room.getFloorId());
                    FacilityFloor floor = floorMap.get(room.getFloorId());
                    if (floor != null) {
                        dto.setFloorNo(floor.getFloorNo());
                        dto.setFloorName(floor.getFloorName());
                        dto.setBuildingId(floor.getBuildingId());
                        FacilityBuilding building = buildingMap.get(floor.getBuildingId());
                        if (building != null) {
                            dto.setBuildingName(building.getBuildingName());
                        }
                    }
                }
            }
        }
        return dto;
    }

    private StaffProfileDTO toStaffProfileDTO(CurrentUser currentUser, User user, StaffProfileEntity profile, boolean includeSensitive) {
        StaffProfileDTO dto = new StaffProfileDTO();
        dto.setStaffId(user.getUserId());
        dto.setRealName(user.getRealName());
        dto.setRole(user.getRole());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        if (profile != null) {
            dto.setJobTitle(profile.getJobTitle());
            dto.setDepartment(profile.getDepartment());
            boolean canViewSensitive = includeSensitive && currentUser.hasRole("admin");
            dto.setCertificationNo(canViewSensitive ? profile.getCertificationNo() : MaskUtil.maskIdNumber(profile.getCertificationNo()));
            dto.setHireDate(profile.getHireDate());
            dto.setSkills(parseSkills(profile.getSkillsJson()));
        } else {
            dto.setSkills(List.of());
        }
        return dto;
    }

    private StaffListItemDTO toStaffListItemDTO(User user, StaffProfileEntity profile) {
        StaffListItemDTO dto = new StaffListItemDTO();
        dto.setStaffId(user.getUserId());
        dto.setRealName(user.getRealName());
        dto.setRole(user.getRole());
        dto.setPhone(user.getPhone());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setStatus(user.getStatus());
        if (profile != null) {
            dto.setJobTitle(profile.getJobTitle());
            dto.setDepartment(profile.getDepartment());
        }
        return dto;
    }

    private void applyElderProfileAllFields(ElderProfileEntity profile, ElderProfileUpdateRequest request) {
        applyElderProfileNursingFields(profile, request);
        if (request.getGender() != null) {
            profile.setGender(normalize(request.getGender()));
        }
        if (request.getBirthday() != null) {
            profile.setBirthday(request.getBirthday());
        }
        if (request.getIdNumber() != null) {
            profile.setIdNumber(trimToNull(request.getIdNumber()));
        }
        if (request.getAddress() != null) {
            profile.setAddress(trimToNull(request.getAddress()));
        }
    }

    private void applyElderProfileNursingFields(ElderProfileEntity profile, ElderProfileUpdateRequest request) {
        if (request.getEmergencyContactName() != null) {
            profile.setEmergencyContactName(trimToNull(request.getEmergencyContactName()));
        }
        if (request.getEmergencyContactPhone() != null) {
            profile.setEmergencyContactPhone(trimToNull(request.getEmergencyContactPhone()));
        }
        if (request.getCareLevel() != null) {
            profile.setCareLevel(trimToNull(request.getCareLevel()));
        }
        if (request.getAllergies() != null) {
            profile.setAllergies(trimToNull(request.getAllergies()));
        }
        if (request.getChronicConditions() != null) {
            profile.setChronicConditions(trimToNull(request.getChronicConditions()));
        }
        if (request.getDietTaboo() != null) {
            profile.setDietTaboo(trimToNull(request.getDietTaboo()));
        }
        if (request.getNotes() != null) {
            profile.setNotes(trimToNull(request.getNotes()));
        }
    }

    private void applyElderProfileSelfFields(ElderProfileEntity profile, ElderProfileUpdateRequest request) {
        if (request.getAddress() != null) {
            profile.setAddress(trimToNull(request.getAddress()));
        }
        if (request.getNotes() != null) {
            profile.setNotes(trimToNull(request.getNotes()));
        }
    }

    private void applyStaffFullFields(StaffProfileEntity profile, StaffProfileUpdateRequest request) {
        if (request.getJobTitle() != null) {
            profile.setJobTitle(trimToNull(request.getJobTitle()));
        }
        if (request.getDepartment() != null) {
            profile.setDepartment(trimToNull(request.getDepartment()));
        }
        if (request.getCertificationNo() != null) {
            profile.setCertificationNo(trimToNull(request.getCertificationNo()));
        }
        if (request.getHireDate() != null) {
            profile.setHireDate(request.getHireDate());
        }
        if (request.getSkills() != null) {
            profile.setSkillsJson(toSkillsJson(request.getSkills()));
        }
    }

    private void applyStaffLeaderFields(StaffProfileEntity profile, StaffProfileUpdateRequest request) {
        if (request.getJobTitle() != null) {
            profile.setJobTitle(trimToNull(request.getJobTitle()));
        }
        if (request.getDepartment() != null) {
            profile.setDepartment(trimToNull(request.getDepartment()));
        }
        if (request.getSkills() != null) {
            profile.setSkillsJson(toSkillsJson(request.getSkills()));
        }
    }

    private void applyStaffSelfFields(StaffProfileEntity profile, StaffProfileUpdateRequest request) {
        if (request.getSkills() != null) {
            profile.setSkillsJson(toSkillsJson(request.getSkills()));
        }
    }

    private void applyStaffUserSelfFields(User user, StaffProfileUpdateRequest request) {
        if (request.getPhone() != null) {
            user.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(trimToNull(request.getAvatarUrl()));
        }
    }

    private void applyUserBasicFields(User user, String realName, String phone, String email, String avatarUrl) {
        if (realName != null) {
            user.setRealName(trimToNull(realName));
        }
        if (phone != null) {
            user.setPhone(trimToNull(phone));
        }
        if (email != null) {
            user.setEmail(trimToNull(email));
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(trimToNull(avatarUrl));
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    private void requireElderRole(User user) {
        if (!"elder".equals(normalize(user.getRole()))) {
            throw new NotFoundException("老人不存在");
        }
    }

    private void requireStaffRole(User user) {
        if (!STAFF_ROLES.contains(normalize(user.getRole()))) {
            throw new NotFoundException("员工不存在");
        }
    }

    private Map<Long, ElderProfileEntity> mapElderProfiles(List<Long> elderIds) {
        if (elderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return elderProfileRepository.findByElderIdIn(elderIds)
                .stream()
                .collect(Collectors.toMap(ElderProfileEntity::getElderId, e -> e));
    }

    private Map<Long, AdmissionRecord> mapActiveAdmissions(List<Long> elderIds) {
        if (elderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AdmissionRecord> records = admissionRecordRepository
                .findByElderIdInAndStatusOrderByCreatedAtDescAdmissionIdDesc(elderIds, "active");
        Map<Long, AdmissionRecord> latest = new HashMap<>();
        for (AdmissionRecord record : records) {
            latest.putIfAbsent(record.getElderId(), record);
        }
        return latest;
    }

    private Map<Long, FacilityBed> mapBedsById(List<Long> bedIds) {
        if (bedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return facilityBedRepository.findAllByBedIdInAndDeletedAtIsNull(bedIds)
                .stream()
                .collect(Collectors.toMap(FacilityBed::getBedId, e -> e));
    }

    private Map<Long, FacilityRoom> mapRoomsById(List<Long> roomIds) {
        if (roomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return facilityRoomRepository.findAllByRoomIdIn(roomIds)
                .stream()
                .collect(Collectors.toMap(FacilityRoom::getRoomId, e -> e));
    }

    private Map<Long, FacilityFloor> mapFloorsById(List<Long> floorIds) {
        if (floorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return facilityFloorRepository.findAllByFloorIdInAndDeletedAtIsNull(floorIds)
                .stream()
                .collect(Collectors.toMap(FacilityFloor::getFloorId, e -> e));
    }

    private Map<Long, FacilityBuilding> mapBuildingsById(List<Long> buildingIds) {
        if (buildingIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return facilityBuildingRepository.findAllByBuildingIdInAndDeletedAtIsNull(buildingIds)
                .stream()
                .collect(Collectors.toMap(FacilityBuilding::getBuildingId, e -> e));
    }

    private Map<Long, StaffProfileEntity> mapStaffProfiles(List<Long> staffIds) {
        if (staffIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return staffProfileRepository.findByStaffIdIn(staffIds)
                .stream()
                .collect(Collectors.toMap(StaffProfileEntity::getStaffId, e -> e));
    }

    private Page<User> searchUsersByRoleWithScope(String role,
                                                  List<Long> scopeIds,
                                                  String keyword,
                                                  String status,
                                                  String careLevel,
                                                  Pageable pageable) {
        String normalizedKeyword = normalizeNullable(keyword);
        String normalizedStatus = normalizeNullable(status);
        String normalizedCareLevel = normalizeNullable(careLevel);

        if (scopeIds != null && scopeIds.isEmpty()) {
            return Page.empty(pageable);
        }
        if (normalizedCareLevel == null) {
            if (scopeIds == null) {
                return userRepository.search(normalizedKeyword, role, normalizedStatus, pageable);
            }
            return userRepository.searchByRoleAndIds(role, scopeIds, normalizedKeyword, normalizedStatus, pageable);
        }

        List<Long> elderIds = scopeIds == null
                ? elderProfileRepository.findElderIdsByCareLevel(normalizedCareLevel)
                : elderProfileRepository.findElderIdsByCareLevelAndElderIds(normalizedCareLevel, scopeIds);
        if (elderIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return userRepository.searchByRoleAndIds(role, elderIds, normalizedKeyword, normalizedStatus, pageable);
    }

    private ProfilePageResponse<StaffListItemDTO> emptyStaffPage(int page, int size) {
        ProfilePageResponse<StaffListItemDTO> response = new ProfilePageResponse<>();
        response.setContent(List.of());
        response.setTotalElements(0);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private List<String> parseSkills(String skillsJson) {
        if (skillsJson == null || skillsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(skillsJson, STRING_LIST_TYPE);
            return list == null ? List.of() : list.stream().filter(v -> v != null && !v.isBlank()).toList();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toSkillsJson(List<String> skills) {
        List<String> safe = skills == null ? List.of() : skills.stream()
                .map(this::trimToNull)
                .filter(v -> v != null && !v.isBlank())
                .toList();
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "skills 格式错误", HttpStatus.BAD_REQUEST);
        }
    }

    private void assertValidGender(String gender) {
        String normalized = normalizeNullable(gender);
        if (normalized == null) {
            return;
        }
        if (!Set.of("male", "female", "unknown").contains(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "gender 仅支持 male/female/unknown", HttpStatus.BAD_REQUEST);
        }
    }

    private void assertValidDate(LocalDate date, String field) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 不能晚于今天", HttpStatus.BAD_REQUEST);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
