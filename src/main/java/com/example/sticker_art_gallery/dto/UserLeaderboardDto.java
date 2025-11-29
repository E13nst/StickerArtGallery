package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для рейтинга пользователей по количеству созданных стикерсетов
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о пользователе в рейтинге")
public class UserLeaderboardDto {

    @Schema(description = "Telegram ID пользователя", example = "123456789")
    private Long userId;

    @Schema(description = "Username пользователя в Telegram", example = "testuser")
    private String username;

    @Schema(description = "Имя пользователя", example = "Test")
    private String firstName;

    @Schema(description = "Фамилия пользователя", example = "User")
    private String lastName;

    @Schema(description = "Общее количество созданных стикерсетов", example = "42")
    private long totalCount;

    @Schema(description = "Количество созданных публичных стикерсетов", example = "28")
    private long publicCount;

    @Schema(description = "Количество созданных приватных стикерсетов", example = "14")
    private long privateCount;
}

