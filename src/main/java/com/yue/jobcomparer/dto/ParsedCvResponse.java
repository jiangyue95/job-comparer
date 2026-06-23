package com.yue.jobcomparer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedCvResponse {

    private String name;
    private String email;
    private String phone;
    private String location;
    private String summary;
    private List<String> skills;
    private List<Project> projects;
    private List<WorkExperience> workExperiences;
    private List<Education> educations;
    private String rawText;

    // Fill only when save=true
    private Long savedCvId;
    private String cvName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Project {
        private String name;
        private String description;
        private List<String> techStack;
        private String url;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperience {
        private String company;
        private String title;
        private String startDate;
        private String endDate;
        @JsonProperty("isCurrent")
        private Boolean isCurrent;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String institution;
        private String degree;
        private String field;
        private Integer startYear;
        private Integer endYear;
    }
}
