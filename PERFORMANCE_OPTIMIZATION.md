# 🚀 Рекомендации по оптимизации производительности

## 📊 Текущие результаты (базовая линия)

### Backend (Real HTTP бенчмарк)
- **Холодный кеш**: avg=1400ms, p95=2422ms, throughput=6.77 req/s
- **Горячий кеш**: avg=1158ms, p95=1716ms, throughput=8.03 req/s
- **Cache Hit Rate**: 83.33%
- **Улучшение от кеша**: 17-29% ускорение

---

## 🔧 Backend оптимизации

### 1. ✅ Уже реализовано

#### Redis кеширование (реализовано)
```yaml
app:
  sticker-cache:
    enabled: true
    ttl-days: 7
    min-size-bytes: 1024
```

**Результат**: 17-29% ускорение, 83% hit rate

---

### 2. 🎯 Приоритетные оптимизации

#### A. Connection Pooling для RestTemplate

**Проблема**: По умолчанию RestTemplate создает новое соединение для каждого запроса

**Решение**: Настроить HttpClient с пулом соединений

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        // Пул соединений
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);           // Максимум соединений
        connectionManager.setDefaultMaxPerRoute(20);  // На один хост
        
        // Настройки таймаутов
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)              // Таймаут подключения
            .setSocketTimeout(10000)              // Таймаут чтения
            .setConnectionRequestTimeout(3000)    // Таймаут из пула
            .build();
        
        // HttpClient с пулом
        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .setKeepAliveStrategy((response, context) -> 30 * 1000) // 30 секунд
            .build();
        
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory(httpClient);
        
        return new RestTemplate(factory);
    }
}
```

**Ожидаемый результат**: 20-30% ускорение за счет переиспользования соединений

---

#### B. HTTP/2 и Keep-Alive для внешнего API

**Проблема**: Каждый запрос к `STICKER_PROCESSOR_URL` создает новое TCP соединение

**Решение**: 
1. Убедиться, что внешний сервис поддерживает HTTP/2
2. Включить Keep-Alive на уровне HttpClient (см. выше)

**Конфигурация Nginx на внешнем сервисе**:
```nginx
upstream sticker_processor {
    server backend:8080;
    keepalive 32;  # Держим 32 соединения открытыми
}

server {
    location /stickers/ {
        proxy_pass http://sticker_processor;
        proxy_http_version 1.1;
        proxy_set_header Connection "";  # Keep-Alive
    }
}
```

**Ожидаемый результат**: 15-25% ускорение, особенно на медленных сетях

---

#### C. Prefetching популярных стикеров

**Идея**: Предзагружать популярные стикеры в кеш при старте приложения

**Реализация**:

```java
@Service
public class StickerPrefetchService {
    
    private final StickerProxyService proxyService;
    private final StickerSetRepository stickerSetRepository;
    
    @Scheduled(cron = "0 0 2 * * *")  // Каждую ночь в 2:00
    public void prefetchPopularStickers() {
        LOGGER.info("🔥 Запуск prefetch популярных стикеров...");
        
        // Получаем топ-20 стикерсетов по лайкам
        List<StickerSet> popular = stickerSetRepository
            .findTop20ByOrderByLikesCountDesc();
        
        int prefetched = 0;
        for (StickerSet set : popular) {
            // Берем первые 4 стикера из каждого сета
            List<String> fileIds = extractFileIds(set, 4);
            
            for (String fileId : fileIds) {
                try {
                    proxyService.getSticker(fileId); // Загрузит и закеширует
                    prefetched++;
                } catch (Exception e) {
                    LOGGER.warn("⚠️ Ошибка prefetch для {}: {}", fileId, e.getMessage());
                }
            }
        }
        
        LOGGER.info("✅ Prefetch завершен: {} стикеров загружено в кеш", prefetched);
    }
}
```

**Ожидаемый результат**: 90%+ hit rate для популярных стикеров

---

#### D. Сжатие больших файлов в Redis

**Проблема**: Большие TGS файлы занимают много места в Redis

**Решение**: Сжимать данные перед сохранением в Redis

```java
@Service
public class StickerCacheService {
    
    private byte[] compress(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            LOGGER.warn("Ошибка сжатия, возвращаем оригинал");
            return data;
        }
    }
    
    private byte[] decompress(byte[] compressed) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка декомпрессии", e);
        }
    }
    
    public void put(String fileId, byte[] data, String contentType) {
        byte[] compressed = compress(data);
        double ratio = (1.0 - (double)compressed.length / data.length) * 100;
        
        LOGGER.info("💾 Сжатие: {} -> {} байт ({:.1f}% экономия)",
                   data.length, compressed.length, ratio);
        
        // Сохраняем сжатые данные
        StickerCacheDto dto = StickerCacheDto.builder()
            .fileId(fileId)
            .data(compressed)  // Сжатые данные!
            .contentType(contentType)
            .compressed(true)  // Флаг сжатия
            .build();
            
        redisTemplate.opsForValue().set(
            CACHE_KEY_PREFIX + fileId, 
            dto, 
            cacheTtlDays, 
            TimeUnit.DAYS
        );
    }
}
```

**Ожидаемый результат**: 30-50% экономия памяти Redis, небольшой overhead CPU

---

#### E. Асинхронная загрузка с WebFlux (опционально)

**Идея**: Переписать `StickerProxyService` на реактивный стек

```java
@Service
public class ReactiveStickerProxyService {
    
