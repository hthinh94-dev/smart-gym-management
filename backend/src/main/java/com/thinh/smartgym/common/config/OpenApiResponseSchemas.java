package com.thinh.smartgym.common.config;

import com.thinh.smartgym.auth.dto.CurrentUserResponse;
import com.thinh.smartgym.auth.dto.LoginResponse;
import com.thinh.smartgym.auth.dto.RegisterResponse;
import com.thinh.smartgym.auth.dto.admin.AdminUserResponse;
import com.thinh.smartgym.auth.dto.admin.LockUserResponse;
import com.thinh.smartgym.auth.dto.admin.UnlockUserResponse;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.PageResponse;
import com.thinh.smartgym.member.dto.MemberProfileResponse;
import com.thinh.smartgym.progress.dto.BodyProgressResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Concrete generic envelopes used only to expose accurate OpenAPI response schemas.
 */
public final class OpenApiResponseSchemas {

    private OpenApiResponseSchemas() {
    }

    @Schema(name = "RegisterSuccessResponse")
    public static final class RegisterSuccessResponse extends ApiResponse<RegisterResponse> {
    }

    @Schema(name = "LoginSuccessResponse")
    public static final class LoginSuccessResponse extends ApiResponse<LoginResponse> {
    }

    @Schema(name = "CurrentUserSuccessResponse")
    public static final class CurrentUserSuccessResponse extends ApiResponse<CurrentUserResponse> {
    }

    @Schema(name = "MemberProfileSuccessResponse")
    public static final class MemberProfileSuccessResponse extends ApiResponse<MemberProfileResponse> {
    }

    public static final class BodyProgressSuccessResponse extends ApiResponse<BodyProgressResponse> {
    }

    public static final class BodyProgressListSuccessResponse
            extends ApiResponse<java.util.List<BodyProgressResponse>> {
    }

    @Schema(name = "AdminUserPageSuccessResponse")
    public static final class AdminUserPageSuccessResponse
            extends ApiResponse<PageResponse<AdminUserResponse>> {
    }

    @Schema(name = "LockUserSuccessResponse")
    public static final class LockUserSuccessResponse extends ApiResponse<LockUserResponse> {
    }

    @Schema(name = "UnlockUserSuccessResponse")
    public static final class UnlockUserSuccessResponse extends ApiResponse<UnlockUserResponse> {
    }
}
