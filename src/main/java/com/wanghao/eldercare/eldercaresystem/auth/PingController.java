package com.wanghao.eldercare.eldercaresystem.auth;

import com.wanghao.eldercare.eldercaresystem.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.success(Map.of("message", "pong"));
    }

    @GetMapping("/admin/ping")
    public ApiResponse<Map<String, String>> adminPing() {
        return ApiResponse.success(Map.of("message", "admin pong"));
    }
}
