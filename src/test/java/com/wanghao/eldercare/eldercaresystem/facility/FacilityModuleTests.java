package com.wanghao.eldercare.eldercaresystem.facility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.facility.*;
import com.wanghao.eldercare.eldercaresystem.dto.facility.*;
import com.wanghao.eldercare.eldercaresystem.entity.facility.*;
import com.wanghao.eldercare.eldercaresystem.entity.facility.FacilityBed;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.*;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.FacilityBedRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.facility.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FacilityModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FacilityBedRepository bedRepository;
    @Autowired
    private FacilityRoomRepository roomRepository;
    @Autowired
    private FacilityFloorRepository floorRepository;
    @Autowired
    private FacilityBuildingRepository buildingRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        bedRepository.deleteAll();
        roomRepository.deleteAll();
        floorRepository.deleteAll();
        buildingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admin_create_building_floor_room_bed_chain_success() throws Exception {
        createUser("admin_facility_1", "admin");
        String token = loginAndGetToken("admin_facility_1", "123456");

        Long buildingId = createBuilding(token, "A栋");
        Long floorId = createFloor(token, buildingId, "3F", "三层");
        Long roomId = createRoom(token, floorId, "301", "double");
        Long bedId = createBed(token, roomId, "301-A");

        assertThat(buildingRepository.findById(buildingId)).isPresent();
        assertThat(floorRepository.findById(floorId)).isPresent();
        assertThat(roomRepository.findById(roomId)).isPresent();
        assertThat(bedRepository.findById(bedId)).isPresent();
    }

    @Test
    void room_delete_should_soft_delete_to_deleted() throws Exception {
        createUser("admin_facility_2", "admin");
        String token = loginAndGetToken("admin_facility_2", "123456");

        Long buildingId = createBuilding(token, "B栋");
        Long floorId = createFloor(token, buildingId, "2F", "二层");
        Long roomId = createRoom(token, floorId, "201", "single");

        mockMvc.perform(delete("/api/facility/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("deleted"));

        FacilityRoom room = roomRepository.findById(roomId).orElseThrow();
        assertThat(room.getStatus()).isEqualTo("deleted");
    }

    @Test
    void deleted_room_number_can_be_reused_in_same_floor() throws Exception {
        createUser("admin_facility_11", "admin");
        String token = loginAndGetToken("admin_facility_11", "123456");

        Long buildingId = createBuilding(token, "L栋");
        Long floorId = createFloor(token, buildingId, "12F", "十二层");
        Long roomId = createRoom(token, floorId, "1201", "single");

        mockMvc.perform(delete("/api/facility/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("deleted"));

        mockMvc.perform(post("/api/facility/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"floorId":%d,"roomNumber":"1201","roomType":"single","note":"reuse"}
                                """.formatted(floorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.roomNumber").value("1201"));
    }

    @Test
    void floor_delete_with_non_available_bed_should_return_40001() throws Exception {
        createUser("admin_facility_12", "admin");
        String token = loginAndGetToken("admin_facility_12", "123456");

        Long buildingId = createBuilding(token, "M栋");
        Long floorId = createFloor(token, buildingId, "13F", "十三层");
        Long roomId = createRoom(token, floorId, "1301", "double");
        Long bedId = createBed(token, roomId, "1301-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"1301-A","status":"maintenance"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("maintenance"));

        mockMvc.perform(delete("/api/facility/floors/{id}", floorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void floor_delete_should_soft_delete_related_rooms_and_beds_then_allow_reuse() throws Exception {
        createUser("admin_facility_13", "admin");
        String token = loginAndGetToken("admin_facility_13", "123456");

        Long buildingId = createBuilding(token, "N栋");
        Long floorId = createFloor(token, buildingId, "14F", "十四层");
        Long roomId = createRoom(token, floorId, "1401", "double");
        Long bedId = createBed(token, roomId, "1401-A");

        mockMvc.perform(delete("/api/facility/floors/{id}", floorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        FacilityFloor deletedFloor = floorRepository.findById(floorId).orElseThrow();
        assertThat(deletedFloor.getDeletedAt()).isNotNull();
        assertThat(deletedFloor.getFloorNo()).isLessThan(0);
        FacilityRoom deletedRoom = roomRepository.findById(roomId).orElseThrow();
        assertThat(deletedRoom.getStatus()).isEqualTo("deleted");
        FacilityBed deletedBed = bedRepository.findById(bedId).orElseThrow();
        assertThat(deletedBed.getDeletedAt()).isNotNull();

        Long newFloorId = createFloor(token, buildingId, "14F", "十四层");
        Long newRoomId = createRoom(token, newFloorId, "1401", "double");
        mockMvc.perform(post("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"bedCode":"1401-A"}
                                """.formatted(newRoomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.bedCode").value("1401-A"));
    }

    @Test
    void building_delete_with_non_available_bed_should_return_40001() throws Exception {
        createUser("admin_facility_14", "admin");
        String token = loginAndGetToken("admin_facility_14", "123456");

        Long buildingId = createBuilding(token, "P栋");
        Long floorId = createFloor(token, buildingId, "15F", "十五层");
        Long roomId = createRoom(token, floorId, "1501", "single");
        Long bedId = createBed(token, roomId, "1501-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"1501-A","status":"occupied"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("occupied"));

        mockMvc.perform(delete("/api/facility/buildings/{id}", buildingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void building_delete_should_soft_delete_related_data_then_allow_reuse() throws Exception {
        createUser("admin_facility_15", "admin");
        String token = loginAndGetToken("admin_facility_15", "123456");

        Long buildingId = createBuilding(token, "Q栋");
        Long floorId = createFloor(token, buildingId, "16F", "十六层");
        Long roomId = createRoom(token, floorId, "1601", "double");
        Long bedId = createBed(token, roomId, "1601-A");

        mockMvc.perform(delete("/api/facility/buildings/{id}", buildingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        FacilityBuilding deletedBuilding = buildingRepository.findById(buildingId).orElseThrow();
        assertThat(deletedBuilding.getDeletedAt()).isNotNull();
        FacilityFloor deletedFloor = floorRepository.findById(floorId).orElseThrow();
        assertThat(deletedFloor.getDeletedAt()).isNotNull();
        assertThat(deletedFloor.getFloorNo()).isLessThan(0);
        FacilityRoom deletedRoom = roomRepository.findById(roomId).orElseThrow();
        assertThat(deletedRoom.getStatus()).isEqualTo("deleted");
        FacilityBed deletedBed = bedRepository.findById(bedId).orElseThrow();
        assertThat(deletedBed.getDeletedAt()).isNotNull();

        Long newBuildingId = createBuilding(token, "Q栋");
        Long newFloorId = createFloor(token, newBuildingId, "16F", "十六层");
        Long newRoomId = createRoom(token, newFloorId, "1601", "double");
        mockMvc.perform(post("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"bedCode":"1601-A"}
                                """.formatted(newRoomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.bedCode").value("1601-A"));
    }

    @Test
    void room_delete_with_occupied_bed_should_return_40001() throws Exception {
        createUser("admin_facility_9", "admin");
        String token = loginAndGetToken("admin_facility_9", "123456");

        Long buildingId = createBuilding(token, "J栋");
        Long floorId = createFloor(token, buildingId, "10F", "十层");
        Long roomId = createRoom(token, floorId, "1001", "double");
        Long bedId = createBed(token, roomId, "1001-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"1001-A","status":"occupied"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("occupied"));

        mockMvc.perform(delete("/api/facility/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void room_delete_should_soft_delete_related_beds() throws Exception {
        createUser("admin_facility_10", "admin");
        String token = loginAndGetToken("admin_facility_10", "123456");

        Long buildingId = createBuilding(token, "K栋");
        Long floorId = createFloor(token, buildingId, "11F", "十一层");
        Long roomId = createRoom(token, floorId, "1101", "double");
        Long bedA = createBed(token, roomId, "1101-A");
        Long bedB = createBed(token, roomId, "1101-B");

        mockMvc.perform(delete("/api/facility/rooms/{id}", roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("deleted"));

        FacilityBed deletedA = bedRepository.findById(bedA).orElseThrow();
        FacilityBed deletedB = bedRepository.findById(bedB).orElseThrow();
        assertThat(deletedA.getDeletedAt()).isNotNull();
        assertThat(deletedB.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .param("roomId", String.valueOf(roomId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void occupied_bed_delete_should_return_40001() throws Exception {
        createUser("admin_facility_3", "admin");
        String token = loginAndGetToken("admin_facility_3", "123456");

        Long buildingId = createBuilding(token, "C栋");
        Long floorId = createFloor(token, buildingId, "1F", "一层");
        Long roomId = createRoom(token, floorId, "101", "single");
        Long bedId = createBed(token, roomId, "101-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"101-A","status":"occupied"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("occupied"));

        mockMvc.perform(delete("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void reserved_bed_delete_should_return_40001() throws Exception {
        createUser("admin_facility_7", "admin");
        String token = loginAndGetToken("admin_facility_7", "123456");

        Long buildingId = createBuilding(token, "H栋");
        Long floorId = createFloor(token, buildingId, "8F", "八层");
        Long roomId = createRoom(token, floorId, "801", "single");
        Long bedId = createBed(token, roomId, "801-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"801-A","status":"reserved"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("reserved"));

        mockMvc.perform(delete("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void occupied_bed_status_update_should_return_40001() throws Exception {
        createUser("admin_facility_16", "admin");
        String token = loginAndGetToken("admin_facility_16", "123456");

        Long buildingId = createBuilding(token, "R栋");
        Long floorId = createFloor(token, buildingId, "17F", "十七层");
        Long roomId = createRoom(token, floorId, "1701", "single");
        Long bedId = createBed(token, roomId, "1701-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"1701-A","status":"occupied"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("occupied"));

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"1701-A","status":"available"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void reserved_bed_status_transition_should_return_40001() throws Exception {
        createUser("admin_facility_17", "admin");
        String token = loginAndGetToken("admin_facility_17", "123456");

        Long buildingId = createBuilding(token, "S栋");
        Long floorId = createFloor(token, buildingId, "18F", "十八层");
        Long roomId = createRoom(token, floorId, "1801", "single");
        Long bedId = createBed(token, roomId, "1801-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"1801-A","status":"reserved"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("reserved"));

        mockMvc.perform(post("/api/facility/beds/{id}/status", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from":"reserved","to":"available"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void maintenance_bed_delete_should_return_40001() throws Exception {
        createUser("admin_facility_8", "admin");
        String token = loginAndGetToken("admin_facility_8", "123456");

        Long buildingId = createBuilding(token, "I栋");
        Long floorId = createFloor(token, buildingId, "9F", "九层");
        Long roomId = createRoom(token, floorId, "901", "single");
        Long bedId = createBed(token, roomId, "901-A");

        mockMvc.perform(put("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"901-A","status":"maintenance"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("maintenance"));

        mockMvc.perform(delete("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void nurse_access_facility_should_return_40301() throws Exception {
        createUser("nurse_facility_1", "nurse");
        String token = loginAndGetToken("nurse_facility_1", "123456");

        mockMvc.perform(post("/api/facility/buildings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buildingName":"D栋"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void list_beds_without_status_should_return_all_statuses() throws Exception {
        createUser("admin_facility_4", "admin");
        String token = loginAndGetToken("admin_facility_4", "123456");

        Long buildingId = createBuilding(token, "E栋");
        Long floorId = createFloor(token, buildingId, "5F", "五层");
        Long roomId = createRoom(token, floorId, "501", "double");
        Long bedA = createBed(token, roomId, "501-A");
        Long bedB = createBed(token, roomId, "501-B");

        mockMvc.perform(put("/api/facility/beds/{id}", bedB)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bedCode":"501-B","status":"occupied"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .param("roomId", String.valueOf(roomId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].bedId").exists())
                .andExpect(jsonPath("$.data.items[0].roomNumber").value("501"))
                .andExpect(jsonPath("$.data.items[1].bedId").exists());
    }

    @Test
    void delete_bed_should_soft_delete_and_hide_from_list() throws Exception {
        createUser("admin_facility_5", "admin");
        String token = loginAndGetToken("admin_facility_5", "123456");

        Long buildingId = createBuilding(token, "F栋");
        Long floorId = createFloor(token, buildingId, "6F", "六层");
        Long roomId = createRoom(token, floorId, "601", "single");
        Long bedId = createBed(token, roomId, "601-A");

        mockMvc.perform(delete("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        FacilityBed deleted = bedRepository.findById(bedId).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .param("roomId", String.valueOf(roomId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void soft_deleted_bed_code_can_be_reused_in_same_room() throws Exception {
        createUser("admin_facility_6", "admin");
        String token = loginAndGetToken("admin_facility_6", "123456");

        Long buildingId = createBuilding(token, "G栋");
        Long floorId = createFloor(token, buildingId, "7F", "七层");
        Long roomId = createRoom(token, floorId, "701", "single");
        Long bedId = createBed(token, roomId, "701-A");

        mockMvc.perform(delete("/api/facility/beds/{id}", bedId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"bedCode":"701-A"}
                                """.formatted(roomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.bedCode").value("701-A"));
    }

    @Test
    void create_bed_with_duplicate_bed_code_in_another_room_should_return_40001() throws Exception {
        createUser("admin_facility_7", "admin");
        String token = loginAndGetToken("admin_facility_7", "123456");

        Long buildingId = createBuilding(token, "H栋");
        Long floorId = createFloor(token, buildingId, "8F", "八层");
        Long roomA = createRoom(token, floorId, "801", "double");
        Long roomB = createRoom(token, floorId, "802", "double");
        createBed(token, roomA, "8A-01");

        mockMvc.perform(post("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"bedCode":"8A-01"}
                                """.formatted(roomB)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    private User createUser(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setStatus("active");
        user.setRealName(username);
        user.setPhone("13800000000");
        user.setEmail(username + "@test.local");
        user.setAvatarUrl("");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path("token").asText();
    }

    private Long createBuilding(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/facility/buildings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingName\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("buildingId").asLong();
    }

    private Long createFloor(String token, Long buildingId, String floorNo, String floorName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/facility/floors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"buildingId":%d,"floorNo":"%s","floorName":"%s"}
                                """.formatted(buildingId, floorNo, floorName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("floorId").asLong();
    }

    private Long createRoom(String token, Long floorId, String roomNumber, String roomType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/facility/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"floorId":%d,"roomNumber":"%s","roomType":"%s","note":"ok"}
                                """.formatted(floorId, roomNumber, roomType)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("roomId").asLong();
    }

    private Long createBed(String token, Long roomId, String bedCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/facility/beds")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"bedCode":"%s"}
                                """.formatted(roomId, bedCode)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("bedId").asLong();
    }
}
