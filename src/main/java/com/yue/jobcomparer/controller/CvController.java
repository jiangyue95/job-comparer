package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.CvCreateRequest;
import com.yue.jobcomparer.dto.CvDetailResponse;
import com.yue.jobcomparer.dto.CvListItemResponse;
import com.yue.jobcomparer.dto.CvUpdateRequest;
import com.yue.jobcomparer.service.CvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cvs")
@RequiredArgsConstructor
public class CvController {

    private final CvService cvService;

    @PostMapping
    public ResponseEntity<CvDetailResponse> create(@Valid @RequestBody CvCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cvService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<CvListItemResponse>> list() {
        return ResponseEntity.ok(cvService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CvDetailResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cvService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CvDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CvUpdateRequest request
    ) {
        return ResponseEntity.ok(cvService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cvService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
