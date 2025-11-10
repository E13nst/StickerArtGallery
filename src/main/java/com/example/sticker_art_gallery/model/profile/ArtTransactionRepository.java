package com.example.sticker_art_gallery.model.profile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtTransactionRepository extends JpaRepository<ArtTransactionEntity, Long> {

    Page<ArtTransactionEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<ArtTransactionEntity> findByExternalId(String externalId);
}

