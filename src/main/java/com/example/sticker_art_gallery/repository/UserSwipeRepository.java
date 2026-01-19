package com.example.sticker_art_gallery.repository;

import com.example.sticker_art_gallery.model.swipe.UserSwipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository для работы со свайпами пользователей
 */
@Repository
public interface UserSwipeRepository extends JpaRepository<UserSwipeEntity, Long> {

    /**
     * Подсчитать количество свайпов пользователя за конкретную дату
     */
    long countByUserIdAndSwipeDate(Long userId, LocalDate swipeDate);

    /**
     * Подсчитать количество свайпов пользователя за период
     */
    long countByUserIdAndSwipeDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Найти свайп по userId и likeId
     */
    Optional<UserSwipeEntity> findByUserIdAndLikeId(Long userId, Long likeId);

    /**
     * Найти свайп по userId и dislikeId
     */
    Optional<UserSwipeEntity> findByUserIdAndDislikeId(Long userId, Long dislikeId);

    /**
     * Получить все свайпы пользователя за дату
     */
    List<UserSwipeEntity> findByUserIdAndSwipeDate(Long userId, LocalDate swipeDate);

    /**
     * Получить все свайпы пользователя за период (для rolling window)
     */
    List<UserSwipeEntity> findByUserIdAndSwipeDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Подсчитать общее количество свайпов пользователя за период
     * Используется для подсчета свайпов для награды
     */
    @Query("SELECT COUNT(us) FROM UserSwipeEntity us " +
           "WHERE us.userId = :userId " +
           "AND us.swipeDate BETWEEN :startDate AND :endDate")
    long countSwipesInPeriod(@Param("userId") Long userId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    /**
     * Получить все свайпы пользователя за определенную дату с сортировкой
     */
    @Query("SELECT us FROM UserSwipeEntity us " +
           "WHERE us.userId = :userId " +
           "AND us.swipeDate = :swipeDate " +
           "ORDER BY us.createdAt ASC")
    List<UserSwipeEntity> findSwipesByUserAndDateOrdered(@Param("userId") Long userId,
                                                         @Param("swipeDate") LocalDate swipeDate);
}
