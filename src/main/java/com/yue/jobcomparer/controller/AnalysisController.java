package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.AnalysisCreateRequest;
import com.yue.jobcomparer.dto.AnalysisResponse;
import com.yue.jobcomparer.dto.AnalysisSummaryResponse;
import com.yue.jobcomparer.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/summary")
    public ResponseEntity<AnalysisSummaryResponse> getSummary() {
        return ResponseEntity.ok(analysisService.getSummary());
    }

    @PostMapping
    public ResponseEntity<AnalysisResponse> create(@Valid @RequestBody AnalysisCreateRequest request) {
        AnalysisResponse response = analysisService.submit(request);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping
    public ResponseEntity<List<AnalysisResponse>> list() {
        return ResponseEntity.ok(analysisService.getHistory());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        analysisService.deleteAnalysis(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/viewed")
    public ResponseEntity<Void> markViewed(@PathVariable Long id) {
        analysisService.markViewed(id);
        return ResponseEntity.noContent().build();
    }
}
