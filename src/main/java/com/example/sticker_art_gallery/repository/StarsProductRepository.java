package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.payment.ProductType;
import com.example.sticker_art_gallery.model.payment.StarsProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StarsProductRepository extends JpaRepository<StarsProductEntity, Long> {

    List<StarsProductEntity> findByProductTypeAndIsEnabledTrue(ProductType productType);

    Optional<StarsProductEntity> findByCode(String code);

    List<StarsProductEntity> findByIsEnabledTrue();
}
