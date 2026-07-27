package com.thinh.smartgym.security;

import com.thinh.smartgym.auth.entity.User;
import com.thinh.smartgym.auth.repository.UserRepository;
import com.thinh.smartgym.common.enums.AccountStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Component bảo vệ trạng thái tài khoản thời gian thực (AccountStatusGuard).
 *
 * Ghi chú kỹ thuật: Do hệ thống sử dụng cơ chế JWT Stateless nên Token đã phát hành không bị thu hồi trực tiếp từ phía Client.
 * Các request tiếp theo chứa Token hợp lệ vẫn sẽ vượt qua JwtAuthenticationFilter. Lớp AccountStatusGuard này đóng vai trò là
 * chốt chặn tầng Service/Endpoint để truy vấn trực tiếp DB, đảm bảo nếu accountStatus chuyển sang LOCKED hoặc DISABLED thì
 * request lập tức bị chặn lại với mã lỗi ACC-004 hoặc ACC-006.
 */
@Component("accountStatusGuard")
@RequiredArgsConstructor
public class AccountStatusGuard {

    private final UserRepository userRepository;

    /**
     * Kiểm tra trạng thái tài khoản dựa trên Email người dùng.
     * Ném ngoại lệ có mã ACC-004 nếu tài khoản bị khóa hoặc ACC-006 nếu bị vô hiệu hóa.
     *
     * @param email Địa chỉ email của người dùng
     * @throws UsernameNotFoundException Nếu không tìm thấy người dùng
     * @throws AccountStatusAccessDeniedException Nếu tài khoản bị LOCKED hoặc DISABLED
     */
    public void validateAccountStatusByEmail(String email) {
        String normalizedEmail = (email != null) ? email.trim().toLowerCase(Locale.ROOT) : "";
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalizedEmail));

        checkStatus(user.getAccountStatus());
    }

    /**
     * Kiểm tra trạng thái tài khoản dựa trên ID người dùng.
     * Ném ngoại lệ có mã ACC-004 nếu tài khoản bị khóa hoặc ACC-006 nếu bị vô hiệu hóa.
     *
     * @param userId ID của người dùng
     * @throws UsernameNotFoundException Nếu không tìm thấy người dùng
     * @throws AccountStatusAccessDeniedException Nếu tài khoản bị LOCKED hoặc DISABLED
     */
    public void validateAccountStatusByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        checkStatus(user.getAccountStatus());
    }

    /**
     * Entry point dạng boolean dành cho Method Security. Tài khoản không ACTIVE
     * vẫn ném ngoại lệ có cấu trúc để giữ đúng mã ACC-004/ACC-006.
     *
     * @param email Địa chỉ email của người dùng
     * @return true nếu tài khoản ACTIVE
     */
    public boolean isAccountActive(String email) {
        validateAccountStatusByEmail(email);
        return true;
    }

    /**
     * Hàm helper nội bộ kiểm tra trạng thái tài khoản và ném ngoại lệ tương ứng.
     *
     * @param status Trạng thái tài khoản AccountStatus
     */
    private void checkStatus(AccountStatus status) {
        if (status == AccountStatus.ACTIVE) {
            return;
        }

        if (status == AccountStatus.LOCKED || status == AccountStatus.DISABLED) {
            throw new AccountStatusAccessDeniedException(status);
        }
    }
}
