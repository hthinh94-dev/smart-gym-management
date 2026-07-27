package com.thinh.smartgym.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bộ lọc xác thực JWT Token cho mọi Request (OncePerRequestFilter).
 * Nhiệm vụ: Bóc tách JWT từ HTTP Header Authorization, kiểm tra tính hợp lệ về mặt kỹ thuật
 * (chữ ký, hạn dùng) và thiết lập thông tin xác thực vào SecurityContextHolder.
 *
 * CHÚ Ý KIẾN TRÚC: Filter này KHÔNG kiểm tra trạng thái accountStatus (LOCKED/DISABLED)
 * để tránh vi phạm ranh giới trách nhiệm. Việc chặn tài khoản bị khóa/vô hiệu hóa được
 * đảm nhiệm riêng bởi AccountStatusGuard.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Bước 1: Đọc Header Authorization từ HTTP Request
        final String authHeader = request.getHeader("Authorization");

        // Bước 2: Bỏ qua Filter nếu Header không tồn tại hoặc không bắt đầu bằng "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bước 3-6: Xác thực token, nạp identity/roles và thiết lập SecurityContext.
        final String jwt = authHeader.substring(7);
        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            SecurityContextHolder.clearContext();
        }

        // Security chain sẽ gọi AuthenticationEntryPoint nếu endpoint yêu cầu xác thực.
        filterChain.doFilter(request, response);
    }
}
