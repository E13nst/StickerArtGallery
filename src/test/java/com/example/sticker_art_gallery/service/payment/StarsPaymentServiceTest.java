package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.payment.CreateInvoiceRequest;
import com.example.sticker_art_gallery.dto.payment.CreateInvoiceResponse;
import com.example.sticker_art_gallery.dto.payment.ProcessPaymentResponse;
import com.example.sticker_art_gallery.dto.payment.TelegramWebhookRequest;
import com.example.sticker_art_gallery.model.payment.InvoiceStatus;
import com.example.sticker_art_gallery.model.payment.StarsInvoiceIntentEntity;
import com.example.sticker_art_gallery.model.payment.StarsPackageEntity;
import com.example.sticker_art_gallery.model.payment.StarsPurchaseEntity;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.StarsInvoiceIntentRepository;
import com.example.sticker_art_gallery.repository.StarsPackageRepository;
import com.example.sticker_art_gallery.repository.StarsPurchaseRepository;
import com.example.sticker_art_gallery.repository.UserRepository;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StarsPaymentServiceTest {

    @Mock
    private StarsPackageRepository starsPackageRepository;

    @Mock
    private StarsInvoiceIntentRepository starsInvoiceIntentRepository;

    @Mock
    private StarsPurchaseRepository starsPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArtRewardService artRewardService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private StarsPaymentService starsPaymentService;

    private StarsPackageEntity activePackage;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        activePackage = new StarsPackageEntity();
        activePackage.setId(10L);
        activePackage.setCode("BASIC");
        activePackage.setName("Basic Pack");
        activePackage.setDescription("250 ART баллов");
        activePackage.setStarsPrice(100);
        activePackage.setArtAmount(250L);
        activePackage.setIsEnabled(true);

        user = new UserEntity();
        user.setId(77L);
        user.setUsername("user77");
    }

    @Test
    @DisplayName("createInvoice: должен создать intent и вернуть invoice URL")
    void createInvoice_shouldCreateIntentAndReturnUrl() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setPackageCode("BASIC");

        when(starsPackageRepository.findByCode("BASIC")).thenReturn(Optional.of(activePackage));
        when(userRepository.findById(77L)).thenReturn(Optional.of(user));
        AppConfig.StickerBot stickerBotConfig = new AppConfig.StickerBot();
        stickerBotConfig.setApiUrl("https://stickerbot.example");
        when(appConfig.getStickerbot()).thenReturn(stickerBotConfig);
        when(appConfig.getUrl()).thenReturn("https://backend.example");
        when(starsInvoiceIntentRepository.save(any(StarsInvoiceIntentEntity.class)))
                .thenAnswer(invocation -> {
                    StarsInvoiceIntentEntity intent = invocation.getArgument(0);
                    if (intent.getId() == null) {
                        intent.setId(123L);
                    }
                    return intent;
                });
        Map<String, Object> externalResponse = new HashMap<>();
        externalResponse.put("ok", true);
        externalResponse.put("invoice_link", "https://t.me/invoice/test123");
        when(restTemplate.exchange(
                eq("https://stickerbot.example/api/payments/create-invoice"),
                eq(HttpMethod.POST),
                any(),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(externalResponse));

        CreateInvoiceResponse response = starsPaymentService.createInvoice(77L, request, "initData=test");

        assertNotNull(response);
        assertEquals("https://t.me/invoice/test123", response.getInvoiceUrl());
        assertEquals(123L, response.getIntentId());
        assertNotNull(response.getStarsPackage());
        assertEquals("BASIC", response.getStarsPackage().getCode());

        verify(starsInvoiceIntentRepository, times(2)).save(any(StarsInvoiceIntentEntity.class));
        verify(restTemplate).exchange(
                eq("https://stickerbot.example/api/payments/create-invoice"),
                eq(HttpMethod.POST),
                any(),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("createInvoice: должен бросать NotFound для несуществующего пакета")
    void createInvoice_shouldThrowWhenPackageNotFound() {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setPackageCode("MISSING");

        when(starsPackageRepository.findByCode("MISSING")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> starsPaymentService.createInvoice(77L, request, "initData=test"));
    }

    @Test
    @DisplayName("createInvoice: должен бросать BadRequest для отключенного пакета")
    void createInvoice_shouldThrowWhenPackageDisabled() {
        StarsPackageEntity disabledPackage = new StarsPackageEntity();
        disabledPackage.setId(11L);
        disabledPackage.setCode("DISABLED");
        disabledPackage.setIsEnabled(false);

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setPackageCode("DISABLED");

        when(starsPackageRepository.findByCode("DISABLED")).thenReturn(Optional.of(disabledPackage));

        assertThrows(IllegalArgumentException.class, () -> starsPaymentService.createInvoice(77L, request, "initData=test"));
    }

    @Test
    @DisplayName("processWebhookPayment: должен связать purchase с intent и пометить intent COMPLETED")
    void processWebhookPayment_shouldLinkIntentAndCompleteIt() {
        TelegramWebhookRequest request = new TelegramWebhookRequest();
        request.setEvent("telegram_stars_payment_succeeded");
        request.setUserId(77L);
        request.setAmountStars(100);
        request.setCurrency("XTR");
        request.setTelegramChargeId("charge_intent_001");
        request.setInvoicePayload("{\"package_id\":10,\"nonce\":\"abc\"}");
        request.setTimestamp(System.currentTimeMillis() / 1000);

        StarsInvoiceIntentEntity intent = new StarsInvoiceIntentEntity();
        intent.setId(500L);
        intent.setUser(user);
        intent.setStarsPackage(activePackage);
        intent.setInvoicePayload(request.getInvoicePayload());
        intent.setStatus(InvoiceStatus.PENDING);

        ArtTransactionEntity artTransaction = new ArtTransactionEntity();
        artTransaction.setId(900L);

        when(starsPurchaseRepository.findByTelegramChargeId("charge_intent_001")).thenReturn(Optional.empty());
        when(starsInvoiceIntentRepository.findByInvoicePayload(request.getInvoicePayload())).thenReturn(Optional.of(intent));
        when(userRepository.findById(77L)).thenReturn(Optional.of(user));
        when(artRewardService.award(eq(77L), eq("PURCHASE_STARS"), eq(250L), any(String.class), eq("charge_intent_001"), eq(null)))
                .thenReturn(artTransaction);
        when(starsPurchaseRepository.save(any(StarsPurchaseEntity.class)))
                .thenAnswer(invocation -> {
                    StarsPurchaseEntity saved = invocation.getArgument(0);
                    saved.setId(700L);
                    return saved;
                });
        when(starsInvoiceIntentRepository.save(any(StarsInvoiceIntentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentResponse response = starsPaymentService.processWebhookPayment(request);

        assertTrue(response.getSuccess());
        assertEquals(700L, response.getPurchaseId());
        assertEquals(250L, response.getArtCredited());
        assertEquals(InvoiceStatus.COMPLETED, intent.getStatus());

        ArgumentCaptor<StarsPurchaseEntity> purchaseCaptor = ArgumentCaptor.forClass(StarsPurchaseEntity.class);
        verify(starsPurchaseRepository).save(purchaseCaptor.capture());
        StarsPurchaseEntity savedPurchase = purchaseCaptor.getValue();
        assertEquals(500L, savedPurchase.getInvoiceIntentId());
        assertNotNull(savedPurchase.getInvoiceIntent());
        assertEquals(10L, savedPurchase.getPackageId());
    }

    @Test
    @DisplayName("processWebhookPayment: должен работать без intent (legacy fallback)")
    void processWebhookPayment_shouldFallbackWithoutIntent() {
        TelegramWebhookRequest request = new TelegramWebhookRequest();
        request.setEvent("telegram_stars_payment_succeeded");
        request.setUserId(77L);
        request.setAmountStars(100);
        request.setCurrency("XTR");
        request.setTelegramChargeId("charge_legacy_001");
        request.setInvoicePayload("{\"package_id\":10}");
        request.setTimestamp(System.currentTimeMillis() / 1000);

        ArtTransactionEntity artTransaction = new ArtTransactionEntity();
        artTransaction.setId(901L);

        when(starsPurchaseRepository.findByTelegramChargeId("charge_legacy_001")).thenReturn(Optional.empty());
        when(starsInvoiceIntentRepository.findByInvoicePayload(request.getInvoicePayload())).thenReturn(Optional.empty());
        when(starsPackageRepository.findById(10L)).thenReturn(Optional.of(activePackage));
        when(userRepository.findById(77L)).thenReturn(Optional.of(user));
        when(artRewardService.award(eq(77L), eq("PURCHASE_STARS"), eq(250L), any(String.class), eq("charge_legacy_001"), eq(null)))
                .thenReturn(artTransaction);
        when(starsPurchaseRepository.save(any(StarsPurchaseEntity.class)))
                .thenAnswer(invocation -> {
                    StarsPurchaseEntity saved = invocation.getArgument(0);
                    saved.setId(701L);
                    return saved;
                });

        ProcessPaymentResponse response = starsPaymentService.processWebhookPayment(request);

        assertTrue(response.getSuccess());
        assertEquals(701L, response.getPurchaseId());
        assertEquals(250L, response.getArtCredited());

        ArgumentCaptor<StarsPurchaseEntity> purchaseCaptor = ArgumentCaptor.forClass(StarsPurchaseEntity.class);
        verify(starsPurchaseRepository).save(purchaseCaptor.capture());
        StarsPurchaseEntity savedPurchase = purchaseCaptor.getValue();
        assertNull(savedPurchase.getInvoiceIntent());
        assertNull(savedPurchase.getInvoiceIntentId());
        verify(starsInvoiceIntentRepository, never()).save(any(StarsInvoiceIntentEntity.class));
    }
}
