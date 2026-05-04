package com.example.sticker_art_gallery.service.payment;

import com.example.sticker_art_gallery.dto.payment.TonConnectMessageDto;

public class TonPayCreateTransferResponse {
    private TonConnectMessageDto message;
    private String reference;
    private String bodyBase64Hash;

    public TonConnectMessageDto getMessage() {
        return message;
    }

    public void setMessage(TonConnectMessageDto message) {
        this.message = message;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getBodyBase64Hash() {
        return bodyBase64Hash;
    }

    public void setBodyBase64Hash(String bodyBase64Hash) {
        this.bodyBase64Hash = bodyBase64Hash;
    }
}
