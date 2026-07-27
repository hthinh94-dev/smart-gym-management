package com.thinh.smartgym.security;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Service phục vụ cơ chế xác thực JWT và Phân quyền cho Spring Security.
 * Chịu trách nhiệm nạp thông tin người dùng từ cơ sở dữ liệu dựa trên Email.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Nạp thông tin chi tiết của người dùng theo Email phục vụ xác thực Spring Security.
     *
     * @param email Địa chỉ email đăng nhập của người dùng
     * @return UserDetails chứa thông tin tài khoản, mật khẩu đã băm và tập hợp quyền (Authorities)
     * @throws UsernameNotFoundException Nếu không tìm thấy người dùng với email tương ứng
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Bước 1: Kiểm tra null/blank và Chuẩn hóa email
        String normalizedEmail = (email != null) ? email.trim().toLowerCase(Locale.ROOT) : "";

        // Bước 2: Truy vấn Database bằng Fetch Join để nạp User kèm Roles trong 1 query
        User user = userRepository.findByEmailWithRolesIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));

        // Bước 3: Ánh xạ Vai trò (Roles) sang GrantedAuthority
        List<GrantedAuthority> authorities = user.getUserRoles().stream()
                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getName() != null)
                .map(userRole -> new SimpleGrantedAuthority(userRole.getRole().getName().name()))
                .map(GrantedAuthority.class::cast)
                .toList();

        // Bước 4: Trả về UserDetails với trạng thái dùng cho luồng đăng nhập.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(user.getAccountStatus() == AccountStatus.DISABLED)
                .accountLocked(user.getAccountStatus() == AccountStatus.LOCKED)
                .build();
    }
}
