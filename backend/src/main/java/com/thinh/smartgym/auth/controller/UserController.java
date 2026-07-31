package com.thinh.smartgym.auth.controller;

import com.thinh.smartgym.auth.dto.CurrentUserResponse;
import com.thinh.smartgym.common.config.OpenApiResponseSchemas;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.security.AccountStatusGuard;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Thông tin người dùng đang xác thực")
public class UserController {

    private final AccountStatusGuard accountStatusGuard;

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Lấy người dùng hiện tại",
            description = "Trả principal hiện hành và kiểm tra lại trạng thái tài khoản từ database.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy người dùng hiện tại thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.CurrentUserSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Tài khoản LOCKED (ACC-004) hoặc DISABLED (ACC-006)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<CurrentUserResponse> currentUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        accountStatusGuard.validateAccountStatusByUserId(principal.getId());

        CurrentUserResponse response = new CurrentUserResponse(
                principal.getId(),
                principal.getFullName(),
                principal.getEmail(),
                principal.getPrimaryRole(),
                principal.getAccountStatus(),
                principal.getCreatedAt()
        );
        return ApiResponse.success("Lấy thông tin người dùng thành công", response);
    }
}
