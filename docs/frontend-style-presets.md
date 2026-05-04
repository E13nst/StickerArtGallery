# Style presets (мини-приложение)

## Публичная ссылка на пресет

В `StylePresetDto` (ответ `GET /api/generation/style-presets`, при необходимости `includeUi=true`):

| Поле | Назначение |
|------|------------|
| `shareableAsDeepLink` | Можно показывать «Поделиться» |
| `deepLinkStartParam` | Строка для `?startapp=` / `start_param`, формат `sag_style_<id>` |
| `deepLinkUrl` | **Готовая** ссылка `https://t.me/<bot>?startapp=<deepLinkStartParam>` — как у `GET /api/referrals/me/link` |

Фронтенд может копировать **`deepLinkUrl`** без сборки имени бота локально.

Если `deepLinkUrl` равна `null` (например, не задано `app.telegram.bot-username` на сервере), остаётся `deepLinkStartParam` для клиентской сборки.
