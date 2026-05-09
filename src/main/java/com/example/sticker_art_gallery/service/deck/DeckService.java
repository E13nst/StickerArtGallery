package com.example.sticker_art_gallery.service.deck;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.SwipeStatsDto;
import com.example.sticker_art_gallery.dto.deck.*;
import com.example.sticker_art_gallery.dto.generation.UserPresetCreationBlueprintDto;
import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedItemDto;
import com.example.sticker_art_gallery.exception.SwipeLimitExceededException;
import com.example.sticker_art_gallery.model.deck.DeckCardEventEntity;
import com.example.sticker_art_gallery.model.deck.UserDeckStateEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskEntity;
import com.example.sticker_art_gallery.model.generation.GenerationTaskStatus;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.referral.ReferralEntity;
import com.example.sticker_art_gallery.model.stylefeed.StyleFeedItemEntity;
import com.example.sticker_art_gallery.repository.ArtTransactionRepository;
import com.example.sticker_art_gallery.repository.GenerationTaskRepository;
import com.example.sticker_art_gallery.repository.ReferralRepository;
import com.example.sticker_art_gallery.repository.deck.DeckCardEventRepository;
import com.example.sticker_art_gallery.repository.deck.UserDeckStateRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemRepository;
import com.example.sticker_art_gallery.service.generation.UserPresetCreationBlueprintService;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.example.sticker_art_gallery.service.profile.UserProfileService;
import com.example.sticker_art_gallery.service.stylefeed.StyleFeedItemService;
import com.example.sticker_art_gallery.service.swipe.SwipeTrackingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Персональная swipe-колода: сборка карточек и действия (стилевые свайпы через {@link StyleFeedItemService}).
 */
