package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.payment.StarsPackageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StarsPackageRepository extends JpaRepository<StarsPackageEntity, Long> {

    List<StarsPackageEntity> findByIsEnabledTrueOrderBySortOrder();

    Optional<StarsPackageEntity> findByCodeAndIsEnabledTrue(String code);

    Optional<StarsPackageEntity> findByCode(String code);
}
