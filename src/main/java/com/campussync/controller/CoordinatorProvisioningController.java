package com.campussync.controller;

import com.campussync.dto.CoordinatorProvisionRequest;
import com.campussync.dto.CoordinatorProvisionResponse;
import com.campussync.service.CoordinatorProvisioningService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coordinators")
public class CoordinatorProvisioningController {
    private final CoordinatorProvisioningService provisioning;

    public CoordinatorProvisioningController(CoordinatorProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @PostMapping
    public ResponseEntity<CoordinatorProvisionResponse> create(
            @Valid @RequestBody CoordinatorProvisionRequest request, Principal principal) {
        CoordinatorProvisionResponse response = provisioning.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{userId}/committees/{committeeId}")
    public ResponseEntity<CoordinatorProvisionResponse> assignExisting(
            @PathVariable Long userId, @PathVariable Long committeeId, Principal principal) {
        return ResponseEntity.ok(provisioning.assignExisting(userId, committeeId, principal));
    }
}
