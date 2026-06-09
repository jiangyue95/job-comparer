package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.AnalysisCreateRequest;
import com.yue.jobcomparer.dto.AnalysisResponse;
import com.yue.jobcomparer.repository.AnalysisRepository;
import com.yue.jobcomparer.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping
    public ResponseEntity<AnalysisResponse> create(@Valid @RequestBody AnalysisCreateRequest request) {
        AnalysisResponse response = analysisService.analyze(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
