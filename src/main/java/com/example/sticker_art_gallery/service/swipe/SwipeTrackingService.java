package com.example.sticker_art_gallery.service.swipe;

import com.example.sticker_art_gallery.exception.SwipeLimitExceededException;
import com.example.sticker_art_gallery.model.Dislike;
import com.example.sticker_art_gallery.model.Like;
import com.example.sticker_art_gallery.model.meme.MemeCandidateDislikeEntity;
import com.example.sticker_art_gallery.model.meme.MemeCandidateLikeEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.swipe.SwipeConfigEntity;
import com.example.sticker_art_gallery.model.swipe.UserSwipeEntity;
import com.example.sticker_art_gallery.repository.UserSwipeRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Сервис для отслеживания свайпов пользователей, проверки лимитов и начисления наград.
 */
@Service
@Transactional
public class SwipeTrackingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwipeTrackingService.class);
    private static final String RULE_SWIPE_REWARD = "SWIPE_REWARD";

    private final SwipeConfigService swipeConfigService;
    private final UserSwipeRepository userSwipeRepository;
    private final UserProfileService userProfileService;
    private final ArtRewardService artRewardService;
    private final ObjectMapper objectMapper;

    public SwipeTrackingService(
            SwipeConfigService swipeConfigService,
            UserSwipeRepository userSwipeRepository,
            UserProfileService userProfileService,
            ArtRewardService artRewardService,
            ObjectMapper objectMapper) {
        this.swipeConfigService = swipeConfigService;
        this.userSwipeRepository = userSwipeRepository;
        this.userProfileService = userProfileService;
        this.artRewardService = artRewardService;
        this.objectMapper = objectMapper;
    }

    /**
     * Записать свайп пользователя.
     * Проверяет лимиты, сохраняет свайп и обрабатывает награды.
     */
    public void recordSwipe(Long userId, UserSwipeEntity.ActionType actionType, Like like, Dislike dislike) {
        LOGGER.debug("📝 Запись свайпа: userId={}, actionType={}", userId, actionType);

        SwipeConfigEntity config = swipeConfigService.getActiveConfig();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        // Проверяем дневной лимит
        int currentDailySwipes = (int) userSwipeRepository.countByUserIdAndSwipeDate(userId, today);
        int dailyLimit = getDailyLimitForUser(userId, config);

        if (dailyLimit > 0 && currentDailySwipes >= dailyLimit) {
            LOGGER.warn("⚠️ Достигнут дневной лимит свайпов: userId={}, current={}, limit={}", 
                       userId, currentDailySwipes, dailyLimit);
            throw new SwipeLimitExceededException(
                dailyLimit, 
                currentDailySwipes, 
                config.getResetType(),
                "Достигнут дневной лимит свайпов"
            );
        }

        // Сохраняем свайп
        UserSwipeEntity swipe = new UserSwipeEntity();
        swipe.setUserId(userId);
        swipe.setActionType(actionType);
        swipe.setLike(like);
        swipe.setDislike(dislike);
        swipe.setSwipeDate(today);
        userSwipeRepository.save(swipe);

        LOGGER.debug("✅ Свайп записан: userId={}, swipeDate={}", userId, today);

        // Обрабатываем награды
        processRewards(userId, config, today);
    }

    /**
     * Записать свайп мем-кандидата.
     * Использует ту же таблицу user_swipes и тот же счётчик дневного лимита.
     * Ровно одна из пары (memeLike, memeDislike) должна быть не null.
     */
    public void recordMemeSwipe(Long userId, UserSwipeEntity.ActionType actionType,
                                MemeCandidateLikeEntity memeLike,
                                MemeCandidateDislikeEntity memeDislike) {
        LOGGER.debug("Запись мем-свайпа: userId={}, actionType={}", userId, actionType);

        SwipeConfigEntity config = swipeConfigService.getActiveConfig();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        int currentDailySwipes = (int) userSwipeRepository.countByUserIdAndSwipeDate(userId, today);
        int dailyLimit = getDailyLimitForUser(userId, config);

        if (dailyLimit > 0 && currentDailySwipes >= dailyLimit) {
            LOGGER.warn("Достигнут дневной лимит мем-свайпов: userId={}, current={}, limit={}",
                    userId, currentDailySwipes, dailyLimit);
            throw new SwipeLimitExceededException(
                    dailyLimit,
                    currentDailySwipes,
                    config.getResetType(),
                    "Достигнут дневной лимит свайпов");
        }

        UserSwipeEntity swipe = new UserSwipeEntity();
        swipe.setUserId(userId);
        swipe.setActionType(actionType);
        swipe.setMemeCandidateLike(memeLike);
        swipe.setMemeCandidateDislike(memeDislike);
        swipe.setSwipeDate(today);
        userSwipeRepository.save(swipe);

        LOGGER.debug("Мем-свайп записан: userId={}, swipeDate={}", userId, today);

        processRewards(userId, config, today);
    }

    /**
     * Проверить, не достигнут ли дневной лимит свайпов для пользователя.
     * 
     * @throws SwipeLimitExceededException если лимит достигнут
     */
    public void checkDailyLimit(Long userId) {
        SwipeConfigEntity config = swipeConfigService.getActiveConfig();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        int currentDailySwipes = (int) userSwipeRepository.countByUserIdAndSwipeDate(userId, today);
        int dailyLimit = getDailyLimitForUser(userId, config);

        if (dailyLimit > 0 && currentDailySwipes >= dailyLimit) {
            throw new SwipeLimitExceededException(
                dailyLimit,
                currentDailySwipes,
                config.getResetType(),
                "Достигнут дневной лимит свайпов"
            );
        }
    }

    /**
     * Получить дневной лимит для пользователя с учетом подписки.
     */
    public int getDailyLimitForUser(Long userId) {
        SwipeConfigEntity config = swipeConfigService.getActiveConfig();
        return getDailyLimitForUser(userId, config);
    }

    /**
     * Получить дневной лимит для пользователя с учетом подписки.
     */
    private int getDailyLimitForUser(Long userId, SwipeConfigEntity config) {
        UserProfileEntity profile = userProfileService.findByUserId(userId)
                .orElseGet(() -> userProfileService.getOrCreateDefault(userId));

        if (profile.hasActiveSubscription()) {
            int premiumLimit = config.getDailyLimitPremium();
            LOGGER.debug("✅ Пользователь {} имеет активную подписку, лимит={}", userId, premiumLimit);
            return premiumLimit; // 0 = безлимит
        }

        return config.getDailyLimitRegular();
    }

    /**
     * Получить статистику свайпов за сегодня.
     */
    @Transactional(readOnly = true)
    public SwipeStats getDailyStats(Long userId) {
        SwipeConfigEntity config = swipeConfigService.getActiveConfig();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        int dailySwipes = (int) userSwipeRepository.countByUserIdAndSwipeDate(userId, today);
        int dailyLimit = getDailyLimitForUser(userId, config);

        UserProfileEntity profile = userProfileService.findByUserId(userId)
                .orElseGet(() -> userProfileService.getOrCreateDefault(userId));
        boolean hasSubscription = profile.hasActiveSubscription();

        // Определяем размер награды с учетом подписки
        long rewardAmount = hasSubscription 
            ? config.getRewardAmountPremium() 
            : config.getRewardAmount();

        // Обрабатываем безлимитный случай
        boolean isUnlimited = dailyLimit == 0;
        int remainingSwipes = isUnlimited ? 0 : Math.max(0, dailyLimit - dailySwipes);

        // Подсчитываем сколько свайпов нужно до следующей награды (используем dailySwipes вместо повторного count)
        int swipesPerReward = config.getSwipesPerReward();
        int swipesUntilReward = swipesPerReward - (dailySwipes % swipesPerReward);
        if (swipesUntilReward == swipesPerReward) {
            swipesUntilReward = 0; // Уже на пороге награды
        }

        return new SwipeStats(
            dailySwipes,
            dailyLimit,
            remainingSwipes,
            hasSubscription,
            profile.getSubscriptionExpiresAt(),
            swipesPerReward,
            swipesUntilReward,
            rewardAmount,
            isUnlimited
        );
    }

    /**
     * Проверить и начислить награды за свайпы.
     */
    private void processRewards(Long userId, SwipeConfigEntity config, LocalDate swipeDate) {
        // Подсчитываем свайпы за сегодня
        int totalSwipes = (int) userSwipeRepository.countByUserIdAndSwipeDate(userId, swipeDate);
        int swipesPerReward = config.getSwipesPerReward();

        if (swipesPerReward <= 0 || totalSwipes % swipesPerReward != 0) {
            // Еще не достигли порога для награды
            return;
        }

        // Вычисляем номер milestone (какую по счету награду начисляем)
        int milestoneNumber = totalSwipes / swipesPerReward;

        // Определяем размер награды с учетом подписки
        UserProfileEntity profile = userProfileService.findByUserId(userId)
                .orElseGet(() -> userProfileService.getOrCreateDefault(userId));
        long rewardAmount = profile.hasActiveSubscription() 
            ? config.getRewardAmountPremium() 
            : config.getRewardAmount();

        // Формируем externalId для идемпотентности
        String externalId = String.format("swipe-reward:%d:%s:%d", userId, swipeDate, milestoneNumber);

        try {
            String metadata = objectMapper.writeValueAsString(Map.of(
                "userId", userId,
                "swipeDate", swipeDate.toString(),
                "milestoneNumber", milestoneNumber,
                "totalSwipes", totalSwipes,
                "hasSubscription", profile.hasActiveSubscription()
            ));

            artRewardService.award(
                userId,
                RULE_SWIPE_REWARD,
                rewardAmount, // overrideAmount
                metadata,
                externalId,
                userId
            );

            LOGGER.info("🎁 Начислена награда за свайпы: userId={}, milestone={}, amount={}, totalSwipes={}, hasSubscription={}",
                       userId, milestoneNumber, rewardAmount, totalSwipes, profile.hasActiveSubscription());
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при начислении награды за свайпы: userId={}, milestone={}", 
                        userId, milestoneNumber, e);
            // Не прерываем транзакцию - награда будет начислена при следующем свайпе
        }
    }

    /**
     * DTO для статистики свайпов
     */
    public static class SwipeStats {
        private final int dailySwipes;
        private final int dailyLimit;
        private final int remainingSwipes;
        private final boolean hasSubscription;
        private final OffsetDateTime subscriptionExpiresAt;
        private final int swipesPerReward;
        private final int swipesUntilReward;
        private final long rewardAmount;
        private final boolean isUnlimited;

        public SwipeStats(int dailySwipes, int dailyLimit, int remainingSwipes,
                         boolean hasSubscription, OffsetDateTime subscriptionExpiresAt,
                         int swipesPerReward, int swipesUntilReward,
                         long rewardAmount, boolean isUnlimited) {
            this.dailySwipes = dailySwipes;
            this.dailyLimit = dailyLimit;
            this.remainingSwipes = remainingSwipes;
            this.hasSubscription = hasSubscription;
            this.subscriptionExpiresAt = subscriptionExpiresAt;
            this.swipesPerReward = swipesPerReward;
            this.swipesUntilReward = swipesUntilReward;
            this.rewardAmount = rewardAmount;
            this.isUnlimited = isUnlimited;
        }

        public int getDailySwipes() { return dailySwipes; }
        public int getDailyLimit() { return dailyLimit; }
        public int getRemainingSwipes() { return remainingSwipes; }
        public boolean isHasSubscription() { return hasSubscription; }
        public OffsetDateTime getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
        public int getSwipesPerReward() { return swipesPerReward; }
        public int getSwipesUntilReward() { return swipesUntilReward; }
        public long getRewardAmount() { return rewardAmount; }
        public boolean isUnlimited() { return isUnlimited; }
    }
}
