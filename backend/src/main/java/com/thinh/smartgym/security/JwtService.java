package com.thinh.smartgym.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Service quản lý và thao tác với JWT Token cho hệ thống Smart Gym Management.
 * Sử dụng thư viện JJWT (io.jsonwebtoken 0.12.x).
 */
@Service
public class JwtService {

    private static final int MINIMUM_SECRET_BYTES = 32;

    @Value("${application.security.jwt.secret}")
    private String secretKey;

    @Value("${application.security.jwt.access-token-expiration-ms:${application.security.jwt.expiration:3600000}}")
    private long jwtExpiration;

    private SecretKey signingKey;

    @PostConstruct
    void initializeSigningKey() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }

        byte[] keyBytes = decodeSecret(secretKey);
        if (keyBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }

        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Tạo Access Token từ thông tin UserDetails.
     * Token chỉ chứa các claims định danh an toàn (subject: email, roles: danh sách quyền).
     * Tuyệt đối KHÔNG chứa password, passwordHash, chỉ số sinh học (height, weight) hay dữ liệu AI.
     *
     * @param userDetails Đối tượng người dùng Spring Security
     * @return Chuỗi JWT Access Token dạng compact
     */
    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", roles);

        return generateAccessToken(extraClaims, userDetails);
    }

    /**
     * Tạo Access Token với các claims mở rộng tùy chỉnh và UserDetails.
     *
     * @param extraClaims Các claims mở rộng
     * @param userDetails Đối tượng người dùng Spring Security
     * @return Chuỗi JWT Access Token dạng compact
     */
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Trích xuất Email (Username) từ Subject claim của JWT Token.
     *
     * @param token Chuỗi JWT Token
     * @return Email của người dùng
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Trích xuất Thời điểm hết hạn (Expiration Date) của JWT Token.
     *
     * @param token Chuỗi JWT Token
     * @return Thời điểm hết hạn của Token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Trích xuất một Claim cụ thể từ Token thông qua hàm resolver.
     *
     * @param token Chuỗi JWT Token
     * @param claimsResolver Hàm trích xuất dữ liệu từ Claims
     * @param <T> Kiểu dữ liệu của Claim cần trích xuất
     * @return Giá trị của Claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Kiểm tra tính hợp lệ của Token đối với UserDetails hiện tại.
     * Token hợp lệ khi Username trùng khớp và Token chưa bị hết hạn.
     *
     * @param token Chuỗi JWT Token
     * @param userDetails Đối tượng người dùng Spring Security
     * @return true nếu Token hợp lệ, ngược lại false
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username != null
                    && username.equalsIgnoreCase(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Kiểm tra Token đã hết hạn hay chưa.
     *
     * @param token Chuỗi JWT Token
     * @return true nếu đã hết hạn, false nếu còn hiệu lực
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Giải mã tất cả Claims trong JWT Token bằng khóa bí mật.
     *
     * @param token Chuỗi JWT Token
     * @return Đối tượng Claims chứa thông tin payload của Token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Tạo HMAC SecretKey để ký và xác thực JWT Token.
     * Tự động xử lý cả khóa định dạng Base64 lẫn UTF-8 Plain Text.
     *
     * @return Khóa SecretKey chuẩn cho thuật toán HMAC SHA
     */
    private SecretKey getSignInKey() {
        if (signingKey == null) {
            throw new IllegalStateException("JWT signing key has not been initialized");
        }
        return signingKey;
    }

    private byte[] decodeSecret(String configuredSecret) {
        byte[] plainTextBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] decodedBytes = Decoders.BASE64.decode(configuredSecret);
            if (decodedBytes.length >= MINIMUM_SECRET_BYTES) {
                return decodedBytes;
            }
        } catch (DecodingException exception) {
            // Plain-text secrets are supported for local deployments.
        }
        return plainTextBytes;
    }
}
