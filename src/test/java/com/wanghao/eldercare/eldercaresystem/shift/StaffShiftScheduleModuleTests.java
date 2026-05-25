package com.wanghao.eldercare.eldercaresystem.shift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.entity.shift.StaffShiftSchedule;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.shift.StaffShiftScheduleRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffShiftScheduleModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StaffShiftScheduleRepository staffShiftScheduleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        staffShiftScheduleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_shift_with_defaults_and_query_stats_week() throws Exception {
        User admin = createUser("admin_shift_schedule_1", "admin", "管理员");
        User caregiver = createUser("caregiver_shift_schedule_1", "caregiver", "刘关华");
        String token = loginAndGetToken(admin.getUsername(), "123456");
        LocalDate shiftDate = LocalDate.of(2026, 5, 25);

        mockMvc.perform(post("/api/staff-shifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffId": %d,
                                  "shiftDate": "%s",
                                  "shiftType": "morning",
                                  "remark": "默认班次"
                                }
                                """.formatted(caregiver.getUserId(), shiftDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.staffId").value(caregiver.getUserId()))
                .andExpect(jsonPath("$.data.staffName").value("刘关华"))
                .andExpect(jsonPath("$.data.roleName").value("护理员"))
                .andExpect(jsonPath("$.data.weekDay").value("周一"))
                .andExpect(jsonPath("$.data.shiftType").value("morning"))
                .andExpect(jsonPath("$.data.shiftTypeName").value("早班"))
                .andExpect(jsonPath("$.data.startTime").value("08:00:00"))
                .andExpect(jsonPath("$.data.endTime").value("12:00:00"))
                .andExpect(jsonPath("$.data.timeRange").value("08:00 - 12:00"))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.statusName").value("生效中"));

        mockMvc.perform(get("/api/staff-shifts")
                        .header("Authorization", "Bearer " + token)
                        .param("staffId", String.valueOf(caregiver.getUserId()))
                        .param("startDate", shiftDate.toString())
                        .param("endDate", shiftDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roleName").value("护理员"))
                .andExpect(jsonPath("$.data[0].timeRange").value("08:00 - 12:00"));

        mockMvc.perform(get("/api/staff-shifts/week")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-05-25")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].staffId").value(caregiver.getUserId()))
                .andExpect(jsonPath("$.data[0].days[0].date").value("2026-05-25"))
                .andExpect(jsonPath("$.data[0].days[0].shifts[0].shiftTypeName").value("早班"));

        mockMvc.perform(get("/api/staff-shifts/stats")
                        .header("Authorization", "Bearer " + token)
                        .param("date", shiftDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onDutyCount").value(1))
                .andExpect(jsonPath("$.data.morningCount").value(1))
                .andExpect(jsonPath("$.data.conflictCount").value(0));

        mockMvc.perform(get("/api/staff-shifts/staff-options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].staffId").value(caregiver.getUserId()))
                .andExpect(jsonPath("$.data[0].roleName").value("护理员"));
    }

    @Test
    void create_conflict_returns_readable_conflicts() throws Exception {
        User admin = createUser("admin_shift_schedule_2", "admin", "管理员");
        User nurse = createUser("nurse_shift_schedule_1", "nurse", "王护士");
        String token = loginAndGetToken(admin.getUsername(), "123456");
        LocalDate shiftDate = LocalDate.of(2026, 5, 24);

        StaffShiftSchedule existing = new StaffShiftSchedule();
        existing.setStaffId(nurse.getUserId());
        existing.setShiftDate(shiftDate);
        existing.setShiftType("morning");
        existing.setStartTime(java.time.LocalTime.of(8, 0));
        existing.setEndTime(java.time.LocalTime.of(12, 0));
        existing.setStatus("active");
        existing.setRemark("已有排班");
        existing.setCreatedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        existing = staffShiftScheduleRepository.save(existing);

        mockMvc.perform(post("/api/staff-shifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffId": %d,
                                  "shiftDate": "%s",
                                  "shiftType": "afternoon",
                                  "startTime": "10:00:00",
                                  "endTime": "14:00:00",
                                  "status": "active"
                                }
                                """.formatted(nurse.getUserId(), shiftDate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"))
                .andExpect(jsonPath("$.message").value("该护理人员在当前日期已有重叠排班，请调整时间。"))
                .andExpect(jsonPath("$.data.conflicts[0].staffId").value(nurse.getUserId()))
                .andExpect(jsonPath("$.data.conflicts[0].existingShiftId").value(existing.getShiftId()))
                .andExpect(jsonPath("$.data.conflicts[0].existingTimeRange").value("08:00 - 12:00"))
                .andExpect(jsonPath("$.data.conflicts[0].newTimeRange").value("10:00 - 14:00"));
    }

    @Test
    void batch_create_daily_succeeds_with_multiple_dates() throws Exception {
        User admin = createUser("admin_shift_schedule_3", "admin", "管理员");
        User caregiver = createUser("caregiver_shift_schedule_2", "caregiver", "李护理");
        String token = loginAndGetToken(admin.getUsername(), "123456");

        mockMvc.perform(post("/api/staff-shifts/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffId": %d,
                                  "startDate": "2026-05-25",
                                  "endDate": "2026-05-31",
                                  "repeatType": "daily",
                                  "weekDays": [],
                                  "shiftType": "morning",
                                  "startTime": "08:00:00",
                                  "endTime": "12:00:00"
                                }
                                """.formatted(caregiver.getUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.createdCount").value(7))
                .andExpect(jsonPath("$.data.items[0].staffId").value(caregiver.getUserId()))
                .andExpect(jsonPath("$.data.items[0].shiftType").value("morning"))
                .andExpect(jsonPath("$.data.items[0].timeRange").value("08:00 - 12:00"))
                .andExpect(jsonPath("$.data.items[6].shiftDate").value("2026-05-31"));
    }

    @Test
    void my_shifts_supports_today_week_range_and_all_views() throws Exception {
        User admin = createUser("admin_shift_schedule_4", "admin", "管理员");
        User caregiver = createUser("caregiver_shift_schedule_3", "caregiver", "赵护理");
        User otherCaregiver = createUser("caregiver_shift_schedule_4", "caregiver", "钱护理");
        String adminToken = loginAndGetToken(admin.getUsername(), "123456");
        String caregiverToken = loginAndGetToken(caregiver.getUsername(), "123456");

        createShift(adminToken, caregiver.getUserId(), "2026-05-25", "morning", "08:00:00", "12:00:00");
        createShift(adminToken, caregiver.getUserId(), "2026-05-27", "afternoon", "14:00:00", "18:00:00");
        createShift(adminToken, caregiver.getUserId(), "2026-06-02", "morning", "08:00:00", "12:00:00");
        createShift(adminToken, otherCaregiver.getUserId(), "2026-05-25", "night", "20:00:00", "23:00:00");

        mockMvc.perform(get("/api/staff-shifts/my")
                        .header("Authorization", "Bearer " + caregiverToken)
                        .param("view", "today")
                        .param("date", "2026-05-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].staffId").value(caregiver.getUserId()))
                .andExpect(jsonPath("$.data[0].shiftDate").value("2026-05-25"))
                .andExpect(jsonPath("$.data[0].shiftType").value("morning"));

        mockMvc.perform(get("/api/staff-shifts/my")
                        .header("Authorization", "Bearer " + caregiverToken)
                        .param("view", "week")
                        .param("date", "2026-05-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].shiftDate").value("2026-05-25"))
                .andExpect(jsonPath("$.data[1].shiftDate").value("2026-05-27"));

        mockMvc.perform(get("/api/staff-shifts/my")
                        .header("Authorization", "Bearer " + caregiverToken)
                        .param("view", "range")
                        .param("startDate", "2026-05-26")
                        .param("endDate", "2026-06-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].shiftDate").value("2026-05-27"))
                .andExpect(jsonPath("$.data[1].shiftDate").value("2026-06-02"));

        MvcResult allResult = mockMvc.perform(get("/api/staff-shifts/my")
                        .header("Authorization", "Bearer " + caregiverToken)
                        .param("view", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andReturn();

        JsonNode allBody = objectMapper.readTree(allResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(allBody.path("data"))
                .allMatch(node -> node.path("staffId").asLong() == caregiver.getUserId());
    }

    @Test
    void my_shifts_range_requires_valid_dates() throws Exception {
        User caregiver = createUser("caregiver_shift_schedule_5", "caregiver", "孙护理");
        String token = loginAndGetToken(caregiver.getUsername(), "123456");

        mockMvc.perform(get("/api/staff-shifts/my")
                        .header("Authorization", "Bearer " + token)
                        .param("view", "range")
                        .param("startDate", "2026-05-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("时间范围查询必须提供合法的 startDate 和 endDate"));
    }

    @Test
    void my_shifts_forbidden_for_non_shift_roles() throws Exception {
        User elder = createUser("elder_shift_schedule_1", "elder", "陈长者");
        String token = loginAndGetToken(elder.getUsername(), "123456");

        mockMvc.perform(get("/api/staff-shifts/my")
                        .header("Authorization", "Bearer " + token)
                        .param("view", "all"))
                .andExpect(status().isForbidden());
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

    private void createShift(String token,
                             Long staffId,
                             String shiftDate,
                             String shiftType,
                             String startTime,
                             String endTime) throws Exception {
        mockMvc.perform(post("/api/staff-shifts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staffId": %d,
                                  "shiftDate": "%s",
                                  "shiftType": "%s",
                                  "startTime": "%s",
                                  "endTime": "%s"
                                }
                                """.formatted(staffId, shiftDate, shiftType, startTime, endTime)))
                .andExpect(status().isOk());
    }

    private User createUser(String username, String role, String realName) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setStatus("active");
        user.setRealName(realName);
        user.setPhone("13800000000");
        user.setEmail(username + "@test.local");
        user.setAvatarUrl("");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
}
