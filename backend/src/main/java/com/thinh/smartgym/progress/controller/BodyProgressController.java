package com.thinh.smartgym.progress.controller;

import com.thinh.smartgym.common.config.OpenApiResponseSchemas;
import com.thinh.smartgym.common.response.ApiResponse;
import com.thinh.smartgym.common.response.ErrorResponse;
import com.thinh.smartgym.progress.dto.BodyProgressResponse;
import com.thinh.smartgym.progress.dto.BodyProgressUpsertRequest;
import com.thinh.smartgym.progress.service.BodyProgressService;
import com.thinh.smartgym.security.AuthenticatedUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/member/body-progress")
@RequiredArgsConstructor
@Tag(name = "Member Body Progress", description = "Lịch sử cân nặng hằng ngày của hội viên")
@SecurityRequirement(name = "bearerAuth")
public class BodyProgressController {

    private final BodyProgressService bodyProgressService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Ghi nhận hoặc cập nhật cân nặng trong ngày",
            description = "Upsert theo User ID trong principal và recordDate theo ngày nghiệp vụ Việt Nam."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Ghi nhận cân nặng thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.BodyProgressSuccessResponse.class
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
                description = "Thiếu ROLE_MEMBER hoặc tài khoản bị chặn",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<BodyProgressResponse> upsert(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody BodyProgressUpsertRequest request
    ) {
        return ApiResponse.success(
                "Ghi nhận chỉ số cân nặng thành công",
                bodyProgressService.upsertCurrentProgress(principal, request)
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Lấy lịch sử cân nặng",
            description = "Chỉ trả các bản ghi thuộc Member hiện hành, sắp xếp recordDate tăng dần."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Lấy lịch sử tiến trình thành công",
                content = @Content(schema = @Schema(
                        implementation = OpenApiResponseSchemas.BodyProgressListSuccessResponse.class
                ))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "JWT thiếu, sai hoặc hết hạn (ACC-005)",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Thiếu ROLE_MEMBER hoặc tài khoản bị chặn",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ApiResponse<List<BodyProgressResponse>> getHistory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Lấy lịch sử tiến trình thể trạng thành công",
                bodyProgressService.getCurrentProgress(principal)
        );
    }
}
