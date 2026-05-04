---
name: implement-feature-toggle
description: Добавляет feature toggle для нового поведения в StickerArtGallery и связывает его с текущей конфигурацией toggles. Use when the user asks to guard functionality behind a flag, disable a flow safely, or expose a new service toggle.
---

# Implement Feature Toggle

## Когда использовать

Используй этот skill, когда нужно включать или выключать новый функционал через конфиг, не ломая основной поток приложения.

## Сначала прочитай

1. `PROJECT_CONTEXT.md`
2. `src/main/java/com/example/sticker_art_gallery/controller/ServiceTogglesController.java`
3. `src/main/java/com/example/sticker_art_gallery/config/AppConfig.java`
4. Ближайший service или controller, где должен стоять новый флаг

## Workflow

1. Найди точку входа нового поведения: controller, service, telegram flow, payment flow или scheduled logic.
2. Найди существующий источник toggle-данных. Сначала проверь `AppConfig` и уже существующие точки чтения флагов.
3. Если в проекте уже есть `ServiceTogglesService`, используй его. Если его нет, создай тонкий service-слой над `AppConfig`, чтобы не читать флаги напрямую из каждого controller.
4. Добавь новое поле в `AppConfig`, если для функции еще нет подходящего toggle.
5. Добавь отображение toggle в `ServiceTogglesController`, если флаг должен быть видим через `/api/config/toggles`.
6. Перед выполнением основной логики проверь toggle в service-слое, а не размазывай проверку по нескольким endpoint'ам.
7. Реализуй graceful degradation: верни безопасный ответ, пропусти часть процесса или покажи понятное сообщение, если фича выключена.
8. Не меняй существующие enabled/disabled сценарии для `nativePaymentEnabled`, `nativeMessagingEnabled`, `supportEnabled` и соседних флагов без явного запроса.
9. Добавь тесты на оба режима: flag on и flag off.

## Обязательные правила

- Не читай feature flag прямо из controller, если можно проверить его в service.
- Не скрывай выключенную фичу через исключение, если возможен мягкий ответ.
- Не дублируй одно и то же условие в нескольких слоях без причины.

## Проверка перед завершением

- Флаг имеет одно место истины.
- Выключенный режим ведет себя безопасно.
- `ServiceTogglesController` отражает новый toggle, если это нужно для диагностики.
- Есть тест на включенный и выключенный режим.