    private final WebClient webClient;
    
    public Mono<byte[]> getSticker(String fileId) {
        return webClient.get()
            .uri("/stickers/{fileId}", fileId)
            .retrieve()
            .bodyToMono(byte[].class)
            .timeout(Duration.ofSeconds(10))
            .retry(2);  // 2 повторных попытки
    }
    
    public Flux<byte[]> getStickers(List<String> fileIds) {
        return Flux.fromIterable(fileIds)
            .flatMap(this::getSticker, 10);  // 10 параллельных запросов
    }
}
```

**Ожидаемый результат**: 2-3x улучшение throughput за счет non-blocking I/O

---

### 3. 📊 Мониторинг и метрики

#### Добавить Micrometer метрики

```java
@Service
public class StickerProxyService {
    
    private final MeterRegistry registry;
    
    public ResponseEntity<Object> getSticker(String fileId) {
        Timer.Sample sample = Timer.start(registry);
        
        try {
            // ... загрузка стикера
            
            sample.stop(Timer.builder("sticker.load.time")
                .tag("cache", cacheHit ? "hit" : "miss")
                .register(registry));
                
            return response;
        } catch (Exception e) {
            registry.counter("sticker.load.errors", "type", e.getClass().getSimpleName())
                .increment();
            throw e;
        }
    }
}
```

**Доступ к метрикам**: `/actuator/prometheus`

---

## 🎨 Frontend оптимизации

### 1. 🎯 Виртуализация списка (Virtual Scrolling)

**Проблема**: Рендеринг 100+ стикеров одновременно перегружает DOM

**Решение**: Рендерить только видимые элементы

#### React + react-window

```jsx
import { FixedSizeGrid } from 'react-window';
import AutoSizer from 'react-virtualized-auto-sizer';

const StickerGallery = ({ stickers }) => {
  const COLUMN_COUNT = 4;
  const ROW_HEIGHT = 150;
  const COLUMN_WIDTH = 150;
  
  const Cell = ({ columnIndex, rowIndex, style }) => {
    const index = rowIndex * COLUMN_COUNT + columnIndex;
    if (index >= stickers.length) return null;
    
    const sticker = stickers[index];
    
    return (
      <div style={style}>
        <StickerItem sticker={sticker} />
      </div>
    );
  };
  
  return (
    <AutoSizer>
      {({ height, width }) => (
        <FixedSizeGrid
          columnCount={COLUMN_COUNT}
          columnWidth={COLUMN_WIDTH}
          height={height}
          rowCount={Math.ceil(stickers.length / COLUMN_COUNT)}
          rowHeight={ROW_HEIGHT}
          width={width}
        >
          {Cell}
        </FixedSizeGrid>
      )}
    </AutoSizer>
  );
};
```

**Результат**: 60 FPS даже с 1000+ стикерами, экономия памяти 90%

---

### 2. 🖼️ Ленивая загрузка изображений (Lazy Loading)

**Проблема**: Браузер загружает все стикеры сразу, даже невидимые

**Решение**: Использовать Intersection Observer API

```jsx
import { useEffect, useRef, useState } from 'react';

