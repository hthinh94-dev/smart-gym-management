package com.thinh.smartgym.membership.controller;

import com.thinh.smartgym.common.config.OpenApiResponseSchemas;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.membership.dto.MembershipPackageResponse;
import com.thinh.smartgym.membership.dto.MembershipPackageUpsertRequest;
import com.thinh.smartgym.membership.service.MembershipPackageService;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Membership Packages", description = "Danh mục công khai và quản trị gói tập")
public class MembershipPackageController {

    private final MembershipPackageService membershipPackageService;

    @GetMapping(value = "/packages", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lấy danh sách gói tập đang hoạt động")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lấy danh sách gói tập thành công",
            content = @Content(schema = @Schema(
                    implementation = OpenApiResponseSchemas.MembershipPackageListSuccessResponse.class
            ))
    )
    public ApiResponse<List<MembershipPackageResponse>> getPublicPackages() {
        return ApiResponse.success(
                "Lấy danh sách gói tập thành công",
                membershipPackageService.getPublicPackages()
        );
    }

    @GetMapping(value = "/admin/packages", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lấy toàn bộ gói tập để quản trị")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy danh sách quản trị thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.MembershipPackageListSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_ADMIN (AUTH-002) hoặc tài khoản Admin không ACTIVE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<List<MembershipPackageResponse>> getAdminPackages(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Lấy danh sách gói tập quản trị thành công",
                membershipPackageService.getAdminPackages(principal)
        );
    }

    @PostMapping(
            value = "/admin/packages",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Tạo gói tập")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Tạo gói tập thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.MembershipPackageSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ (VAL-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Tên chuẩn hóa bị trùng (SUB-007)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_ADMIN (AUTH-002) hoặc tài khoản Admin không ACTIVE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<MembershipPackageResponse>> createPackage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody MembershipPackageUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo gói tập thành công",
                membershipPackageService.createPackage(principal, request)
        ));
    }

    @PutMapping(
            value = "/admin/packages/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Cập nhật gói tập")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Cập nhật gói tập thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.MembershipPackageSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy gói tập (SUB-002)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Tên chuẩn hóa bị trùng (SUB-007)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu hoặc ID không hợp lệ (VAL-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_ADMIN (AUTH-002) hoặc tài khoản Admin không ACTIVE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<MembershipPackageResponse> updatePackage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Parameter(description = "ID gói tập", example = "1")
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody MembershipPackageUpsertRequest request
    ) {
        return ApiResponse.success(
                "Cập nhật gói tập thành công",
                membershipPackageService.updatePackage(principal, id, request)
        );
    }

    @DeleteMapping(value = "/admin/packages/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ngừng bán gói tập")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Vô hiệu hóa gói tập thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.MembershipPackageSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy gói tập (SUB-002)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "ID không hợp lệ (VAL-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_ADMIN (AUTH-002) hoặc tài khoản Admin không ACTIVE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<MembershipPackageResponse> deactivatePackage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Parameter(description = "ID gói tập", example = "1")
            @PathVariable @Min(1) Long id
    ) {
        return ApiResponse.success(
                "Vô hiệu hóa gói tập thành công",
                membershipPackageService.deactivatePackage(principal, id)
        );
    }
}
