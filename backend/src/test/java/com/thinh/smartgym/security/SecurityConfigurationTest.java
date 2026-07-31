package com.thinh.smartgym.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigurationTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private RestAccessDeniedHandler accessDeniedHandler;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    private SecurityConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new SecurityConfiguration(
                jwtAuthenticationFilter,
                userDetailsService,
                authenticationEntryPoint,
                accessDeniedHandler
        );
    }

    /** Kiểm tra BCrypt được cấu hình strength 12 và vẫn xác minh đúng raw password. */
    @Test
    @DisplayName("PasswordEncoder dung BCrypt strength 12")
    void passwordEncoder_ShouldUseBcryptStrengthTwelve() {
        PasswordEncoder encoder = configuration.passwordEncoder();

        String encoded = encoder.encode("SecurePass1");

        assertThat(encoded).matches("^\\$2[aby]\\$12\\$.*");
        assertThat(encoder.matches("SecurePass1", encoded)).isTrue();
        assertThat(encoder.matches("WrongPass1", encoded)).isFalse();
    }

    /** Kiểm tra DAO provider dùng đúng UserDetailsService và password encoder của hệ thống. */
    @Test
    @DisplayName("AuthenticationProvider xac thuc bang UserDetailsService va BCrypt")
    void authenticationProvider_WithValidCredentials_ShouldAuthenticate() {
        PasswordEncoder encoder = configuration.passwordEncoder();
        when(userDetailsService.loadUserByUsername("member@smartgym.com"))
                .thenReturn(new User(
                        "member@smartgym.com",
                        encoder.encode("SecurePass1"),
                        List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
                ));
        AuthenticationProvider provider = configuration.authenticationProvider();
        Authentication request = UsernamePasswordAuthenticationToken.unauthenticated(
                "member@smartgym.com",
                "SecurePass1"
        );

        Authentication result = provider.authenticate(request);

        assertThat(result).isNotNull();
        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_MEMBER");
        verify(userDetailsService).loadUserByUsername("member@smartgym.com");
    }

    /** Kiểm tra AuthenticationManager bean chỉ ủy quyền cho Spring AuthenticationConfiguration. */
    @Test
    @DisplayName("AuthenticationManager lay tu AuthenticationConfiguration")
    void authenticationManager_ShouldDelegateToSpringConfiguration() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        AuthenticationManager result = configuration.authenticationManager(authenticationConfiguration);

        assertThat(result).isSameAs(authenticationManager);
        verify(authenticationConfiguration).getAuthenticationManager();
    }
}
