package com.thinh.smartgym.auth.service;

import com.thinh.smartgym.auth.dto.admin.AdminUserResponse;
import com.thinh.smartgym.auth.dto.admin.LockUserRequest;
import com.thinh.smartgym.auth.dto.admin.LockUserResponse;
import com.thinh.smartgym.auth.dto.admin.UnlockUserResponse;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.auth.repository.projection.AdminUserProjection;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.common.response.PageResponse;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final AccountStatusGuard accountStatusGuard;
    private final Clock businessClock;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(
            AuthenticatedUserPrincipal admin,
            int page,
            int size,
            RoleName role,
            AccountStatus status,
            String search
    ) {
        validateAdmin(admin);
        validatePage(page, size);

        String normalizedSearch = normalizeSearch(search);
        LocalDate today = businessDate();
        Page<AdminUserResponse> users = userRepository.findAdminUsers(
                        role == null ? null : role.name(),
                        status == null ? null : status.name(),
                        normalizedSearch,
                        today,
                        PageRequest.of(page, size)
                )
                .map(this::toAdminUserResponse);
        return PageResponse.from(users);
    }

    @Transactional
    public LockUserResponse lockUser(
            AuthenticatedUserPrincipal admin,
            Long targetUserId,
            LockUserRequest request
    ) {
        validateAdmin(admin);
        String reason = validateLockReason(request == null ? null : request.reason());
        User target = findTargetForUpdate(targetUserId);

        if (admin.getId().equals(target.getId())) {
            throw validationError(
                    "Admin không thể tự khóa tài khoản của chính mình.",
                    Map.of("userId", targetUserId, "constraint", "self-lock is not allowed")
            );
        }
        if (userRoleRepository.existsByUserIdAndRoleName(target.getId(), RoleName.ROLE_ADMIN)) {
            throw validationError(
                    "Không thể khóa tài khoản Admin khác trong phạm vi MVP.",
                    Map.of("userId", targetUserId, "constraint", "locking another admin is not allowed")
            );
        }
        if (target.getAccountStatus() != AccountStatus.ACTIVE) {
            throw invalidTargetStatus(
                    target,
                    "Chỉ tài khoản đang ở trạng thái ACTIVE mới có thể được khóa."
            );
        }

        boolean hasActiveSubscription = userRepository.countActiveSubscriptions(
                target.getId(),
                businessDate()
        ) > 0;
        target.setAccountStatus(AccountStatus.LOCKED);
        userRepository.saveAndFlush(target);

        return new LockUserResponse(
                target.getId(),
                target.getFullName(),
                target.getAccountStatus(),
                admin.getEmail(),
                Instant.now(businessClock),
                reason,
                hasActiveSubscription
                        ? "ACTIVE (không thay đổi)"
                        : "NO_ACTIVE_SUBSCRIPTION (không thay đổi)"
        );
    }

    @Transactional
    public UnlockUserResponse unlockUser(
            AuthenticatedUserPrincipal admin,
            Long targetUserId
    ) {
        validateAdmin(admin);
        User target = findTargetForUpdate(targetUserId);
        if (target.getAccountStatus() != AccountStatus.LOCKED) {
            throw invalidTargetStatus(
                    target,
                    "Chỉ tài khoản đang ở trạng thái LOCKED mới có thể được mở khóa."
            );
        }

        target.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.saveAndFlush(target);
        return new UnlockUserResponse(
                target.getId(),
                target.getFullName(),
                target.getAccountStatus(),
                admin.getEmail(),
                Instant.now(businessClock)
        );
    }

    private void validateAdmin(AuthenticatedUserPrincipal admin) {
        if (admin == null || admin.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
        accountStatusGuard.validateAccountStatusByUserId(admin.getId());
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw validationError(
                    "Chỉ số trang không hợp lệ.",
                    Map.of("field", "page", "constraint", "page phải lớn hơn hoặc bằng 0")
            );
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw validationError(
                    "Kích thước trang không hợp lệ.",
                    Map.of("field", "size", "constraint", "size phải từ 1 đến 100")
            );
        }
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDate businessDate() {
        return LocalDate.now(businessClock.withZone(BUSINESS_ZONE));
    }

    private AdminUserResponse toAdminUserResponse(AdminUserProjection projection) {
        return new AdminUserResponse(
                projection.getId(),
                projection.getFullName(),
                projection.getEmail(),
                RoleName.valueOf(projection.getRole()),
                AccountStatus.valueOf(projection.getAccountStatus()),
                projection.getCreatedAt().toInstant(),
                projection.getHasActiveSubscription() != null
                        && projection.getHasActiveSubscription() > 0
        );
    }

    private User findTargetForUpdate(Long targetUserId) {
        if (targetUserId == null || targetUserId < 1) {
            throw validationError(
                    "ID tài khoản không hợp lệ.",
                    Map.of("field", "id", "constraint", "id phải lớn hơn 0")
            );
        }
        return userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> validationError(
                        "Không tìm thấy tài khoản người dùng.",
                        Map.of("userId", targetUserId)
                ));
    }

    private String validateLockReason(String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.length() < 10 || normalized.length() > 500) {
            throw validationError(
                    "Lý do khóa tài khoản là bắt buộc và phải có từ 10 đến 500 ký tự.",
                    Map.of("field", "reason", "constraint", "reason phải có từ 10 đến 500 ký tự")
            );
        }

        String searchableReason = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        boolean mentionsSubscription = searchableReason.contains("goi")
                || searchableReason.contains("subscription")
                || searchableReason.contains("membership");
        boolean mentionsExpiration = searchableReason.contains("het han")
                || searchableReason.contains("expired")
                || searchableReason.contains("expiration");
        if (mentionsSubscription && mentionsExpiration) {
            throw validationError(
                    "Không được khóa tài khoản chỉ vì gói tập hoặc subscription hết hạn.",
                    Map.of(
                            "field", "reason",
                            "constraint", "Hết hạn gói tập không phải là lý do khóa tài khoản"
                    )
            );
        }
        return normalized;
    }

    private BusinessException invalidTargetStatus(User target, String message) {
        return validationError(
                message,
                Map.of(
                        "userId", target.getId(),
                        "currentAccountStatus", target.getAccountStatus().name()
                )
        );
    }

    private BusinessException validationError(String message, Object details) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message, details);
    }
}
