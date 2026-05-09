package com.example.sticker_art_gallery.service.stylefeed;

import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedVotesResetResultDto;
import com.example.sticker_art_gallery.dto.stylefeed.StyleFeedVotesStatsDto;
import com.example.sticker_art_gallery.repository.UserSwipeRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemDislikeRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemLikeRepository;
import com.example.sticker_art_gallery.repository.stylefeed.StyleFeedItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Агрегаты и точечный сброс голосов style feed (без затрагивания likes/dislikes стикерсетов и user_preset_likes).
 */
@Service
public class StyleFeedVotesAdminService {

    private final StyleFeedItemLikeRepository likeRepository;
    private final StyleFeedItemDislikeRepository dislikeRepository;
    private final StyleFeedItemRepository styleFeedItemRepository;
    private final UserSwipeRepository userSwipeRepository;

    public StyleFeedVotesAdminService(StyleFeedItemLikeRepository likeRepository,
                                      StyleFeedItemDislikeRepository dislikeRepository,
                                      StyleFeedItemRepository styleFeedItemRepository,
                                      UserSwipeRepository userSwipeRepository) {
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.styleFeedItemRepository = styleFeedItemRepository;
        this.userSwipeRepository = userSwipeRepository;
    }

    @Transactional(readOnly = true)
    public StyleFeedVotesStatsDto getStats() {
        StyleFeedVotesStatsDto dto = new StyleFeedVotesStatsDto();
        dto.setStyleFeedLikesTotal(likeRepository.count());
        dto.setStyleFeedDislikesTotal(dislikeRepository.count());
        dto.setDistinctVoters(likeRepository.countDistinctVotersNative());
        return dto;
    }

    /**
     * Удалить все голоса пользователя в ленте style feed и пересчитать счётчики на карточках.
     * {@code userId} — Telegram ID (= {@code users.id}), как во всех пользовательских контроллерах ленты.
     */
    @Transactional
    public StyleFeedVotesResetResultDto resetAllStyleFeedVotesForUser(long userId) {
        Set<Long> affectedIds = new LinkedHashSet<>();
        affectedIds.addAll(likeRepository.findDistinctStyleFeedItemIdsByUserId(userId));
        affectedIds.addAll(dislikeRepository.findDistinctStyleFeedItemIdsByUserId(userId));

        int deletedSwipes = userSwipeRepository.deleteStyleFeedSwipesByUserId(userId);
        int deletedLikes = likeRepository.deleteAllByUserId(userId);
        int deletedDislikes = dislikeRepository.deleteAllByUserId(userId);

        if (!affectedIds.isEmpty()) {
            styleFeedItemRepository.recountVoteColumnsForIds(affectedIds);
            styleFeedItemRepository.refreshDerivedVisibilityForIds(affectedIds);
        }

        StyleFeedVotesResetResultDto out = new StyleFeedVotesResetResultDto();
        out.setUserId(userId);
        out.setDeletedStyleFeedSwipeRows(deletedSwipes);
        out.setDeletedLikeRows(deletedLikes);
        out.setDeletedDislikeRows(deletedDislikes);
        out.setRecomputedStyleFeedItemRows(affectedIds.size());
        return out;
    }
}
