package com.example.sticker_art_gallery.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для валидации TON-адресов
 */
@Documented
@Constraint(validatedBy = TonAddressValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTonAddress {
    String message() default "Некорректный формат TON-адреса";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}







