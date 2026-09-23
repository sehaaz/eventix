# EventTix — Etkinlik Bileti Satış Platformu (Mikroservis)

**Stack:** Java 21 + Spring Boot 3 + Spring Cloud · PostgreSQL 16 · React 18 (Vite) · RabbitMQ 3 · Docker Compose
**Mimari:** Microservices (4 iş servisi + gateway + discovery)
**Hedef süre:** 5–6 hafta (günde ~2 saat)
**Repo:** `github.com/sehaaz/eventtix` (monorepo, servis başına klasör)

---

## 1. Amaç

Kullanıcıların etkinlik listesini görüp bilet satın aldığı, satın alma akışının birden fazla servise yayıldığı mikroservis uygulaması.

Projenin teknik çekirdeği: **sipariş oluşturma tek bir veritabanı transaction'ına sığmıyor.** Kontenjan `event-service`'te, sipariş `order-service`'te, bilet `ticket-service`'te ve her birinin kendi şeması var. Bu yüzden akış RabbitMQ üzerinden **saga (choreography)** ile yürütülür; bir adım başarısız olursa telafi (compensation) mesajı yayınlanır. Mülakatta anlatılacak asıl konu budur.

## 2. Kapsam

**Var:**
- Kayıt / giriş (JWT), rol: `USER`, `ADMIN`
- Etkinlik listeleme, filtreleme (şehir, tarih), detay
- Bilet satın alma (adet bazlı, koltuk seçimi yok)
- Saga ile dağıtık sipariş akışı + telafi adımları
- Sipariş durumu takibi, "Biletlerim" sayfası
- QR kodlu bilet PDF'i indirme
- Admin: etkinlik CRUD, kontenjan yönetimi
- Tek komutla ayağa kalkma: `docker compose up`

**Yok (bilinçli olarak kapsam dışı):**
- Gerçek ödeme entegrasyonu → `order-service` içinde sahte `PaymentService`
- Kubernetes, Helm, service mesh, CI/CD pipeline
- Config Server, distributed tracing (Sleuth/Zipkin) — v2 notu olarak README'de dursun
- Koltuk haritası, iade/değişim, çoklu dil

## 3. Servisler

| Servis | Port | Sorumluluk | Şema |
|---|---|---|---|
| `discovery-service` | 8761 | Eureka Server — servis kaydı | — |
| `gateway-service` | 8080 | Tek giriş noktası, routing, JWT doğrulama, CORS, rate limit | — |
| `auth-service` | 8081 | Kullanıcı, kayıt/giriş, JWT üretimi | `auth_schema` |
| `event-service` | 8082 | Etkinlik CRUD, **kontenjan rezervasyonu/iadesi** | `event_schema` |
| `order-service` | 8083 | Sipariş yaşam döngüsü, saga orkestrasyonu | `order_schema` |
| `ticket-service` | 8084 | Bilet üretimi (QR + PDF), bilet sorgulama | `ticket_schema` |
| `notification-service` | 8085 | Mail gönderimi (MailHog) | — (stateless) |
| `frontend` | 3000 | React SPA, sadece gateway ile konuşur | — |

**Veritabanı:** tek PostgreSQL container, servis başına **ayrı şema ve ayrı DB kullanıcısı**. Bir servis başka servisin şemasına erişemez (GRANT verilmez). Mantıksal olarak database-per-service; fiziksel olarak tek container — lokal geliştirmeyi hafif tutmak için bilinçli tercih, README'de belirtilir.

## 4. Mimari

