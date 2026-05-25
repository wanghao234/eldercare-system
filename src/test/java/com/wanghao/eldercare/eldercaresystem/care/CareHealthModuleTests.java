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
import com.wanghao.eldercare.eldercaresystem.entity.health.VitalSignRecord;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void list_endpoints_accept_page_and_size_without_time_range() throws Exception {
        User elder = createUser("elderCareCompat", "elder");
        User nurse = createUser("nurseCareCompat", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseCareCompat", "123456");
        String today = LocalDate.now().toString();

        mockMvc.perform(post("/api/care/bowel-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"bristolType":4,"amount":"normal","incontinence":0,"bloodFlag":0}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/weight-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"weightKg":62.5,"measureCtx":"morning"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/health/vitals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"heartRate":80,"source":"manual"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"lunch","intakeRatio":100}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"drinkType":"water","volumeMl":180}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/care/bowel-records")
                        .header("Authorization", "Bearer " + token)
                        .param("date", today)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/care/weight-records")
                        .header("Authorization", "Bearer " + token)
                        .param("date", today)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/health/vitals")
                        .header("Authorization", "Bearer " + token)
                        .param("date", today)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void meal_records_support_last7days_and_all() throws Exception {
        User elder = createUser("elderMealRange", "elder");
        User nurse = createUser("nurseMealRange", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseMealRange", "123456");

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"breakfast","intakeRatio":75,"recordTime":"%s 08:00:00"}
                                """.formatted(elder.getUserId(), LocalDate.now().minusDays(1))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"lunch","intakeRatio":100,"recordTime":"%s 12:00:00"}
                                """.formatted(elder.getUserId(), LocalDate.now().minusDays(6))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"dinner","intakeRatio":50,"recordTime":"%s 18:00:00"}
                                """.formatted(elder.getUserId(), LocalDate.now().minusDays(8))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("date", "last7days"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("date", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void meal_records_support_from_to_range() throws Exception {
        User elder = createUser("elderMealWindow", "elder");
        User nurse = createUser("nurseMealWindow", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseMealWindow", "123456");

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"breakfast","intakeRatio":75,"recordTime":"2026-03-01 08:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"lunch","intakeRatio":100,"recordTime":"2026-03-03 12:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"dinner","intakeRatio":50,"recordTime":"2026-03-05 18:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/care/meal-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("from", "2026-03-02 00:00:00")
                        .param("to", "2026-03-04 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].mealType").value("lunch"));
    }

    @Test
    void other_record_lists_support_last7days_all_and_range() throws Exception {
        User elder = createUser("elderRangeBundle", "elder");
        User nurse = createUser("nurseRangeBundle", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseRangeBundle", "123456");

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
                                {"elderId":%d,"drinkType":"tea","volumeMl":180,"recordTime":"2026-03-03 09:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/bowel-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"bristolType":4,"amount":"normal","incontinence":0,"bloodFlag":0,"recordTime":"2026-03-01 10:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/care/bowel-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"bristolType":5,"amount":"small","incontinence":0,"bloodFlag":0,"recordTime":"2026-03-03 11:00:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care/weight-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"weightKg":62.0,"measureCtx":"morning","recordTime":"2026-03-01 07:30:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/care/weight-records")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"weightKg":62.4,"measureCtx":"morning","recordTime":"2026-03-03 07:30:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/health/vitals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"heartRate":72,"source":"manual","recordTime":"2026-03-01 08:30:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/health/vitals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"heartRate":78,"source":"manual","recordTime":"2026-03-03 08:30:00"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/care/fluid-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("date", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/care/bowel-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("date", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/care/weight-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("date", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/health/vitals")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("date", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/care/bowel-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("from", "2026-03-02 00:00:00")
                        .param("to", "2026-03-04 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/care/weight-records")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("from", "2026-03-02 00:00:00")
                        .param("to", "2026-03-04 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/health/vitals")
                        .header("Authorization", "Bearer " + token)
                        .param("elderId", elder.getUserId().toString())
                        .param("from", "2026-03-02 00:00:00")
                        .param("to", "2026-03-04 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void elder_can_upload_vitals_from_apple_watch() throws Exception {
        User elder = createUser("elderWatch1", "elder");
        String token = loginAndGetToken("elderWatch1", "123456");

        mockMvc.perform(post("/api/health/vitals/apple-watch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"deviceId":"AW-ULTRA-001","deviceName":"Apple Watch Ultra 2","heartRate":72,"spo2":97,"temperature":36.5,"bloodGlucose":5.2}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.elderId").value(elder.getUserId()))
                .andExpect(jsonPath("$.data.source").value("device"))
                .andExpect(jsonPath("$.data.deviceType").value("apple_watch"))
                .andExpect(jsonPath("$.data.deviceId").value("aw-ultra-001"))
                .andExpect(jsonPath("$.data.recordedBy").value(elder.getUserId()));
    }

    @Test
    void elder_cannot_upload_apple_watch_vitals_for_another_elder() throws Exception {
        User elderA = createUser("elderWatch2A", "elder");
        User elderB = createUser("elderWatch2B", "elder");
        String token = loginAndGetToken("elderWatch2A", "123456");

        mockMvc.perform(post("/api/health/vitals/apple-watch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"deviceId":"AW-ULTRA-002","heartRate":75}
                                """.formatted(elderB.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
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
    void nurse_can_update_and_delete_bound_records() throws Exception {
        User elder = createUser("elderCareUpdate", "elder");
        User nurse = createUser("nurseCareUpdate", "nurse");
        bind(elder.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseCareUpdate", "123456");

        MealIntakeRecord meal = new MealIntakeRecord();
        meal.setElderId(elder.getUserId());
        meal.setMealType("breakfast");
        meal.setIntakeRatio(50);
        meal.setRecordedBy(nurse.getUserId());
        meal.setRecordTime(LocalDateTime.of(2026, 3, 1, 8, 0));
        meal.setCreatedAt(LocalDateTime.now());
        meal = mealRepo.save(meal);

        VitalSignRecord vital = new VitalSignRecord();
        vital.setElderId(elder.getUserId());
        vital.setHeartRate(76);
        vital.setSource("manual");
        vital.setRecordedBy(nurse.getUserId());
        vital.setRecordTime(LocalDateTime.of(2026, 3, 1, 9, 0));
        vital.setCreatedAt(LocalDateTime.now());
        vital = vitalRepo.save(vital);

        mockMvc.perform(put("/api/care/meal-records/{mealId}", meal.getMealId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"mealType":"lunch","intakeRatio":75,"dietType":"soft","note":"已更新"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.mealType").value("lunch"))
                .andExpect(jsonPath("$.data.intakeRatio").value(75));

        mockMvc.perform(delete("/api/health/vitals/{vitalId}", vital.getVitalId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void update_delete_unbound_record_returns_403() throws Exception {
        User elder = createUser("elderCareNoAccess", "elder");
        User nurseA = createUser("nurseCareNoAccessA", "nurse");
        User nurseB = createUser("nurseCareNoAccessB", "nurse");
        bind(elder.getUserId(), nurseA.getUserId());
        String tokenB = loginAndGetToken("nurseCareNoAccessB", "123456");

        WeightRecord weight = new WeightRecord();
        weight.setElderId(elder.getUserId());
        weight.setWeightKg(61.5);
        weight.setRecordedBy(nurseA.getUserId());
        weight.setRecordTime(LocalDateTime.of(2026, 3, 1, 7, 30));
        weight.setCreatedAt(LocalDateTime.now());
        weight = weightRepo.save(weight);

        mockMvc.perform(put("/api/care/weight-records/{weightId}", weight.getWeightId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"weightKg":62.0,"measureCtx":"morning"}
                                """.formatted(elder.getUserId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(delete("/api/care/weight-records/{weightId}", weight.getWeightId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void update_record_cannot_change_elderId() throws Exception {
        User elderA = createUser("elderCareSameA", "elder");
        User elderB = createUser("elderCareSameB", "elder");
        User nurse = createUser("nurseCareSame", "nurse");
        bind(elderA.getUserId(), nurse.getUserId());
        bind(elderB.getUserId(), nurse.getUserId());
        String token = loginAndGetToken("nurseCareSame", "123456");

        FluidIntakeRecord fluid = new FluidIntakeRecord();
        fluid.setElderId(elderA.getUserId());
        fluid.setDrinkType("water");
        fluid.setVolumeMl(200);
        fluid.setRecordedBy(nurse.getUserId());
        fluid.setRecordTime(LocalDateTime.of(2026, 3, 1, 10, 0));
        fluid.setCreatedAt(LocalDateTime.now());
        fluid = fluidRepo.save(fluid);

        mockMvc.perform(put("/api/care/fluid-records/{fluidId}", fluid.getFluidId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderId":%d,"drinkType":"water","volumeMl":250}
                                """.formatted(elderB.getUserId())))
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
