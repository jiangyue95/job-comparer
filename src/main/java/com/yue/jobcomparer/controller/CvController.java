package com.yue.jobcomparer.controller;

import com.yue.jobcomparer.dto.*;
import com.yue.jobcomparer.service.CvParsingService;
import com.yue.jobcomparer.service.CvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cvs")
@RequiredArgsConstructor
public class CvController {

    private final CvService cvService;
    private final CvParsingService cvParsingService;

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
    @PostMapping(value = "/parse-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ParsedCvResponse> parsePdf(
            @RequestPart("file")MultipartFile file,
            @RequestParam(required = false, defaultValue = "") String cvName,
            @RequestParam(defaultValue = "false") boolean save
            ) {
        return ResponseEntity.ok(cvParsingService.parsePdf(file, cvName, save));
    }
}
