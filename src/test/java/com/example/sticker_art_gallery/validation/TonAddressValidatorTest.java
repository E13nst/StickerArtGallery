package com.example.sticker_art_gallery.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для TonAddressValidator
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты TonAddressValidator")
class TonAddressValidatorTest {

    private TonAddressValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new TonAddressValidator();
        validator.initialize(null);

        // Используем lenient для моков, которые могут не вызываться в некоторых тестах
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("isValid должен вернуть true для валидного TON-адреса с префиксом EQ")
    void isValid_shouldReturnTrueForValidEQAddress() {
        // Arrange - ровно 48 символов (EQ + 46 символов)
        String validAddress = "EQDummyWalletAddress1234567890123456789012345678"; // 48 символов

        // Act
        boolean result = validator.isValid(validAddress, context);

        // Assert
        assertThat(result).isTrue();
        assertThat(validAddress.length()).isEqualTo(48);
    }

    @Test
    @DisplayName("isValid должен вернуть true для валидного TON-адреса с префиксом UQ")
    void isValid_shouldReturnTrueForValidUQAddress() {
        // Arrange - ровно 48 символов (UQ + 46 символов)
        String validAddress = "UQDummyWalletAddress1234567890123456789012345678"; // 48 символов

        // Act
        boolean result = validator.isValid(validAddress, context);

        // Assert
        assertThat(result).isTrue();
        assertThat(validAddress.length()).isEqualTo(48);
    }

    @Test
    @DisplayName("isValid должен вернуть true для валидного TON-адреса с префиксом kQ")
    void isValid_shouldReturnTrueForValidKQAddress() {
        // Arrange - ровно 48 символов (kQ + 46 символов)
        String validAddress = "kQDummyWalletAddress1234567890123456789012345678"; // 48 символов

        // Act
        boolean result = validator.isValid(validAddress, context);

        // Assert
        assertThat(result).isTrue();
        assertThat(validAddress.length()).isEqualTo(48);
    }

    @Test
    @DisplayName("isValid должен вернуть false для адреса с неверной длиной")
    void isValid_shouldReturnFalseForInvalidLength() {
        // Arrange
        String shortAddress = "EQShort"; // Меньше 48 символов
        String longAddress = "EQDummyWalletAddress123456789012345678901234567890"; // Больше 48 символов

        // Act & Assert
        assertThat(validator.isValid(shortAddress, context)).isFalse();
        assertThat(validator.isValid(longAddress, context)).isFalse();

        verify(context, times(2)).disableDefaultConstraintViolation();
        verify(context, times(2)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("isValid должен вернуть false для адреса с неверным префиксом")
    void isValid_shouldReturnFalseForInvalidPrefix() {
        // Arrange
        String invalidPrefixAddress = "XXDummyWalletAddress123456789012345678901234"; // Неверный префикс

        // Act
        boolean result = validator.isValid(invalidPrefixAddress, context);

        // Assert
        assertThat(result).isFalse();
        verify(context, times(1)).disableDefaultConstraintViolation();
        verify(context, times(1)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("isValid должен вернуть false для адреса с недопустимыми символами")
    void isValid_shouldReturnFalseForInvalidCharacters() {
        // Arrange
        String invalidCharsAddress = "EQDummyWalletAddress1234567890123456789012@#"; // Недопустимые символы

        // Act
        boolean result = validator.isValid(invalidCharsAddress, context);

        // Assert
        assertThat(result).isFalse();
        verify(context, times(1)).disableDefaultConstraintViolation();
        verify(context, times(1)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("isValid должен вернуть true для null (обрабатывается @NotBlank)")
    void isValid_shouldReturnTrueForNull() {
        // Act
        boolean result = validator.isValid(null, context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValid должен вернуть true для пустой строки (обрабатывается @NotBlank)")
    void isValid_shouldReturnTrueForBlank() {
        // Act
        boolean result = validator.isValid("   ", context);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isValid должен вернуть true для адреса с подчеркиванием и дефисом")
    void isValid_shouldReturnTrueForAddressWithUnderscoreAndDash() {
        // Arrange - ровно 48 символов с _ и -
        String validAddress = "EQDummy_Wallet-Address12345678901234567890123456"; // 48 символов

        // Act
        boolean result = validator.isValid(validAddress, context);

        // Assert
        assertThat(result).isTrue();
        assertThat(validAddress.length()).isEqualTo(48);
    }
}

