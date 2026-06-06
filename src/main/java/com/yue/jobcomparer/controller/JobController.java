package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.*;
import com.yue.jobcomparer.entity.JobStatus;
import com.yue.jobcomparer.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobDetailResponse> create(@Valid @RequestBody JobCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<JobListItemResponse>> list(
            @RequestParam(required = false) JobStatus status) {
        return ResponseEntity.ok(jobService.list(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request
    ) {
        return ResponseEntity.ok(jobService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<JobDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateStatusRequest request
    ) {
        return ResponseEntity.ok(jobService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
