package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.dto.payment.TonPaymentCreateConflictResponse;

public class TonPaymentCreateConflictException extends RuntimeException {

    private final TonPaymentCreateConflictResponse response;

    public TonPaymentCreateConflictException(TonPaymentCreateConflictResponse response) {
        super(response != null ? response.getMessage() : null);
        this.response = response;
    }

    public TonPaymentCreateConflictResponse getResponse() {
        return response;
    }
}
