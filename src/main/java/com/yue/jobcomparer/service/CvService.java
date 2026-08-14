package com.yue.jobcomparer.service;

import com.yue.jobcomparer.config.QuotaProperties;
import com.yue.jobcomparer.dto.CvCreateRequest;
import com.yue.jobcomparer.dto.CvDetailResponse;
import com.yue.jobcomparer.dto.CvListItemResponse;
import com.yue.jobcomparer.dto.CvUpdateRequest;
import com.yue.jobcomparer.entity.AuditAction;
import com.yue.jobcomparer.entity.Cv;
import com.yue.jobcomparer.entity.User;
import com.yue.jobcomparer.exception.CvLimitExceededException;
import com.yue.jobcomparer.exception.CvNotFoundException;
import com.yue.jobcomparer.exception.DuplicateCvNameException;
import com.yue.jobcomparer.repository.CvRepository;
import com.yue.jobcomparer.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CvService {

    private final CvRepository cvRepository;
    private final SecurityUtils securityUtils;
    private final QuotaProperties quotaProperties;
    private final AuditLogService auditLogService;

    @Transactional
    public CvDetailResponse create(CvCreateRequest request) {
        User user = securityUtils.getCurrentUser();
        Long userId = user.getId();
        int maxCvs = quotaProperties.limitsFor(user.getPlan()).getMaxCvs();

        long currentCount = cvRepository.countByUserIdAndDeletedAtIsNull(userId);
        if (currentCount >= maxCvs) {
            throw new CvLimitExceededException(
                    "You can have at most " + maxCvs + " active CVs. " +
                            "Please delete an existing one before creating a new one."
            );
        }

        Cv cv = Cv.builder()
                .userId(userId)
                .cvName(request.getCvName())
                .content(request.getContent())
                .build();

        try {
            cvRepository.save(cv);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCvNameException(
                    "A CV with name '" + request.getCvName() +"' already exists"
            );
        }

        auditLogService.recordResourceEvent(AuditAction.CV_CREATE, cv.getId());

        return toDetailResponse(cv);
    }

    public List<CvListItemResponse> list() {
        Long userId = securityUtils.getCurrentUserId();

        return cvRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toListItemResponse)
                .toList();
    }

    public CvDetailResponse getById(Long cvId) {
        Long userId = securityUtils.getCurrentUserId();
        Cv cv = cvRepository.findByIdAndUserIdAndDeletedAtIsNull(cvId, userId)
                .orElseThrow(() -> new CvNotFoundException("Cv not found: " + cvId));

        return toDetailResponse(cv);
    }

    @Transactional
    public CvDetailResponse update(Long cvId, CvUpdateRequest request) {
        Long userId = securityUtils.getCurrentUserId();

        Cv cv = cvRepository.findByIdAndUserIdAndDeletedAtIsNull(cvId, userId)
                .orElseThrow(() -> new CvNotFoundException("CV not found: " + cvId));

        cv.setCvName(request.getCvName());
        cv.setContent(request.getContent());

        try {
            cvRepository.save(cv);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCvNameException(
                    "A CV with name '" + request.getCvName() + "' already exists."
            );
        }

        auditLogService.recordResourceEvent(AuditAction.CV_UPDATE, cv.getId());

        return toDetailResponse(cv);
    }

    @Transactional
    public void delete(Long cvId) {
        Long userId = securityUtils.getCurrentUserId();

        Cv cv = cvRepository.findByIdAndUserIdAndDeletedAtIsNull(cvId, userId)
                .orElseThrow(() -> new CvNotFoundException("CV not found: " + cvId));

        cv.setDeletedAt(LocalDateTime.now());
        cvRepository.save(cv);

        auditLogService.recordResourceEvent(AuditAction.CV_DELETE, cv.getId());
    }

    private CvDetailResponse toDetailResponse(Cv cv) {
        return new CvDetailResponse(
                cv.getId(),
                cv.getCvName(),
                cv.getContent(),
                cv.getCreatedAt(),
                cv.getUpdatedAt()
        );
    }

    private CvListItemResponse toListItemResponse(Cv cv) {
        return new CvListItemResponse(
                cv.getId(),
                cv.getCvName(),
                cv.getCreatedAt(),
                cv.getUpdatedAt()
        );
    }
}
