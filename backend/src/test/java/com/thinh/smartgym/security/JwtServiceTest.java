package com.thinh.smartgym.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit Test kiểm thử các chức năng của JwtService.
 * Kiểm tra việc khởi tạo token, bóc tách username và xác thực tính hợp lệ của token.
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private UserDetails userDetails;

    private static final String MOCK_EMAIL = "thinh.member@smartgym.com";
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_TIME_MS = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_TIME_MS);
        jwtService.initializeSigningKey();
    }

    @Test
    @DisplayName("Tạo JWT Token thành công, trả về chuỗi hợp lệ đủ 3 phần header/payload/signature")
    void generateToken_ShouldReturnValidNonNullToken() {
        // Arrange
        when(userDetails.getUsername()).thenReturn(MOCK_EMAIL);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(userDetails).getAuthorities();

        // Act
        String token = jwtService.generateAccessToken(userDetails);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Trích xuất Username từ Token khớp chính xác với Email ban đầu")
    void extractUsername_ShouldReturnCorrectEmail() {
        // Arrange
        when(userDetails.getUsername()).thenReturn(MOCK_EMAIL);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(userDetails).getAuthorities();
        String token = jwtService.generateAccessToken(userDetails);

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(MOCK_EMAIL);
    }

    @Test
    @DisplayName("Token hợp lệ khi kiểm tra với đúng UserDetails tạo ra nó")
    void isTokenValid_WithCorrectUser_ShouldReturnTrue() {
        // Arrange
        when(userDetails.getUsername()).thenReturn(MOCK_EMAIL);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(userDetails).getAuthorities();
        String token = jwtService.generateAccessToken(userDetails);

        // Act
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Token không hợp lệ (trả về false) khi kiểm tra với UserDetails của người dùng khác")
    void isTokenValid_WithDifferentUser_ShouldReturnFalse() {
        // Arrange
        when(userDetails.getUsername()).thenReturn(MOCK_EMAIL);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(userDetails).getAuthorities();
        String token = jwtService.generateAccessToken(userDetails);

        UserDetails otherUserDetails = mock(UserDetails.class);
        when(otherUserDetails.getUsername()).thenReturn("other.user@smartgym.com");

        // Act
        boolean isValid = jwtService.isTokenValid(token, otherUserDetails);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Token hết hạn được xác định là không hợp lệ")
    void isTokenValid_WithExpiredToken_ShouldReturnFalse() {
        when(userDetails.getUsername()).thenReturn(MOCK_EMAIL);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(userDetails).getAuthorities();
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        String token = jwtService.generateAccessToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    @DisplayName("Token ký bằng secret khác được xác định là không hợp lệ")
    void isTokenValid_WithDifferentSigningKey_ShouldReturnFalse() {
        when(userDetails.getUsername()).thenReturn(MOCK_EMAIL);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(userDetails).getAuthorities();
        String token = jwtService.generateAccessToken(userDetails);

        JwtService otherJwtService = new JwtService();
        ReflectionTestUtils.setField(
                otherJwtService,
                "secretKey",
                "69314A38704D346257327351356A58456D4C6E3972457155334B6647345A7856"
        );
        ReflectionTestUtils.setField(otherJwtService, "jwtExpiration", EXPIRATION_TIME_MS);
        otherJwtService.initializeSigningKey();

        assertThat(otherJwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    @DisplayName("Từ chối khởi tạo khi JWT secret ngắn hơn 32 byte")
    void initializeSigningKey_WithShortSecret_ShouldFailFast() {
        JwtService invalidJwtService = new JwtService();
        ReflectionTestUtils.setField(invalidJwtService, "secretKey", "short-secret");

        assertThatThrownBy(invalidJwtService::initializeSigningKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
