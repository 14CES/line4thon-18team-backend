package com.dotlo.line4thon_18team.global.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    record HealthResponse(String status) {}

    // 배포 후 서버가 떴는지 브라우저로 확인하는 용도
    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("UP");
    }
}