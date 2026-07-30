package com.thinh.smartgym.security;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class AuthenticatedUserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final RoleName primaryRole;
    private final AccountStatus accountStatus;
    private final Instant createdAt;
    private final List<GrantedAuthority> authorities;

    private AuthenticatedUserPrincipal(
            Long id,
            String fullName,
            String email,
            String passwordHash,
            RoleName primaryRole,
            AccountStatus accountStatus,
            Instant createdAt,
            List<GrantedAuthority> authorities
    ) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.primaryRole = primaryRole;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.authorities = List.copyOf(authorities);
    }

    public static AuthenticatedUserPrincipal from(User user) {
        List<RoleName> roles = user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getName() != null)
                .map(userRole -> userRole.getRole().getName())
                .distinct()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();

        if (roles.isEmpty()) {
            throw new UsernameNotFoundException("User has no assigned system role");
        }

        List<GrantedAuthority> authorities = roles.stream()
                .map(RoleName::name)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new AuthenticatedUserPrincipal(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                roles.getFirst(),
                user.getAccountStatus(),
                user.getCreatedAt(),
                authorities
        );
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public RoleName getPrimaryRole() {
        return primaryRole;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus != AccountStatus.DISABLED;
    }

    @Override
    public String toString() {
        return "AuthenticatedUserPrincipal{id=" + id + ", email='" + email + "', accountStatus="
                + accountStatus + "}";
    }
}
