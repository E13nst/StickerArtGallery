package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Репозиторий для аналитических запросов с группировкой по временным бакетам.
 * Все запросы используют UTC; для таблиц с TIMESTAMP без TZ применяется AT TIME ZONE 'UTC'.
 * Привязан к UserEntity только для создания proxy Spring Data.
 */
@Repository
public interface AnalyticsRepository extends JpaRepository<UserEntity, Long> {

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, created_at) AS bucket FROM users " +
            "WHERE created_at >= :from AND created_at < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countNewUsersByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                         @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, (created_at AT TIME ZONE 'UTC')::timestamptz) AS bucket FROM stickersets " +
            "WHERE (created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (created_at AT TIME ZONE 'UTC')::timestamptz < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countStickerSetsByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                             @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, (created_at AT TIME ZONE 'UTC')::timestamptz) AS bucket FROM likes " +
            "WHERE (created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (created_at AT TIME ZONE 'UTC')::timestamptz < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countLikesByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                       @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, (created_at AT TIME ZONE 'UTC')::timestamptz) AS bucket FROM dislikes " +
            "WHERE (created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (created_at AT TIME ZONE 'UTC')::timestamptz < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countDislikesByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                          @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, created_at) AS bucket FROM user_swipes " +
            "WHERE created_at >= :from AND created_at < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countSwipesByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                        @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COALESCE(SUM(sub.delta), 0)::bigint FROM (" +
            "SELECT date_trunc(:granularity, created_at) AS bucket, delta FROM art_transactions " +
            "WHERE direction = 'CREDIT' AND created_at >= :from AND created_at < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> sumArtEarnedByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                         @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COALESCE(ABS(SUM(sub.delta)), 0)::bigint FROM (" +
            "SELECT date_trunc(:granularity, created_at) AS bucket, delta FROM art_transactions " +
            "WHERE direction = 'DEBIT' AND created_at >= :from AND created_at < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> sumArtSpentByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                        @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, started_at) AS bucket FROM generation_audit_sessions " +
            "WHERE started_at >= :from AND started_at < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countGenerationRunsByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                                @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, started_at) AS bucket FROM generation_audit_sessions " +
            "WHERE started_at >= :from AND started_at < :to AND final_status = 'COMPLETED'" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countGenerationSuccessByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                                   @Param("granularity") String granularity);

    @Query(value = "SELECT sub.bucket, COUNT(*)::bigint FROM (" +
            "SELECT date_trunc(:granularity, created_at) AS bucket FROM referral_events " +
            "WHERE created_at >= :from AND created_at < :to" +
            ") sub GROUP BY sub.bucket ORDER BY sub.bucket", nativeQuery = true)
    List<Object[]> countReferralEventsByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                                 @Param("granularity") String granularity);

    /** Активные пользователи по бакетам: уникальные user_id по всем типам событий за бакет. */
    @Query(value = "SELECT bucket, COUNT(DISTINCT user_id)::bigint FROM (" +
            "SELECT date_trunc(:granularity, (l.created_at AT TIME ZONE 'UTC')::timestamptz) AS bucket, l.user_id FROM likes l " +
            "WHERE (l.created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (l.created_at AT TIME ZONE 'UTC')::timestamptz < :to " +
            "UNION ALL " +
            "SELECT date_trunc(:granularity, (d.created_at AT TIME ZONE 'UTC')::timestamptz), d.user_id FROM dislikes d " +
            "WHERE (d.created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (d.created_at AT TIME ZONE 'UTC')::timestamptz < :to " +
            "UNION ALL " +
            "SELECT date_trunc(:granularity, (s.created_at AT TIME ZONE 'UTC')::timestamptz), s.user_id FROM stickersets s " +
            "WHERE (s.created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (s.created_at AT TIME ZONE 'UTC')::timestamptz < :to " +
            "UNION ALL " +
            "SELECT date_trunc(:granularity, us.created_at), us.user_id FROM user_swipes us " +
            "WHERE us.created_at >= :from AND us.created_at < :to " +
            "UNION ALL " +
            "SELECT date_trunc(:granularity, gs.started_at), gs.user_id FROM generation_audit_sessions gs " +
            "WHERE gs.started_at >= :from AND gs.started_at < :to" +
            ") AS active WHERE bucket IS NOT NULL GROUP BY bucket ORDER BY bucket", nativeQuery = true)
    List<Object[]> countActiveUsersByBucket(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to,
                                            @Param("granularity") String granularity);

    @Query(value = "SELECT re.event_type, COUNT(*)::bigint FROM referral_events re " +
            "WHERE re.created_at >= :from AND re.created_at < :to GROUP BY re.event_type ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countReferralEventsByType(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = "SELECT (gae.stage || '_' || gae.event_status), COUNT(*)::bigint FROM generation_audit_events gae " +
            "WHERE gae.created_at >= :from AND gae.created_at < :to GROUP BY gae.stage, gae.event_status ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countGenerationByStageAndStatus(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    @Query(value = "SELECT COUNT(DISTINCT user_id)::bigint FROM (" +
            "SELECT user_id FROM likes WHERE (created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (created_at AT TIME ZONE 'UTC')::timestamptz < :to " +
            "UNION ALL SELECT user_id FROM dislikes WHERE (created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (created_at AT TIME ZONE 'UTC')::timestamptz < :to " +
            "UNION ALL SELECT user_id FROM stickersets WHERE (created_at AT TIME ZONE 'UTC')::timestamptz >= :from AND (created_at AT TIME ZONE 'UTC')::timestamptz < :to " +
            "UNION ALL SELECT user_id FROM user_swipes WHERE created_at >= :from AND created_at < :to " +
            "UNION ALL SELECT user_id FROM generation_audit_sessions WHERE started_at >= :from AND started_at < :to" +
            ") AS active", nativeQuery = true)
    long countActiveUsersInPeriod(@Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);
}
