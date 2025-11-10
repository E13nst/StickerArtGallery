package com.example.sticker_art_gallery.service.statistics;

import com.example.sticker_art_gallery.dto.StatisticsDto;
import com.example.sticker_art_gallery.model.profile.ArtTransactionDirection;
import com.example.sticker_art_gallery.model.profile.ArtTransactionRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.user.UserRepository;
import com.example.sticker_art_gallery.repository.LikeRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
}


