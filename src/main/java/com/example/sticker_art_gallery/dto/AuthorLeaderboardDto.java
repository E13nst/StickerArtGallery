package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для рейтинга авторов по количеству созданных стикерсетов
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация об авторе в рейтинге")
public class AuthorLeaderboardDto {

    @Schema(description = "Telegram ID автора", example = "123456789")
    private Long authorId;

    @Schema(description = "Username автора в Telegram", example = "testauthor")
    private String username;

    @Schema(description = "Имя автора", example = "Test")
    private String firstName;

    @Schema(description = "Фамилия автора", example = "Author")
    private String lastName;

    @Schema(description = "Общее количество созданных стикерсетов", example = "42")
    private long totalCount;

    @Schema(description = "Количество созданных публичных стикерсетов", example = "28")
    private long publicCount;

    @Schema(description = "Количество созданных приватных стикерсетов", example = "14")
    private long privateCount;
}

