package com.thinh.smartgym;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SmartGymApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
    }

    @Test
    void passwordEncoder_ShouldUseBcryptStrength12() {
        String encodedPassword = passwordEncoder.encode("TestPassword1");

        org.assertj.core.api.Assertions.assertThat(encodedPassword).startsWith("$2a$12$");
        org.assertj.core.api.Assertions.assertThat(passwordEncoder.matches("TestPassword1", encodedPassword)).isTrue();
    }

    @Test
    void protectedRequestWithoutToken_ShouldReturnAcc005() throws Exception {
        mockMvc.perform(get("/api/v1/member/security-probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ACC-005"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.details", aMapWithSize(0)));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void adminRequestWithMemberRole_ShouldReturnStandardForbiddenResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security-probe"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH-002"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.details", aMapWithSize(0)));
    }

    @Test
    void openApiDocument_ShouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.openapi").isNotEmpty())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/lock'].patch.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/unlock'].patch.security[0].bearerAuth").isArray());
    }

    @Test
    void openApiDocument_ShouldExposeAllM1ResponseContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['201']"
                        + ".content['application/json'].schema['$ref']",
                        endsWith("/RegisterSuccessResponse")))
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/register'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['200']"
                        + ".content['application/json'].schema['$ref']",
                        endsWith("/LoginSuccessResponse")))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.responses['500']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['200']"
                        + ".content['application/json'].schema['$ref']",
                        endsWith("/CurrentUserSuccessResponse")))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.responses['200']"
                        + ".content['application/json'].schema['$ref']",
                        endsWith("/AdminUserPageSuccessResponse")))
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/lock'].patch.responses['200']"
                        + ".content['application/json'].schema['$ref']",
                        endsWith("/LockUserSuccessResponse")))
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/lock'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/lock'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/lock'].patch.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/unlock'].patch.responses['200']"
                        + ".content['application/json'].schema['$ref']",
                        endsWith("/UnlockUserSuccessResponse")))
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/unlock'].patch.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/unlock'].patch.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users/{id}/unlock'].patch.responses['403']").exists());
    }

    @Test
    void openApiDocument_ShouldMarkSecretsAsWriteOnly() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.password.writeOnly").value(true))
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.confirmPassword.writeOnly").value(true))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password.writeOnly").value(true))
                .andExpect(jsonPath("$.components.schemas.RegisterRequest.required",
                        hasItems("fullName", "email", "password", "confirmPassword")))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.required",
                        hasItems("email", "password")))
                .andExpect(jsonPath("$.components.schemas.RegisterResponse.properties.password").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.LoginResponse.properties.password").doesNotExist());
    }

    @Test
    void openApiDocument_ShouldExposeAdminPaginationParametersAndTypedData() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].get.parameters[*].name",
                        hasItems("page", "size", "role", "status", "search")))
                .andExpect(jsonPath("$.components.schemas.AdminUserPageSuccessResponse.properties.data").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponseAdminUserResponse.properties.content.type")
                        .value("array"))
                .andExpect(jsonPath(
                                "$.components.schemas.PageResponseAdminUserResponse.properties.content.items['$ref']",
                                endsWith("/AdminUserResponse")))
                .andExpect(jsonPath("$.components.schemas.PageResponseAdminUserResponse.properties.totalElements").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponseAdminUserResponse.properties.totalPages").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponseAdminUserResponse.properties.currentPage").exists())
                .andExpect(jsonPath("$.components.schemas.PageResponseAdminUserResponse.properties.pageSize").exists());
    }

    @Test
    void openApiDocument_ShouldKeepSuccessAndErrorEnvelopesDistinct() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.RegisterSuccessResponse.required",
                        hasItems("success", "message", "data")))
                .andExpect(jsonPath("$.components.schemas.RegisterSuccessResponse.properties.errorCode")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.RegisterSuccessResponse.properties.details")
                        .doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.required",
                        hasItems("success", "errorCode", "message", "details")))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.required", not(hasItem("data"))));
    }

}
