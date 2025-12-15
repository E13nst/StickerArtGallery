package com.example.sticker_art_gallery.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Валидатор TON-адресов
 * 
 * Проверяет:
 * - Длину (48 символов)
 * - Префикс (EQ, UQ, kQ)
 * - Base64url формат (символы A-Za-z0-9_-)
 * 
 * Checksum НЕ проверяется на этом этапе
 */
public class TonAddressValidator implements ConstraintValidator<ValidTonAddress, String> {

    private static final int TON_ADDRESS_LENGTH = 48;
    private static final String[] VALID_PREFIXES = {"EQ", "UQ", "kQ"};
    private static final String BASE64URL_PATTERN = "^[A-Za-z0-9_-]+$";

    @Override
    public void initialize(ValidTonAddress constraintAnnotation) {
        // Инициализация не требуется
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // null и пустые значения обрабатываются @NotBlank
        }

        // Проверка длины
        if (value.length() != TON_ADDRESS_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "TON-адрес должен содержать 48 символов, получено: " + value.length()
            ).addConstraintViolation();
            return false;
        }

        // Проверка префикса (первые 2 символа должны быть EQ, UQ или kQ)
        String prefix = value.substring(0, 2);
        boolean isValidPrefix = false;
        for (String validPrefix : VALID_PREFIXES) {
            if (validPrefix.equals(prefix)) {
                isValidPrefix = true;
                break;
            }
        }
        if (!isValidPrefix) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "TON-адрес должен начинаться с EQ, UQ или kQ, получено: " + prefix
            ).addConstraintViolation();
            return false;
        }

        // Проверка Base64url формата
        if (!value.matches(BASE64URL_PATTERN)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "TON-адрес содержит недопустимые символы. Разрешены только A-Z, a-z, 0-9, _ и -"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}

