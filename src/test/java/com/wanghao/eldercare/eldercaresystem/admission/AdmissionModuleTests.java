package com.wanghao.eldercare.eldercaresystem.admission;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdmissionModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private AdmissionRecordRepository admissionRecordRepository;

    @Autowired
    private DischargeRecordRepository dischargeRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        dischargeRecordRepository.deleteAll();
        admissionRecordRepository.deleteAll();
        bedRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admission_active_sets_bed_occupied() throws Exception {
        createUser("adminA", "admin");
        User elder = createUser("elderA", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminA", "123456");

        mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "status":"active",
                                  "startDate":"2030-01-01"
                                }
                                """.formatted(elder.getUserId(), bed.getBedId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        Bed updated = bedRepository.findById(bed.getBedId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("occupied");
    }

    @Test
    void discharge_complete_sets_bed_available_and_admission_ended() throws Exception {
        createUser("adminB", "admin");
        User elder = createUser("elderB", "elder");
        Bed bed = createBed("available");

        String adminToken = loginAndGetToken("adminB", "123456");
        Long admissionId = createAdmission(adminToken, elder.getUserId(), bed.getBedId());
        Long dischargeId = createDischarge(adminToken, admissionId);

        mockMvc.perform(post("/api/discharges/{id}/settlement", dischargeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "settlementAmount":1000,
                                  "refundAmount":200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("settling"));

        mockMvc.perform(post("/api/discharges/{id}/complete", dischargeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualDate":"2030-01-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));

        Bed updatedBed = bedRepository.findById(bed.getBedId()).orElseThrow();
        AdmissionRecord updatedAdmission = admissionRecordRepository.findById(admissionId).orElseThrow();

        assertThat(updatedBed.getStatus()).isEqualTo("available");
        assertThat(updatedAdmission.getStatus()).isEqualTo("ended");
        assertThat(updatedAdmission.getEndDate()).isEqualTo(LocalDate.of(2030, 1, 10));
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

    private Bed createBed(String status) {
        Bed bed = new Bed();
        bed.setBedId(1L + bedRepository.count());
        bed.setRoomId(1L);
        bed.setBedNo("B-" + bed.getBedId());
        bed.setStatus(status);
        return bedRepository.save(bed);
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

    private Long createAdmission(String token, Long elderId, Long bedId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/admissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "bedId":%d,
                                  "status":"active",
                                  "startDate":"2030-01-01"
                                }
                                """.formatted(elderId, bedId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("id").asLong();
    }

    private Long createDischarge(String token, Long admissionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/discharges")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "admissionId":%d,
                                  "reason":"康复出院"
                                }
                                """.formatted(admissionId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("id").asLong();
    }
}
