# API: Admin Analytics Dashboard

## Endpoint

```
GET /api/admin/analytics/dashboard
```

Доступ: только роль `ADMIN`. Требуется заголовок `X-Telegram-Init-Data` с валидным initData администратора.

## Query-параметры

| Параметр       | Тип   | Обязательный | Описание |
|----------------|-------|--------------|----------|
| from           | string| да           | Начало диапазона (ISO-8601), например `2025-02-01T00:00:00Z` |
| to             | string| да           | Конец диапазона (ISO-8601) |
| granularity    | string| да           | Размер бакета для таймсерий: `hour`, `day`, `week` |
| tz             | string| нет          | Часовой пояс для бакетов (по умолчанию `UTC`), например `Europe/Moscow` |

Ограничения:
- Максимальный диапазон: 365 дней (`to - from <= 365`).
- `from` и `to` должны быть валидными датами/временем в прошлом или текущем моменте.

## Ответ 200 OK

Тело: `AnalyticsDashboardResponseDto` (JSON).

- **from, to, granularity, tz** — эхо переданных параметров.
- **kpiCards** — объект `DashboardKpiDto`: агрегаты за период (totalUsers, newUsers, activeUsers, createdStickerSets, likes, dislikes, swipes, artEarned, artSpent, generationRuns, generationSuccessRate, referralConversions, referralEventsTotal).
- **timeseries** — объект `DashboardTimeseriesDto`: для каждой метрики список `TimeBucketPointDto` с полями `bucketStart` (ISO-8601), `value` (число) или `values` (объект ключ–число).
- **breakdowns** — объект `DashboardBreakdownsDto`: topUsers, topStickerSets (списки объектов), referralByType, generationByStageStatus (ключ–число).

## Ошибки

- **400** — невалидные или отсутствующие `from`/`to`/`granularity`, либо диапазон > 365 дней.
- **401** — не авторизован.
- **403** — нет роли ADMIN.
