package com.thinh.smartgym.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CoreConfigurationTest {

    /** Kiểm tra nghiệp vụ dùng UTC clock để tránh sai lệch token/audit theo timezone máy chủ. */
    @Test
    @DisplayName("Business clock su dung UTC")
    void businessClock_ShouldUseUtc() {
        Instant before = Instant.now();

        Clock clock = new BusinessClockConfiguration().businessClock();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(clock.instant()).isBetween(before, Instant.now());
    }

    /** Kiểm tra cấu hình JPA bật auditing cho createdAt và updatedAt. */
    @Test
    @DisplayName("JpaAuditingConfiguration bat EnableJpaAuditing")
    void jpaAuditingConfiguration_ShouldEnableAuditing() {
        assertThat(JpaAuditingConfiguration.class.getAnnotation(EnableJpaAuditing.class)).isNotNull();
    }

    /** Kiểm tra metadata OpenAPI giữ đúng định danh API công khai của đồ án. */
    @Test
    @DisplayName("OpenAPI metadata dung title va version")
    void openApiConfiguration_ShouldDeclareProjectMetadata() {
        OpenAPIDefinition definition = OpenApiConfiguration.class.getAnnotation(OpenAPIDefinition.class);

        assertThat(definition).isNotNull();
        assertThat(definition.info().title()).isEqualTo("Smart Gym Management API");
        assertThat(definition.info().version()).isEqualTo("v1");
        assertThat(definition.info().description()).isNotBlank();
    }

    /** Kiểm tra Swagger khai báo đúng HTTP Bearer JWT để thử protected API. */
    @Test
    @DisplayName("OpenAPI khai bao bearerAuth JWT")
    void openApiConfiguration_ShouldDeclareBearerJwtScheme() {
        SecurityScheme scheme = OpenApiConfiguration.class.getAnnotation(SecurityScheme.class);

        assertThat(scheme).isNotNull();
        assertThat(scheme.name()).isEqualTo("bearerAuth");
        assertThat(scheme.scheme()).isEqualTo("bearer");
        assertThat(scheme.bearerFormat()).isEqualTo("JWT");
    }
}