const LazySticker = ({ fileId, alt }) => {
  const [isLoaded, setIsLoaded] = useState(false);
  const [isInView, setIsInView] = useState(false);
  const imgRef = useRef(null);
  
  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setIsInView(true);
          observer.disconnect();
        }
      },
      {
        rootMargin: '200px',  // Начинаем загрузку за 200px до появления
      }
    );
    
    if (imgRef.current) {
      observer.observe(imgRef.current);
    }
    
    return () => observer.disconnect();
  }, []);
  
  return (
    <div 
      ref={imgRef} 
      className="sticker-container"
      style={{ minHeight: '150px' }}
    >
      {isInView ? (
        <img
          src={`/api/stickers/${fileId}`}
          alt={alt}
          loading="lazy"  // Native lazy loading как fallback
          onLoad={() => setIsLoaded(true)}
          style={{ opacity: isLoaded ? 1 : 0 }}
        />
      ) : (
        <div className="skeleton-loader" />  // Placeholder
      )}
    </div>
  );
};
```

**Результат**: Загрузка только видимых + следующих 200px, экономия трафика 70%+

---

### 3. 🎭 Прогрессивная загрузка (Progressive Loading)

**Идея**: Сначала показываем миниатюры, потом полные версии

```jsx
const ProgressiveSticker = ({ fileId, thumbnailUrl }) => {
  const [loaded, setLoaded] = useState(false);
  
  return (
    <div className="progressive-sticker">
      {/* Размытая миниатюра (10-20 KB) */}
      <img 
        src={thumbnailUrl} 
        alt="thumbnail"
        className="thumbnail"
        style={{ filter: loaded ? 'blur(0)' : 'blur(10px)' }}
      />
      
      {/* Полное изображение (200+ KB) */}
      <img
        src={`/api/stickers/${fileId}`}
        alt="sticker"
        className="full-image"
        onLoad={() => setLoaded(true)}
        style={{ opacity: loaded ? 1 : 0 }}
      />
    </div>
  );
};
```

**Backend поддержка**:
```java
// Endpoint для миниатюр
@GetMapping("/stickers/{fileId}/thumbnail")
public ResponseEntity<byte[]> getThumbnail(@PathVariable String fileId) {
    // Генерируем или берем из кеша миниатюру 50x50
    // ...
}
```

---

### 4. 📦 Batch Loading (пакетная загрузка)

**Проблема**: 80 отдельных HTTP запросов для 80 стикеров

**Решение**: Загружать пачками по 10-20 штук

```jsx
const useBatchStickerLoader = (fileIds) => {
  const [stickers, setStickers] = useState({});
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const BATCH_SIZE = 20;
    const batches = [];
    
    // Делим на батчи
    for (let i = 0; i < fileIds.length; i += BATCH_SIZE) {
      batches.push(fileIds.slice(i, i + BATCH_SIZE));
    }
    
    // Загружаем батчи последовательно
    const loadBatches = async () => {
      for (const batch of batches) {
        // Загружаем батч параллельно
        const promises = batch.map(fileId =>
          fetch(`/api/stickers/${fileId}`)
            .then(r => r.blob())
            .then(blob => ({ fileId, blob }))
        );
        
        const results = await Promise.allSettled(promises);
        
        // Обновляем состояние по мере загрузки
        const newStickers = {};
        results.forEach((result, idx) => {
          if (result.status === 'fulfilled') {
            newStickers[batch[idx]] = result.value.blob;
          }
        });
        
        setStickers(prev => ({ ...prev, ...newStickers }));
      }
      
      setLoading(false);
    };
    
    loadBatches();
  }, [fileIds]);
  
  return { stickers, loading };
};
```

**Результат**: Плавная загрузка, видимый прогресс для пользователя

---

### 5. 💾 Service Worker для offline кеша

**Идея**: Кешировать стикеры на уровне браузера для мгновенной загрузки

```javascript
// service-worker.js
const CACHE_NAME = 'sticker-cache-v1';
const CACHE_DURATION = 7 * 24 * 60 * 60 * 1000; // 7 дней

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  
  // Кешируем только запросы к /api/stickers/*
  if (url.pathname.startsWith('/api/stickers/')) {
    event.respondWith(
      caches.open(CACHE_NAME).then(async (cache) => {
        // Проверяем кеш
        const cached = await cache.match(event.request);
        
        if (cached) {
          const cacheTime = new Date(cached.headers.get('date')).getTime();
          const now = Date.now();
          
          // Если кеш свежий - отдаем
          if (now - cacheTime < CACHE_DURATION) {
            return cached;
          }
        }
        
        // Загружаем с сервера
        const response = await fetch(event.request);
        
        // Кешируем успешные ответы
        if (response.ok) {
          cache.put(event.request, response.clone());
        }
        
        return response;
      })
    );
  }
});
```

**Регистрация в React**:
```jsx
// index.jsx
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/service-worker.js')
    .then(reg => console.log('✅ Service Worker зарегистрирован'))
    .catch(err => console.error('❌ Ошибка Service Worker:', err));
}
```

**Результат**: Мгновенная загрузка повторно просмотренных стикеров

---

### 6. 🎬 Оптимизация Lottie анимаций

**Проблема**: TGS файлы могут тормозить на слабых устройствах

**Решение**: Настройки рендеринга Lottie

```jsx
import Lottie from 'react-lottie-player';

const AnimatedSticker = ({ fileId }) => {
  return (
    <Lottie
      path={`/api/stickers/${fileId}`}
      play
      loop
      renderer="canvas"         // Canvas быстрее чем SVG
      rendererSettings={{
        preserveAspectRatio: 'xMidYMid slice',
        progressiveLoad: true,  // Прогрессивная загрузка
        hideOnTransparent: true,
      }}
      style={{ width: 150, height: 150 }}
    />
  );
};
```

**Паузить невидимые анимации**:
```jsx
const [isVisible, setIsVisible] = useState(false);

