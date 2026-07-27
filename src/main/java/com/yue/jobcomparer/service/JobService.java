package com.yue.jobcomparer.service;

import com.yue.jobcomparer.dto.*;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.Job;
import com.yue.jobcomparer.entity.JobStatus;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.exception.JobLimitExceededException;
import com.yue.jobcomparer.exception.JobNotFoundException;
import com.yue.jobcomparer.repository.JobRepository;
import com.yue.jobcomparer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final int MAX_JOB_PER_USER = 50;

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public JobDetailResponse create(JobCreateRequest request) {
        Long userId = getCurrentUserId();

        long currentCount = jobRepository.countByUserIdAndDeletedAtIsNull(userId);
        if (currentCount >= MAX_JOB_PER_USER) {
            throw new JobLimitExceededException(
                    "You can have at most " + MAX_JOB_PER_USER + " active jobs. " +
                            "Please delete one before creating a new one."
            );
        }

        Job job = Job.builder()
                .userId(userId)
                .jobTitle(request.getJobTitle())
                .company(request.getCompany())
                .jobDescription(request.getJobDescription())
                .jobUrl(request.getJobUrl())
                .status(request.getStatus())
                .appliedAt(request.getAppliedAt())
                .notes(request.getNotes())
                .build();

        jobRepository.save(job);

        auditLogService.recordResourceEvent(AuditAction.JOB_CREATE, job.getId());

        return toDetailResponse(job);
    }

    public List<JobListItemResponse> list(JobStatus statusFilter) {
        Long userId = getCurrentUserId();

        List<Job> jobs = (statusFilter == null)
                ? jobRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                : jobRepository.findAllByUserIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(userId, statusFilter);

        return jobs.stream()
                .map(this::toListItemResponse)
                .toList();
    }

    public JobDetailResponse getById(Long jobId) {
        Long userId = getCurrentUserId();
        Job job = jobRepository.findByIdAndUserIdAndDeletedAtIsNull(jobId, userId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));
        return toDetailResponse(job);
    }

    @Transactional
    public JobDetailResponse update(Long jobId, JobUpdateRequest request) {
        Long userId = getCurrentUserId();
        Job job = jobRepository.findByIdAndUserIdAndDeletedAtIsNull(jobId, userId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));

        job.setJobTitle(request.getJobTitle());
        job.setCompany(request.getCompany());
        job.setJobDescription(request.getJobDescription());
        job.setJobUrl(request.getJobUrl());
        job.setStatus(request.getStatus());
        job.setAppliedAt(request.getAppliedAt());
        job.setNotes(request.getNotes());

        jobRepository.save(job);

        auditLogService.recordResourceEvent(AuditAction.JOB_UPDATE, job.getId());

        return toDetailResponse(job);
    }

    @Transactional
    public JobDetailResponse updateStatus(Long jobId, JobUpdateStatusRequest request) {
        Long userId = getCurrentUserId();
        Job job = jobRepository.findByIdAndUserIdAndDeletedAtIsNull(jobId, userId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));

        job.setStatus(request.getStatus());
        jobRepository.save(job);

        auditLogService.recordResourceEvent(AuditAction.JOB_STATUS_UPDATE, job.getId());

        return toDetailResponse(job);
    }

    @Transactional
    public void delete(Long jobId) {
        Long userId = getCurrentUserId();
        Job job = jobRepository.findByIdAndUserIdAndDeletedAtIsNull(jobId, userId)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + jobId));

        job.setDeletedAt(LocalDateTime.now());
        jobRepository.save(job);

        auditLogService.recordResourceEvent(AuditAction.JOB_DELETE, job.getId());
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: " + email));
        return user.getId();
    }

    private JobDetailResponse toDetailResponse(Job job) {
        return new JobDetailResponse(
                job.getId(),
                job.getJobTitle(),
                job.getCompany(),
                job.getJobDescription(),
                job.getJobUrl(),
                job.getStatus(),
                job.getAppliedAt(),
                job.getNotes(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private JobListItemResponse toListItemResponse(Job job) {
        return new JobListItemResponse(
                job.getId(),
                job.getJobTitle(),
                job.getCompany(),
                job.getJobUrl(),
                job.getStatus(),
                job.getAppliedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
