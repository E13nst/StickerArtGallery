package com.example.sticker_art_gallery.service.statistics;

import com.example.sticker_art_gallery.dto.*;
import com.example.sticker_art_gallery.model.profile.ArtTransactionDirection;
import com.example.sticker_art_gallery.model.profile.ArtTransactionRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.repository.LikeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final StickerSetRepository stickerSetRepository;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ArtTransactionRepository artTransactionRepository;

    public StatisticsService(StickerSetRepository stickerSetRepository,
                             LikeRepository likeRepository,
                             UserRepository userRepository,
                             ArtTransactionRepository artTransactionRepository) {
        this.stickerSetRepository = stickerSetRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.artTransactionRepository = artTransactionRepository;
    }

    @Cacheable("serviceStatistics")
    public StatisticsDto getStatistics() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime dayAgo = nowUtc.minusDays(1);
        LocalDateTime weekAgo = nowUtc.minusWeeks(1);

        OffsetDateTime nowOffsetUtc = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime dayAgoOffset = nowOffsetUtc.minusDays(1);
        OffsetDateTime weekAgoOffset = nowOffsetUtc.minusWeeks(1);

        StatisticsDto dto = new StatisticsDto();

        populateStickerSetStats(dto, dayAgo);
        populateLikeStats(dto, dayAgo);
        populateUserStats(dto, dayAgo, weekAgo, dayAgoOffset, weekAgoOffset);
        populateArtStats(dto, dayAgoOffset);

        return dto;
    }

    private void populateStickerSetStats(StatisticsDto dto, LocalDateTime dayAgo) {
        StatisticsDto.StickerSetStats stats = dto.getStickerSets();
        stats.setTotal(stickerSetRepository.count());
        stats.setDaily(stickerSetRepository.countByCreatedAtAfter(dayAgo));
    }

    private void populateLikeStats(StatisticsDto dto, LocalDateTime dayAgo) {
        StatisticsDto.LikeStats stats = dto.getLikes();
        stats.setTotal(likeRepository.count());
        stats.setDaily(likeRepository.countByCreatedAtAfter(dayAgo));
    }

    private void populateUserStats(StatisticsDto dto,
                                   LocalDateTime dayAgo,
                                   LocalDateTime weekAgo,
                                   OffsetDateTime dayAgoOffset,
                                   OffsetDateTime weekAgoOffset) {
        StatisticsDto.UserStats stats = dto.getUsers();
        stats.setTotal(userRepository.count());
        stats.setNewDaily(userRepository.countByCreatedAtAfter(dayAgoOffset));
        stats.setNewWeekly(userRepository.countByCreatedAtAfter(weekAgoOffset));

        Set<Long> activeDaily = new HashSet<>();
        mergeUserIds(activeDaily, likeRepository.findDistinctUserIdsByCreatedAtAfter(dayAgo));
        mergeUserIds(activeDaily, stickerSetRepository.findDistinctUserIdsByCreatedAtAfter(dayAgo));
        stats.setActiveDaily(activeDaily.size());

        Set<Long> activeWeekly = new HashSet<>();
        mergeUserIds(activeWeekly, likeRepository.findDistinctUserIdsByCreatedAtAfter(weekAgo));
        mergeUserIds(activeWeekly, stickerSetRepository.findDistinctUserIdsByCreatedAtAfter(weekAgo));
        stats.setActiveWeekly(activeWeekly.size());
    }

    private void populateArtStats(StatisticsDto dto, OffsetDateTime dayAgoOffset) {
        StatisticsDto.ArtStats artStats = dto.getArt();
        StatisticsDto.AmountStats earnedStats = artStats.getEarned();
        StatisticsDto.AmountStats spentStats = artStats.getSpent();

        earnedStats.setTotal(safeValue(artTransactionRepository.sumDeltaByDirection(ArtTransactionDirection.CREDIT)));
        earnedStats.setDaily(safeValue(artTransactionRepository.sumDeltaByDirectionSince(
                ArtTransactionDirection.CREDIT, dayAgoOffset)));

        spentStats.setTotal(safeAbs(artTransactionRepository.sumDeltaByDirection(ArtTransactionDirection.DEBIT)));
        spentStats.setDaily(safeAbs(artTransactionRepository.sumDeltaByDirectionSince(
                ArtTransactionDirection.DEBIT, dayAgoOffset)));
    }

    private void mergeUserIds(Set<Long> target, List<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            target.addAll(userIds);
        }
    }

    private long safeValue(Long value) {
        return value != null ? value : 0L;
    }

    private long safeAbs(Long value) {
        long safe = safeValue(value);
        return safe < 0 ? Math.abs(safe) : safe;
    }

    @Cacheable("userStatistics")
    public UserStatisticsDto getUserStatistics() {
        OffsetDateTime nowOffsetUtc = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime dayAgoOffset = nowOffsetUtc.minusDays(1);
        OffsetDateTime weekAgoOffset = nowOffsetUtc.minusWeeks(1);

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime dayAgo = nowUtc.minusDays(1);
        LocalDateTime weekAgo = nowUtc.minusWeeks(1);

        UserStatisticsDto dto = new UserStatisticsDto();
        dto.setTotal(userRepository.count());
        dto.setDaily(userRepository.countByCreatedAtAfter(dayAgoOffset));
        dto.setWeekly(userRepository.countByCreatedAtBetween(weekAgoOffset, nowOffsetUtc));

        Set<Long> activeDaily = new HashSet<>();
        mergeUserIds(activeDaily, likeRepository.findDistinctUserIdsByCreatedAtAfter(dayAgo));
        mergeUserIds(activeDaily, stickerSetRepository.findDistinctUserIdsByCreatedAtAfter(dayAgo));
        dto.setActiveDaily(activeDaily.size());

        Set<Long> activeWeekly = new HashSet<>();
        mergeUserIds(activeWeekly, likeRepository.findDistinctUserIdsByCreatedAtAfter(weekAgo));
        mergeUserIds(activeWeekly, stickerSetRepository.findDistinctUserIdsByCreatedAtAfter(weekAgo));
        dto.setActiveWeekly(activeWeekly.size());

        return dto;
    }

    @Cacheable("stickerSetStatistics")
    public StickerSetStatisticsDto getStickerSetStatistics() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime dayAgo = nowUtc.minusDays(1);
        LocalDateTime weekAgo = nowUtc.minusWeeks(1);

        StickerSetStatisticsDto dto = new StickerSetStatisticsDto();
        dto.setTotal(stickerSetRepository.count());
        dto.setTotalPublic(stickerSetRepository.countByVisibility(StickerSetVisibility.PUBLIC));
        dto.setTotalPrivate(stickerSetRepository.countByVisibility(StickerSetVisibility.PRIVATE));

        dto.setDaily(stickerSetRepository.countByCreatedAtAfter(dayAgo));
        dto.setDailyPublic(stickerSetRepository.countByVisibilityAndCreatedAtAfter(StickerSetVisibility.PUBLIC, dayAgo));
        dto.setDailyPrivate(stickerSetRepository.countByVisibilityAndCreatedAtAfter(StickerSetVisibility.PRIVATE, dayAgo));

        // Для weekly используем countByCreatedAtAfter, так как это последние 7 дней
        dto.setWeekly(stickerSetRepository.countByCreatedAtAfter(weekAgo));
        dto.setWeeklyPublic(stickerSetRepository.countByVisibilityAndCreatedAtAfter(StickerSetVisibility.PUBLIC, weekAgo));
        dto.setWeeklyPrivate(stickerSetRepository.countByVisibilityAndCreatedAtAfter(StickerSetVisibility.PRIVATE, weekAgo));

        return dto;
    }

    @Cacheable("likeStatistics")
    public LikeStatisticsDto getLikeStatistics() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime dayAgo = nowUtc.minusDays(1);
        LocalDateTime weekAgo = nowUtc.minusWeeks(1);

        LikeStatisticsDto dto = new LikeStatisticsDto();
        dto.setTotal(likeRepository.count());
        dto.setDaily(likeRepository.countByCreatedAtAfter(dayAgo));
        dto.setWeekly(likeRepository.countByCreatedAtBetween(weekAgo, nowUtc));

        return dto;
    }

    public PageResponse<UserLeaderboardDto> getUserLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> result = stickerSetRepository.findTopUsersByStickerSetCount(pageable);

        List<UserLeaderboardDto> leaderboard = result.getContent().stream()
                .map(row -> {
                    Long userId = (Long) row[0];
                    Long totalCount = ((Number) row[1]).longValue();
                    Long publicCount = ((Number) row[2]).longValue();
                    Long privateCount = ((Number) row[3]).longValue();

                    UserLeaderboardDto dto = new UserLeaderboardDto();
                    dto.setUserId(userId);
                    dto.setTotalCount(totalCount);
                    dto.setPublicCount(publicCount);
                    dto.setPrivateCount(privateCount);

                    // Получаем данные пользователя из кэша
                    UserEntity user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        dto.setUsername(user.getUsername());
                        dto.setFirstName(user.getFirstName());
                        dto.setLastName(user.getLastName());
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponse.of(result, leaderboard);
    }
}


