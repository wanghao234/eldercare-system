package com.wanghao.eldercare.eldercaresystem.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wanghao.eldercare.eldercaresystem.user.User;
import com.wanghao.eldercare.eldercaresystem.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileUploadModuleTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        cleanupUploadDir();
    }

    @Test
    void family_can_upload_png_success() throws Exception {
        createUser("family_upload_1", "family");
        String token = loginAndGetToken("family_upload_1", "123456");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.png",
                "image/png",
                "fake-png-data".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.fileId").isNotEmpty())
                .andExpect(jsonPath("$.data.fileName").value("evidence.png"))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.containsString("/uploads/")))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String url = root.path("data").path("url").asText();
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        Path saved = fileStorageService.getStorageAbsolutePath().resolve(fileName);
        assertThat(Files.exists(saved)).isTrue();
    }

    @Test
    void upload_invalid_extension_should_return_40001() throws Exception {
        createUser("family_upload_2", "family");
        String token = loginAndGetToken("family_upload_2", "123456");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "xxx".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40001"));
    }

    private void cleanupUploadDir() throws IOException {
        Path dir = fileStorageService.getStorageAbsolutePath();
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        }
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
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return root.path("data").path("token").asText();
    }
}

