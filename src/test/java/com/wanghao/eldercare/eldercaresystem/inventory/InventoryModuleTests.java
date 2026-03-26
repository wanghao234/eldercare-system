package com.wanghao.eldercare.eldercaresystem.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.inventory.*;
import com.wanghao.eldercare.eldercaresystem.dto.inventory.*;
import com.wanghao.eldercare.eldercaresystem.entity.inventory.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.User;
import com.wanghao.eldercare.eldercaresystem.mapper.inventory.*;
import com.wanghao.eldercare.eldercaresystem.mapper.user.UserRepository;
import com.wanghao.eldercare.eldercaresystem.service.inventory.*;
import java.math.BigDecimal;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SupplyIssueRecordRepository issueRecordRepository;
    @Autowired
    private SupplyStockRepository stockRepository;
    @Autowired
    private SupplyItemRepository itemRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        issueRecordRepository.deleteAll();
        stockRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void nurse_issue_success_should_deduct_stock() throws Exception {
        createUser("admin_inv_1", "admin");
        createUser("nurse_inv_1", "nurse");

        String adminToken = loginAndGetToken("admin_inv_1", "123456");
        String nurseToken = loginAndGetToken("nurse_inv_1", "123456");

        Long itemId = createItem(adminToken, "一次性手套");
        Long stockId = createStock(adminToken, itemId, "1F仓库", "100.00", "10.00");

        mockMvc.perform(post("/api/inventory/issues")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplyItemId":%d,
                                  "location":"1F仓库",
                                  "quantity":15.00,
                                  "note":"护理领用"
                                }
                                """.formatted(itemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.quantity").value(15.00));

        SupplyStock stock = stockRepository.findById(stockId).orElseThrow();
        assertThat(stock.getQuantity()).isEqualByComparingTo(new BigDecimal("85.00"));
    }

    @Test
    void issue_quantity_exceeds_stock_should_return_40001() throws Exception {
        createUser("admin_inv_2", "admin");
        createUser("nurse_inv_2", "nurse");

        String adminToken = loginAndGetToken("admin_inv_2", "123456");
        String nurseToken = loginAndGetToken("nurse_inv_2", "123456");

        Long itemId = createItem(adminToken, "消毒湿巾");
        createStock(adminToken, itemId, "2F仓库", "5.00", "1.00");

        mockMvc.perform(post("/api/inventory/issues")
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplyItemId":%d,
                                  "location":"2F仓库",
                                  "quantity":8.00,
                                  "note":"超量领用"
                                }
                                """.formatted(itemId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    @Test
    void nurse_cannot_update_stock_threshold_should_return_40301() throws Exception {
        createUser("admin_inv_3", "admin");
        createUser("nurse_inv_3", "nurse");

        String adminToken = loginAndGetToken("admin_inv_3", "123456");
        String nurseToken = loginAndGetToken("nurse_inv_3", "123456");

        Long itemId = createItem(adminToken, "护理垫");
        Long stockId = createStock(adminToken, itemId, "3F仓库", "20.00", "5.00");

        mockMvc.perform(put("/api/inventory/stocks/{id}", stockId)
                        .header("Authorization", "Bearer " + nurseToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity":20.00,
                                  "minThreshold":2.00
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    private Long createItem(String token, String itemName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/inventory/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "itemName":"%s",
                                  "category":"护理耗材",
                                  "unit":"个"
                                }
                                """.formatted(itemName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("supplyItemId").asLong();
    }

    private Long createStock(String token,
                             Long itemId,
                             String location,
                             String quantity,
                             String minThreshold) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/inventory/stocks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplyItemId":%d,
                                  "location":"%s",
                                  "quantity":%s,
                                  "minThreshold":%s
                                }
                                """.formatted(itemId, location, quantity, minThreshold)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("stockId").asLong();
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

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("token").asText();
    }
}
