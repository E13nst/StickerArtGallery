package com.example.sticker_art_gallery.service.payment;

class TonPayCreateTransferRequest {
    private Long intentId;
    private Long amountNano;
    private String asset;
    private String recipientAddr;
    private String senderAddr;
    private String commentToSender;
    private String commentToRecipient;

    TonPayCreateTransferRequest(Long intentId,
                                Long amountNano,
                                String asset,
                                String recipientAddr,
                                String senderAddr,
                                String commentToSender,
                                String commentToRecipient) {
        this.intentId = intentId;
        this.amountNano = amountNano;
        this.asset = asset;
        this.recipientAddr = recipientAddr;
        this.senderAddr = senderAddr;
        this.commentToSender = commentToSender;
        this.commentToRecipient = commentToRecipient;
    }

    public Long getIntentId() { return intentId; }
    public void setIntentId(Long intentId) { this.intentId = intentId; }
    public Long getAmountNano() { return amountNano; }
    public void setAmountNano(Long amountNano) { this.amountNano = amountNano; }
    public String getAsset() { return asset; }
    public void setAsset(String asset) { this.asset = asset; }
    public String getRecipientAddr() { return recipientAddr; }
    public void setRecipientAddr(String recipientAddr) { this.recipientAddr = recipientAddr; }
    public String getSenderAddr() { return senderAddr; }
    public void setSenderAddr(String senderAddr) { this.senderAddr = senderAddr; }
    public String getCommentToSender() { return commentToSender; }
    public void setCommentToSender(String commentToSender) { this.commentToSender = commentToSender; }
    public String getCommentToRecipient() { return commentToRecipient; }
    public void setCommentToRecipient(String commentToRecipient) { this.commentToRecipient = commentToRecipient; }
}
