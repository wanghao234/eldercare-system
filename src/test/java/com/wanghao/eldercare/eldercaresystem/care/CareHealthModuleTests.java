package com.wanghao.eldercare.eldercaresystem.care;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.care.*;
import com.wanghao.eldercare.eldercaresystem.dto.care.*;
import com.wanghao.eldercare.eldercaresystem.entity.care.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.care.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.health.VitalSignRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.care.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareHealthModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CareTeamAssignmentRepository assignmentRepository;
    @Autowired
    private MealIntakeRecordRepository mealRepo;
    @Autowired
    private FluidIntakeRecordRepository fluidRepo;
    @Autowired
    private BowelRecordRepository bowelRepo;
    @Autowired
    private WeightRecordRepository weightRepo;
    @Autowired
    private VitalSignRecordRepository vitalRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mealRepo.deleteAll();
        fluidRepo.deleteAll();
        bowelRepo.deleteAll();
        weightRepo.deleteAll();
        vitalRepo.deleteAll();
        assignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nurse_can_record_meal_fluid_vitals_for_bound_elder() throws Exception {
        User elder = createUser("elderCare1", "elder");
        User nurse = createUser("nurseCare1", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseCare1", "123456");

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"breakfast","intakeRatio":75,"dietType":"normal"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"drinkType":"water","volumeMl":200}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(post("/api/health/vitals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"heartRate":78,"systolicBp":120,"diastolicBp":78,"spo2":98,"temperature":36.6,"bloodGlucose":5.4,"source":"manual"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void nurseB_record_for_unbound_elder_returns_403() throws Exception {
        User elder = createUser("elderCare2", "elder");
        User nurseA = createUser("nurseCare2A", "nurse");
        User nurseB = createUser("nurseCare2B", "nurse");
        bind(elder.getUserId(), nurseA.getUserId());

        String tokenB = loginAndGetToken("nurseCare2B", "123456");

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"breakfast","intakeRatio":75}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(get("/api/health/vitals")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("elderId", elder.getUserId().toString())
                        .param("from", "2026-03-01 00:00:00")
                        .param("to", "2026-03-01 23:59:59"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void admin_can_list_vitals_without_elderId() throws Exception {
        User elderA = createUser("elderCareAdminA", "elder");
        User elderB = createUser("elderCareAdminB", "elder");
        User nurse = createUser("nurseCareAdmin", "nurse");
        bind(elderA.getUserId(), nurse.getUserId());
        bind(elderB.getUserId(), nurse.getUserId());

        String nurseToken = loginAndGetToken("nurseCareAdmin", "123456");
        String adminToken = loginAndGetToken(createUser("adminCare", "admin").getUsername(), "123456");

        mockMvc.perform(post("/api/health/vitals")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"heartRate":76,"source":"manual","recordTime":"2026-03-01 09:00:00"}
                                """.formatted(elderA.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/health/vitals")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"heartRate":82,"source":"manual","recordTime":"2026-03-01 10:00:00"}
                                """.formatted(elderB.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/health/vitals")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-03-01 00:00:00")
                        .param("to", "2026-03-01 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void meal_intake_ratio_invalid_returns_40001() throws Exception {
        User elder = createUser("elderCareInvalid", "elder");
        User nurse = createUser("nurseCareInvalid", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseCareInvalid", "123456");

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"breakfast","intakeRatio":60}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void daily_summary_returns_aggregated_fluid_total() throws Exception {
        User elder = createUser("elderCare3", "elder");
        User nurse = createUser("nurseCare3", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseCare3", "123456");
        LocalDate date = LocalDate.of(2026, 3, 1);

        mockMvc.perform(post("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"drinkType":"water","volumeMl":200,"recordTime":"2026-03-01 08:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"drinkType":"soup","volumeMl":300,"recordTime":"2026-03-01 12:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/elders/{elderId}/daily-summary", elder.getUserId())
                        .header("Authorization", "Bearer " + token)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.fluidTotalMl").value(500))
                .andExpect(jsonPath("$.data.date").value("2026-03-01"));
    }

    @Test
    void risk_assessment_low_fluid_returns_medium_or_high() throws Exception {
        User elder = createUser("elderCare4", "elder");
        User nurse = createUser("nurseCare4", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseCare4", "123456");

        mockMvc.perform(post("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"drinkType":"water","volumeMl":300,"recordTime":"2026-02-28 09:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"drinkType":"water","volumeMl":200,"recordTime":"2026-03-01 09:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/elders/{elderId}/risk-assessment", elder.getUserId())
                        .header("Authorization", "Bearer " + token)
                        .param("date", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.dehydrationRisk").value(org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is("medium"), org.hamcrest.Matchers.is("high"))));
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

    private void bind(Long elderId, Long nurseId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setIsActive(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
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
