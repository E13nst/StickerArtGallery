package com.example.sticker_art_gallery.repository.meme;

import com.example.sticker_art_gallery.model.meme.MemeCandidateDislikeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemeCandidateDislikeRepository extends JpaRepository<MemeCandidateDislikeEntity, Long> {

    boolean existsByUserIdAndMemeCandidateId(Long userId, Long memeCandidateId);

    Optional<MemeCandidateDislikeEntity> findByUserIdAndMemeCandidateId(Long userId, Long memeCandidateId);

    /**
     * Pessimistic write lock — для атомарного удаления противоположной оценки.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM MemeCandidateDislikeEntity d WHERE d.userId = :userId AND d.memeCandidate.id = :candidateId")
    Optional<MemeCandidateDislikeEntity> findByUserIdAndCandidateIdForUpdate(
            @Param("userId") Long userId,
            @Param("candidateId") Long candidateId);

    void deleteByUserIdAndMemeCandidateId(Long userId, Long memeCandidateId);
}