```
                        ┌──────────────┐
                        │ React SPA    │ :3000
                        └──────┬───────┘
                               │ HTTP (yalnız gateway)
                               ▼
                    ┌──────────────────────┐        ┌────────────────────┐
                    │  gateway-service     │◄──────►│ discovery-service  │
                    │  :8080  JWT + route  │        │ :8761 (Eureka)     │
                    └──┬────┬────┬────┬────┘        └────────────────────┘
                       │    │    │    │                      ▲
        ┌──────────────┘    │    │    └──────────────┐       │ register
        ▼                   ▼    ▼                   ▼       │
  ┌───────────┐     ┌────────────┐  ┌─────────────┐  ┌──────────────┐
  │   auth    │     │   event    │  │    order    │  │   ticket     │
  │   :8081   │     │   :8082    │  │    :8083    │  │   :8084      │
  └─────┬─────┘     └─────┬──────┘  └──────┬──────┘  └──────┬───────┘
        │                 │                │                │
        │                 └────────┬───────┴────────┬───────┘
        │                          │  AMQP (pub/sub) │
        │                          ▼                 ▼
        │                 ┌──────────────────────────────┐
        │                 │  RabbitMQ  :5672 / :15672    │
        │                 │  eventtix.exchange (topic)   │
        │                 └──────────────┬───────────────┘
        │                                │
        │                                ▼
        │                    ┌────────────────────────┐
        │                    │  notification-service  │ :8085 → MailHog
        │                    └────────────────────────┘
        │
        └──────── hepsi ────► ┌──────────────────────────┐
                              │ PostgreSQL :5432         │
                              │ auth_ / event_ /         │
                              │ order_ / ticket_ schema  │
                              └──────────────────────────┘
```

**İletişim kuralı:**
- **Senkron (REST):** sadece okuma ve sadece gateway → servis, bir de `order-service` → `event-service` (etkinlik fiyat/başlık okuma, OpenFeign ile). Başka senkron servis-servis çağrısı yok.
- **Asenkron (RabbitMQ):** durum değiştiren her cross-service iş.

## 5. Saga — Sipariş Akışı

**Exchange:** `eventtix.exchange` (topic), tüm queue'lar `durable`.

### Mutlu yol

```
1. POST /api/orders  (gateway → order-service)
   order-service: orders(PENDING) insert  →  publish "order.created"

2. event-service ← order.created
   Koşullu UPDATE ile kontenjan rezerve et
   ├─ başarılı  → publish "quota.reserved"
   └─ yetersiz  → publish "quota.rejected"

3. order-service ← quota.reserved
   orders.status = QUOTA_RESERVED  →  publish "order.confirmed"

4. ticket-service ← order.confirmed
   quantity kadar ticket üret (UUID + QR PNG + PDF)
   ├─ başarılı → publish "ticket.generated"
   └─ hata     → publish "ticket.failed"

5. order-service ← ticket.generated
   orders.status = COMPLETED  →  publish "order.completed"

6. notification-service ← order.completed
   Mail gönder (MailHog)
```

### Telafi (compensation) yolları

```
quota.rejected  → order-service: orders.status = FAILED (reason = QUOTA)
                                 publish "order.failed"
                → notification-service: "bilet alınamadı" maili

ticket.failed   → order-service: orders.status = FAILED (reason = TICKET)
                                 publish "order.cancelled"
                → event-service ← order.cancelled: kontenjanı geri ver
                → notification-service: bilgilendirme maili
```

### Routing key / queue eşlemesi

| Routing key | Queue | Tüketen |
|---|---|---|
| `order.created` | `event.quota.queue` | event-service |
| `quota.reserved` | `order.quota-reserved.queue` | order-service |
| `quota.rejected` | `order.quota-rejected.queue` | order-service |
| `order.confirmed` | `ticket.generate.queue` | ticket-service |
| `ticket.generated` | `order.ticket-generated.queue` | order-service |
| `ticket.failed` | `order.ticket-failed.queue` | order-service |
| `order.cancelled` | `event.quota-release.queue` | event-service |
| `order.completed` | `notification.success.queue` | notification-service |
| `order.failed` | `notification.failure.queue` | notification-service |

**Dayanıklılık kuralları (her consumer için geçerli):**
- Her queue'ya `x-dead-letter-exchange: eventtix.dlx` → `<queue>.dlq`. 3 denemede başarısız mesaj DLQ'ya.
- **Idempotency:** her mesajda `sagaId` (= orderId) var. Consumer işi yapmadan önce "bu sagaId için daha önce yapıldı mı" kontrol eder. `ticket-service`'te `order_id UNIQUE`, `event-service`'te `quota_reservations(order_id UNIQUE)` tablosu bunu garanti eder.
- **Publish, commit'ten sonra:** `TransactionSynchronizationManager.registerSynchronization(...afterCommit)`. Rollback olan bir işlem için mesaj yayınlanmaz.
- Mesaj formatı ortak: `{ sagaId, eventType, occurredAt, payload }`

## 6. Veri Modeli

**auth_schema**
```sql
users(id PK, email UNIQUE, password_hash, full_name, role, created_at)
```

