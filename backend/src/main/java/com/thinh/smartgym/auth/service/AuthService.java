package com.thinh.smartgym.auth.service;

import com.thinh.smartgym.auth.dto.LoginRequest;
import com.thinh.smartgym.auth.dto.LoginResponse;
import com.thinh.smartgym.auth.dto.LoginUserResponse;
import com.thinh.smartgym.auth.dto.RegisterRequest;
import com.thinh.smartgym.auth.dto.RegisterResponse;
import com.thinh.smartgym.auth.entity.Role;
import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.entity.UserRole;
import com.thinh.smartgym.auth.repository.RoleRepository;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.auth.repository.UserRoleRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.exception.BusinessException;
import com.thinh.smartgym.common.exception.ErrorCode;
import com.thinh.smartgym.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?!\\s)(?!.*\\s$)(?=.*[A-Z])(?=.*\\d).{8,72}$"
    );
    private static final String PASSWORD_CONSTRAINT =
            "Mật khẩu phải từ 8 đến 72 ký tự, chứa ít nhất 1 chữ hoa và 1 chữ số, "
                    + "không có khoảng trắng ở đầu hoặc cuối.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedFullName = normalizeAndValidateFullName(request.getFullName());
        String normalizedEmail = normalizeAndValidateEmail(request.getEmail());
        validatePassword(request.getPassword(), request.getConfirmPassword());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw duplicateEmail(normalizedEmail);
        }

        Role memberRole = roleRepository.findByName(RoleName.ROLE_MEMBER)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR));

        User user = new User(
                normalizedFullName,
                normalizedEmail,
                passwordEncoder.encode(request.getPassword()),
                AccountStatus.ACTIVE
        );

        try {
            User savedUser = userRepository.saveAndFlush(user);
            UserRole savedUserRole = userRoleRepository.saveAndFlush(new UserRole(savedUser, memberRole));
            savedUser.attachUserRole(savedUserRole);
            return toRegisterResponse(savedUser, memberRole);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueConstraintViolation(exception)) {
                throw duplicateEmail(normalizedEmail);
            }
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = normalizeLoginEmail(request.getEmail());
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (LockedException exception) {
            throw accountStatusError(ErrorCode.ACCOUNT_LOCKED, AccountStatus.LOCKED);
        } catch (DisabledException exception) {
            throw accountStatusError(ErrorCode.ACCOUNT_DISABLED, AccountStatus.DISABLED);
        } catch (BadCredentialsException | UsernameNotFoundException exception) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        } catch (AuthenticationException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }

        User user = userRepository.findByEmailWithRolesIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        ensureActive(user.getAccountStatus());

        RoleName primaryRole = resolvePrimaryRole(user);
        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR);
        }
        String accessToken = jwtService.generateAccessToken(userDetails);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                new LoginUserResponse(user.getId(), user.getFullName(), user.getEmail(), primaryRole)
        );
    }

    private String normalizeAndValidateFullName(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim();
        if (normalized.isBlank() || normalized.length() > 100) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    Map.of(
                            "field", "fullName",
                            "constraint", "Họ tên là bắt buộc và không được vượt quá 100 ký tự."
                    )
            );
        }
        return normalized;
    }

    private String normalizeAndValidateEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.length() > 150 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    Map.of(
                            "field", "email",
                            "constraint", "Email là bắt buộc, đúng định dạng và không vượt quá 150 ký tự.",
                            "rejectedValue", normalized
                    )
            );
        }
        return normalized;
    }

    private void validatePassword(String password, String confirmPassword) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(
                    ErrorCode.INVALID_PASSWORD,
                    Map.of("field", "password", "constraint", PASSWORD_CONSTRAINT)
            );
        }
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            throw new BusinessException(
                    ErrorCode.INVALID_PASSWORD,
                    Map.of(
                            "field", "confirmPassword",
                            "constraint", "Xác nhận mật khẩu phải khớp chính xác với mật khẩu."
                    )
            );
        }
    }

    private BusinessException duplicateEmail(String normalizedEmail) {
        return new BusinessException(
                ErrorCode.EMAIL_ALREADY_EXISTS,
                Map.of("field", "email", "rejectedValue", normalizedEmail)
        );
    }

    private boolean isEmailUniqueConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                if (constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT).endsWith("uk_users_email")) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("uk_users_email")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private RegisterResponse toRegisterResponse(User user, Role role) {
        return new RegisterResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                role.getName(),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }

    private String normalizeLoginEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureActive(AccountStatus accountStatus) {
        if (accountStatus == AccountStatus.LOCKED) {
            throw accountStatusError(ErrorCode.ACCOUNT_LOCKED, AccountStatus.LOCKED);
        }
        if (accountStatus == AccountStatus.DISABLED) {
            throw accountStatusError(ErrorCode.ACCOUNT_DISABLED, AccountStatus.DISABLED);
        }
    }

    private BusinessException accountStatusError(ErrorCode errorCode, AccountStatus accountStatus) {
        return new BusinessException(errorCode, Map.of("accountStatus", accountStatus.name()));
    }

    private RoleName resolvePrimaryRole(User user) {
        return user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getName() != null)
                .map(userRole -> userRole.getRole().getName())
                .distinct()
                .min(Comparator.comparingInt(RoleName::ordinal))
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_CONFIGURATION_ERROR));
    }
}
