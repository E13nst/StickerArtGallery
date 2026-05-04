package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.config.AppConfig;
import com.example.sticker_art_gallery.dto.payment.*;
import com.example.sticker_art_gallery.model.payment.*;
import com.example.sticker_art_gallery.model.profile.ArtTransactionEntity;
import com.example.sticker_art_gallery.model.user.UserEntity;
import com.example.sticker_art_gallery.repository.*;
import com.example.sticker_art_gallery.service.profile.ArtRewardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TonArtPaymentServiceTest {

    private static final String SENDER = "UQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String RECIPIENT = "EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @Mock
    private StarsPackageRepository starsPackageRepository;
    @Mock
    private TonPaymentIntentRepository intentRepository;
    @Mock
    private TonPurchaseRepository purchaseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtRewardService artRewardService;
    @Mock
    private TonPayAdapterClient tonPayAdapterClient;
    @Mock
    private TonPaymentSettingsService tonPaymentSettingsService;

    private TonArtPaymentService service;
    private StarsPackageEntity activePackage;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        AppConfig appConfig = new AppConfig();
        AppConfig.TonPay tonPay = new AppConfig.TonPay();
        tonPay.setMerchantWalletAddress(RECIPIENT);
        tonPay.setAdapterUrl("http://adapter");
        tonPay.setChain("testnet");
        appConfig.setTonpay(tonPay);

        service = new TonArtPaymentService(
                starsPackageRepository,
                intentRepository,
                purchaseRepository,
                userRepository,
                artRewardService,
                tonPayAdapterClient,
                tonPaymentSettingsService,
                appConfig,
                new ObjectMapper()
        );

        activePackage = new StarsPackageEntity();
        activePackage.setId(10L);
        activePackage.setCode("BASIC");
        activePackage.setName("Basic Pack");
        activePackage.setStarsPrice(100);
        activePackage.setTonPriceNano(250_000_000L);
        activePackage.setArtAmount(250L);
        activePackage.setIsEnabled(true);

        user = new UserEntity();
        user.setId(77L);
    }

    @Test
    @DisplayName("createPayment: замораживает пакет и сохраняет TON Pay tracking до отправки кошелька")
    void createPayment_shouldPersistIntentAndReturnTonConnectMessage() {
        CreateTonPaymentRequest request = new CreateTonPaymentRequest();
        request.setPackageCode("BASIC");
        request.setSenderAddress(SENDER);

        when(starsPackageRepository.findByCode("BASIC")).thenReturn(Optional.of(activePackage));
        when(userRepository.findById(77L)).thenReturn(Optional.of(user));
        when(tonPaymentSettingsService.resolveMerchantWalletAddress()).thenReturn(RECIPIENT);
        when(intentRepository.save(any(TonPaymentIntentEntity.class))).thenAnswer(invocation -> {
            TonPaymentIntentEntity intent = invocation.getArgument(0);
            if (intent.getId() == null) {
                intent.setId(500L);
            }
            return intent;
        });
        when(tonPayAdapterClient.createTransfer(any(TonPayCreateTransferRequest.class))).thenReturn(transferResponse());

        CreateTonPaymentResponse response = service.createPayment(77L, request);

        assertEquals(500L, response.getIntentId());
        assertEquals(TonPaymentStatus.READY, response.getStatus());
        assertEquals("ref-500", response.getReference());
        assertEquals(250_000_000L, response.getAmountNano());
        assertNotNull(response.getMessage());
        assertEquals(RECIPIENT, response.getMessage().getAddress());

        ArgumentCaptor<TonPaymentIntentEntity> intentCaptor = ArgumentCaptor.forClass(TonPaymentIntentEntity.class);
        verify(intentRepository, times(2)).save(intentCaptor.capture());
        TonPaymentIntentEntity saved = intentCaptor.getAllValues().get(1);
        assertEquals("BASIC", saved.getPackageCode());
        assertEquals(SENDER, saved.getSenderAddress());
        assertEquals(RECIPIENT, saved.getRecipientAddress());
        assertEquals("hash-500", saved.getBodyBase64Hash());
    }

    @Test
    @DisplayName("processWebhook: успешный TON Pay webhook начисляет ART и создаёт purchase")
    void processWebhook_shouldAwardArtAndCreatePurchase() {
        TonPaymentIntentEntity intent = readyIntent();
        TonPayWebhookRequest webhook = successWebhook(250_000_000L);
        ArtTransactionEntity artTx = new ArtTransactionEntity();
        artTx.setId(900L);

        when(intentRepository.findWithLockByReference("ref-500")).thenReturn(Optional.of(intent));
        when(purchaseRepository.findByReference("ref-500")).thenReturn(Optional.empty());
        when(purchaseRepository.existsByTxHash("tx-500")).thenReturn(false);
        when(artRewardService.award(eq(77L), eq(ArtRewardService.RULE_PURCHASE_TON), eq(250L), anyString(), eq("tonpay:ref-500"), eq(null)))
                .thenReturn(artTx);
        when(purchaseRepository.save(any(TonPurchaseEntity.class))).thenAnswer(invocation -> {
            TonPurchaseEntity purchase = invocation.getArgument(0);
            purchase.setId(700L);
            return purchase;
        });
        when(intentRepository.save(any(TonPaymentIntentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProcessPaymentResponse response = service.processWebhook(webhook);

        assertTrue(response.getSuccess());
        assertEquals(700L, response.getPurchaseId());
        assertEquals(250L, response.getArtCredited());
        assertEquals(TonPaymentStatus.COMPLETED, intent.getStatus());

        ArgumentCaptor<TonPurchaseEntity> purchaseCaptor = ArgumentCaptor.forClass(TonPurchaseEntity.class);
        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertEquals("tx-500", purchaseCaptor.getValue().getTxHash());
        assertEquals(250_000_000L, purchaseCaptor.getValue().getTonPaidNano());
    }

    @Test
    @DisplayName("processWebhook: mismatch суммы помечает intent FAILED и не начисляет ART")
    void processWebhook_shouldRejectAmountMismatch() {
        TonPaymentIntentEntity intent = readyIntent();
        TonPayWebhookRequest webhook = successWebhook(1L);

        when(intentRepository.findWithLockByReference("ref-500")).thenReturn(Optional.of(intent));
        when(purchaseRepository.findByReference("ref-500")).thenReturn(Optional.empty());
        when(intentRepository.save(any(TonPaymentIntentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(IllegalArgumentException.class, () -> service.processWebhook(webhook));

        assertEquals(TonPaymentStatus.FAILED, intent.getStatus());
        assertTrue(intent.getFailureReason().contains("amount mismatch"));
        verify(artRewardService, never()).award(anyLong(), anyString(), anyLong(), any(), any(), any());
        verify(purchaseRepository, never()).save(any());
    }

    private TonPayCreateTransferResponse transferResponse() {
        TonConnectMessageDto message = new TonConnectMessageDto();
        message.setAddress(RECIPIENT);
        message.setAmount("250000000");
        message.setPayload("payload");

        TonPayCreateTransferResponse response = new TonPayCreateTransferResponse();
        response.setMessage(message);
        response.setReference("ref-500");
        response.setBodyBase64Hash("hash-500");
        return response;
    }

    private TonPaymentIntentEntity readyIntent() {
        TonPaymentIntentEntity intent = new TonPaymentIntentEntity();
        intent.setId(500L);
        intent.setUser(user);
        intent.setStarsPackage(activePackage);
        intent.setPackageCode(activePackage.getCode());
        intent.setStatus(TonPaymentStatus.READY);
        intent.setExpectedAmountNano(250_000_000L);
        intent.setAsset("TON");
        intent.setArtAmount(250L);
        intent.setSenderAddress(SENDER);
        intent.setRecipientAddress(RECIPIENT);
        intent.setReference("ref-500");
        intent.setBodyBase64Hash("hash-500");
        return intent;
    }

    private TonPayWebhookRequest successWebhook(long rawAmount) {
        TonPayWebhookRequest request = new TonPayWebhookRequest();
        request.setEvent("transfer.completed");
        request.setTimestamp("2026-05-04T12:00:00Z");

        TonPayWebhookRequest.Data data = new TonPayWebhookRequest.Data();
        data.setStatus("success");
        data.setReference("ref-500");
        data.setBodyBase64Hash("hash-500");
        data.setRawAmount(String.valueOf(rawAmount));
        data.setAmount("0.25");
        data.setAsset("TON");
        data.setSenderAddr(SENDER);
        data.setRecipientAddr(RECIPIENT);
        data.setTxHash("tx-500");
        data.setTraceId("trace-500");
        request.setData(data);
        return request;
    }
}
