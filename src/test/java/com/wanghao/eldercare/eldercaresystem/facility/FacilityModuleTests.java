package com.wanghao.eldercare.eldercaresystem.facility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.user.User;
import com.wanghao.eldercare.eldercaresystem.user.UserRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

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
                .andExpect(jsonPath("$.data.items[1].bedId").exists());
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
