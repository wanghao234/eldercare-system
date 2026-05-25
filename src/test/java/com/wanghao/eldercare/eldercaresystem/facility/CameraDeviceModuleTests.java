package com.wanghao.eldercare.eldercaresystem.facility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.entity.facility.CameraDevice;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.facility.CameraDeviceRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
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

@SpringBootTest(properties = "security.auth.block-family-elder-login=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CameraDeviceModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CameraDeviceRepository cameraDeviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        cameraDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admin_can_crud_camera_device() throws Exception {
        createUser("camera_admin", "admin", "camera_admin@test.local");
        String token = loginAndGetToken("camera_admin", "123456");

        MvcResult createResult = mockMvc.perform(post("/api/cameras")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cameraName":"走廊摄像头A",
                                  "cameraCode":"CAM-A-001",
                                  "cameraType":"webcam",
                                  "elderId":1,
                                  "roomId":101,
                                  "bedId":1,
                                  "locationText":"2号楼 3层 301门口",
                                  "mapX":320,
                                  "mapY":180,
                                  "status":"online",
                                  "remark":"测试设备"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.cameraName").value("走廊摄像头A"))
                .andReturn();

        long cameraId = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data")
                .path("cameraId")
                .asLong();

        mockMvc.perform(get("/api/cameras")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "CAM-A-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].cameraId").value(cameraId));

        mockMvc.perform(get("/api/cameras/{cameraId}", cameraId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cameraCode").value("CAM-A-001"));

        mockMvc.perform(put("/api/cameras/{cameraId}", cameraId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cameraName":"走廊摄像头A-更新",
                                  "cameraCode":"CAM-A-001",
                                  "cameraType":"ip_camera",
                                  "streamUrl":"rtsp://127.0.0.1/live",
                                  "elderId":1,
                                  "roomId":102,
                                  "bedId":2,
                                  "locationText":"2号楼 3层 302门口",
                                  "mapX":321,
                                  "mapY":181,
                                  "status":"offline",
                                  "remark":"已更新"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cameraName").value("走廊摄像头A-更新"))
                .andExpect(jsonPath("$.data.status").value("offline"));

        CameraDevice updated = cameraDeviceRepository.findById(cameraId).orElseThrow();
        assertThat(updated.getRoomId()).isEqualTo(102L);
        assertThat(updated.getBedId()).isEqualTo(2L);
        assertThat(updated.getStreamUrl()).isEqualTo("rtsp://127.0.0.1/live");

        mockMvc.perform(delete("/api/cameras/{cameraId}", cameraId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        assertThat(cameraDeviceRepository.findById(cameraId)).isEmpty();
    }

    @Test
    void non_admin_cannot_manage_camera_device() throws Exception {
        createUser("camera_nurse", "nurse", "camera_nurse@test.local");
        String token = loginAndGetToken("camera_nurse", "123456");

        mockMvc.perform(post("/api/cameras")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cameraName":"无权限摄像头"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    private User createUser(String username, String role, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setStatus("active");
        user.setRealName(username);
        user.setPhone("13800000000");
        user.setEmail(email);
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
}
