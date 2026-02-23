package com.example.sticker_art_gallery.service.analytics;

import com.example.sticker_art_gallery.dto.analytics.*;
import com.example.sticker_art_gallery.dto.UserLeaderboardDto;
import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.repository.*;
import com.example.sticker_art_gallery.service.statistics.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int MAX_DAYS_RANGE = 365;
    private static final Set<String> ALLOWED_GRANULARITIES = Set.of("hour", "day", "week");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;
    private final StickerSetRepository stickerSetRepository;
    private final LikeRepository likeRepository;
    private final DislikeRepository dislikeRepository;
    private final UserSwipeRepository userSwipeRepository;
    private final ArtTransactionRepository artTransactionRepository;
    private final GenerationAuditSessionRepository generationAuditSessionRepository;
    private final ReferralEventRepository referralEventRepository;
    private final StatisticsService statisticsService;

    public AnalyticsService(AnalyticsRepository analyticsRepository,
                            UserRepository userRepository,
                            StickerSetRepository stickerSetRepository,
                            LikeRepository likeRepository,
                            DislikeRepository dislikeRepository,
                            UserSwipeRepository userSwipeRepository,
                            ArtTransactionRepository artTransactionRepository,
                            GenerationAuditSessionRepository generationAuditSessionRepository,
                            ReferralEventRepository referralEventRepository,
                            StatisticsService statisticsService) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
        this.stickerSetRepository = stickerSetRepository;
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.userSwipeRepository = userSwipeRepository;
        this.artTransactionRepository = artTransactionRepository;
        this.generationAuditSessionRepository = generationAuditSessionRepository;
        this.referralEventRepository = referralEventRepository;
        this.statisticsService = statisticsService;
    }

    /**
     * Валидация и получение данных дашборда за период.
     *
     * @param from         начало периода (ISO-8601)
     * @param to           конец периода (ISO-8601)
     * @param granularity  hour | day | week
     * @param tz           часовой пояс (например UTC)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "analyticsDashboard", unless = "#result == null")
    public AnalyticsDashboardResponseDto getDashboard(String from, String to, String granularity, String tz) {
        OffsetDateTime fromOdt = OffsetDateTime.parse(from);
        OffsetDateTime toOdt = OffsetDateTime.parse(to);
        if (fromOdt.isAfter(toOdt)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        long days = ChronoUnit.DAYS.between(fromOdt.toLocalDate(), toOdt.toLocalDate());
        if (days > MAX_DAYS_RANGE) {
            throw new IllegalArgumentException("Date range must not exceed " + MAX_DAYS_RANGE + " days");
        }
        String gran = granularity == null || granularity.isBlank() ? "day" : granularity.toLowerCase();
        if (!ALLOWED_GRANULARITIES.contains(gran)) {
            throw new IllegalArgumentException("granularity must be one of: hour, day, week");
        }
        String zone = (tz == null || tz.isBlank()) ? "UTC" : tz;

        LOGGER.debug("📊 Analytics dashboard: from={}, to={}, granularity={}", from, to, gran);

        DashboardKpiDto kpi = buildKpi(fromOdt, toOdt);
        DashboardTimeseriesDto timeseries = buildTimeseries(fromOdt, toOdt, gran);
        DashboardBreakdownsDto breakdowns = buildBreakdowns(fromOdt, toOdt);

        AnalyticsDashboardResponseDto response = new AnalyticsDashboardResponseDto();
        response.setFrom(from);
        response.setTo(to);
        response.setGranularity(gran);
        response.setTz(zone);
        response.setKpiCards(kpi);
        response.setTimeseries(timeseries);
        response.setBreakdowns(breakdowns);
        return response;
    }

    private DashboardKpiDto buildKpi(OffsetDateTime from, OffsetDateTime to) {
        DashboardKpiDto kpi = new DashboardKpiDto();

        kpi.setTotalUsers(userRepository.count());
        kpi.setNewUsers(userRepository.countByCreatedAtBetween(from, to));

        kpi.setActiveUsers(analyticsRepository.countActiveUsersInPeriod(from, to));

        java.time.LocalDateTime fromLdt = from.toLocalDateTime();
        java.time.LocalDateTime toLdt = to.toLocalDateTime();
        kpi.setCreatedStickerSets(stickerSetRepository.countByCreatedAtBetween(fromLdt, toLdt));
        kpi.setLikes(likeRepository.countByCreatedAtBetween(fromLdt, toLdt));
        kpi.setDislikes(dislikeRepository.countByCreatedAtBetween(fromLdt, toLdt));
        kpi.setSwipes(userSwipeRepository.countByCreatedAtBetween(from, to));

        Long artEarned = artTransactionRepository.sumDeltaByDirectionBetween(
                com.example.sticker_art_gallery.model.profile.ArtTransactionDirection.CREDIT, from, to);
        Long artSpent = artTransactionRepository.sumDeltaByDirectionBetween(
                com.example.sticker_art_gallery.model.profile.ArtTransactionDirection.DEBIT, from, to);
        kpi.setArtEarned(artEarned != null ? artEarned : 0L);
        kpi.setArtSpent(artSpent != null ? Math.abs(artSpent) : 0L);

        long genRuns = generationAuditSessionRepository.countByStartedAtBetween(from, to);
        long genSuccess = generationAuditSessionRepository.countByStartedAtBetweenAndFinalStatus(from, to, "COMPLETED");
        kpi.setGenerationRuns(genRuns);
        kpi.setGenerationSuccessRate(genRuns > 0 ? (100.0 * genSuccess / genRuns) : 0.0);

        long refTotal = referralEventRepository.countByCreatedAtBetween(from, to);
        kpi.setReferralEventsTotal(refTotal);
        kpi.setReferralConversions(refTotal);

        return kpi;
    }

    private DashboardTimeseriesDto buildTimeseries(OffsetDateTime from, OffsetDateTime to, String granularity) {
        DashboardTimeseriesDto dto = new DashboardTimeseriesDto();
        dto.setNewUsers(toPoints(analyticsRepository.countNewUsersByBucket(from, to, granularity)));
        dto.setActiveUsers(toPoints(analyticsRepository.countActiveUsersByBucket(from, to, granularity)));
        dto.setCreatedStickerSets(toPoints(analyticsRepository.countStickerSetsByBucket(from, to, granularity)));
        dto.setLikes(toPoints(analyticsRepository.countLikesByBucket(from, to, granularity)));
        dto.setDislikes(toPoints(analyticsRepository.countDislikesByBucket(from, to, granularity)));
        dto.setSwipes(toPoints(analyticsRepository.countSwipesByBucket(from, to, granularity)));
        dto.setArtEarned(toPoints(analyticsRepository.sumArtEarnedByBucket(from, to, granularity)));
        dto.setArtSpent(toPoints(analyticsRepository.sumArtSpentByBucket(from, to, granularity)));
        dto.setGenerationRuns(toPoints(analyticsRepository.countGenerationRunsByBucket(from, to, granularity)));
        dto.setGenerationSuccess(toPoints(analyticsRepository.countGenerationSuccessByBucket(from, to, granularity)));
        dto.setReferralEvents(toPoints(analyticsRepository.countReferralEventsByBucket(from, to, granularity)));
        return dto;
    }

    private List<TimeBucketPointDto> toPoints(List<Object[]> rows) {
        if (rows == null) return Collections.emptyList();
        return rows.stream()
                .map(r -> {
                    TimeBucketPointDto p = new TimeBucketPointDto();
                    if (r.length >= 1 && r[0] != null) {
                        if (r[0] instanceof java.sql.Timestamp) {
                            p.setBucketStart(OffsetDateTime.ofInstant(((java.sql.Timestamp) r[0]).toInstant(), ZoneOffset.UTC).format(ISO));
                        } else if (r[0] instanceof java.time.OffsetDateTime) {
                            p.setBucketStart(((OffsetDateTime) r[0]).format(ISO));
                        } else {
                            p.setBucketStart(r[0].toString());
                        }
                    }
                    if (r.length >= 2 && r[1] != null) {
                        p.setValue(((Number) r[1]).longValue());
                    }
                    return p;
                })
                .collect(Collectors.toList());
    }

    private DashboardBreakdownsDto buildBreakdowns(OffsetDateTime from, OffsetDateTime to) {
        DashboardBreakdownsDto dto = new DashboardBreakdownsDto();

        var leaderboard = statisticsService.getUserLeaderboard(0, 10, null);
        List<Map<String, Object>> topUsers = leaderboard.getContent().stream()
                .map(this::userLeaderboardToMap)
                .collect(Collectors.toList());
        dto.setTopUsers(topUsers);

        var topStickers = likeRepository.findTopStickerSetsByLikes(PageRequest.of(0, 10));
        List<Map<String, Object>> topStickerSets = topStickers.getContent().stream()
                .map(this::stickerSetLikesToMap)
                .collect(Collectors.toList());
        dto.setTopStickerSets(topStickerSets);

        List<Object[]> refByType = analyticsRepository.countReferralEventsByType(from, to);
        Map<String, Long> refMap = new LinkedHashMap<>();
        for (Object[] r : refByType) {
            if (r.length >= 2 && r[0] != null) {
                refMap.put(r[0].toString(), ((Number) r[1]).longValue());
            }
        }
        dto.setReferralByType(refMap);

        List<Object[]> genByStage = analyticsRepository.countGenerationByStageAndStatus(from, to);
        Map<String, Long> genMap = new LinkedHashMap<>();
        for (Object[] r : genByStage) {
            if (r.length >= 2 && r[0] != null) {
                genMap.put(r[0].toString(), ((Number) r[1]).longValue());
            }
        }
        dto.setGenerationByStageStatus(genMap);

        return dto;
    }

    private Map<String, Object> userLeaderboardToMap(UserLeaderboardDto u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getUserId());
        m.put("username", u.getUsername());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("totalCount", u.getTotalCount());
        m.put("publicCount", u.getPublicCount());
        m.put("privateCount", u.getPrivateCount());
        return m;
    }

    private Map<String, Object> stickerSetLikesToMap(Object[] row) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (row.length >= 1 && row[0] instanceof StickerSet) {
            StickerSet s = (StickerSet) row[0];
            m.put("id", s.getId());
            m.put("title", s.getTitle());
            m.put("name", s.getName());
            m.put("userId", s.getUserId());
            m.put("likesCount", row.length >= 2 && row[1] != null ? ((Number) row[1]).longValue() : s.getLikesCount());
        }
        return m;
    }
}
