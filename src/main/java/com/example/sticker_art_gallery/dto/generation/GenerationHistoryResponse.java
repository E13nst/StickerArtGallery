package com.example.sticker_art_gallery.dto.generation;

import com.example.sticker_art_gallery.dto.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "История генераций пользователя")
public class GenerationHistoryResponse extends PageResponse<GenerationStatusResponse> {

    public GenerationHistoryResponse() {
        super();
    }

    public GenerationHistoryResponse(List<GenerationStatusResponse> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last, boolean hasNext, boolean hasPrevious) {
        super(content, page, size, totalElements, totalPages, first, last, hasNext, hasPrevious);
    }
}
