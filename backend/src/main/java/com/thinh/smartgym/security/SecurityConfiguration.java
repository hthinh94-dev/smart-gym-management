package com.thinh.smartgym.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Lớp cấu hình bảo mật trung tâm cho dự án Smart Gym Management API.
 * Áp dụng Spring Security 6.x / Spring Boot 3.x với kiến trúc Stateless JWT Authentication,
 * Phân quyền dựa trên vai trò (RBAC) và xử lý ngoại lệ chuẩn REST API (JSON).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    /**
     * Cấu hình Chuỗi Filter Bảo mật (Security Filter Chain).
     *
     * @param http Đối tượng HttpSecurity dùng để xây dựng cấu hình
     * @return SecurityFilterChain hoàn chỉnh
     * @throws Exception Ngoại lệ khi xây dựng cấu hình HttpSecurity
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Áp dụng danh sách CORS origin cụ thể từ biến môi trường.
                .cors(Customizer.withDefaults())

                // Disable CSRF vì ứng dụng sử dụng Stateless REST API với JWT Token
                .csrf(AbstractHttpConfigurer::disable)

                // Cấu hình Phân quyền Endpoints (RBAC)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/member/**").hasRole("MEMBER")
                        .anyRequest().authenticated()
                )

                // Cấu hình Session Management ở chế độ STATELESS
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Cấu hình Xử lý ngoại lệ chuẩn REST API
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Đăng ký AuthenticationProvider và chèn JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Bean Mã hóa Mật khẩu sử dụng thuật toán BCrypt.
     *
     * @return PasswordEncoder BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Bean Cung cấp Cơ chế Xác thực dựa trên DAO (CustomUserDetailsService + BCryptPasswordEncoder).
     *
     * @return AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Bean Quản lý Xác thực (AuthenticationManager) cho toàn ứng dụng.
     *
     * @param config Cấu hình xác thực từ Spring Security
     * @return AuthenticationManager
     * @throws Exception Ngoại lệ khi lấy AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
