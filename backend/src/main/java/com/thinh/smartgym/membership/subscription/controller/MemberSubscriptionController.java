package com.thinh.smartgym.membership.subscription.controller;

import com.thinh.smartgym.common.config.OpenApiResponseSchemas;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.membership.subscription.dto.CreateSubscriptionRequest;
import com.thinh.smartgym.membership.subscription.dto.SubscriptionResponse;
import com.thinh.smartgym.membership.subscription.service.MemberSubscriptionService;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Member Subscriptions", description = "Đăng ký và xem gói tập hiện hành của hội viên")
@SecurityRequirement(name = "bearerAuth")
public class MemberSubscriptionController {

    private final MemberSubscriptionService memberSubscriptionService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Gửi yêu cầu đăng ký gói tập mới")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Tạo yêu cầu PENDING thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.SubscriptionSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Dữ liệu không hợp lệ (VAL-001)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_MEMBER (AUTH-002) hoặc tài khoản không ACTIVE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy package (SUB-002)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Package inactive, đã có ACTIVE hoặc PENDING (SUB-003/SUB-004/SUB-006)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateSubscriptionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Yêu cầu đăng ký gói tập đã được gửi thành công. Vui lòng chờ quản trị viên phê duyệt.",
                memberSubscriptionService.createNewSubscription(principal, request)
        ));
    }

    @GetMapping(value = "/current", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lấy Subscription ACTIVE hiện hành")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy gói tập hiện hành thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.SubscriptionSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_MEMBER (AUTH-002) hoặc tài khoản không ACTIVE",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Không có Subscription ACTIVE hiện hành (SUB-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<SubscriptionResponse> getCurrentSubscription(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Lấy thông tin gói tập hiện hành thành công",
                memberSubscriptionService.getCurrentSubscription(principal)
        );
    }
}
