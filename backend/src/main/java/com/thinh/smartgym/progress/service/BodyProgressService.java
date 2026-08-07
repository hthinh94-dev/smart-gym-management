package com.thinh.smartgym.progress.service;

import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.progress.dto.BodyProgressResponse;
import com.thinh.smartgym.progress.dto.BodyProgressUpsertRequest;
import com.thinh.smartgym.progress.entity.BodyProgress;
import com.thinh.smartgym.progress.repository.BodyProgressRepository;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BodyProgressService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BodyProgressRepository bodyProgressRepository;
    private final AccountStatusGuard accountStatusGuard;
    private final Clock clock;

    @Transactional
    public BodyProgressResponse upsertCurrentProgress(
            AuthenticatedUserPrincipal principal,
            BodyProgressUpsertRequest request
    ) {
        Long memberId = requireMemberId(principal);
        accountStatusGuard.validateAccountStatusByUserId(memberId);
        validateRequest(request);

        if (request.muscleMassKg() == null && request.fatMassKg() == null) {
            bodyProgressRepository.upsertAtomic(memberId, request.recordDate(), request.weightKg());
        } else {
            bodyProgressRepository.upsertAtomicWithComposition(
                    memberId,
                    request.recordDate(),
                    request.weightKg(),
                    request.muscleMassKg(),
                    request.fatMassKg()
            );
        }

        BodyProgress progress = bodyProgressRepository
                .findByMember_IdAndRecordDate(memberId, request.recordDate())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR));
        return BodyProgressResponse.from(progress);
    }

    @Transactional(readOnly = true)
    public List<BodyProgressResponse> getCurrentProgress(AuthenticatedUserPrincipal principal) {
        Long memberId = requireMemberId(principal);
        accountStatusGuard.validateAccountStatusByUserId(memberId);
        return bodyProgressRepository.findByMember_IdOrderByRecordDateAsc(memberId)
                .stream()
                .map(BodyProgressResponse::from)
                .toList();
    }

    private Long requireMemberId(AuthenticatedUserPrincipal principal) {
        if (principal == null || principal.getId() == null || principal.getId() <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
        if (principal.getAuthorities().stream()
                .noneMatch(authority -> RoleName.ROLE_MEMBER.name().equals(authority.getAuthority()))) {
            throw new AccessDeniedException("ROLE_MEMBER is required");
        }
        return principal.getId();
    }

    private void validateRequest(BodyProgressUpsertRequest request) {
        if (request == null || request.recordDate() == null || request.weightKg() == null) {
            throw validation("request", "Ngày ghi nhận và cân nặng là bắt buộc.");
        }
        if (request.weightKg().signum() <= 0) {
            throw validation("weightKg", "Cân nặng phải lớn hơn 0.");
        }
        if (request.muscleMassKg() != null && request.muscleMassKg().compareTo(request.weightKg()) > 0) {
            throw validation("muscleMassKg", "Khối lượng cơ không thể lớn hơn cân nặng.");
        }
        if (request.fatMassKg() != null && request.fatMassKg().compareTo(request.weightKg()) > 0) {
            throw validation("fatMassKg", "Khối lượng mỡ không thể lớn hơn cân nặng.");
        }
        LocalDate today = LocalDate.now(clock.withZone(BUSINESS_ZONE));
        if (request.recordDate().isAfter(today)) {
            throw validation("recordDate", "Ngày ghi nhận không được ở tương lai.");
        }
    }

    private BusinessException validation(String field, String constraint) {
        return new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                "Dữ liệu tiến trình thể trạng không hợp lệ.",
                java.util.Map.of("field", field, "constraint", constraint)
        );
    }
}
