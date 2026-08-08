package com.thinh.smartgym.membership.subscription.service;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.membership.entity.MembershipPackage;
import com.thinh.smartgym.membership.repository.MembershipPackageRepository;
import com.thinh.smartgym.membership.subscription.dto.CreateSubscriptionRequest;
import com.thinh.smartgym.membership.subscription.dto.SubscriptionResponse;
import com.thinh.smartgym.membership.subscription.entity.MemberSubscription;
import com.thinh.smartgym.membership.subscription.entity.SubscriptionStatus;
import com.thinh.smartgym.membership.subscription.repository.MemberSubscriptionRepository;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberSubscriptionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final MembershipPackageRepository membershipPackageRepository;
    private final UserRepository userRepository;
    private final AccountStatusGuard accountStatusGuard;
    private final Clock clock;

    @Transactional
    public SubscriptionResponse createNewSubscription(
            AuthenticatedUserPrincipal principal,
            CreateSubscriptionRequest request
    ) {
        Long memberId = validateMember(principal);
        accountStatusGuard.validateAccountStatusByUserId(memberId);
        if (request == null || request.packageId() == null || request.packageId() < 1) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    Map.of("field", "packageId", "constraint", "packageId phải lớn hơn 0")
            );
        }

        User member = userRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR));
        MembershipPackage membershipPackage = membershipPackageRepository.findById(request.packageId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBERSHIP_PACKAGE_NOT_FOUND,
                        Map.of("packageId", request.packageId())
                ));

        if (!membershipPackage.isActive()) {
            throw new BusinessException(
                    ErrorCode.MEMBERSHIP_PACKAGE_INACTIVE,
                    Map.of("packageId", membershipPackage.getId(), "packageStatus", "INACTIVE")
            );
        }

        LocalDate businessDate = businessDate();
        memberSubscriptionRepository.findCurrentByMemberId(memberId, businessDate)
                .ifPresent(active -> {
                    throw new BusinessException(
                            ErrorCode.ACTIVE_SUBSCRIPTION_ALREADY_EXISTS,
                            Map.of(
                                    "currentActiveSubscriptionId", active.getId(),
                                    "currentEndDate", active.getEndDate()
                            )
                    );
                });
        memberSubscriptionRepository.findByMemberIdAndStatus(memberId, SubscriptionStatus.PENDING)
                .ifPresent(pending -> {
                    throw pendingConflict(pending.getId());
                });

        MemberSubscription subscription = new MemberSubscription(member, membershipPackage);
        try {
            return toResponse(memberSubscriptionRepository.saveAndFlush(subscription), businessDate);
        } catch (DataIntegrityViolationException exception) {
            throw pendingConflict(null);
        }
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getCurrentSubscription(AuthenticatedUserPrincipal principal) {
        Long memberId = validateMember(principal);
        accountStatusGuard.validateAccountStatusByUserId(memberId);
        LocalDate businessDate = businessDate();
        MemberSubscription subscription = memberSubscriptionRepository
                .findCurrentByMemberId(memberId, businessDate)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SUBSCRIPTION_NOT_FOUND,
                        Map.of("memberId", memberId)
                ));
        return toResponse(subscription, businessDate);
    }

    private Long validateMember(AuthenticatedUserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
        boolean isMember = principal.getAuthorities().stream()
                .anyMatch(authority -> RoleName.ROLE_MEMBER.name().equals(authority.getAuthority()));
        if (!isMember) {
            throw new AccessDeniedException("ROLE_MEMBER is required");
        }
        return principal.getId();
    }

    private LocalDate businessDate() {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE));
    }

    private SubscriptionResponse toResponse(MemberSubscription subscription, LocalDate businessDate) {
        Long daysRemaining = subscription.getEndDate() == null
                ? null
                : ChronoUnit.DAYS.between(businessDate, subscription.getEndDate());
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getMember().getId(),
                subscription.getMembershipPackage().getId(),
                subscription.getPackageNameSnapshot(),
                subscription.getPackagePriceSnapshot(),
                subscription.getStatus(),
                subscription.getCreatedAt(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                daysRemaining,
                subscription.getApprovedAt()
        );
    }

    private BusinessException pendingConflict(Long pendingRequestId) {
        Map<String, Object> details = pendingRequestId == null
                ? Map.of("pendingRequestStatus", SubscriptionStatus.PENDING.name())
                : Map.of(
                        "pendingRequestId", pendingRequestId,
                        "pendingRequestStatus", SubscriptionStatus.PENDING.name()
                );
        return new BusinessException(ErrorCode.PENDING_SUBSCRIPTION_ALREADY_EXISTS, details);
    }
}
