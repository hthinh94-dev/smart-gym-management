package com.thinh.smartgym.auth.controller;

import com.thinh.smartgym.auth.dto.admin.AdminUserResponse;
import com.thinh.smartgym.auth.dto.admin.LockUserRequest;
import com.thinh.smartgym.auth.dto.admin.LockUserResponse;
import com.thinh.smartgym.auth.dto.admin.UnlockUserResponse;
import com.thinh.smartgym.auth.service.AdminUserService;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.PageResponse;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Users", description = "Quản lý trạng thái tài khoản người dùng")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tài khoản có phân trang và bộ lọc")
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) @Size(max = 150) String search
    ) {
        PageResponse<AdminUserResponse> response = adminUserService.getUsers(
                principal,
                page,
                size,
                role,
                status,
                search
        );
        return ApiResponse.success("Lấy danh sách người dùng thành công", response);
    }

    @PatchMapping("/{id}/lock")
    @Operation(summary = "Khóa tài khoản ACTIVE mà không thay đổi subscription")
    public ApiResponse<LockUserResponse> lockUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody LockUserRequest request
    ) {
        return ApiResponse.success(
                "Tài khoản đã được khóa thành công. Người dùng sẽ không thể đăng nhập cho đến khi được mở khóa.",
                adminUserService.lockUser(principal, id, request)
        );
    }

    @PatchMapping("/{id}/unlock")
    @Operation(summary = "Mở khóa tài khoản LOCKED mà không thay đổi subscription")
    public ApiResponse<UnlockUserResponse> unlockUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable @Min(1) Long id
    ) {
        return ApiResponse.success(
                "Tài khoản đã được mở khóa thành công. Người dùng có thể đăng nhập bình thường.",
                adminUserService.unlockUser(principal, id)
        );
    }
}