useEffect(() => {
  const observer = new IntersectionObserver(([entry]) => {
    setIsVisible(entry.isIntersecting);
  });
  
  observer.observe(ref.current);
  return () => observer.disconnect();
}, []);

return (
  <Lottie
    play={isVisible}  // Анимация только если видна!
    // ...
  />
);
```

**Результат**: Экономия CPU 60-80% на невидимых анимациях

---

### 7. 📱 Адаптивное качество для мобильных

**Идея**: На мобильных загружать версии меньшего разрешения

```jsx
const useResponsiveSticker = (fileId) => {
  const isMobile = window.innerWidth < 768;
  const quality = isMobile ? 'low' : 'high';
  
  const url = `/api/stickers/${fileId}?quality=${quality}`;
  
  return url;
};
```

**Backend**:
```java
@GetMapping("/stickers/{fileId}")
public ResponseEntity<byte[]> getSticker(
    @PathVariable String fileId,
    @RequestParam(defaultValue = "high") String quality
) {
    if ("low".equals(quality)) {
        // Возвращаем версию меньшего размера
        return getStickerLowQuality(fileId);
    }
    return getStickerHighQuality(fileId);
}
```

---

## 🎯 Приоритизация оптимизаций

### Backend (по приоритету):
1. **Connection Pooling** - легко, большой эффект (20-30%)
2. **Prefetching** - средняя сложность, высокий hit rate (90%+)
3. **Compression** - легко, экономия памяти (30-50%)
4. **WebFlux** - сложно, но 2-3x throughput

### Frontend (по приоритету):
1. **Lazy Loading** - легко, критично для UX
2. **Virtual Scrolling** - средне, огромное ускорение (60 FPS)
3. **Service Worker** - средне, мгновенные повторные загрузки
4. **Batch Loading** - легко, снижение нагрузки на сервер
5. **Lottie оптимизации** - легко, экономия CPU

---

## 📊 Ожидаемые результаты после всех оптимизаций

### Backend
- **Cold cache**: avg=1000-1100ms (было 1400ms) ⚡ **-28%**
- **Hot cache**: avg=700-800ms (было 1158ms) ⚡ **-38%**
- **Throughput**: 12-15 req/s (было 6.77-8.03) ⚡ **+80%**

### Frontend
- **Initial load**: 1-2 секунды (только видимые)
- **Scroll FPS**: 60 FPS постоянно
- **Memory usage**: -70% (виртуализация)
- **Repeat visit**: мгновенно (Service Worker)

---

## 🔍 Как измерять эффективность

### Backend метрики
```bash
# Запуск бенчмарка
make test-allure-serve --tests RealHttpBenchmarkTest

# Метрики из Prometheus
curl http://localhost:8080/actuator/prometheus | grep sticker
```

### Frontend метрики
```javascript
// Chrome DevTools → Performance
// Lighthouse → Performance audit

// Web Vitals
import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

getCLS(console.log);  // Cumulative Layout Shift
getFID(console.log);  // First Input Delay
getFCP(console.log);  // First Contentful Paint
getLCP(console.log);  // Largest Contentful Paint
getTTFB(console.log); // Time to First Byte
```

---

## 🚀 Roadmap внедрения

### Неделя 1: Quick Wins (Backend)
- [ ] Connection Pooling для RestTemplate
- [ ] HTTP/2 Keep-Alive настройки
- [ ] Compression в Redis (опционально)

### Неделя 2: Frontend Foundation
- [ ] Lazy Loading с Intersection Observer
- [ ] Virtual Scrolling (react-window)
- [ ] Batch Loading логика

### Неделя 3: Advanced Features
- [ ] Prefetching популярных стикеров
- [ ] Service Worker кеширование
- [ ] Lottie оптимизации (pause invisible)

### Неделя 4: Monitoring & Fine-tuning
- [ ] Micrometer метрики
- [ ] Grafana дашборды
- [ ] A/B тестирование оптимизаций
- [ ] Финальный бенчмарк и сравнение

---

## 📚 Полезные ссылки

### Backend
- [Spring RestTemplate Connection Pooling](https://www.baeldung.com/httpclient-connection-management)
- [Redis Compression Strategies](https://redis.io/docs/manual/compression/)
- [Spring WebFlux Guide](https://spring.io/guides/gs/reactive-rest-service/)

### Frontend
- [react-window docs](https://react-window.vercel.app/)
- [Intersection Observer API](https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API)
- [Service Worker Cookbook](https://serviceworke.rs/)
- [Web Vitals](https://web.dev/vitals/)
- [Lottie Performance Best Practices](https://airbnb.io/lottie/#/web)

---

**Автор**: AI Assistant  
**Дата**: 27 октября 2025  
**Версия**: 1.0

