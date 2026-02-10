package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    /**
     * Найти профиль по userId (Telegram ID)
     */
    Optional<UserProfileEntity> findByUserId(Long userId);

    /**
     * Найти профиль по userId с блокировкой для обновления
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select up from UserProfileEntity up where up.userId = :userId")
    Optional<UserProfileEntity> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Проверить существование профиля по userId
     */
    boolean existsByUserId(Long userId);
    
    /**
     * Найти все профили с фильтрами и пагинацией (для админ-панели)
     * Показываем только данные из user_profiles без JOIN с users для упрощения
     */
    @Query(value = "SELECT up.* FROM user_profiles up " +
           "LEFT JOIN users u ON up.user_id = u.id " +
           "WHERE (:role IS NULL OR CAST(up.role AS TEXT) = :role) " +
           "AND (:isBlocked IS NULL OR up.is_blocked = :isBlocked) " +
           "AND (:subscriptionStatus IS NULL OR CAST(up.subscription_status AS TEXT) = :subscriptionStatus) " +
           "AND (:minBalance IS NULL OR up.art_balance >= :minBalance) " +
           "AND (:maxBalance IS NULL OR up.art_balance <= :maxBalance) " +
           "AND (:createdAfter IS NULL OR up.created_at >= CAST(:createdAfter AS timestamp)) " +
           "AND (:createdBefore IS NULL OR up.created_at <= CAST(:createdBefore AS timestamp)) " +
           "AND (:search IS NULL OR :search = '' OR CAST(up.user_id AS TEXT) LIKE CONCAT('%', :search, '%')) " +
           "AND (:userUsername IS NULL OR :userUsername = '' OR u.username LIKE CONCAT('%', :userUsername, '%')) " +
           "AND (:userFirstName IS NULL OR :userFirstName = '' OR u.first_name LIKE CONCAT('%', :userFirstName, '%')) " +
           "AND (:userLastName IS NULL OR :userLastName = '' OR u.last_name LIKE CONCAT('%', :userLastName, '%')) " +
           "AND (:userLanguageCode IS NULL OR :userLanguageCode = '' OR u.language_code = :userLanguageCode) " +
           "AND (:userIsPremium IS NULL OR u.is_premium = :userIsPremium) " +
           "ORDER BY up.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM user_profiles up " +
           "LEFT JOIN users u ON up.user_id = u.id " +
           "WHERE (:role IS NULL OR CAST(up.role AS TEXT) = :role) " +
           "AND (:isBlocked IS NULL OR up.is_blocked = :isBlocked) " +
           "AND (:subscriptionStatus IS NULL OR CAST(up.subscription_status AS TEXT) = :subscriptionStatus) " +
           "AND (:minBalance IS NULL OR up.art_balance >= :minBalance) " +
           "AND (:maxBalance IS NULL OR up.art_balance <= :maxBalance) " +
           "AND (:createdAfter IS NULL OR up.created_at >= CAST(:createdAfter AS timestamp)) " +
           "AND (:createdBefore IS NULL OR up.created_at <= CAST(:createdBefore AS timestamp)) " +
           "AND (:search IS NULL OR :search = '' OR CAST(up.user_id AS TEXT) LIKE CONCAT('%', :search, '%')) " +
           "AND (:userUsername IS NULL OR :userUsername = '' OR u.username LIKE CONCAT('%', :userUsername, '%')) " +
           "AND (:userFirstName IS NULL OR :userFirstName = '' OR u.first_name LIKE CONCAT('%', :userFirstName, '%')) " +
           "AND (:userLastName IS NULL OR :userLastName = '' OR u.last_name LIKE CONCAT('%', :userLastName, '%')) " +
           "AND (:userLanguageCode IS NULL OR :userLanguageCode = '' OR u.language_code = :userLanguageCode) " +
           "AND (:userIsPremium IS NULL OR u.is_premium = :userIsPremium)",
           nativeQuery = true)
    Page<UserProfileEntity> findAllWithFilters(
            @Param("role") String role,
            @Param("isBlocked") Boolean isBlocked,
            @Param("subscriptionStatus") String subscriptionStatus,
            @Param("minBalance") Long minBalance,
            @Param("maxBalance") Long maxBalance,
            @Param("createdAfter") String createdAfter,
            @Param("createdBefore") String createdBefore,
            @Param("search") String search,
            @Param("userUsername") String userUsername,
            @Param("userFirstName") String userFirstName,
            @Param("userLastName") String userLastName,
            @Param("userLanguageCode") String userLanguageCode,
            @Param("userIsPremium") Boolean userIsPremium,
            Pageable pageable
    );
}
