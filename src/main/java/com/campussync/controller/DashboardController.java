package com.campussync.controller;

import com.campussync.dto.AdminDashboardResponse;
import com.campussync.dto.CoordinatorDashboardResponse;
import com.campussync.dto.StudentDashboardResponse;
import com.campussync.service.DashboardService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboards;

    public DashboardController(DashboardService dashboards) {
        this.dashboards = dashboards;
    }

    @GetMapping("/admin")
    public AdminDashboardResponse administrator(Principal principal) {
        return dashboards.administrator(principal);
    }

    @GetMapping("/student")
    public StudentDashboardResponse student(Principal principal) {
        return dashboards.student(principal);
    }

    @GetMapping("/coordinator")
    public CoordinatorDashboardResponse coordinator(Principal principal) {
        return dashboards.coordinator(principal);
    }
}
