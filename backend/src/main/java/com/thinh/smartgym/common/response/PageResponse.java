package com.thinh.smartgym.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Envelope phân trang chuẩn, page bắt đầu từ 0")
public record PageResponse<T>(
        @Schema(description = "Danh sách phần tử của trang hiện tại", requiredMode = Schema.RequiredMode.REQUIRED)
        List<T> content,
        @Schema(example = "52", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
        @Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages,
        @Schema(example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int currentPage,
        @Schema(example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int pageSize
) {

    public PageResponse {
        content = List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }
}