**event_schema**
```sql
events(id PK, title, description, venue, city, event_date TIMESTAMPTZ,
       price NUMERIC(10,2), total_quota INT, sold_count INT DEFAULT 0,
       image_url, created_at)

quota_reservations(order_id BIGINT PK, event_id, quantity, created_at)  -- idempotency
```

Kontenjan rezervasyonu tek koşullu UPDATE ile:
```sql
UPDATE events SET sold_count = sold_count + :qty
WHERE id = :id AND sold_count + :qty <= total_quota;
```
Dönen satır 0 ise → `quota.rejected`. İade: `sold_count = sold_count - :qty`.

**order_schema**
```sql
orders(id PK, user_id, event_id, event_title, quantity,
       unit_price NUMERIC(10,2), total_price NUMERIC(10,2),
       status VARCHAR,        -- PENDING|QUOTA_RESERVED|COMPLETED|FAILED|CANCELLED
       failure_reason VARCHAR, created_at, updated_at)
```
`event_title` ve `unit_price` sipariş anında kopyalanır (denormalizasyon) — etkinlik sonradan güncellenirse sipariş geçmişi bozulmaz.

**ticket_schema**
```sql
tickets(id PK, order_id, user_id, event_id, ticket_code UNIQUE,
        qr_path, pdf_path, used BOOLEAN DEFAULT false, created_at)
CREATE UNIQUE INDEX uq_tickets_order ON tickets(order_id, seq_no);
```

Şema yönetimi: her serviste kendi **Flyway** migration'ları, `ddl-auto: validate`.

## 7. API (gateway üzerinden)

| Method | Endpoint | Yönlendirme | Yetki |
|---|---|---|---|
| POST | `/api/auth/register` | auth-service | — |
| POST | `/api/auth/login` | auth-service | — |
| GET | `/api/events` | event-service | — |
| GET | `/api/events/{id}` | event-service | — |
| POST/PUT/DELETE | `/api/events/**` | event-service | ADMIN |
| POST | `/api/orders` | order-service | USER |
| GET | `/api/orders/me` | order-service | USER |
| GET | `/api/orders/{id}` | order-service | USER |
| GET | `/api/tickets/me` | ticket-service | USER |
| GET | `/api/tickets/{code}/pdf` | ticket-service | USER |

**Gateway'in işi:** JWT imzasını doğrular, `X-User-Id` ve `X-User-Role` header'larını ekleyip aşağı iletir. İş servisleri JWT parse etmez, bu header'lara güvenir — ağ dışarıya kapalı olduğu için (compose network) kabul edilebilir, README'de not düşülür.

Hata formatı tüm servislerde tek tip (`@RestControllerAdvice`, ortak `common-dto` modülü):
```json
{ "timestamp": "...", "status": 409, "error": "QUOTA_EXCEEDED", "message": "Yeterli bilet yok" }
```

Dokümantasyon: her serviste springdoc, gateway'de tek Swagger UI'da birleştirilir (`/swagger-ui.html` → dropdown).

## 8. Frontend (React + Vite)

```
src/
├── api/client.js          # axios, baseURL = gateway, JWT interceptor
├── pages/
│   ├── EventListPage.jsx      # filtre + kart grid
│   ├── EventDetailPage.jsx    # detay + adet + "Satın Al"
│   ├── OrderStatusPage.jsx    # saga durumu (aşama aşama gösterim)
│   ├── MyTicketsPage.jsx      # bilet kartları + PDF indir
│   ├── LoginPage.jsx / RegisterPage.jsx
│   └── admin/EventAdminPage.jsx
├── components/  # Navbar, EventCard, TicketCard, ProtectedRoute, SagaStepper
├── context/AuthContext.jsx
└── App.jsx
```

Sipariş asenkron olduğu için `OrderStatusPage`, 2 sn'de bir `GET /api/orders/{id}` ile polling yapar ve `SagaStepper` bileşeninde adımları gösterir: *Sipariş alındı → Kontenjan ayrıldı → Biletler hazır*. Bu ekran, mikroservis mimarisini demo GIF'inde **görünür** kılar — README'nin en değerli parçası.

Stil: Tailwind CSS. Ekstra state kütüphanesi yok.

## 9. Repo ve Build Yapısı (monorepo)

