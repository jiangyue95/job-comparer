package com.yue.jobcomparer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yue.jobcomparer.ai.AiClient;
import com.yue.jobcomparer.dto.ParsedCvResponse;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.exception.AiResponseParseException;
import com.yue.jobcomparer.exception.DuplicateCvNameException;
import com.yue.jobcomparer.exception.FileSizeLimitException;
import com.yue.jobcomparer.exception.PdfParsingException;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.util.AiResponseUtils;
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
    private static final int CV_PARSE_MAX_TOKENS = 4096;

    private static final String PROMPT_TEMPLATE = """
                          You are an expert CV parser. Extract structured information from the following CV text and return ONLY a JSON object.                                                                                               \s
            
                          <cv_text>
                          {cv_text}                                                                                                                                                                                                           \s
                          </cv_text>                                                                                                                                                                                                          \s
            
                          Return ONLY a JSON object with this exact structure:                                                                                                                                                                \s
                          {  \s
                            "name": "<full name>",
                            "email": "<email address or null>",                                                                                                                                                                               \s
                            "phone": "<phone number or null>",
                            "location": "<city/country or null>",                                                                                                                                                                             \s
                            "summary": "<professional summary or null>",                                                                                                                                                                      \s
                            "skills": ["<skill1>", "<skill2>"],
                            "projects": [
                              {
                                "name": "<project name>",
                                "description": "<what the project does and your role>",
                                "techStack": ["<tech1>", "<tech2>"],
                                "url": "<project url or null>"
                                }
                            ],
                            "workExperiences": [                                                                                                                                                                                               \s
                              {                                                                                                                                                                                                               \s
                                "company": "<company name>",
                                "title": "<job title>",                                                                                                                                                                                       \s
                                "startDate": "<YYYY-MM or descriptive>",
                                "endDate": "<YYYY-MM or descriptive or null>",
                                "isCurrent": <true|false>,                                                                                                                                                                                    \s
                                "description": "<role description>"
                              }                                                                                                                                                                                                               \s
                            ],
                            "educations": [                                                                                                                                                                                                    \s
                              {
                                "institution": "<school name>",
                                "degree": "<degree type>",
                                "field": "<field of study>",
                                "startYear": <year as integer or null>,
                                "endYear": <year as integer or null>                                                                                                                                                                          \s
                              }
                            ]                                                                                                                                                                                                                 \s
                          }
                          Do not include any text, markdown formatting, or explanation outside the JSON object.
                          """;

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final CvRepository cvRepository;
    private final SecurityUtils securityUtils;

    public ParsedCvResponse parsePdf(MultipartFile file, String cvName, boolean save) {
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new FileSizeLimitException("File exceeds 2MB limit");
        }

        String rawText = extractText(file);
        String prompt = PROMPT_TEMPLATE.replace("{cv_text}", rawText);
        String aiResponse = aiClient.chat(prompt, CV_PARSE_MAX_TOKENS);

        log.debug("CV parse AI raw response: {}", aiResponse);

        String cleaned = AiResponseUtils.stripMarkdownFence(aiResponse);

        ParsedCvResponse result;
        try {
            result = objectMapper.readValue(cleaned, ParsedCvResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse CV AI response as JSON: {}", aiResponse, e);
            throw new AiResponseParseException("AI response could not be parsed as JSON");
        }

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
