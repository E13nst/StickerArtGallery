package com.example.sticker_art_gallery.model.payment;

/**
 * Статус намерения покупки (invoice intent)
 */
public enum InvoiceStatus {
    PENDING,      // Ожидает оплаты
    COMPLETED,    // Успешно оплачено
    FAILED,       // Ошибка оплаты
    EXPIRED,      // Истек срок действия invoice
    CANCELLED     // Отменено
}
