package com.example.sticker_art_gallery.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TonPayWebhookRequest {

    private String event;
    private String timestamp;
    private Data data;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String amount;
        private String rawAmount;
        private String senderAddr;
        private String recipientAddr;
        private String asset;
        private String assetTicker;
        private String status;
        private String reference;
        private String bodyBase64Hash;
        private String txHash;
        private String traceId;
        private String commentToSender;
        private String commentToRecipient;
        private String date;
        private Integer errorCode;
        private String errorMessage;

        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getRawAmount() { return rawAmount; }
        public void setRawAmount(String rawAmount) { this.rawAmount = rawAmount; }
        public String getSenderAddr() { return senderAddr; }
        public void setSenderAddr(String senderAddr) { this.senderAddr = senderAddr; }
        public String getRecipientAddr() { return recipientAddr; }
        public void setRecipientAddr(String recipientAddr) { this.recipientAddr = recipientAddr; }
        public String getAsset() { return asset; }
        public void setAsset(String asset) { this.asset = asset; }
        public String getAssetTicker() { return assetTicker; }
        public void setAssetTicker(String assetTicker) { this.assetTicker = assetTicker; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getBodyBase64Hash() { return bodyBase64Hash; }
        public void setBodyBase64Hash(String bodyBase64Hash) { this.bodyBase64Hash = bodyBase64Hash; }
        public String getTxHash() { return txHash; }
        public void setTxHash(String txHash) { this.txHash = txHash; }
        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
        public String getCommentToSender() { return commentToSender; }
        public void setCommentToSender(String commentToSender) { this.commentToSender = commentToSender; }
        public String getCommentToRecipient() { return commentToRecipient; }
        public void setCommentToRecipient(String commentToRecipient) { this.commentToRecipient = commentToRecipient; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Integer getErrorCode() { return errorCode; }
        public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