@Service
public class DeckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckService.class);

    private static final int WELCOME_ACCOUNT_AGE_DAYS = 7;

    private final UserDeckStateRepository userDeckStateRepository;
    private final DeckCardEventRepository deckCardEventRepository;
    private final StyleFeedItemRepository styleFeedItemRepository;
    private final StyleFeedItemService styleFeedItemService;
    private final SwipeTrackingService swipeTrackingService;
    private final ArtRewardService artRewardService;
    private final ArtTransactionRepository artTransactionRepository;
    private final UserProfileService userProfileService;
    private final GenerationTaskRepository generationTaskRepository;
    private final ReferralRepository referralRepository;
    private final UserPresetCreationBlueprintService blueprintService;
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;

    public DeckService(UserDeckStateRepository userDeckStateRepository,
                       DeckCardEventRepository deckCardEventRepository,
                       StyleFeedItemRepository styleFeedItemRepository,
                       StyleFeedItemService styleFeedItemService,
                       SwipeTrackingService swipeTrackingService,
                       ArtRewardService artRewardService,
                       ArtTransactionRepository artTransactionRepository,
                       UserProfileService userProfileService,
                       GenerationTaskRepository generationTaskRepository,
                       ReferralRepository referralRepository,
                       UserPresetCreationBlueprintService blueprintService,
                       ObjectMapper objectMapper,
                       AppConfig appConfig) {
        this.userDeckStateRepository = userDeckStateRepository;
        this.deckCardEventRepository = deckCardEventRepository;
        this.styleFeedItemRepository = styleFeedItemRepository;
        this.styleFeedItemService = styleFeedItemService;
        this.swipeTrackingService = swipeTrackingService;
        this.artRewardService = artRewardService;
        this.artTransactionRepository = artTransactionRepository;
        this.userProfileService = userProfileService;
        this.generationTaskRepository = generationTaskRepository;
        this.referralRepository = referralRepository;
        this.blueprintService = blueprintService;
        this.objectMapper = objectMapper;
        this.appConfig = appConfig;
    }

    @Transactional(readOnly = true)
    public DeckCardsResponse getDeckCards(Long userId, Integer limitRaw) {
        int limit = sanitizeLimit(limitRaw);
        UserProfileEntity profile = userProfileService.findByUserId(userId)
                .orElseGet(() -> userProfileService.getOrCreateDefault(userId));

        SwipeTrackingService.SwipeStats swipeStats = swipeTrackingService.getDailyStats(userId);
        SwipeStatsDto statsDto = toSwipeStatsDto(swipeStats);

        UserDeckStateEntity state = userDeckStateRepository.findById(userId).orElse(null);
        int inRun = state != null ? state.getStyleSwipesInRun() : 0;
        long completions = state != null ? state.getDeckCompletionsTotal() : 0;
        boolean premium = swipeStats.isHasSubscription();
        int threshold = thresholdFor(premium);

        DeckProgressDto progress = new DeckProgressDto();
        progress.setStyleSwipesInCurrentRun(inRun);
        progress.setSwipesRequiredForDeckReward(threshold);
        progress.setSwipesRemainingUntilDeckReward(Math.max(0, threshold - inRun));
        progress.setPremium(premium);
        progress.setDeckCompletionsTotal(completions);

        List<ScoredCard> scored = new ArrayList<>();

        addWelcomeAndOnboarding(scored, profile);
        addWelcomeBonusCard(scored, userId);
        addReferralCard(scored, userId);
        addDailyCheckinCard(scored, userId);
        addLastGenerationCard(scored, userId);
        addBlueprintCards(scored, userId);
        addCustomPromptCard(scored, userId);

        boolean limitReached = !swipeStats.isUnlimited()
                && swipeStats.getDailyLimit() > 0
                && swipeStats.getRemainingSwipes() == 0;

        if (limitReached) {
            scored.add(new ScoredCard(8500, buildSwipeLimitCard()));
        } else {
            addStylePresetCards(scored, userId, limit, profile.getUserId());
        }

        scored.sort(Comparator.comparingInt(ScoredCard::priority));
        List<DeckCardDto> cards = scored.stream()
                .map(ScoredCard::card)
                .limit(limit)
                .collect(Collectors.toList());

        if (cards.stream().noneMatch(c -> c.getType() == DeckCardType.STYLE_PRESET)
                && !limitReached
                && cards.stream().noneMatch(c -> c.getType() == DeckCardType.SWIPE_LIMIT)) {
            cards.add(buildDeckEmptyCard());
        }

        DeckCardsResponse response = new DeckCardsResponse();
        response.setCards(cards);
        response.setSwipeStats(statsDto);
        response.setDeckProgress(progress);
        return response;
    }

    @Transactional
    public DeckCardActionResponse performAction(Long userId, DeckCardActionRequest request) {
        DeckCardAction action = request.getAction();
        String instanceId = request.getCardInstanceId() != null
                ? request.getCardInstanceId().trim()
                : "";

        DeckCardActionResponse resp = new DeckCardActionResponse();
        resp.setSuccess(true);

        if (instanceId.startsWith(DeckConstants.PREFIX_STYLE_FEED_ITEM)) {
            return handleStyleSwipe(userId, instanceId, action, resp);
        }
        if (instanceId.startsWith(DeckConstants.PREFIX_GEN_RESULT)) {
            return handleGenerationCard(userId, instanceId, action, resp);
        }
        if (instanceId.startsWith(DeckConstants.PREFIX_BLUEPRINT)) {
            return handleBlueprint(userId, instanceId, action, resp);
        }
        if (DeckConstants.CARD_CUSTOM_PROMPT.equals(instanceId)) {
            return handleCustomPrompt(userId, action, resp);
        }
        if (instanceId.startsWith(DeckConstants.PREFIX_WELCOME_STEP)) {
            return handleWelcomeStep(userId, instanceId, action, resp);
        }
        if (DeckConstants.CARD_WELCOME_BONUS.equals(instanceId)) {
            return handleWelcomeBonusClaim(userId, action, resp);
        }
        if (instanceId.startsWith(DeckConstants.PREFIX_DAILY)) {
            return handleDaily(userId, instanceId, action, resp);
        }
        if (DeckConstants.CARD_REFERRAL_INFO.equals(instanceId)) {
            return handleReferralInfo(userId, action, resp);
        }
        if (DeckConstants.CARD_DECK_EMPTY.equals(instanceId) || DeckConstants.CARD_SWIPE_LIMIT.equals(instanceId)) {
            resp.setMessage("no-op");
            resp.setDeckProgress(loadProgress(userId));
            resp.setSwipeStats(loadSwipeStats(userId));
            return resp;
        }

        throw new IllegalArgumentException("Неизвестная карточка: " + instanceId);
    }

    private DeckCardActionResponse handleStyleSwipe(Long userId, String instanceId, DeckCardAction action,
                                                   DeckCardActionResponse resp) {
        long itemId = parseSfiId(instanceId);
        if (action != DeckCardAction.LIKE && action != DeckCardAction.DISLIKE) {
            throw new IllegalArgumentException("Для стилевой карточки допустимы только LIKE и DISLIKE");
        }

        boolean isSwipe = true;
        try {
            if (action == DeckCardAction.LIKE) {
                styleFeedItemService.likeFeedItem(userId, itemId, isSwipe);
            } else {
                styleFeedItemService.dislikeFeedItem(userId, itemId, isSwipe);
            }
        } catch (SwipeLimitExceededException e) {
            throw e;
        }

        ArtTransactionEntity deckAward = incrementDeckRunAndMaybeAward(userId);
        if (deckAward != null) {
            resp.setArtDelta(deckAward.getDelta());
            resp.setBalanceAfter(deckAward.getBalanceAfter());
        } else {
            UserProfileEntity profile = userProfileService.getOrCreateDefault(userId);
            resp.setBalanceAfter(profile.getArtBalance());
        }

        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    /**
     * @return транзакция награды за прогон колоды или null
     */
    private ArtTransactionEntity incrementDeckRunAndMaybeAward(Long userId) {
        UserDeckStateEntity state = userDeckStateRepository.findByUserIdForUpdate(userId).orElseGet(() -> {
            UserDeckStateEntity s = new UserDeckStateEntity();
            s.setUserId(userId);
            s.setStyleSwipesInRun(0);
            s.setDeckCompletionsTotal(0);
            return userDeckStateRepository.save(s);
        });

        UserProfileEntity profile = userProfileService.findByUserId(userId)
                .orElseGet(() -> userProfileService.getOrCreateDefault(userId));
        int threshold = profile.hasActiveSubscription()
                ? DeckConstants.PREMIUM_SWIPES_PER_DECK_REWARD
                : DeckConstants.FREE_SWIPES_PER_DECK_REWARD;

        state.setStyleSwipesInRun(state.getStyleSwipesInRun() + 1);
        ArtTransactionEntity award = null;
        if (state.getStyleSwipesInRun() >= threshold) {
            long ordinal = state.getDeckCompletionsTotal() + 1;
            String externalId = "deck-completion:" + userId + ":" + ordinal;
            String metadata = metadataJson(Map.of("userId", userId, "completionOrdinal", ordinal));
            award = artRewardService.award(userId, ArtRewardService.RULE_DECK_RUN_COMPLETE, null,
                    metadata, externalId, userId);
            state.setDeckCompletionsTotal(ordinal);
            state.setStyleSwipesInRun(0);
            LOGGER.info("Награда за прогон колоды: userId={}, ordinal={}", userId, ordinal);
        }
        userDeckStateRepository.save(state);
        return award;
    }

    private DeckCardActionResponse handleGenerationCard(Long userId, String instanceId, DeckCardAction action,
                                                        DeckCardActionResponse resp) {
        String taskId = instanceId.substring(DeckConstants.PREFIX_GEN_RESULT.length());
        if (action != DeckCardAction.DISMISS && action != DeckCardAction.OPEN) {
            throw new IllegalArgumentException("Для карточки результата генерации допустимы DISMISS и OPEN");
        }
        GenerationTaskEntity task = generationTaskRepository.findByTaskId(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Задача не найдена"));
        if (!userId.equals(task.getUserProfile().getUserId())) {
            throw new IllegalArgumentException("Доступ к задаче запрещён");
        }
        recordOnce(userId, instanceId, DeckCardType.LAST_GENERATION.name(), action.name(), null);
        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    private DeckCardActionResponse handleBlueprint(Long userId, String instanceId, DeckCardAction action,
                                                   DeckCardActionResponse resp) {
        if (action != DeckCardAction.DISMISS && action != DeckCardAction.PRIMARY_CTA) {
            throw new IllegalArgumentException("Для карточки blueprint допустимы DISMISS и PRIMARY_CTA");
        }
        recordOnce(userId, instanceId, DeckCardType.CREATE_STYLE_BLUEPRINT.name(), action.name(), null);
        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    private DeckCardActionResponse handleCustomPrompt(Long userId, DeckCardAction action, DeckCardActionResponse resp) {
        if (action != DeckCardAction.DISMISS && action != DeckCardAction.PRIMARY_CTA) {
            throw new IllegalArgumentException("Для custom-prompt допустимы DISMISS и PRIMARY_CTA");
        }
        recordOnce(userId, DeckConstants.CARD_CUSTOM_PROMPT, DeckCardType.CUSTOM_PROMPT.name(), action.name(), null);
        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    private DeckCardActionResponse handleWelcomeStep(Long userId, String instanceId, DeckCardAction action,
                                                     DeckCardActionResponse resp) {
        if (action != DeckCardAction.DISMISS) {
            throw new IllegalArgumentException("Для приветственного шага допустим только DISMISS");
        }
        recordOnce(userId, instanceId, DeckCardType.WELCOME_STEP.name(), action.name(), null);
        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    private DeckCardActionResponse handleWelcomeBonusClaim(Long userId, DeckCardAction action,
                                                           DeckCardActionResponse resp) {
        if (action != DeckCardAction.CLAIM && action != DeckCardAction.DISMISS) {
            throw new IllegalArgumentException("Для welcome-bonus допустимы CLAIM и DISMISS");
        }
        if (action == DeckCardAction.DISMISS) {
            recordOnce(userId, DeckConstants.CARD_WELCOME_BONUS, DeckCardType.WELCOME_BONUS_CLAIM.name(),
                    action.name(), null);
            resp.setDeckProgress(loadProgress(userId));
            resp.setSwipeStats(loadSwipeStats(userId));
            return resp;
        }
        String externalId = "welcome-bonus:" + userId;
        if (artTransactionRepository.findByExternalId(externalId).isPresent()) {
            throw new IllegalArgumentException("Приветственный бонус уже получен");
        }
        String metadata = metadataJson(Map.of("userId", userId));
        ArtTransactionEntity tx = artRewardService.award(
                userId, ArtRewardService.RULE_WELCOME_BONUS, null, metadata, externalId, userId);
        recordOnce(userId, DeckConstants.CARD_WELCOME_BONUS, DeckCardType.WELCOME_BONUS_CLAIM.name(),
                DeckCardAction.CLAIM.name(), null);
        resp.setArtDelta(tx.getDelta());
        resp.setBalanceAfter(tx.getBalanceAfter());
        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    private DeckCardActionResponse handleDaily(Long userId, String instanceId, DeckCardAction action,
                                               DeckCardActionResponse resp) {
        String iso = instanceId.substring(DeckConstants.PREFIX_DAILY.length());
        LocalDate date;
        try {
            date = LocalDate.parse(iso);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Некорректная дата карточки");
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (!date.equals(today)) {
            throw new IllegalArgumentException("Карточка ежедневного бонуса устарела");
        }
        if (action != DeckCardAction.CLAIM && action != DeckCardAction.DISMISS) {
            throw new IllegalArgumentException("Для daily-checkin допустимы CLAIM и DISMISS");
        }
        String key = DeckConstants.PREFIX_DAILY + iso;
        if (action == DeckCardAction.DISMISS) {
            recordOnce(userId, key, DeckCardType.DAILY_CHECKIN.name(), action.name(), null);
            resp.setDeckProgress(loadProgress(userId));
            resp.setSwipeStats(loadSwipeStats(userId));
            return resp;
        }
        String externalId = "daily-bonus:" + userId + ":" + iso;
        if (artTransactionRepository.findByExternalId(externalId).isPresent()) {
            throw new IllegalArgumentException("Ежедневный бонус уже получен");
        }
        String metadata = metadataJson(Map.of("userId", userId, "bonusDate", iso));
        ArtTransactionEntity tx = artRewardService.award(
                userId, ArtRewardService.RULE_DAILY_BONUS, null, metadata, externalId, userId);
        recordOnce(userId, key, DeckCardType.DAILY_CHECKIN.name(), DeckCardAction.CLAIM.name(), null);
        resp.setArtDelta(tx.getDelta());
        resp.setBalanceAfter(tx.getBalanceAfter());
        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    private DeckCardActionResponse handleReferralInfo(Long userId, DeckCardAction action, DeckCardActionResponse resp) {
        if (action != DeckCardAction.DISMISS && action != DeckCardAction.OPEN) {
            throw new IllegalArgumentException("Для referral-info допустимы DISMISS и OPEN");
        }
        recordOnce(userId, DeckConstants.CARD_REFERRAL_INFO, DeckCardType.REFERRAL_INFO.name(), action.name(), null);
        resp.setDeckProgress(loadProgress(userId));
        resp.setSwipeStats(loadSwipeStats(userId));
        return resp;
    }

    private void recordOnce(Long userId, String cardKey, String cardType, String action, Map<String, Object> meta) {
        if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, cardKey, action)) {
            return;
        }
        DeckCardEventEntity e = new DeckCardEventEntity();
        e.setUserId(userId);
        e.setCardKey(cardKey);
        e.setCardType(cardType);
        e.setAction(action);
        e.setMetadataJson(meta);
        try {
            deckCardEventRepository.save(e);
        } catch (Exception ex) {
            LOGGER.debug("Повтор события колоды (race): userId={}, key={}, action={}", userId, cardKey, action);
        }
    }

    private DeckProgressDto loadProgress(Long userId) {
        SwipeTrackingService.SwipeStats s = swipeTrackingService.getDailyStats(userId);
        UserDeckStateEntity state = userDeckStateRepository.findById(userId).orElse(null);
        int inRun = state != null ? state.getStyleSwipesInRun() : 0;
        DeckProgressDto p = new DeckProgressDto();
        p.setStyleSwipesInCurrentRun(inRun);
        p.setPremium(s.isHasSubscription());
        int th = thresholdFor(s.isHasSubscription());
        p.setSwipesRequiredForDeckReward(th);
        p.setSwipesRemainingUntilDeckReward(Math.max(0, th - inRun));
        p.setDeckCompletionsTotal(state != null ? state.getDeckCompletionsTotal() : 0);
        return p;
    }

    private SwipeStatsDto loadSwipeStats(Long userId) {
        return toSwipeStatsDto(swipeTrackingService.getDailyStats(userId));
    }

    private static int thresholdFor(boolean premium) {
        return premium ? DeckConstants.PREMIUM_SWIPES_PER_DECK_REWARD : DeckConstants.FREE_SWIPES_PER_DECK_REWARD;
    }

    private void addWelcomeAndOnboarding(List<ScoredCard> scored, UserProfileEntity profile) {
        java.time.OffsetDateTime cutoff = java.time.OffsetDateTime.now().minusDays(WELCOME_ACCOUNT_AGE_DAYS);
        if (profile.getCreatedAt() == null || profile.getCreatedAt().isBefore(cutoff)) {
            return;
        }
        Long uid = profile.getUserId();
        for (int step = 1; step <= 3; step++) {
            String key = DeckConstants.PREFIX_WELCOME_STEP + step;
            if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(uid, key, DeckCardAction.DISMISS.name())) {
                continue;
            }
            DeckCardDto c = new DeckCardDto();
            c.setCardInstanceId(key);
            c.setType(DeckCardType.WELCOME_STEP);
            c.setPriority(10 + step);
            c.setConsumesSwipeLimit(false);
            c.setGrantsSwipeReward(false);
            c.setActions(List.of(DeckCardAction.DISMISS));
            c.setPayload(Map.of("step", step, "title", "Онбординг", "body", "Шаг " + step));
            scored.add(new ScoredCard(10 + step, c));
        }
    }

    private void addWelcomeBonusCard(List<ScoredCard> scored, Long userId) {
        if (artTransactionRepository.findByExternalId("welcome-bonus:" + userId).isPresent()) {
            return;
        }
        if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, DeckConstants.CARD_WELCOME_BONUS,
                DeckCardAction.CLAIM.name())
                || deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, DeckConstants.CARD_WELCOME_BONUS,
                DeckCardAction.DISMISS.name())) {
            return;
        }
        DeckCardDto c = new DeckCardDto();
        c.setCardInstanceId(DeckConstants.CARD_WELCOME_BONUS);
        c.setType(DeckCardType.WELCOME_BONUS_CLAIM);
        c.setPriority(25);
        c.setConsumesSwipeLimit(false);
        c.setGrantsSwipeReward(false);
        c.setActions(List.of(DeckCardAction.CLAIM, DeckCardAction.DISMISS));
        c.setPayload(Map.of("title", "Приветственный бонус", "body", "Получите бонус ART (правило WELCOME_BONUS)."));
        scored.add(new ScoredCard(25, c));
    }

    private void addReferralCard(List<ScoredCard> scored, Long userId) {
        Optional<ReferralEntity> ref = referralRepository.findByReferredUserId(userId);
        if (ref.isEmpty()) {
            return;
        }
        if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, DeckConstants.CARD_REFERRAL_INFO,
                DeckCardAction.DISMISS.name())
                || deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, DeckConstants.CARD_REFERRAL_INFO,
                DeckCardAction.OPEN.name())) {
            return;
        }
        ReferralEntity r = ref.get();
        DeckCardDto c = new DeckCardDto();
        c.setCardInstanceId(DeckConstants.CARD_REFERRAL_INFO);
        c.setType(DeckCardType.REFERRAL_INFO);
        c.setPriority(35);
        c.setConsumesSwipeLimit(false);
        c.setGrantsSwipeReward(false);
        c.setActions(List.of(DeckCardAction.OPEN, DeckCardAction.DISMISS));
        c.setPayload(Map.of(
                "referrerUserId", r.getReferrerUserId(),
                "title", "Вы перешли по приглашению",
                "body", "Бонус за реферальную программу начисляется по правилам сервиса."));
        scored.add(new ScoredCard(35, c));
    }

    private void addDailyCheckinCard(List<ScoredCard> scored, Long userId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String iso = today.toString();
        String externalId = "daily-bonus:" + userId + ":" + iso;
        if (artTransactionRepository.findByExternalId(externalId).isPresent()) {
            return;
        }
        String key = DeckConstants.PREFIX_DAILY + iso;
        if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, key, DeckCardAction.CLAIM.name())
                || deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, key, DeckCardAction.DISMISS.name())) {
            return;
        }
        DeckCardDto c = new DeckCardDto();
        c.setCardInstanceId(key);
        c.setType(DeckCardType.DAILY_CHECKIN);
        c.setPriority(40);
        c.setConsumesSwipeLimit(false);
        c.setGrantsSwipeReward(false);
        c.setActions(List.of(DeckCardAction.CLAIM, DeckCardAction.DISMISS));
        c.setPayload(Map.of("bonusDate", iso, "title", "Ежедневный бонус", "body", "Заберите ART за вход сегодня."));
        scored.add(new ScoredCard(40, c));
    }

    private void addLastGenerationCard(List<ScoredCard> scored, Long userId) {
        Optional<GenerationTaskEntity> taskOpt = generationTaskRepository
                .findFirstByUserProfile_UserIdAndStatusAndCachedImageIdIsNotNullOrderByCompletedAtDesc(
                        userId, GenerationTaskStatus.COMPLETED);
        if (taskOpt.isEmpty()) {
            return;
        }
        GenerationTaskEntity t = taskOpt.get();
        String instanceId = DeckConstants.PREFIX_GEN_RESULT + t.getTaskId();
        if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, instanceId,
                DeckCardAction.DISMISS.name())) {
            return;
        }
        DeckCardDto c = new DeckCardDto();
        c.setCardInstanceId(instanceId);
        c.setType(DeckCardType.LAST_GENERATION);
        c.setPriority(50);
        c.setConsumesSwipeLimit(false);
        c.setGrantsSwipeReward(false);
        c.setActions(List.of(DeckCardAction.OPEN, DeckCardAction.DISMISS));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", t.getTaskId());
        payload.put("imageUrl", t.getImageUrl());
        payload.put("promptPreview", truncate(t.getPrompt(), 200));
        c.setPayload(payload);
        scored.add(new ScoredCard(50, c));
    }

    private void addBlueprintCards(List<ScoredCard> scored, Long userId) {
        List<UserPresetCreationBlueprintDto> blueprints = blueprintService.listEnabledForUser();
        int p = 60;
        int cap = 0;
        for (UserPresetCreationBlueprintDto bp : blueprints) {
            if (bp.getCode() == null || cap >= 3) {
                continue;
            }
            cap++;
            String id = DeckConstants.PREFIX_BLUEPRINT + bp.getCode();
            if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, id,
                    DeckCardAction.DISMISS.name())) {
                continue;
            }
            DeckCardDto c = new DeckCardDto();
            c.setCardInstanceId(id);
            c.setType(DeckCardType.CREATE_STYLE_BLUEPRINT);
            c.setPriority(p++);
            c.setConsumesSwipeLimit(false);
            c.setGrantsSwipeReward(false);
            c.setActions(List.of(DeckCardAction.PRIMARY_CTA, DeckCardAction.DISMISS));
            c.setPayload(Map.of(
                    "blueprintCode", bp.getCode(),
                    "title", "Создать стиль",
                    "body", "Шаблон: " + bp.getCode()));
            scored.add(new ScoredCard(c.getPriority(), c));
        }
    }

    private void addCustomPromptCard(List<ScoredCard> scored, Long userId) {
        if (deckCardEventRepository.existsByUserIdAndCardKeyAndAction(userId, DeckConstants.CARD_CUSTOM_PROMPT,
                DeckCardAction.DISMISS.name())) {
            return;
        }
        DeckCardDto c = new DeckCardDto();
        c.setCardInstanceId(DeckConstants.CARD_CUSTOM_PROMPT);
        c.setType(DeckCardType.CUSTOM_PROMPT);
        c.setPriority(75);
        c.setConsumesSwipeLimit(false);
        c.setGrantsSwipeReward(false);
        c.setActions(List.of(DeckCardAction.PRIMARY_CTA, DeckCardAction.DISMISS));
        c.setPayload(Map.of("title", "Свой промпт", "body", "Генерация по собственному тексту."));
        scored.add(new ScoredCard(75, c));
    }

    private void addStylePresetCards(List<ScoredCard> scored, Long userId, int limit, Long profileUserId) {
        int nonStyle = (int) scored.stream()
                .filter(s -> s.card.getType() != DeckCardType.STYLE_PRESET)
                .count();
        int styleSlots = Math.max(0, limit - nonStyle);
        if (styleSlots <= 0) {
            return;
        }
        int deckQueryLimit = Math.min(200, Math.max(styleSlots * 10, 40));
        List<Long> orderedIds = appConfig.getStyleFeed().isRepeatRatedQaActiveForUser(userId)
                ? styleFeedItemRepository.findIdsForDeckVisibleOnly(deckQueryLimit)
                : styleFeedItemRepository.findIdsForDeckOrdered(userId, deckQueryLimit);
        if (orderedIds.isEmpty()) {
            return;
        }
        List<Long> shuffled = shuffleWithDailySeed(new ArrayList<>(orderedIds), profileUserId);
        int prio = 100;
        for (int i = 0; i < Math.min(styleSlots, shuffled.size()); i++) {
            Long sid = shuffled.get(i);
            Optional<StyleFeedItemEntity> opt = styleFeedItemRepository.findById(sid);
            if (opt.isEmpty()) {
                continue;
            }
            StyleFeedItemDto dto = StyleFeedItemDto.fromEntity(opt.get());
            DeckCardDto c = new DeckCardDto();
            c.setCardInstanceId(DeckConstants.PREFIX_STYLE_FEED_ITEM + sid);
            c.setType(DeckCardType.STYLE_PRESET);
            c.setPriority(prio++);
            c.setConsumesSwipeLimit(true);
            c.setGrantsSwipeReward(true);
            c.setActions(List.of(DeckCardAction.LIKE, DeckCardAction.DISLIKE));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("styleFeedItem", dto);
            c.setPayload(payload);
            scored.add(new ScoredCard(c.getPriority(), c));
        }
    }

    private static List<Long> shuffleWithDailySeed(List<Long> ids, long userId) {
        long seed = userId * 31L + LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        Collections.shuffle(ids, new Random(seed));
        return ids;
    }

    private DeckCardDto buildSwipeLimitCard() {
        DeckCardDto c = new DeckCardDto();
        c.setCardInstanceId(DeckConstants.CARD_SWIPE_LIMIT);
        c.setType(DeckCardType.SWIPE_LIMIT);
        c.setPriority(8500);
        c.setConsumesSwipeLimit(false);
        c.setGrantsSwipeReward(false);
        c.setActions(List.of(DeckCardAction.DISMISS));
        c.setPayload(Map.of("title", "Лимит свайпов", "body", "На сегодня лимит исчерпан. Зайдите завтра или оформите премиум."));
        return c;
    }

    private DeckCardDto buildDeckEmptyCard() {
        DeckCardDto c = new DeckCardDto();
        c.setCardInstanceId(DeckConstants.CARD_DECK_EMPTY);
        c.setType(DeckCardType.DECK_EMPTY);
        c.setPriority(9000);
        c.setConsumesSwipeLimit(false);
        c.setGrantsSwipeReward(false);
        c.setActions(List.of(DeckCardAction.DISMISS));
        c.setPayload(Map.of("title", "Пока пусто", "body", "Нет новых стилей для оценки. Загляните позже."));
        return c;
    }

    private static SwipeStatsDto toSwipeStatsDto(SwipeTrackingService.SwipeStats s) {
        return new SwipeStatsDto(
                s.getDailySwipes(),
                s.getDailyLimit(),
                s.getRemainingSwipes(),
                s.isHasSubscription(),
                s.getSubscriptionExpiresAt(),
                s.getSwipesPerReward(),
                s.getSwipesUntilReward(),
                s.getRewardAmount(),
                s.isUnlimited()
        );
    }

    private static int sanitizeLimit(Integer raw) {
        if (raw == null || raw < 1) {
            return DeckConstants.DEFAULT_DECK_BATCH_SIZE;
        }
        return Math.min(raw, DeckConstants.DEFAULT_DECK_BATCH_SIZE);
    }

    private static long parseSfiId(String instanceId) {
        String num = instanceId.substring(DeckConstants.PREFIX_STYLE_FEED_ITEM.length());
        try {
            return Long.parseLong(num);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный id style feed");
        }
    }

    private String metadataJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("metadata", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }

    private record ScoredCard(int priority, DeckCardDto card) {
    }
}
