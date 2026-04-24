package com.example.sticker_art_gallery.service.telegram;

import com.example.sticker_art_gallery.service.telegram.chat.StickerGalleryFlowService;
import com.example.sticker_art_gallery.service.telegram.inline.InlineGenerationService;
import com.example.sticker_art_gallery.service.telegram.inline.InlineQueryService;
import com.example.sticker_art_gallery.service.telegram.payment.PaymentUpdateService;
import com.example.sticker_art_gallery.service.telegram.support.SupportBridgeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateRouterService")
class UpdateRouterServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TelegramUpdateDedupService dedupService;

    @Mock
    private StickerGalleryFlowService stickerGalleryFlowService;

    @Mock
    private SupportBridgeService supportBridgeService;

    @Mock
    private PaymentUpdateService paymentUpdateService;

    @Mock
    private InlineQueryService inlineQueryService;

    @Mock
    private InlineGenerationService inlineGenerationService;

    @Test
    @DisplayName("обрабатывает /start с payload из deep link")
    void routeUpdate_ShouldHandleStartWithPayload() throws Exception {
        UpdateRouterService service = new UpdateRouterService(
                dedupService,
                stickerGalleryFlowService,
                supportBridgeService,
                paymentUpdateService,
                inlineQueryService,
                inlineGenerationService
        );

        when(dedupService.isDuplicate(100L)).thenReturn(false);
        when(supportBridgeService.handleSupportReply(org.mockito.ArgumentMatchers.any())).thenReturn(false);

        JsonNode update = objectMapper.readTree("""
                {
                  "update_id": 100,
                  "message": {
                    "message_id": 1,
                    "chat": { "id": 555 },
                    "from": { "id": 777, "first_name": "Alex" },
                    "text": "/start inline_generation"
                  }
                }
                """);

        service.routeUpdate(update);

        verify(stickerGalleryFlowService).handleStart(update.path("message"));
        verify(stickerGalleryFlowService, never()).handleHelp(update.path("message"));
    }
}
