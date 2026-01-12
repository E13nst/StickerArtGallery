package com.example.sticker_art_gallery.service.statistics;

import com.example.sticker_art_gallery.dto.StatisticsDto;
import com.example.sticker_art_gallery.model.profile.ArtTransactionDirection;
import com.example.sticker_art_gallery.repository.ArtTransactionRepository;
import com.example.sticker_art_gallery.repository.StickerSetRepository;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.repository.LikeRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Epic("Статистика сервиса")
@Feature("Агрегирование ключевых метрик")
@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private StickerSetRepository stickerSetRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArtTransactionRepository artTransactionRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    @DisplayName("getStatistics должен корректно агрегировать метрики")
    void getStatistics_shouldAggregateMetrics() {
        when(stickerSetRepository.count()).thenReturn(120L);
        when(stickerSetRepository.countByCreatedAtAfter(any())).thenReturn(7L);
        when(stickerSetRepository.findDistinctUserIdsByCreatedAtAfter(any()))
                .thenReturn(List.of(4L))
                .thenReturn(List.of(3L, 4L, 5L));

        when(likeRepository.count()).thenReturn(450L);
        when(likeRepository.countByCreatedAtAfter(any())).thenReturn(25L);
        when(likeRepository.findDistinctUserIdsByCreatedAtAfter(any()))
                .thenReturn(List.of(1L, 2L, 3L))
                .thenReturn(List.of(1L, 2L, 3L, 6L));

        when(userRepository.count()).thenReturn(900L);
        when(userRepository.countByCreatedAtAfter(any(OffsetDateTime.class)))
                .thenReturn(30L)
                .thenReturn(80L);

        when(artTransactionRepository.sumDeltaByDirection(eq(ArtTransactionDirection.CREDIT))).thenReturn(1_200L);
        when(artTransactionRepository.sumDeltaByDirection(eq(ArtTransactionDirection.DEBIT))).thenReturn(-500L);
        when(artTransactionRepository.sumDeltaByDirectionSince(eq(ArtTransactionDirection.CREDIT), any()))
                .thenReturn(150L);
        when(artTransactionRepository.sumDeltaByDirectionSince(eq(ArtTransactionDirection.DEBIT), any()))
                .thenReturn(-120L);

        StatisticsDto stats = statisticsService.getStatistics();

        assertThat(stats.getStickerSets().getTotal()).isEqualTo(120L);
        assertThat(stats.getStickerSets().getDaily()).isEqualTo(7L);

        assertThat(stats.getLikes().getTotal()).isEqualTo(450L);
        assertThat(stats.getLikes().getDaily()).isEqualTo(25L);

        assertThat(stats.getUsers().getTotal()).isEqualTo(900L);
        assertThat(stats.getUsers().getNewDaily()).isEqualTo(30L);
        assertThat(stats.getUsers().getNewWeekly()).isEqualTo(80L);
        assertThat(stats.getUsers().getActiveDaily()).isEqualTo(4L);
        assertThat(stats.getUsers().getActiveWeekly()).isEqualTo(6L);

        assertThat(stats.getArt().getEarned().getTotal()).isEqualTo(1_200L);
        assertThat(stats.getArt().getEarned().getDaily()).isEqualTo(150L);
        assertThat(stats.getArt().getSpent().getTotal()).isEqualTo(500L);
        assertThat(stats.getArt().getSpent().getDaily()).isEqualTo(120L);
    }
}


