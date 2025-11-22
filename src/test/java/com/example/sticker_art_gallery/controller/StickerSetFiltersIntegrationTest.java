package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
import com.example.sticker_art_gallery.model.telegram.StickerSetState;
import com.example.sticker_art_gallery.model.telegram.StickerSetVisibility;
import com.example.sticker_art_gallery.model.telegram.StickerSetType;
import com.example.sticker_art_gallery.testdata.TestDataBuilder;
import com.example.sticker_art_gallery.teststeps.StickerSetTestSteps;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@Epic("Стикерсеты")
@Feature("Фильтры списка: officialOnly, authorId, hasAuthorOnly")
class StickerSetFiltersIntegrationTest {

	@Autowired
	private StickerSetTestSteps testSteps;

	@Autowired
	private StickerSetRepository stickerSetRepository;

	private String initData;
	private final Long userId = TestDataBuilder.TEST_USER_ID;

	@BeforeEach
	void setUp() {
		testSteps.createTestUserAndProfile(userId);
		initData = testSteps.createValidInitData(userId);

		// Удаляем существующие тестовые стикерсеты
		stickerSetRepository.findByNameIgnoreCase("s1_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);
		stickerSetRepository.findByNameIgnoreCase("s2_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);
		stickerSetRepository.findByNameIgnoreCase("s3_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);
		stickerSetRepository.findByNameIgnoreCase("blocked_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);

		// S1: type=USER, authorId=null
		StickerSet s1 = new StickerSet();
		s1.setUserId(userId);
		s1.setTitle("S1");
		s1.setName("s1_by_StickerGalleryBot");
		s1.setState(StickerSetState.ACTIVE);
		s1.setVisibility(StickerSetVisibility.PUBLIC);
		s1.setType(StickerSetType.USER);
		s1.setAuthorId(null);
		stickerSetRepository.save(s1);

		// S2: type=OFFICIAL, authorId=111
		StickerSet s2 = new StickerSet();
		s2.setUserId(userId);
		s2.setTitle("S2");
		s2.setName("s2_by_StickerGalleryBot");
		s2.setState(StickerSetState.ACTIVE);
		s2.setVisibility(StickerSetVisibility.PUBLIC);
		s2.setType(StickerSetType.OFFICIAL);
		s2.setAuthorId(111L);
		stickerSetRepository.save(s2);

		// S3: type=USER, authorId=222
		StickerSet s3 = new StickerSet();
		s3.setUserId(userId);
		s3.setTitle("S3");
		s3.setName("s3_by_StickerGalleryBot");
		s3.setState(StickerSetState.ACTIVE);
		s3.setVisibility(StickerSetVisibility.PUBLIC);
		s3.setType(StickerSetType.USER);
		s3.setAuthorId(222L);
		stickerSetRepository.save(s3);

		// S4: Заблокированный стикерсет (не должен отображаться в галерее)
		StickerSet s4 = new StickerSet();
		s4.setUserId(userId);
		s4.setTitle("Blocked StickerSet");
		s4.setName("blocked_by_StickerGalleryBot");
		s4.setState(StickerSetState.BLOCKED);
		s4.setVisibility(StickerSetVisibility.PUBLIC);
		s4.setType(StickerSetType.USER);
		s4.setAuthorId(null);
		s4.setBlockReason("Test block reason");
		stickerSetRepository.save(s4);
	}

	@AfterEach
	void tearDown() {
		// Удаляем тестовые стикерсеты по именам
		stickerSetRepository.findByNameIgnoreCase("s1_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);
		stickerSetRepository.findByNameIgnoreCase("s2_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);
		stickerSetRepository.findByNameIgnoreCase("s3_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);
		stickerSetRepository.findByNameIgnoreCase("blocked_by_StickerGalleryBot").ifPresent(stickerSetRepository::delete);
	}

	@Test
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
		testSteps.getStickerSetsWithFilters(null, 222L, null, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()", org.hamcrest.Matchers.is(1)))
				.andExpect(jsonPath("$.content[0].authorId").value(222L));
	}

	@Test
	@Story("hasAuthorOnly")
	@DisplayName("hasAuthorOnly=true возвращает только с authorId!=null")
	void filterHasAuthorOnly() throws Exception {
		testSteps.getStickerSetsWithFilters(null, null, true, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].authorId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));
	}

	@Test
	@Story("Исключение заблокированных стикерсетов")
	@DisplayName("Заблокированные стикерсеты не отображаются в /api/stickersets")
	@Description("Заблокированные стикерсеты не должны отображаться в основном списке галереи, " +
				 "даже если они публичные")
	@Severity(SeverityLevel.CRITICAL)
	void blockedStickerSetsShouldNotBeVisibleInGallery() throws Exception {
		testSteps.getStickerSetsWithFilters(null, null, null, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("blocked_by_StickerGalleryBot"))))
				.andExpect(jsonPath("$.content[*].state", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("BLOCKED"))))
				.andExpect(jsonPath("$.content[*].state").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))));
	}
}
