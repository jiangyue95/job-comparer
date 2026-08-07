package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.ParsedCvResponse;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.exception.DuplicateCvNameException;
import com.yue.jobcomparer.exception.FileSizeLimitException;
import com.yue.jobcomparer.exception.PdfParsingException;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvParsingService {

    private static final long MAX_FILE_BYTES = 2 * 1024 * 1024L;

    private final CvRepository cvRepository;
    private final SecurityUtils securityUtils;

    public ParsedCvResponse parsePdf(MultipartFile file, String cvName, boolean save) {
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new FileSizeLimitException("File exceeds 2MB limit");
        }

        String rawText = extractText(file);
        ParsedCvResponse result = new ParsedCvResponse();
        result.setRawText(rawText);

        if (save) {
            Long userId = securityUtils.getCurrentUserId();
            String name = (cvName != null && !cvName.isBlank()) ? cvName: deriveNameFromFile(file);
            try {
                Cv saved = cvRepository.save(Cv.builder()
                        .userId(userId)
                        .cvName(name)
                        .content(rawText)
                        .build());
                result.setSavedCvId(saved.getId());
                result.setCvName(saved.getCvName());
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateCvNameException("A CV with the name '" + name + "' already exists.");
            }
        }

        return result;
    }

    private String extractText(MultipartFile file) {
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new PdfParsingException("Failed to parse PDF: " + e.getMessage(), e);
        }
    }

    private String deriveNameFromFile(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "Uploaded CV";
        }
        int dot = original.lastIndexOf('.');
        return dot > 9 ? original.substring(0, dot) : original;
    }
}
