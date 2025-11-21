package com.example.sticker_art_gallery.model.telegram;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Безопасный базовый класс для репозиториев, который блокирует deleteAll()
 * только для StickerSet (для защиты продакшн данных в интеграционных тестах)
 */
public class SafeStickerSetRepositoryBase<T, ID> extends SimpleJpaRepository<T, ID> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SafeStickerSetRepositoryBase.class);
    private final Class<T> domainClass;
    
    // Конструктор, принимающий JpaEntityInformation (используется Spring Data JPA)
    public SafeStickerSetRepositoryBase(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.domainClass = entityInformation.getJavaType();
    }
    
    // Конструктор, принимающий Class (для обратной совместимости)
    public SafeStickerSetRepositoryBase(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.domainClass = domainClass;
    }
    
    /**
     * БЛОКИРОВАННЫЙ метод deleteAll() для StickerSet - всегда выбрасывает исключение
     * 
     * ⚠️ ВНИМАНИЕ: Этот метод ЗАБЛОКИРОВАН для StickerSet для защиты данных!
     * 
     * Интеграционные тесты работают с продакшн БД, поэтому deleteAll() 
     * может удалить все реальные данные пользователей.
     * 
     * Для других сущностей метод работает нормально (для совместимости с существующими тестами).
     * 
     * Вместо deleteAll() для StickerSet используйте:
     * - Удаление конкретных записей по ID: repository.deleteById(id)
     * - Удаление по имени: repository.findByName(name).ifPresent(repository::delete)
     * - Удаление списка: repository.deleteAll(List<StickerSet>) - только для конкретных сущностей
     * 
     * @throws IllegalStateException если вызывается для StickerSet
     */
    @Override
    public void deleteAll() {
        // Блокируем только для StickerSet
        if (StickerSet.class.equals(domainClass)) {
            String errorMessage = 
                    "🚨 КРИТИЧЕСКАЯ ОШИБКА БЕЗОПАСНОСТИ!\n" +
                    "\n" +
                    "Попытка вызвать deleteAll() на StickerSetRepository!\n" +
                    "\n" +
                    "⚠️  Этот метод ЗАБЛОКИРОВАН для защиты данных.\n" +
                    "Интеграционные тесты работают с ПРОДАКШН БД, поэтому deleteAll()\n" +
                    "может удалить ВСЕ реальные данные пользователей!\n" +
                    "\n" +
                    "✅ Используйте вместо этого:\n" +
                    "   - repository.deleteById(id) - удаление по ID\n" +
                    "   - repository.findByName(name).ifPresent(repository::delete) - удаление по имени\n" +
                    "   - repository.deleteAll(List<StickerSet>) - удаление конкретного списка\n" +
                    "\n" +
                    "📚 Stack trace вызова:";
            
            LOGGER.error(errorMessage);
            
            // Выводим полный stack trace для отладки
            RuntimeException exception = new IllegalStateException("deleteAll() заблокирован для StickerSet");
            LOGGER.error("Stack trace:", exception);
            
            throw new IllegalStateException(errorMessage + "\n\nСм. логи для полного stack trace.");
        }
        
        // Для других сущностей разрешаем (для совместимости)
        super.deleteAll();
    }
}

