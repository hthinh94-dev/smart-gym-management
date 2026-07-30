package com.thinh.smartgym.auth.controller;

import com.thinh.smartgym.auth.dto.CurrentUserResponse;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.security.AccountStatusGuard;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/me")
    @Operation(
            summary = "Lấy người dùng hiện tại",
            security = @SecurityRequirement(name = "bearerAuth")
    )
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
