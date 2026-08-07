package com.thinh.smartgym.membership.service;

import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.membership.dto.MembershipPackageResponse;
import com.thinh.smartgym.membership.dto.MembershipPackageUpsertRequest;
import com.thinh.smartgym.membership.entity.MembershipPackage;
import com.thinh.smartgym.membership.repository.MembershipPackageRepository;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MembershipPackageService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MIN_DURATION_DAYS = 1;
    private static final int MAX_DURATION_DAYS = 3650;

    private final MembershipPackageRepository membershipPackageRepository;
    private final AccountStatusGuard accountStatusGuard;

    @Transactional(readOnly = true)
    public List<MembershipPackageResponse> getPublicPackages() {
        return membershipPackageRepository.findByActiveTrueOrderByDurationDaysAscIdAsc()
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipPackageResponse> getAdminPackages(AuthenticatedUserPrincipal admin) {
        validateAdmin(admin);
        return membershipPackageRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public MembershipPackageResponse createPackage(
            AuthenticatedUserPrincipal admin,
            MembershipPackageUpsertRequest request
    ) {
        validateAdmin(admin);
        ValidatedPackageInput input = validateAndNormalize(request);
        if (membershipPackageRepository.existsByNormalizedName(input.normalizedName())) {
            throw duplicateName(input.normalizedName());
        }

        MembershipPackage membershipPackage = new MembershipPackage(
                input.name(),
                input.normalizedName(),
                input.description(),
                input.durationDays().shortValue(),
                input.price()
        );
        return toAdminResponse(saveWithUniqueConstraintMapping(membershipPackage, input.normalizedName()));
    }

    @Transactional
    public MembershipPackageResponse updatePackage(
            AuthenticatedUserPrincipal admin,
            Long packageId,
            MembershipPackageUpsertRequest request
    ) {
        validateAdmin(admin);
        MembershipPackage membershipPackage = findPackage(packageId);
        ValidatedPackageInput input = validateAndNormalize(request);
        if (membershipPackageRepository.existsByNormalizedNameAndIdNot(input.normalizedName(), packageId)) {
            throw duplicateName(input.normalizedName());
        }

        membershipPackage.update(
                input.name(),
                input.normalizedName(),
                input.description(),
                input.durationDays().shortValue(),
                input.price()
        );
        return toAdminResponse(saveWithUniqueConstraintMapping(membershipPackage, input.normalizedName()));
    }

    @Transactional
    public MembershipPackageResponse deactivatePackage(
            AuthenticatedUserPrincipal admin,
            Long packageId
    ) {
        validateAdmin(admin);
        MembershipPackage membershipPackage = findPackage(packageId);
        membershipPackage.deactivate();
        return toAdminResponse(membershipPackageRepository.saveAndFlush(membershipPackage));
    }

    private MembershipPackage saveWithUniqueConstraintMapping(
            MembershipPackage membershipPackage,
            String normalizedName
    ) {
        try {
            return membershipPackageRepository.saveAndFlush(membershipPackage);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateName(normalizedName);
        }
    }

    private MembershipPackage findPackage(Long packageId) {
        if (packageId == null || packageId < 1) {
            throw validationError("ID gói tập không hợp lệ.", Map.of(
                    "field", "id",
                    "constraint", "id phải lớn hơn 0"
            ));
        }
        return membershipPackageRepository.findById(packageId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MEMBERSHIP_PACKAGE_NOT_FOUND,
                        Map.of("packageId", packageId)
                ));
    }

    private ValidatedPackageInput validateAndNormalize(MembershipPackageUpsertRequest request) {
        if (request == null) {
            throw validationError("Dữ liệu gói tập là bắt buộc.", Map.of("field", "request"));
        }

        String name = normalizeWhitespace(request.name());
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw validationError("Tên gói tập phải có từ 3 đến 100 ký tự.", Map.of(
                    "field", "name",
                    "constraint", "size must be between 3 and 100"
            ));
        }

        Integer durationDays = request.durationDays();
        if (durationDays == null || durationDays < MIN_DURATION_DAYS || durationDays > MAX_DURATION_DAYS) {
            throw validationError("Thời lượng gói tập phải từ 1 đến 3650 ngày.", Map.of(
                    "field", "durationDays",
                    "constraint", "must be between 1 and 3650"
            ));
        }

        BigDecimal price = request.price();
        if (price == null || price.signum() < 0 || price.scale() > 2 || price.precision() - price.scale() > 10) {
            throw validationError("Giá gói tập không hợp lệ.", Map.of(
                    "field", "price",
                    "constraint", "must be non-negative with at most 10 integer and 2 fraction digits"
            ));
        }

        String description = normalizeNullableText(request.description());
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw validationError("Mô tả gói tập không được vượt quá 1000 ký tự.", Map.of(
                    "field", "description",
                    "constraint", "size must be at most 1000"
            ));
        }

        return new ValidatedPackageInput(
                name,
                name.toLowerCase(Locale.ROOT),
                description,
                durationDays,
                price
        );
    }

    private String normalizeWhitespace(String value) {
        if (value == null) {
            return "";
        }
        return WHITESPACE.matcher(value.trim()).replaceAll(" ");
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateAdmin(AuthenticatedUserPrincipal admin) {
        if (admin == null || admin.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
        accountStatusGuard.validateAccountStatusByUserId(admin.getId());
    }

    private BusinessException duplicateName(String normalizedName) {
        return new BusinessException(
                ErrorCode.MEMBERSHIP_PACKAGE_NAME_ALREADY_EXISTS,
                Map.of("field", "name", "normalizedName", normalizedName)
        );
    }

    private BusinessException validationError(String message, Object details) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message, details);
    }

    private MembershipPackageResponse toPublicResponse(MembershipPackage membershipPackage) {
        return new MembershipPackageResponse(
                membershipPackage.getId(),
                membershipPackage.getName(),
                membershipPackage.getDurationDays().intValue(),
                membershipPackage.getPrice(),
                responseDescription(membershipPackage),
                null,
                null,
                null
        );
    }

    private MembershipPackageResponse toAdminResponse(MembershipPackage membershipPackage) {
        return new MembershipPackageResponse(
                membershipPackage.getId(),
                membershipPackage.getName(),
                membershipPackage.getDurationDays().intValue(),
                membershipPackage.getPrice(),
                responseDescription(membershipPackage),
                membershipPackage.isActive(),
                membershipPackage.getCreatedAt(),
                membershipPackage.getUpdatedAt()
        );
    }

    private record ValidatedPackageInput(
            String name,
            String normalizedName,
            String description,
            Integer durationDays,
            BigDecimal price
    ) {
    }

    private String responseDescription(MembershipPackage membershipPackage) {
        return membershipPackage.getDescription() == null ? "" : membershipPackage.getDescription();
    }
}
