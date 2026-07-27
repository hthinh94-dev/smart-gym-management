package com.thinh.smartgym.security;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "signed-access-token";
    private static final String EMAIL = "member@smartgym.com";

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Request không có Bearer token đi tiếp mà không gọi JwtService")
    void doFilterInternal_WithoutBearerToken_ShouldContinueChain() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Token hợp lệ thiết lập SecurityContext từ UserDetails")
    void doFilterInternal_WithValidToken_ShouldSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(userDetails).getAuthorities();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(userDetails);
        verify(userDetails, never()).isAccountNonLocked();
        verify(userDetails, never()).isEnabled();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token sai chữ ký hoặc cấu trúc không thiết lập SecurityContext")
    void doFilterInternal_WithInvalidToken_ShouldContinueUnauthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenThrow(new MalformedJwtException("invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userDetailsService, never()).loadUserByUsername(EMAIL);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token của user không còn tồn tại đi tiếp dưới dạng chưa xác thực")
    void doFilterInternal_WithMissingUser_ShouldContinueUnauthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
        when(userDetailsService.loadUserByUsername(EMAIL))
                .thenThrow(new UsernameNotFoundException("user not found"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
