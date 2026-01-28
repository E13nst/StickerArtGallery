"""
Handlers для обработки платежей Telegram Stars
Интеграция с Java API для валидации и обработки платежей
"""
import asyncio
import logging
import os
from typing import Optional
import httpx
from telegram import Update
from telegram.ext import ContextTypes
from telegram.error import TelegramError

logger = logging.getLogger(__name__)

# Конфигурация из переменных окружения
JAVA_API_URL = os.getenv("JAVA_API_URL", "http://localhost:8080")
SERVICE_TOKEN = os.getenv("SERVICE_TOKEN", "")

if not SERVICE_TOKEN:
    logger.warning("⚠️ SERVICE_TOKEN не установлен. Internal API вызовы не будут работать.")


async def pre_checkout_query_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """
    Обработчик pre_checkout_query - валидация платежа перед оплатой
    Вызывается когда пользователь нажимает "Pay" в invoice
    """
    query = update.pre_checkout_query
    if not query:
        logger.warning("⚠️ pre_checkout_query пуст")
        return

    user_id = query.from_user.id
    invoice_payload = query.invoice_payload
    total_amount = query.total_amount

    logger.info(f"🔍 Валидация платежа: userId={user_id}, payload={invoice_payload}, amount={total_amount}")

    try:
        # Вызываем Java API для валидации
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                f"{JAVA_API_URL}/api/internal/stars/validate-payment",
                json={
                    "invoicePayload": invoice_payload,
                    "userId": user_id,
                    "totalAmount": total_amount
                },
                headers={
                    "X-Service-Token": SERVICE_TOKEN,
                    "Content-Type": "application/json"
                }
            )

            if response.status_code == 200:
                result = response.json()
                is_valid = result.get("valid", False)
                error_message = result.get("errorMessage")

                if is_valid:
                    logger.info(f"✅ Платеж валиден: userId={user_id}, payload={invoice_payload}")
                    await query.answer(ok=True)
                else:
                    logger.warn(f"❌ Платеж невалиден: {error_message}")
                    await query.answer(ok=False, error_message=error_message or "Invalid payment")
            else:
                logger.error(f"❌ Ошибка валидации платежа: HTTP {response.status_code}")
                await query.answer(ok=False, error_message="Validation error")

    except httpx.TimeoutException:
        logger.error("❌ Timeout при валидации платежа")
        await query.answer(ok=False, error_message="Timeout")
    except Exception as e:
        logger.error(f"❌ Ошибка при валидации платежа: {e}", exc_info=True)
        await query.answer(ok=False, error_message="Internal error")


async def successful_payment_handler(update: Update, context: ContextTypes.DEFAULT_TYPE) -> None:
    """
    Обработчик successful_payment - обработка успешного платежа
    Вызывается после успешной оплаты invoice
    """
    message = update.message
    if not message or not message.successful_payment:
        logger.warning("⚠️ successful_payment пуст")
        return

    payment = message.successful_payment
    user_id = message.from_user.id
    invoice_payload = payment.invoice_payload
    telegram_payment_id = payment.telegram_payment_charge_id
    telegram_charge_id = payment.telegram_payment_charge_id  # В Telegram это одно и то же поле

    logger.info(f"💰 Обработка успешного платежа: userId={user_id}, paymentId={telegram_payment_id}, "
                f"chargeId={telegram_charge_id}, payload={invoice_payload}")

    try:
        # Вызываем Java API для обработки платежа
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{JAVA_API_URL}/api/internal/stars/process-payment",
                json={
                    "telegramPaymentId": telegram_payment_id,
                    "telegramChargeId": telegram_charge_id,
                    "invoicePayload": invoice_payload,
                    "userId": user_id
                },
                headers={
                    "X-Service-Token": SERVICE_TOKEN,
                    "Content-Type": "application/json"
                }
            )

            if response.status_code == 200:
                result = response.json()
                success = result.get("success", False)
                purchase_id = result.get("purchaseId")
                art_credited = result.get("artCredited")

                if success:
                    logger.info(f"✅ Платеж успешно обработан: purchaseId={purchase_id}, artCredited={art_credited}")
                    
                    # Отправляем подтверждение пользователю
                    confirmation_text = (
                        f"✅ Платеж успешно обработан!\n\n"
                        f"💰 Начислено ART: {art_credited}\n"
                        f"📦 ID покупки: {purchase_id}"
                    )
                    
                    try:
                        await message.reply_text(confirmation_text)
                    except TelegramError as e:
                        logger.warning(f"⚠️ Не удалось отправить подтверждение: {e}")
                else:
                    error_message = result.get("errorMessage", "Unknown error")
                    logger.error(f"❌ Ошибка обработки платежа: {error_message}")
                    
                    # Отправляем сообщение об ошибке
                    try:
                        await message.reply_text(
                            f"⚠️ Произошла ошибка при обработке платежа.\n"
                            f"Пожалуйста, обратитесь в поддержку.\n"
                            f"Payment ID: {telegram_payment_id}"
                        )
                    except TelegramError as e:
                        logger.warning(f"⚠️ Не удалось отправить сообщение об ошибке: {e}")
            else:
                logger.error(f"❌ Ошибка обработки платежа: HTTP {response.status_code}")
                try:
                    await message.reply_text(
                        f"⚠️ Ошибка при обработке платежа.\n"
                        f"Пожалуйста, обратитесь в поддержку.\n"
                        f"Payment ID: {telegram_payment_id}"
                    )
                except TelegramError as e:
                    logger.warning(f"⚠️ Не удалось отправить сообщение об ошибке: {e}")

    except httpx.TimeoutException:
        logger.error("❌ Timeout при обработке платежа")
        try:
            await message.reply_text(
                "⚠️ Таймаут при обработке платежа.\n"
                "Платеж будет обработан позже. Пожалуйста, обратитесь в поддержку."
            )
        except TelegramError as e:
            logger.warning(f"⚠️ Не удалось отправить сообщение: {e}")
    except Exception as e:
        logger.error(f"❌ Ошибка при обработке платежа: {e}", exc_info=True)
        try:
            await message.reply_text(
                "⚠️ Произошла ошибка при обработке платежа.\n"
                "Пожалуйста, обратитесь в поддержку."
            )
        except TelegramError as e:
            logger.warning(f"⚠️ Не удалось отправить сообщение: {e}")
