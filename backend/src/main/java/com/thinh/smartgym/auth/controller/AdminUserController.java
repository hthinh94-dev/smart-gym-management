package com.thinh.smartgym.auth.controller;

import com.thinh.smartgym.auth.dto.admin.AdminUserResponse;
import com.thinh.smartgym.auth.dto.admin.LockUserRequest;
import com.thinh.smartgym.auth.dto.admin.LockUserResponse;
import com.thinh.smartgym.auth.dto.admin.UnlockUserResponse;
import com.thinh.smartgym.auth.service.AdminUserService;
import com.thinh.smartgym.common.config.OpenApiResponseSchemas;
import com.thinh.smartgym.common.enums.AccountStatus;
import com.thinh.smartgym.common.enums.RoleName;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.PageResponse;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Lấy danh sách tài khoản",
            description = "Phân trang, tìm theo tên/email, lọc role/status và tính động subscription còn hiệu lực."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách tài khoản thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.AdminUserPageSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Query parameter không hợp lệ (VAL-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_ADMIN (AUTH-002) hoặc Admin bị khóa/vô hiệu hóa (ACC-004/ACC-006)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<PageResponse<AdminUserResponse>> getUsers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Parameter(description = "Chỉ số trang, bắt đầu từ 0", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Số phần tử mỗi trang, từ 1 đến 100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Lọc theo role hệ thống")
            @RequestParam(required = false) RoleName role,
            @Parameter(description = "Lọc theo trạng thái tài khoản")
            @RequestParam(required = false) AccountStatus status,
            @Parameter(description = "Tìm không phân biệt hoa thường theo họ tên hoặc email", example = "member@gmail.com")
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

    @PatchMapping(
            value = "/{id}/lock",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Khóa tài khoản",
            description = "Chuyển Member ACTIVE sang LOCKED; không cho tự khóa/Admin khác và không đổi subscription."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Khóa tài khoản thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.LockUserSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "ID, trạng thái target hoặc lý do khóa không hợp lệ (VAL-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_ADMIN (AUTH-002) hoặc Admin bị khóa/vô hiệu hóa (ACC-004/ACC-006)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<LockUserResponse> lockUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Parameter(description = "ID tài khoản ACTIVE cần khóa", example = "102")
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody LockUserRequest request
    ) {
        return ApiResponse.success(
                "Tài khoản đã được khóa thành công. Người dùng sẽ không thể đăng nhập cho đến khi được mở khóa.",
                adminUserService.lockUser(principal, id, request)
        );
    }

    @PatchMapping(value = "/{id}/unlock", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Mở khóa tài khoản",
            description = "Chuyển tài khoản LOCKED sang ACTIVE; không mở DISABLED và không đổi subscription."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Mở khóa tài khoản thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.UnlockUserSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "ID hoặc trạng thái target không hợp lệ (VAL-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_ADMIN (AUTH-002) hoặc Admin bị khóa/vô hiệu hóa (ACC-004/ACC-006)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<UnlockUserResponse> unlockUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Parameter(description = "ID tài khoản LOCKED cần mở khóa", example = "102")
            @PathVariable @Min(1) Long id
    ) {
        return ApiResponse.success(
                "Tài khoản đã được mở khóa thành công. Người dùng có thể đăng nhập bình thường.",
                adminUserService.unlockUser(principal, id)
        );
    }
}
