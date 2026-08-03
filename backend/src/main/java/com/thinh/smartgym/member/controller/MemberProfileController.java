package com.thinh.smartgym.member.controller;

import com.thinh.smartgym.common.config.OpenApiResponseSchemas;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.member.dto.MemberProfileResponse;
import com.thinh.smartgym.member.service.MemberProfileService;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member/profile")
@RequiredArgsConstructor
@Tag(name = "Member Profile", description = "Hồ sơ thể trạng và dinh dưỡng của hội viên")
@SecurityRequirement(name = "bearerAuth")
public class MemberProfileController {

    private final MemberProfileService memberProfileService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Lấy hồ sơ hội viên hiện hành",
            description = "Đọc hồ sơ theo User ID trong principal; không nhận memberId từ client."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy hồ sơ thể trạng thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.MemberProfileSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_MEMBER (AUTH-002) hoặc tài khoản bị chặn (ACC-004/ACC-006)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Hội viên chưa hoàn thiện hồ sơ (PROF-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<MemberProfileResponse> getCurrentProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Lấy hồ sơ thể trạng thành công",
                memberProfileService.getCurrentProfile(principal)
        );
    }
}
