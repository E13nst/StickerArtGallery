package com.example.sticker_art_gallery.dto.payment;

public enum TonPaymentCreateConflictCode {
    INTENT_ALREADY_EXISTS,
    SENDER_ADDRESS_MISMATCH,
    TON_PAYMENTS_DISABLED,
    MERCHANT_WALLET_NOT_CONFIGURED,
    UNKNOWN_CONFLICT
}