```
eventtix/
├── docker-compose.yml
├── README.md
├── docs/architecture.png
├── common-dto/            # olay sınıfları + ApiError (her servis bağımlılık alır)
├── discovery-service/
├── gateway-service/
├── auth-service/
├── event-service/
├── order-service/
├── ticket-service/
├── notification-service/
└── frontend/
```

Her Java servisi kendi `pom.xml`'i olan bağımsız Maven projesi; `common-dto` `mvn install` ile lokal repoya kurulur (parent POM ile aggregate edilebilir). Servis içi paket yapısı ortak:

```
com.sehaaz.eventtix.<servis>/
├── config/        SecurityConfig, RabbitConfig, OpenApiConfig
├── api/           Controller + dto
├── domain/        Entity, Repository, Service
├── messaging/     Publisher, Listener
└── common/        GlobalExceptionHandler
```

## 10. Docker

| Servis | Image / Build | Port |
|---|---|---|
| `postgres` | `postgres:16-alpine` + `init-schemas.sql` | 5432 |
| `rabbitmq` | `rabbitmq:3-management-alpine` | 5672, 15672 |
| `mailhog` | `mailhog/mailhog` | 1025, 8025 |
| `discovery` | multi-stage (maven → JRE 21) | 8761 |
| `gateway` | multi-stage | 8080 |
| `auth` / `event` / `order` / `ticket` / `notification` | multi-stage | 8081–8085 |
| `frontend` | node build → nginx | 3000 |

`init-schemas.sql` dört şemayı ve dört DB kullanıcısını oluşturur. Healthcheck + `depends_on: service_healthy` zinciri: postgres & rabbitmq → discovery → gateway & iş servisleri.

Tek komut: `docker compose up --build` (ilk build uzun sürer, README'de belirt).

## 11. Test

- Servis başına birim testleri — JUnit 5 + Mockito
- `event-service`: kontenjan yarışı testi — 10 paralel thread, kontenjan 5 → tam 5 rezervasyon başarılı
- `order-service`: saga durum makinesi testleri (her olay için doğru geçiş ve telafi)
- **Uçtan uca saga testi** — Testcontainers (PostgreSQL + RabbitMQ): sipariş oluştur → `COMPLETED` ve bilet sayısı doğru
- **Telafi testi:** ticket-service'i hata verecek şekilde zorla → sipariş `FAILED` ve kontenjan **geri verilmiş** olmalı. Bu test projenin en güçlü kanıtı, README'de çıktısını göster.

## 12. Yol Haritası

| Hafta | İş |
|---|---|
| 1 | Docker Compose iskeleti, PostgreSQL + şemalar, `discovery` + `gateway`, `auth-service` (JWT) |
| 2 | `event-service`: CRUD, Flyway, Swagger, gateway routing, kontenjan UPDATE mantığı |
| 3 | `order-service`: sipariş modeli, RabbitMQ config, `order.created` → `quota.reserved/rejected` turu |
| 4 | `ticket-service`: QR + PDF, saga'nın kalan adımları, DLQ, idempotency |
| 5 | Telafi yolları, `notification-service`, Testcontainers saga testleri |
| 6 | React arayüzü + SagaStepper, README + mimari şeması + demo GIF |

Servis servis ilerle; her servis bittiğinde commit'le. Tek seferde "initial commit" atma.

## 13. README'de Bulunması Gerekenler

1. Bir cümlelik ne olduğu + demo GIF (SagaStepper ekranı görünsün)
2. Mimari şeması (yukarıdaki ASCII veya Excalidraw PNG)
3. Servis tablosu: isim, port, sorumluluk, şema
4. `docker compose up --build` → `localhost:3000`, demo kullanıcı bilgileri
5. Swagger, Eureka (`:8761`), RabbitMQ Management (`:15672`), MailHog (`:8025`) linkleri
6. **"Neden mikroservis ve dağıtık transaction'ı nasıl çözdün?"** — saga akış diyagramı + telafi adımları. Mülakatta sorulacak asıl soru bu.
7. **"Aynı mesaj iki kez gelirse ne olur?"** — idempotency stratejisi, 3 cümle.
8. Telafi testinin terminal çıktısı (kontenjanın geri verildiği görülsün)
9. "v2'de ne eklerdim" — config server, distributed tracing, K8s. Farkındalığı gösterir.
