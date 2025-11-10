package com.example.sticker_art_gallery.dto;

/**
 * DTO со сводной статистикой по сервису.
 */
public class StatisticsDto {

    private StickerSetStats stickerSets = new StickerSetStats();
    private LikeStats likes = new LikeStats();
    private UserStats users = new UserStats();
    private ArtStats art = new ArtStats();

    public StickerSetStats getStickerSets() {
        return stickerSets;
    }

    public void setStickerSets(StickerSetStats stickerSets) {
        this.stickerSets = stickerSets;
    }

    public LikeStats getLikes() {
        return likes;
    }

    public void setLikes(LikeStats likes) {
        this.likes = likes;
    }

    public UserStats getUsers() {
        return users;
    }

    public void setUsers(UserStats users) {
        this.users = users;
    }

    public ArtStats getArt() {
        return art;
    }

    public void setArt(ArtStats art) {
        this.art = art;
    }

    public static class StickerSetStats {
        private long total;
        private long daily;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getDaily() {
            return daily;
        }

        public void setDaily(long daily) {
            this.daily = daily;
        }
    }

    public static class LikeStats {
        private long total;
        private long daily;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getDaily() {
            return daily;
        }

        public void setDaily(long daily) {
            this.daily = daily;
        }
    }

    public static class UserStats {
        private long total;
        private long activeDaily;
        private long activeWeekly;
        private long newDaily;
        private long newWeekly;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getActiveDaily() {
            return activeDaily;
        }

        public void setActiveDaily(long activeDaily) {
            this.activeDaily = activeDaily;
        }

        public long getActiveWeekly() {
            return activeWeekly;
        }

        public void setActiveWeekly(long activeWeekly) {
            this.activeWeekly = activeWeekly;
        }

        public long getNewDaily() {
            return newDaily;
        }

        public void setNewDaily(long newDaily) {
            this.newDaily = newDaily;
        }

        public long getNewWeekly() {
            return newWeekly;
        }

        public void setNewWeekly(long newWeekly) {
            this.newWeekly = newWeekly;
        }
    }

    public static class ArtStats {
        private AmountStats earned = new AmountStats();
        private AmountStats spent = new AmountStats();

        public AmountStats getEarned() {
            return earned;
        }

        public void setEarned(AmountStats earned) {
            this.earned = earned;
        }

        public AmountStats getSpent() {
            return spent;
        }

        public void setSpent(AmountStats spent) {
            this.spent = spent;
        }
    }

    public static class AmountStats {
        private long total;
        private long daily;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getDaily() {
            return daily;
        }

        public void setDaily(long daily) {
            this.daily = daily;
        }
    }
}


