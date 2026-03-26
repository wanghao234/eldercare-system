package com.wanghao.eldercare.eldercaresystem.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.billing.*;
import com.wanghao.eldercare.eldercaresystem.dto.billing.*;
import com.wanghao.eldercare.eldercaresystem.entity.billing.*;
import com.wanghao.eldercare.eldercaresystem.entity.careteam.CareTeamAssignment;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.billing.*;
import com.wanghao.eldercare.eldercaresystem.mapper.careteam.CareTeamAssignmentRepository;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.billing.*;
import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CareTeamAssignmentRepository careTeamAssignmentRepository;
    @Autowired
    private FeeItemRepository feeItemRepository;
    @Autowired
    private BillItemRepository billItemRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        billItemRepository.deleteAll();
        billRepository.deleteAll();
        feeItemRepository.deleteAll();
        careTeamAssignmentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void admin_create_fee_item_generate_bill_pay_success() throws Exception {
        createUser("admin_bill_1", "admin");
        User elder = createUser("elder_bill_1", "elder");
        String adminToken = loginAndGetToken("admin_bill_1", "123456");

        Long feeItemId = createFeeItem(adminToken, "床位费", "accommodation", "天", new BigDecimal("100.00"));
        Long billId = generateBill(adminToken, elder.getUserId(), feeItemId, new BigDecimal("3.00"));

        mockMvc.perform(post("/api/billing/bills/{billId}/pay", billId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount":300.00,
                                  "method":"offline",
                                  "transactionNo":"T20260228001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("paid"));

        Bill bill = billRepository.findById(billId).orElseThrow();
        assertThat(bill.getStatus()).isEqualTo("paid");
        assertThat(paymentRepository.findByBillIdOrderByPaidAtDesc(billId)).hasSize(1);
    }

    @Test
    void family_can_only_access_bound_elder_bills() throws Exception {
        createUser("admin_bill_2", "admin");
        User family = createUser("family_bill_1", "family");
        User elder1 = createUser("elder_bill_2", "elder");
        User elder2 = createUser("elder_bill_3", "elder");

        bindFamily(elder1.getUserId(), family.getUserId());

        Bill bill1 = saveBill(elder1.getUserId(), new BigDecimal("120.00"));
        Bill bill2 = saveBill(elder2.getUserId(), new BigDecimal("80.00"));

        String familyToken = loginAndGetToken("family_bill_1", "123456");

        mockMvc.perform(get("/api/billing/bills?page=0&size=10")
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].billId").value(bill1.getBillId().intValue()));

        mockMvc.perform(get("/api/billing/bills")
                        .header("Authorization", "Bearer " + familyToken)
                        .param("elderId", String.valueOf(elder2.getUserId()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));

        mockMvc.perform(get("/api/billing/bills/{billId}", bill2.getBillId())
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    private Long createFeeItem(String token,
                               String itemName,
                               String category,
                               String unit,
                               BigDecimal unitPrice) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/billing/fee-items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemName":"%s",
                                  "category":"%s",
                                  "unit":"%s",
                                  "unitPrice":%s
                                }
                                """.formatted(itemName, category, unit, unitPrice.toPlainString())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("feeItemId").asLong();
    }

    private Long generateBill(String token,
                              Long elderId,
                              Long feeItemId,
                              BigDecimal quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/billing/bills/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "elderId":%d,
                                  "periodStart":"%s",
                                  "periodEnd":"%s",
                                  "items":[
                                    {"feeItemId":%d,"quantity":%s}
                                  ]
                                }
                                """.formatted(elderId, LocalDate.now(), LocalDate.now().plusDays(29), feeItemId, quantity.toPlainString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.totalAmount").value(300.00))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("billId").asLong();
    }

    private Bill saveBill(Long elderId, BigDecimal totalAmount) {
        Bill bill = new Bill();
        bill.setElderId(elderId);
        bill.setPeriodStart(LocalDate.of(2026, 2, 1));
        bill.setPeriodEnd(LocalDate.of(2026, 2, 28));
        bill.setTotalAmount(totalAmount);
        bill.setStatus("unpaid");
        bill.setGeneratedAt(LocalDateTime.now());
        bill.setCreatedBy(1L);
        return billRepository.save(bill);
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

    private void bindFamily(Long elderId, Long familyId) {
        CareTeamAssignment assignment = new CareTeamAssignment();
        assignment.setElderId(elderId);
        assignment.setFamilyId(familyId);
        assignment.setNurseId(null);
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
}
