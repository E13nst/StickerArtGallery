package com.example.sticker_art_gallery.model.profile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface ArtTransactionRepository extends JpaRepository<ArtTransactionEntity, Long> {

    Page<ArtTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<ArtTransactionEntity> findByExternalId(String externalId);

    @Query("SELECT COALESCE(SUM(t.delta), 0) FROM ArtTransactionEntity t WHERE t.direction = :direction")
    Long sumDeltaByDirection(@Param("direction") ArtTransactionDirection direction);

    @Query("SELECT COALESCE(SUM(t.delta), 0) FROM ArtTransactionEntity t " +
           "WHERE t.direction = :direction AND t.createdAt >= :since")
    Long sumDeltaByDirectionSince(@Param("direction") ArtTransactionDirection direction,
                                  @Param("since") OffsetDateTime since);
}

