package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.admin.GalleryKvSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GalleryKvSettingRepository extends JpaRepository<GalleryKvSettingEntity, String> {
}
