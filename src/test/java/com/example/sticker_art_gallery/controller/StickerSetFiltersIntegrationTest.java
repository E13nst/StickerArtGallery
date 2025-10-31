package com.example.sticker_art_gallery.controller;

import com.example.sticker_art_gallery.model.telegram.StickerSet;
import com.example.sticker_art_gallery.model.telegram.StickerSetRepository;
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

		stickerSetRepository.deleteAll();

		// S1: official=false, authorId=null
		StickerSet s1 = new StickerSet();
		s1.setUserId(userId);
		s1.setTitle("S1");
		s1.setName("s1_by_StickerGalleryBot");
		s1.setIsPublic(true);
		s1.setIsBlocked(false);
		s1.setIsOfficial(false);
		s1.setAuthorId(null);
		stickerSetRepository.save(s1);

		// S2: official=true, authorId=111
		StickerSet s2 = new StickerSet();
		s2.setUserId(userId);
		s2.setTitle("S2");
		s2.setName("s2_by_StickerGalleryBot");
		s2.setIsPublic(true);
		s2.setIsBlocked(false);
		s2.setIsOfficial(true);
		s2.setAuthorId(111L);
		stickerSetRepository.save(s2);

		// S3: official=false, authorId=222
		StickerSet s3 = new StickerSet();
		s3.setUserId(userId);
		s3.setTitle("S3");
		s3.setName("s3_by_StickerGalleryBot");
		s3.setIsPublic(true);
		s3.setIsBlocked(false);
		s3.setIsOfficial(false);
		s3.setAuthorId(222L);
		stickerSetRepository.save(s3);
	}

	@AfterEach
	void tearDown() {
		stickerSetRepository.deleteAll();
	}

	@Test
	@Story("officialOnly")
	@DisplayName("officialOnly=true возвращает только официальные")
	void filterOfficialOnly() throws Exception {
		testSteps.getStickerSetsWithFilters(true, null, null, initData)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].isOfficial").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))));
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
}
