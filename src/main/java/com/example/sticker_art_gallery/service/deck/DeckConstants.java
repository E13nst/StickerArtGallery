package com.example.sticker_art_gallery.service.deck;

/**
 * Размер пачки {@code GET /api/deck/cards} и прогон ассоциативной колоды до награды (free).
 * Премиум: награда каждые {@link #PREMIUM_SWIPES_PER_DECK_REWARD} стилевых свайпов.
 */
public final class DeckConstants {

    /** Параметр {@code limit} по умолчанию и размер ответа API. */
    public static final int DEFAULT_DECK_BATCH_SIZE = 20;

    /** Стилевых свайпов до награды за прогон колоды для пользователя без активной подписки. */
    public static final int FREE_SWIPES_PER_DECK_REWARD = 20;

    /** Стилевых свайпов до награды при активной подписке (бесконечная лента, но не бесконечный ART). */
    public static final int PREMIUM_SWIPES_PER_DECK_REWARD = 40;

    public static final String PREFIX_STYLE_FEED_ITEM = "sfi-";
    public static final String PREFIX_GEN_RESULT = "result-";
    public static final String PREFIX_BLUEPRINT = "blueprint-";
    public static final String PREFIX_WELCOME_STEP = "welcome-step-";
    public static final String CARD_WELCOME_BONUS = "welcome-bonus";
    public static final String PREFIX_DAILY = "daily-";
    public static final String CARD_REFERRAL_INFO = "referral-info";
    public static final String CARD_CUSTOM_PROMPT = "custom-prompt";
    public static final String CARD_DECK_EMPTY = "deck-empty";
    public static final String CARD_SWIPE_LIMIT = "swipe-limit";

    private DeckConstants() {
    }
}
