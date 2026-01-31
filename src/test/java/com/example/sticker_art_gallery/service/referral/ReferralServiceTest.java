package com.example.sticker_art_gallery.service.referral;

import com.example.sticker_art_gallery.dto.referral.ReferralLinkDto;
import com.example.sticker_art_gallery.model.profile.UserProfileEntity;
import com.example.sticker_art_gallery.model.referral.ReferralCodeEntity;
import com.example.sticker_art_gallery.model.referral.ReferralEntity;
import com.example.sticker_art_gallery.model.referral.ReferralEventEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.ReferralCodeRepository;
import com.example.sticker_art_gallery.repository.ReferralEventRepository;
import com.example.sticker_art_gallery.repository.ReferralRepository;
import com.example.sticker_art_gallery.repository.UserProfileRepository;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для ReferralService.
 * 
 * ⚠️ ВНИМАНИЕ: Эти тесты работают с продовой БД!
 * Все изменения откатываются через @Transactional, но будьте осторожны.
 * Удаляются только тестовые данные по конкретным ID.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("integration")
@Epic("Реферальная программа")
@Feature("ReferralService")
@DisplayName("Интеграционные тесты сервиса реферальной программы")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReferralServiceTest {

    @Autowired
    private ReferralService referralService;

    @Autowired
    private ReferralCodeRepository referralCodeRepository;

    @Autowired
    private ReferralRepository referralRepository;

    @Autowired
    private ReferralEventRepository referralEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    private static final Long TEST_REFERRER_USER_ID = 999001L;
    private static final Long TEST_REFERRED_USER_ID = 999002L;

    @BeforeAll
    void setUpOnce() {
        // Создаём тестовых пользователей один раз для всех тестов
        createTestUser(TEST_REFERRER_USER_ID);
        createTestUser(TEST_REFERRED_USER_ID);
    }
    
    @BeforeEach
    void setUp() {
        // Безопасная очистка: удаляем только тестовые данные по конкретным ID
        cleanupTestReferralData();
    }
    
    @AfterAll
    void tearDown() {
        // Финальная очистка тестовых данных после всех тестов
        cleanupTestReferralData();
    }
    
    /**
     * Безопасная очистка тестовых данных.
     * Удаляет только данные, связанные с тестовыми пользователями по конкретным ID.
     * ⚠️ ВАЖНО: Не использует deleteAll() - удаляет только тестовые данные!
     */
    private void cleanupTestReferralData() {
        // Удаляем referral_events для тестовых пользователей
        // Используем поиск по referral_id, чтобы не затронуть другие данные
        List<ReferralEntity> testReferrals = referralRepository.findAll().stream()
                .filter(r -> r.getReferrerUserId().equals(TEST_REFERRER_USER_ID) || 
                           r.getReferrerUserId().equals(TEST_REFERRED_USER_ID) ||
                           r.getReferredUserId().equals(TEST_REFERRER_USER_ID) ||
                           r.getReferredUserId().equals(TEST_REFERRED_USER_ID))
                .toList();
        
        for (ReferralEntity referral : testReferrals) {
            // Удаляем связанные events
            List<ReferralEventEntity> events = referralEventRepository.findByReferral_Id(referral.getId());
            referralEventRepository.deleteAll(events);
            // Удаляем сам referral
            referralRepository.delete(referral);
        }
        
        // Удаляем referral_codes только для тестовых пользователей
        if (referralCodeRepository.existsById(TEST_REFERRER_USER_ID)) {
            referralCodeRepository.deleteById(TEST_REFERRER_USER_ID);
        }
        if (referralCodeRepository.existsById(TEST_REFERRED_USER_ID)) {
            referralCodeRepository.deleteById(TEST_REFERRED_USER_ID);
        }
    }
    
    /**
     * Создаёт тестового пользователя и профиль, если их нет.
     */
    private void createTestUser(Long userId) {
        // Создаём UserEntity если не существует
        if (!userRepository.existsById(userId)) {
            UserEntity user = TestDataBuilder.createTestUser(userId);
            userRepository.save(user);
        }
        
        // Создаём UserProfileEntity если не существует
        if (!userProfileRepository.existsByUserId(userId)) {
            UserProfileEntity profile = TestDataBuilder.createTestUserProfile(userId);
            userProfileRepository.save(profile);
        }
    }

    @Test
    @Story("Генерация реферальной ссылки")
    @DisplayName("Должен создать уникальный реферальный код при первом вызове")
    @Description("Проверяет, что при первом вызове getOrCreateMyReferralLink создаётся новый код")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCreateReferralCodeOnFirstCall() {
        // Given
        assertFalse(referralCodeRepository.existsById(TEST_REFERRER_USER_ID));

        // When
        ReferralLinkDto link = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);

        // Then
        assertNotNull(link);
        assertNotNull(link.getCode());
        assertEquals("ref_" + link.getCode(), link.getStartParam());
        assertTrue(link.getUrl().contains(link.getStartParam()));
        
        // Проверяем, что код сохранён в БД
        Optional<ReferralCodeEntity> saved = referralCodeRepository.findById(TEST_REFERRER_USER_ID);
        assertTrue(saved.isPresent());
        assertEquals(link.getCode(), saved.get().getCode());
    }

    @Test
    @Story("Генерация реферальной ссылки")
    @DisplayName("Должен вернуть существующий код при повторном вызове")
    @Description("Проверяет идемпотентность генерации реферального кода")
    @Severity(SeverityLevel.CRITICAL)
    void shouldReturnExistingCodeOnSecondCall() {
        // Given
        ReferralLinkDto firstLink = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);

        // When
        ReferralLinkDto secondLink = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);

        // Then
        assertEquals(firstLink.getCode(), secondLink.getCode());
        assertEquals(firstLink.getStartParam(), secondLink.getStartParam());
        assertEquals(firstLink.getUrl(), secondLink.getUrl());
    }

    @Test
    @Story("Обработка первой аутентификации")
    @DisplayName("Должен создать реферальную связь и начислить бонус приглашённому")
    @Description("Проверяет, что при первой аутентификации с валидным startParam создаётся referral и начисляется 100 ART")
    @Severity(SeverityLevel.CRITICAL)
    void shouldCreateReferralAndAwardInviteeBonus() {
        // Given
        ReferralLinkDto referrerLink = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);
        String startParam = referrerLink.getStartParam();
        String metadata = "{\"source\":\"test\"}";

        // When
        referralService.onFirstAuthentication(TEST_REFERRED_USER_ID, startParam, metadata);

        // Then
        Optional<ReferralEntity> referral = referralRepository.findByReferredUserId(TEST_REFERRED_USER_ID);
        assertTrue(referral.isPresent());
        assertEquals(TEST_REFERRER_USER_ID, referral.get().getReferrerUserId());
        assertEquals(TEST_REFERRED_USER_ID, referral.get().getReferredUserId());
        assertEquals(startParam, referral.get().getStartParam());
        assertNotNull(referral.get().getInviteeBonusAwardedAt());
    }

    @Test
    @Story("Обработка первой аутентификации")
    @DisplayName("Не должен создать повторную реферальную связь")
    @Description("Проверяет идемпотентность: повторный вызов с тем же пользователем не создаёт дубликатов")
    @Severity(SeverityLevel.CRITICAL)
    void shouldNotCreateDuplicateReferral() {
        // Given
        ReferralLinkDto referrerLink = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);
        String startParam = referrerLink.getStartParam();
        
        referralService.onFirstAuthentication(TEST_REFERRED_USER_ID, startParam, "{}");

        // When - повторный вызов
        referralService.onFirstAuthentication(TEST_REFERRED_USER_ID, startParam, "{}");

        // Then - только одна запись
        long count = referralRepository.count();
        assertEquals(1, count);
    }

    @Test
    @Story("Обработка первой аутентификации")
    @DisplayName("Должен блокировать self-referral")
    @Description("Проверяет, что пользователь не может пригласить сам себя")
    @Severity(SeverityLevel.CRITICAL)
    void shouldBlockSelfReferral() {
        // Given
        ReferralLinkDto link = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);

        // When - пытаемся пригласить сам себя
        referralService.onFirstAuthentication(TEST_REFERRER_USER_ID, link.getStartParam(), "{}");

        // Then - реферальная связь не создана
        Optional<ReferralEntity> referral = referralRepository.findByReferredUserId(TEST_REFERRER_USER_ID);
        assertFalse(referral.isPresent());
    }

    @Test
    @Story("Обработка первой генерации")
    @DisplayName("Должен начислить бонус рефереру за первую генерацию приглашённого")
    @Description("Проверяет, что при первой генерации приглашённого рефереру начисляется 50 ART")
    @Severity(SeverityLevel.CRITICAL)
    void shouldAwardReferrerOnFirstGeneration() {
        // Given - создаём реферальную связь
        ReferralLinkDto referrerLink = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);
        referralService.onFirstAuthentication(TEST_REFERRED_USER_ID, referrerLink.getStartParam(), "{}");

        // When - первая генерация приглашённого
        String taskId = "test-task-123";
        referralService.onFirstGeneration(TEST_REFERRED_USER_ID, taskId);

        // Then
        Optional<ReferralEntity> referral = referralRepository.findByReferredUserId(TEST_REFERRED_USER_ID);
        assertTrue(referral.isPresent());
        assertNotNull(referral.get().getReferrerFirstGenerationAwardedAt());
    }

    @Test
    @Story("Обработка первой генерации")
    @DisplayName("Не должен начислить бонус рефереру повторно")
    @Description("Проверяет идемпотентность начисления бонуса за первую генерацию")
    @Severity(SeverityLevel.CRITICAL)
    void shouldNotAwardReferrerTwice() {
        // Given
        ReferralLinkDto referrerLink = referralService.getOrCreateMyReferralLink(TEST_REFERRER_USER_ID);
        referralService.onFirstAuthentication(TEST_REFERRED_USER_ID, referrerLink.getStartParam(), "{}");
        
        referralService.onFirstGeneration(TEST_REFERRED_USER_ID, "task-1");

        // When - повторная генерация
        referralService.onFirstGeneration(TEST_REFERRED_USER_ID, "task-2");

        // Then - только одно начисление
        Optional<ReferralEntity> referral = referralRepository.findByReferredUserId(TEST_REFERRED_USER_ID);
        assertTrue(referral.isPresent());
        assertNotNull(referral.get().getReferrerFirstGenerationAwardedAt());
    }

    @Test
    @Story("Обработка первой генерации")
    @DisplayName("Не должен начислить бонус если нет реферальной связи")
    @Description("Проверяет, что бонус за генерацию начисляется только если пользователь был приглашён")
    @Severity(SeverityLevel.NORMAL)
    void shouldNotAwardIfNoReferral() {
        // Given - пользователь без реферальной связи
        Long nonReferredUserId = 999999L;

        // When
        referralService.onFirstGeneration(nonReferredUserId, "task-1");

        // Then - ничего не происходит, ошибок нет
        assertFalse(referralRepository.findByReferredUserId(nonReferredUserId).isPresent());
    }
}
