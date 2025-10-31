package com.example.sticker_art_gallery.model.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository для работы с категориями стикерсетов
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Найти категорию по ключу
     * @param key уникальный ключ категории
     * @return Optional с категорией
     */
    Optional<Category> findByKey(String key);

    /**
     * Найти категории по списку ключей
     * @param keys список ключей
     * @return список категорий
     */
    List<Category> findByKeyIn(Set<String> keys);

    /**
     * Найти все активные категории
     * @return список активных категорий
     */
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();

    /**
     * Найти все активные категории по списку ключей
     * @param keys список ключей
     * @return список активных категорий
     */
    List<Category> findByKeyInAndIsActiveTrue(Set<String> keys);

    /**
     * Проверить существование категории по ключу
     * @param key уникальный ключ
     * @return true если существует
     */
    boolean existsByKey(String key);

    /**
     * Проверить существование категории по ключу (кроме указанного ID)
     * Используется при обновлении для проверки уникальности
     * @param key ключ категории
     * @param id ID категории для исключения из проверки
     * @return true если существует другая категория с таким ключом
     */
    boolean existsByKeyAndIdNot(String key, Long id);

    /**
     * Получить количество стикерсетов в категории
     * @param categoryId ID категории
     * @return количество стикерсетов
     */
    @Query("SELECT COUNT(ss) FROM StickerSet ss JOIN ss.categories c WHERE c.id = :categoryId")
    long countStickerSetsByCategory(Long categoryId);

    /**
     * Найти популярные категории (с наибольшим количеством стикерсетов)
     * @param limit количество категорий
     * @return список популярных категорий
     */
    @Query("SELECT c FROM Category c JOIN c.stickerSets ss " +
           "WHERE c.isActive = true " +
           "GROUP BY c.id ORDER BY COUNT(ss) DESC")
    List<Category> findPopularCategories(int limit);

    interface CategoryCountProjection {
        Long getCategoryId();
        Long getCnt();
    }

    /**
     * Подсчет количества публичных и не заблокированных стикерсетов по активным категориям
     * с учетом дополнительных фильтров (officialOnly, authorId, hasAuthorOnly)
     */
    @Query("SELECT c.id as categoryId, COUNT(ss) as cnt " +
           "FROM Category c LEFT JOIN c.stickerSets ss ON " +
           "(ss.isPublic = true AND ss.isBlocked = false " +
           "AND (:officialOnly = false OR ss.isOfficial = true) " +
           "AND (:authorId IS NULL OR ss.authorId = :authorId) " +
           "AND (:hasAuthorOnly = false OR ss.authorId IS NOT NULL)) " +
           "WHERE c.isActive = true " +
           "GROUP BY c.id")
    List<CategoryCountProjection> countStickerSetsByActiveCategories(boolean officialOnly, Long authorId, boolean hasAuthorOnly);
}

