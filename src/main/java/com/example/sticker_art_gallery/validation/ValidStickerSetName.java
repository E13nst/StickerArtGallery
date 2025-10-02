package com.example.sticker_art_gallery.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Аннотация для валидации имени стикерсета или URL стикерсета
 */
@Documented
@Constraint(validatedBy = StickerSetNameValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidStickerSetName {
    String message() default "Некорректное имя стикерсета или URL. Ожидается имя стикерсета или URL вида https://t.me/addstickers/имя_стикерсета";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
