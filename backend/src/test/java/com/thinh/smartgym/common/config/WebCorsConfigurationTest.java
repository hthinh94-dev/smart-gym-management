package com.thinh.smartgym.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebCorsConfigurationTest {

    @Test
    @DisplayName("CORS chi giu cac origin cu the da trim va loai trung")
    void corsConfiguration_ShouldUseExplicitDistinctOrigins() {
        WebCorsConfiguration webCorsConfiguration = new WebCorsConfiguration(
                "http://localhost:5173, https://staging.smartgym.example.com, http://localhost:5173"
        );
        CorsConfigurationSource source = webCorsConfiguration.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "OPTIONS",
                "/api/v1/auth/register"
        );

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly(
                "http://localhost:5173",
                "https://staging.smartgym.example.com"
        );
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
        assertThat(configuration.getAllowedMethods()).contains("POST", "OPTIONS");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("CORS fail-fast khi cau hinh wildcard hoac danh sach rong")
    void corsConfiguration_ShouldRejectWildcardAndEmptyOrigins() {
        assertThatThrownBy(() -> new WebCorsConfiguration("*"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain '*'");

        assertThatThrownBy(() -> new WebCorsConfiguration("https://*.smartgym.example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain '*'");

        assertThatThrownBy(() -> new WebCorsConfiguration(" , "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("explicit origins");
    }
}
