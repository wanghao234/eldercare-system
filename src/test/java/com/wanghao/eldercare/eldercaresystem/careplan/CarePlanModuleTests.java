package com.wanghao.eldercare.eldercaresystem.careplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.careplan.*;
import com.wanghao.eldercare.eldercaresystem.dto.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careplan.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.careplan.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.careplan.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
class CarePlanModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarePlanRepository carePlanRepository;

    @Autowired
    private CarePlanChangeRepository carePlanChangeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        carePlanChangeRepository.deleteAll();
        carePlanRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void approve_creates_new_active_and_inactivates_old() throws Exception {
        User elder = createUser("elderCP1", "elder");
        User nurse = createUser("nurse1", "nurse");
        createUser("leader1", "nurse_leader");
        bindNurse(elder.getUserId(), nurse.getUserId());

        CarePlan oldPlan = new CarePlan();
        oldPlan.setElderId(elder.getUserId());
        oldPlan.setVersion(1);
        oldPlan.setStatus("active");
        oldPlan.setPlanTitle("旧计划");
        oldPlan.setPlanContentJson("{\"a\":1}");
        oldPlan.setEffectiveDate(LocalDate.now().minusDays(5));
        oldPlan.setCreatedBy(1L);
        oldPlan.setCreatedAt(LocalDateTime.now().minusDays(5));
        oldPlan.setUpdatedAt(LocalDateTime.now().minusDays(5));
        oldPlan = carePlanRepository.save(oldPlan);

        CarePlan draftPlan = new CarePlan();
        draftPlan.setElderId(elder.getUserId());
        draftPlan.setVersion(2);
        draftPlan.setStatus("draft");
        draftPlan.setCareTime("新版护理计划");
        draftPlan.setCareContent("{\"a\":2}");
        draftPlan.setStartDate(LocalDate.now());
        draftPlan.setCreatedBy(nurse.getUserId());
        draftPlan.setCreatedAt(LocalDateTime.now().minusHours(2));
        draftPlan.setUpdatedAt(LocalDateTime.now().minusHours(2));
        draftPlan = carePlanRepository.save(draftPlan);

        String nurseToken = loginAndGetToken("nurse1", "123456");
        String leaderToken = loginAndGetToken("leader1", "123456");

        Long changeId = createChange(nurseToken, elder.getUserId(), draftPlan.getCarePlanId(), null, false);

        MvcResult approveResult = mockMvc.perform(post("/api/care-plan-changes/{id}/approve", changeId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.newPlanId").isNumber())
                .andExpect(jsonPath("$.data.generatedTaskCount").isNumber())
                .andReturn();

        JsonNode response = objectMapper.readTree(approveResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        Long newPlanId = response.path("data").path("newPlanId").asLong();

        List<CarePlan> plans = carePlanRepository.findAll();
        assertThat(plans).hasSize(2);

        CarePlan activePlan = carePlanRepository.findByElderIdAndStatus(elder.getUserId(), "active").orElseThrow();
        assertThat(activePlan.getVersion()).isEqualTo(2);
        assertThat(activePlan.getCarePlanId()).isEqualTo(draftPlan.getCarePlanId());
        assertThat(activePlan.getPlanTitle()).isEqualTo("新版护理计划");

        CarePlan oldAfter = carePlanRepository.findById(oldPlan.getCarePlanId()).orElseThrow();
        assertThat(oldAfter.getStatus()).isEqualTo("inactive");

        List<Task> tasks = taskRepository.findByRelatedBizTypeAndRelatedBizId("care_plan", newPlanId);
        assertThat(tasks).isNotEmpty();
    }

    @Test
    void special_change_requires_doctor_second_review_before_activation() throws Exception {
        User elder = createUser("elderCPSpecial", "elder");
        User nurse = createUser("nurseSpecial", "nurse");
        createUser("leaderSpecial", "nurse_leader");
        createUser("doctorSpecial", "doctor");
        bindNurse(elder.getUserId(), nurse.getUserId());

        CarePlan oldPlan = new CarePlan();
        oldPlan.setElderId(elder.getUserId());
        oldPlan.setVersion(1);
        oldPlan.setStatus("active");
        oldPlan.setPlanTitle("普通护理计划");
        oldPlan.setPlanContentJson("{\"level\":\"normal\"}");
        oldPlan.setEffectiveDate(LocalDate.now().minusDays(3));
        oldPlan.setCreatedBy(nurse.getUserId());
        oldPlan.setCreatedAt(LocalDateTime.now().minusDays(3));
        oldPlan.setUpdatedAt(LocalDateTime.now().minusDays(3));
        carePlanRepository.save(oldPlan);

        String nurseToken = loginAndGetToken("nurseSpecial", "123456");
        String leaderToken = loginAndGetToken("leaderSpecial", "123456");
        String doctorToken = loginAndGetToken("doctorSpecial", "123456");

        MvcResult createResult = mockMvc.perform(post("/api/care-plan-changes")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "reason":"术后重点观察",
                                  "changeType":"special",
                                  "requiresDoctorReview":true,
                                  "proposedTitle":"术后恢复计划",
                                  "proposedContent":{
                                    "careTime":"术后恢复计划",
                                    "careContent":"重点监测生命体征",
                                    "startDate":"2026-03-30",
                                    "dietPlan":"流食过渡"
                                  }
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        Long changeId = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/care-plan-changes/{id}/approve", changeId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"护士长初审通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("doctor_review"));

        CarePlan stillActive = carePlanRepository.findByElderIdAndStatus(elder.getUserId(), "active").orElseThrow();
        assertThat(stillActive.getCareTime()).isEqualTo("普通护理计划");

        mockMvc.perform(post("/api/care-plan-changes/{id}/approve", changeId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"医生复审通过\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.newPlanId").isNumber());

        CarePlan activePlan = carePlanRepository.findByElderIdAndStatus(elder.getUserId(), "active").orElseThrow();
        assertThat(activePlan.getVersion()).isEqualTo(2);
        assertThat(activePlan.getCareTime()).isEqualTo("术后恢复计划");
        assertThat(activePlan.getDietPlan()).isEqualTo("流食过渡");
    }

    @Test
    void regenerate_tasks_replaces_future_unfinished_without_duplicate() throws Exception {
        User elder = createUser("elderCP2", "elder");
        User nurse = createUser("nurse2", "nurse");
        createUser("adminCP2", "admin");
        bindNurse(elder.getUserId(), nurse.getUserId());

        CarePlan plan = new CarePlan();
        plan.setElderId(elder.getUserId());
        plan.setVersion(1);
        plan.setStatus("active");
        plan.setPlanTitle("基础护理计划");
        plan.setPlanContentJson("""
                {
                  "templateKey":"basic_rounding_v1",
                  "items":[
                    {"type":"care","title":"翻身","times":["08:00","20:00"],"priority":"high","notes":"防压疮"}
                  ]
                }
                """);
        plan.setEffectiveDate(LocalDate.now());
        plan.setCreatedBy(1L);
        plan.setCreatedAt(LocalDateTime.now().minusDays(1));
        plan.setUpdatedAt(LocalDateTime.now().minusDays(1));
        plan = carePlanRepository.save(plan);

        Task stale = new Task();
        stale.setElderId(elder.getUserId());
        stale.setTaskType("care");
        stale.setTitle("旧任务");
        stale.setStatus("pending");
        stale.setDueAt(LocalDateTime.now().plusHours(4));
        stale.setRelatedBizType("care_plan");
        stale.setRelatedBizId(plan.getCarePlanId());
        stale.setCreatedAt(LocalDateTime.now().minusHours(1));
        stale.setUpdatedAt(LocalDateTime.now().minusHours(1));
        taskRepository.save(stale);

        String adminToken = loginAndGetToken("adminCP2", "123456");
        mockMvc.perform(post("/api/care-plans/{id}/regenerate-tasks", plan.getCarePlanId())
                        .param("days", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        List<Task> tasks = taskRepository.findByRelatedBizTypeAndRelatedBizId("care_plan", plan.getCarePlanId());
        assertThat(tasks).isNotEmpty();
        assertThat(tasks.stream().noneMatch(t -> "旧任务".equals(t.getTitle()))).isTrue();

        Set<String> uniqueKeys = tasks.stream()
                .map(t -> t.getTitle() + "|" + t.getDueAt())
                .collect(Collectors.toSet());
        assertThat(uniqueKeys.size()).isEqualTo(tasks.size());
    }

    @Test
    void nurse_approve_non_visible_elder_change_returns_40301() throws Exception {
        User elder = createUser("elderCP3", "elder");
        createUser("leader3", "nurse_leader");
        createUser("nurse3", "nurse");

        String leaderToken = loginAndGetToken("leader3", "123456");
        String nurseToken = loginAndGetToken("nurse3", "123456");

        Long changeId = createChange(leaderToken, elder.getUserId(), null, null, false);
        mockMvc.perform(post("/api/care-plan-changes/{id}/approve", changeId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"尝试审批\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    @Test
    void list_care_plans_with_page_params_returns_200() throws Exception {
        User elder = createUser("elderCPList", "elder");
        createUser("elderCPList2", "elder");
        createUser("adminCPList", "admin");

        CarePlan plan = new CarePlan();
        plan.setElderId(elder.getUserId());
        plan.setVersion(1);
        plan.setStatus("active");
        plan.setCareTime("早晚巡房");
        plan.setCareContent("{\"items\":[{\"type\":\"rounding\",\"times\":[\"08:00\",\"20:00\"]}]}");
        plan.setStartDate(LocalDate.now());
        plan.setCreatedBy(1L);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        carePlanRepository.save(plan);

        String adminToken = loginAndGetToken("adminCPList", "123456");
        mockMvc.perform(get("/api/care-plans")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void nurse_list_only_shows_bound_elders_with_or_without_plan() throws Exception {
        User elder1 = createUser("elderListN1", "elder");
        User elder2 = createUser("elderListN2", "elder");
        User nurse = createUser("nurseListOnly", "nurse");
        bindNurse(elder1.getUserId(), nurse.getUserId());

        CarePlan draft = new CarePlan();
        draft.setElderId(elder1.getUserId());
        draft.setVersion(1);
        draft.setStatus("draft");
        draft.setCareTime("基础护理");
        draft.setCreatedBy(nurse.getUserId());
        draft.setCreatedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());
        carePlanRepository.save(draft);

        String nurseToken = loginAndGetToken("nurseListOnly", "123456");
        mockMvc.perform(get("/api/care-plans")
                        .header("Authorization", "Bearer " + nurseToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].elderId").value(elder1.getUserId()))
                .andExpect(jsonPath("$.data.content[0].hasCarePlan").value(true));
    }

    @Test
    void doctor_list_only_shows_doctor_review_items() throws Exception {
        User elder1 = createUser("elderDoctorList1", "elder");
        User elder2 = createUser("elderDoctorList2", "elder");
        User nurse = createUser("nurseDoctorList", "nurse");
        createUser("leaderDoctorList", "nurse_leader");
        createUser("doctorDoctorList", "doctor");
        bindNurse(elder1.getUserId(), nurse.getUserId());
        bindNurse(elder2.getUserId(), nurse.getUserId());

        CarePlan active1 = new CarePlan();
        active1.setElderId(elder1.getUserId());
        active1.setVersion(1);
        active1.setStatus("active");
        active1.setCareTime("老人1计划");
        active1.setCreatedBy(nurse.getUserId());
        active1.setCreatedAt(LocalDateTime.now().minusDays(1));
        active1.setUpdatedAt(LocalDateTime.now().minusDays(1));
        carePlanRepository.save(active1);

        CarePlan active2 = new CarePlan();
        active2.setElderId(elder2.getUserId());
        active2.setVersion(1);
        active2.setStatus("active");
        active2.setCareTime("老人2计划");
        active2.setCreatedBy(nurse.getUserId());
        active2.setCreatedAt(LocalDateTime.now().minusDays(1));
        active2.setUpdatedAt(LocalDateTime.now().minusDays(1));
        carePlanRepository.save(active2);

        String nurseToken = loginAndGetToken("nurseDoctorList", "123456");
        String leaderToken = loginAndGetToken("leaderDoctorList", "123456");
        String doctorToken = loginAndGetToken("doctorDoctorList", "123456");

        Long changeId = createChange(nurseToken, elder1.getUserId(), null, "special", true);
        mockMvc.perform(post("/api/care-plan-changes/{id}/approve", changeId)
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"进入医生复审\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("doctor_review"));

        mockMvc.perform(get("/api/care-plans")
                        .header("Authorization", "Bearer " + doctorToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].elderId").value(elder1.getUserId()))
                .andExpect(jsonPath("$.data.content[0].pendingChangeStatus").value("doctor_review"));
    }

    @Test
    void nurse_can_create_update_delete_care_plan_draft() throws Exception {
        User elder = createUser("elderCRUD", "elder");
        User nurse = createUser("nurseCRUD", "nurse");
        bindNurse(elder.getUserId(), nurse.getUserId());
        String nurseToken = loginAndGetToken("nurseCRUD", "123456");

        MvcResult createResult = mockMvc.perform(post("/api/care-plans")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId": %d,
                                  "status": "draft",
                                  "startDate": "2026-03-01",
                                  "endDate": "2026-03-31",
                                  "careTime": "每天08:00/20:00",
                                  "careContent": "翻身与巡房",
                                  "medicationReminder": "饭后服药",
                                  "dietPlan": "低盐"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.elderId").value(elder.getUserId()))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.endDate").value("2026-03-31"))
                .andReturn();

        Long planId = objectMapper.readTree(createResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("carePlanId").asLong();

        mockMvc.perform(put("/api/care-plans/{id}", planId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId": %d,
                                  "version": 1,
                                  "status": "draft",
                                  "startDate": "2026-03-01",
                                  "endDate": "2026-04-01",
                                  "careTime": "每天09:00",
                                  "careContent": "改为白天巡房",
                                  "medicationReminder": "早餐后",
                                  "dietPlan": "低脂"
                                }
                                """.formatted(elder.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.endDate").value("2026-04-01"));

        mockMvc.perform(delete("/api/care-plans/{id}", planId)
                        .header("Authorization", "Bearer " + nurseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        assertThat(carePlanRepository.findById(planId)).isEmpty();
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

    private Long createChange(String token,
                              Long elderId,
                              Long draftPlanId,
                              String changeType,
                              boolean requiresDoctorReview) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/care-plan-changes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "draftPlanId":%s,
                                  "reason":"调整护理强度",
                                  "changeType":%s,
                                  "requiresDoctorReview":%s,
                                  "proposedTitle":"新版护理计划",
                                  "proposedContent":{
                                    "templateKey":"basic_rounding_v1",
                                    "items":[
                                      {"type":"rounding","title":"巡房","times":["08:00","14:00"],"priority":"normal","notes":"观察"}
                                    ]
                                  }
                                }
                                """.formatted(
                                elderId,
                                draftPlanId == null ? "null" : draftPlanId.toString(),
                                changeType == null ? "null" : "\"" + changeType + "\"",
                                requiresDoctorReview)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();
    }
}
