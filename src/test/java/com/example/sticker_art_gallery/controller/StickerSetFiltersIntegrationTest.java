package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.testdata.TestConstants;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.testdata.StickerSetTestBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@Tag("filters")
@Tag("fast")
@Epic("Стикерсеты")
@Feature("Фильтры списка: officialOnly, authorId, hasAuthorOnly")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StickerSetFiltersIntegrationTest {

	// Используем константы из TestConstants
	private static final String TEST_STICKERSET_S1 = TestConstants.TEST_STICKERSET_S1;
	private static final String TEST_STICKERSET_S2 = TestConstants.TEST_STICKERSET_S2;
	private static final String TEST_STICKERSET_S3 = TestConstants.TEST_STICKERSET_S3;
	private static final String TEST_STICKERSET_BLOCKED = TestConstants.TEST_STICKERSET_BLOCKED;

	@Autowired
	private StickerSetTestSteps testSteps;

	@Autowired
	private StickerSetRepository stickerSetRepository;

	private String initData;
	private final Long userId = TestDataBuilder.TEST_USER_ID;

	@BeforeAll
	void setUp() {
		// Создаем пользователя и профиль один раз для всех тестов
		testSteps.createTestUserAndProfile(userId);
		initData = testSteps.createValidInitData(userId);

		// Удаляем существующие тестовые стикерсеты (на случай предыдущих запусков)
		testSteps.cleanupTestStickerSets(
			TEST_STICKERSET_S1,
			TEST_STICKERSET_S2,
			TEST_STICKERSET_S3,
			TEST_STICKERSET_BLOCKED
		);

		// Создаем тестовые стикерсеты один раз для всех тестов используя StickerSetTestBuilder
		// S1: type=USER, authorId=null
		StickerSet s1 = StickerSetTestBuilder.builder()
				.withUserId(userId)
				.withTitle("S1")
				.withName(TEST_STICKERSET_S1)
				.build();
		stickerSetRepository.save(s1);

		// S2: type=OFFICIAL, authorId=111
		StickerSet s2 = StickerSetTestBuilder.builder()
				.withUserId(userId)
				.withTitle("S2")
				.withName(TEST_STICKERSET_S2)
				.asOfficial()
				.withAuthorId(TestConstants.TEST_AUTHOR_ID_111)
				.build();
		stickerSetRepository.save(s2);

		// S3: type=USER, authorId=222
		StickerSet s3 = StickerSetTestBuilder.builder()
				.withUserId(userId)
				.withTitle("S3")
				.withName(TEST_STICKERSET_S3)
				.withAuthorId(TestConstants.TEST_AUTHOR_ID_222)
				.build();
		stickerSetRepository.save(s3);

		// S4: Заблокированный стикерсет (не должен отображаться в галерее)
		StickerSet s4 = StickerSetTestBuilder.builder()
				.withUserId(userId)
				.withTitle("Blocked StickerSet")
				.withName(TEST_STICKERSET_BLOCKED)
				.asBlocked("Test block reason")
				.build();
		stickerSetRepository.save(s4);
	}

	@AfterAll
	void tearDown() {
		// Удаляем тестовые стикерсеты один раз после всех тестов
		testSteps.cleanupTestStickerSets(
			TEST_STICKERSET_S1,
			TEST_STICKERSET_S2,
			TEST_STICKERSET_S3,
			TEST_STICKERSET_BLOCKED
		);
	}

	@Test
	@Timeout(value = 3, unit = java.util.concurrent.TimeUnit.SECONDS)
	@Story("officialOnly")
	@DisplayName("officialOnly=true возвращает только официальные")
	void filterOfficialOnly() throws Exception {
		testSteps.getStickerSetsWithFilters(true, null, null, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].type").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("OFFICIAL"))))
		.andExpect(jsonPath("$.content[*].isOfficial").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true)))); // обратная совместимость
	}

	@Test
	@Story("authorId")
	@DisplayName("authorId фильтрует по автору")
	void filterByAuthorId() throws Exception {
		testSteps.getStickerSetsWithFilters(null, TestConstants.TEST_AUTHOR_ID_222, null, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
				.andExpect(jsonPath("$.content[0].authorId").value(TestConstants.TEST_AUTHOR_ID_222));
	}

	@Test
	@Story("hasAuthorOnly")
	@DisplayName("hasAuthorOnly=true возвращает только с authorId!=null")
	void filterHasAuthorOnly() throws Exception {
		testSteps.getStickerSetsWithFilters(null, null, true, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].authorId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));
	}

	@ParameterizedTest
	@CsvSource({
			"true, '', '', OFFICIAL",
			"'', 111, '', OFFICIAL",
			"'', 222, '', USER",
			"'', '', true, USER"
	})
	@Story("Комбинированные фильтры")
	@DisplayName("Фильтры: officialOnly={0}, authorId={1}, hasAuthorOnly={2} -> тип {3}")
	@Tag("filters")
	void filterWithCombinations(String officialOnlyStr, String authorIdStr, String hasAuthorOnlyStr, String expectedType) throws Exception {
		Boolean officialOnly = "true".equals(officialOnlyStr) ? Boolean.TRUE : (officialOnlyStr.isEmpty() ? null : Boolean.FALSE);
		Long authorId = authorIdStr.isEmpty() ? null : Long.parseLong(authorIdStr);
		Boolean hasAuthorOnly = "true".equals(hasAuthorOnlyStr) ? Boolean.TRUE : (hasAuthorOnlyStr.isEmpty() ? null : Boolean.FALSE);
		
		testSteps.getStickerSetsWithFilters(
				officialOnly,
				authorId,
				hasAuthorOnly,
				initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	@Tag("critical")
	@Story("Исключение заблокированных стикерсетов")
	@DisplayName("Заблокированные стикерсеты не отображаются в /api/stickersets")
	@Description("Заблокированные стикерсеты не должны отображаться в основном списке галереи, " +
				 "даже если они публичные")
	@Severity(SeverityLevel.CRITICAL)
	void blockedStickerSetsShouldNotBeVisibleInGallery() throws Exception {
		testSteps.getStickerSetsWithFilters(null, null, null, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(TEST_STICKERSET_BLOCKED))))
				.andExpect(jsonPath("$.content[*].state", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("BLOCKED"))))
				.andExpect(jsonPath("$.content[*].state").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))));
	}
}
