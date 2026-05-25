package com.wanghao.eldercare.eldercaresystem.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.entity.admission.AdmissionRecord;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.CarePlan;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.CarePlanTask;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.shift.StaffShiftSchedule;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.admission.AdmissionRecordRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.CarePlanRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.CarePlanTaskRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.shift.StaffShiftScheduleRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CarePlanTaskModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarePlanRepository carePlanRepository;

    @Autowired
    private CarePlanTaskRepository carePlanTaskRepository;

    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @Autowired
    private StaffShiftScheduleRepository staffShiftScheduleRepository;

    @Autowired
    private AdmissionRecordRepository admissionRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        carePlanTaskRepository.deleteAll();
        carePlanRepository.deleteAll();
        admissionRecordRepository.deleteAll();
        staffShiftScheduleRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void generate_draft_tasks_then_confirm_and_nurse_list_sorted_by_scheduled_at() throws Exception {
        User elder = createUser("elderTask1", "elder");
        User nurse = createUser("nurseTask1", "nurse");
        createUser("adminTask1", "admin");
        bindNurse(elder.getUserId(), nurse.getUserId());
        createActiveAdmission(elder.getUserId(), nurse.getUserId());

        CarePlan plan = createPlan(
                elder.getUserId(),
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 10),
                "每日",
                "08:00",
                "血压监测",
                null,
                null,
                null);

        String adminToken = loginAndGetToken("adminTask1", "123456");
        String nurseToken = loginAndGetToken("nurseTask1", "123456");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/generate-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.generatedCount").value(4));

        List<CarePlanTask> generatedTasks = carePlanTaskRepository
                .findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(plan.getCarePlanId());
        assertThat(generatedTasks).hasSize(4);
        assertThat(generatedTasks).extracting(CarePlanTask::getStatus)
                .containsOnly("draft");
        assertThat(generatedTasks).extracting(task -> task.getScheduledAt().toString())
                .containsExactly(
                        "2026-05-07T08:00",
                        "2026-05-08T08:00",
                        "2026-05-09T08:00",
                        "2026-05-10T08:00");

        mockMvc.perform(get("/api/care-plan-tasks/my")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(post("/api/care-plans/{carePlanId}/confirm-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.confirmedTaskCount").value(4));

        mockMvc.perform(get("/api/care-plan-tasks/my")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].scheduledAt").value("2026-05-07 08:00:00"))
                .andExpect(jsonPath("$.data[1].scheduledAt").value("2026-05-08 08:00:00"))
                .andExpect(jsonPath("$.data[2].scheduledAt").value("2026-05-09 08:00:00"))
                .andExpect(jsonPath("$.data[3].scheduledAt").value("2026-05-10 08:00:00"));
    }

    @Test
    void by_plan_returns_schedule_fields_for_draft_preview() throws Exception {
        User elder = createUser("elderTask2", "elder");
        User nurse = createUser("nurseTask2", "nurse");
        createUser("adminTask2", "admin");
        bindNurse(elder.getUserId(), nurse.getUserId());
        createActiveAdmission(elder.getUserId(), nurse.getUserId());

        CarePlan plan = createPlan(
                elder.getUserId(),
                LocalDate.of(2026, 5, 7),
                LocalDate.of(2026, 5, 31),
                "每周一次",
                null,
                null,
                null,
                "每周沟通一次",
                null);

        String adminToken = loginAndGetToken("adminTask2", "123456");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/generate-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(4));

        MvcResult result = mockMvc.perform(get("/api/care-plan-tasks/by-plan/{carePlanId}", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scheduledDate").value("2026-05-07"))
                .andExpect(jsonPath("$.data[0].scheduledTime").value("14:00:00"))
                .andExpect(jsonPath("$.data[0].scheduledAt").value("2026-05-07 14:00:00"))
                .andExpect(jsonPath("$.data[0].assignedNurseId").value(nurse.getUserId()))
                .andExpect(jsonPath("$.data[0].assignedNurseName").value("nurseTask2"))
                .andExpect(jsonPath("$.data[0].taskSource").value("care_plan"))
                .andExpect(jsonPath("$.data[0].taskGroupKey").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("data").size()).isEqualTo(4);
    }

    @Test
    void generate_tasks_assigns_by_schedule_first_then_confirm_visible_to_nurse() throws Exception {
        User elder = createUser("elderTask3", "elder");
        User nurse = createUser("nurseTask3", "nurse");
        createUser("adminTask3", "admin");
        bindNurse(elder.getUserId(), nurse.getUserId());
        createActiveAdmission(elder.getUserId(), nurse.getUserId());
        createShift(nurse.getUserId(), LocalDate.of(2026, 5, 25), LocalTime.of(8, 0), LocalTime.of(12, 0));

        CarePlan plan = createPlan(
                elder.getUserId(),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 5, 25),
                "每日",
                "08:00",
                "血压监测",
                null,
                null,
                null);

        String adminToken = loginAndGetToken("adminTask3", "123456");
        String nurseToken = loginAndGetToken("nurseTask3", "123456");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/generate-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(1));

        List<CarePlanTask> tasks = carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(plan.getCarePlanId());
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getAssignedNurseId()).isEqualTo(nurse.getUserId());
        assertThat(tasks.get(0).getStatus()).isEqualTo("draft");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/confirm-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedTaskCount").value(1));

        mockMvc.perform(get("/api/care-plan-tasks/my")
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("pending"))
                .andExpect(jsonPath("$.data[0].scheduledAt").value("2026-05-25 08:00:00"));
    }

    @Test
    void auto_assign_uses_binding_fallback_when_no_shift() throws Exception {
        User elder = createUser("elderTask4", "elder");
        User nurse = createUser("nurseTask4", "nurse");
        createUser("adminTask4", "admin");
        bindNurse(elder.getUserId(), nurse.getUserId());
        createActiveAdmission(elder.getUserId(), nurse.getUserId());

        CarePlan plan = createPlan(
                elder.getUserId(),
                LocalDate.of(2026, 5, 26),
                LocalDate.of(2026, 5, 26),
                "每日",
                "08:00",
                "血压监测",
                null,
                null,
                null);

        String adminToken = loginAndGetToken("adminTask4", "123456");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/generate-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/care-plans/{carePlanId}/auto-assign-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carePlanId").value(plan.getCarePlanId()))
                .andExpect(jsonPath("$.data.assignedCount").value(1))
                .andExpect(jsonPath("$.data.scheduleMatchedCount").value(0))
                .andExpect(jsonPath("$.data.fallbackAssignedCount").value(1))
                .andExpect(jsonPath("$.data.unassignedCount").value(0));

        List<CarePlanTask> tasks = carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(plan.getCarePlanId());
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getAssignedNurseId()).isEqualTo(nurse.getUserId());
        assertThat(tasks.get(0).getStatus()).isEqualTo("draft");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/confirm-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedTaskCount").value(1));
    }

    @Test
    void confirm_tasks_fails_when_elder_has_no_binding() throws Exception {
        User elder = createUser("elderTask5", "elder");
        createUser("adminTask5", "admin");
        createActiveAdmission(elder.getUserId(), 1L);

        CarePlan plan = createPlan(
                elder.getUserId(),
                LocalDate.of(2026, 5, 27),
                LocalDate.of(2026, 5, 27),
                "每日",
                "08:00",
                "血压监测",
                null,
                null,
                null);

        String adminToken = loginAndGetToken("adminTask5", "123456");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/generate-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<CarePlanTask> tasks = carePlanTaskRepository.findAllByCarePlanIdOrderByScheduledAtAscCreatedAtAscTaskIdAsc(plan.getCarePlanId());
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getAssignedNurseId()).isNull();
        assertThat(tasks.get(0).getStatus()).isEqualTo("draft");

        mockMvc.perform(post("/api/care-plans/{carePlanId}/confirm-tasks", plan.getCarePlanId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("存在未分配护理人员的任务，请先完成护理团队绑定、补充排班或手动指定护理人员后再确认分配。"));
    }

    private CarePlan createPlan(Long elderId,
                                LocalDate startDate,
                                LocalDate endDate,
                                String executionFrequency,
                                String careTime,
                                String healthMonitoring,
                                String dailyCare,
                                String psychologicalCare,
                                String safetyPrecaution) {
        CarePlan plan = new CarePlan();
        plan.setElderId(elderId);
        plan.setVersion(1);
        plan.setStatus("active");
        plan.setStartDate(startDate);
        plan.setEndDate(endDate);
        plan.setExecutionFrequency(executionFrequency);
        plan.setCareTime(careTime);
        plan.setAiGenerated(Boolean.FALSE);
        plan.setHealthMonitoring(healthMonitoring);
        plan.setDailyCare(dailyCare);
        plan.setPsychologicalCare(psychologicalCare);
        plan.setSafetyPrecaution(safetyPrecaution);
        plan.setCreatedBy(1L);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        return carePlanRepository.save(plan);
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

    private void bindNurse(Long elderId, Long nurseId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setNurseId(nurseId);
        assignment.setIsActive(1);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());
        careTeamAssignmentRepository.save(assignment);
    }

    private AdmissionRecord createActiveAdmission(Long elderId, Long createdBy) {
        AdmissionRecord admission = new AdmissionRecord();
        admission.setElderId(elderId);
        admission.setBedId(elderId + 2000);
        admission.setStatus("active");
        admission.setStartDate(LocalDate.now().minusDays(1));
        admission.setDepositAmount(java.math.BigDecimal.ZERO);
        admission.setCreatedBy(createdBy);
        admission.setCreatedAt(LocalDateTime.now().minusDays(1));
        admission.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return admissionRecordRepository.save(admission);
    }

    private void createShift(Long staffId, LocalDate shiftDate, LocalTime startTime, LocalTime endTime) {
        StaffShiftSchedule shift = new StaffShiftSchedule();
        shift.setStaffId(staffId);
        shift.setShiftDate(shiftDate);
        shift.setShiftType("morning");
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setStatus("active");
        shift.setCreatedAt(LocalDateTime.now());
        shift.setUpdatedAt(LocalDateTime.now());
        staffShiftScheduleRepository.save(shift);
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
