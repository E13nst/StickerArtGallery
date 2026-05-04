package com.example.sticker_art_gallery.service.payment;

public interface TonPayAdapterClient {
    TonPayCreateTransferResponse createTransfer(TonPayCreateTransferRequest request);
}
