# EventTix — AI ile Geliştirme Promptları

Bu dosya, `PROJE-1-EventTix-Mimari.md` dosyasını AI'a adım adım uygulatmak içindir.

**Kullanım:**
1. Boş bir klasör aç, `git init` yap.
2. Mimari dosyasını (`PROJE-1-EventTix-Mimari.md`) ve bu dosyayı repo köküne kopyala.
3. Aşağıdaki **Faz 0** promptunu bir kere çalıştır (repo kuralları oluşur).
4. Sonra her fazı sırayla çalıştır. **Bir faz bitmeden diğerine geçme.** Her fazın sonundaki doğrulama adımını kendin çalıştır.
5. Her faz sonunda commit at.

**Altın kural:** AI'a "her şeyi yap" deme. Her prompt tek bir servis veya tek bir akış içindir. Uzun promptlar dağınık kod üretir.

---

## Faz 0 — Repo kuralları (bir kere)

```
Bu repoda EventTix adında bir mikroservis projesi geliştireceğim. Mimarinin tamamı
PROJE-1-EventTix-Mimari.md dosyasında. Önce bu dosyayı oku.

Görev: Repo köküne, bundan sonraki tüm çalışmalarında uyacağın kuralları içeren bir
CLAUDE.md dosyası oluştur. İçeriği şunlar olsun:

- Mimari kaynak: PROJE-1-EventTix-Mimari.md. Mimariye aykırı bir şey yapma, aykırı
  bir şey gerekiyorsa önce sor.
- İstenen değişikliğin en küçük hâlini yap. Fazladan özellik, config, soyutlama ekleme.
- Sormadan yeni dependency ekleme.
- Her servis Spring Boot 3 + Java 21. Paket yapısı: config / api / domain / messaging / common.
- Lombok kullan. DTO'lar record.
- Veritabanı şeması sadece Flyway ile yönetilir, ddl-auto: validate.
- Yeni kod yazmadan önce repodaki en yakın örüntüyü bul ve ona uy.
- Testler JUnit 5 + Mockito; entegrasyon testleri Testcontainers.
- Değişiklik sonrası özet: ne değişti, nasıl test edilir, varsa risk. Fazlası yok.

Sadece CLAUDE.md dosyasını oluştur, başka bir şey yapma.
```

---

## Faz 1 — İskelet + altyapı (Hafta 1)

### 1.1 Monorepo iskeleti ve docker-compose

```
Mimarideki bölüm 9 ve 10'a göre monorepo iskeletini kur:

- Kök klasörde .gitignore (Java + Node + IDE), README.md (şimdilik başlık ve bir cümle)
- docker-compose.yml: postgres:16-alpine, rabbitmq:3-management-alpine, mailhog
  servisleri. Her birine healthcheck ekle.
- postgres için init-schemas.sql: auth_schema, event_schema, order_schema, ticket_schema
  şemalarını ve her biri için ayrı DB kullanıcısını oluştursun. Her kullanıcı yalnızca
  kendi şemasına yetkili olsun.
- .env.example dosyası.

Henüz Java servisi ekleme. Sadece altyapı ayağa kalksın.
```

**Doğrula:** `docker compose up -d` → `docker compose ps` üçü de healthy. `localhost:15672` (guest/guest) ve `localhost:8025` açılıyor.

### 1.2 common-dto modülü

```
Mimarideki bölüm 5 ve 7'ye göre common-dto adında bir Maven modülü oluştur:

- Saga olay sınıfları (record): OrderCreatedEvent, QuotaReservedEvent, QuotaRejectedEvent,
  OrderConfirmedEvent, TicketGeneratedEvent, TicketFailedEvent, OrderCancelledEvent,
  OrderCompletedEvent, OrderFailedEvent.
- Hepsinin ortak alanları: sagaId, eventType, occurredAt. Ortak bir arayüz veya
  abstract kayıt kullan.
- ApiError record'u (timestamp, status, error, message).
- Exchange, routing key ve queue isimlerini tutan bir sabitler sınıfı (mimarideki
  bölüm 5'teki tabloya birebir uy).

Başka bir şey ekleme. mvn install ile lokal repoya kurulabilsin.
```

**Doğrula:** `cd common-dto && mvn clean install` başarılı.

### 1.3 discovery + gateway

```
discovery-service ve gateway-service'i oluştur:

- discovery-service: Eureka Server, port 8761, başka hiçbir şey yok.
- gateway-service: Spring Cloud Gateway, port 8080. Mimarideki bölüm 7'deki route
  tablosunu uygula. Eureka'dan servis keşfi yapsın (lb:// ile).
- Gateway'de JWT doğrulama filtresi: token imzasını doğrula, geçerliyse X-User-Id ve
  X-User-Role header'larını ekleyip isteği aşağı ilet. Geçersizse 401 dön.
  Public endpoint'ler: /api/auth/**, GET /api/events/**
- CORS: localhost:3000'e izin ver.
- İkisi için de multi-stage Dockerfile ve docker-compose.yml'e servis tanımı ekle.

auth-service henüz yok; gateway o route'u şimdilik tanımlasın ama test etme.
```

**Doğrula:** `docker compose up --build` → `localhost:8761` Eureka arayüzünde gateway kayıtlı görünüyor.

**Commit:** `chore: monorepo iskeleti, altyapı, discovery ve gateway`

---

## Faz 2 — auth-service + event-service (Hafta 1–2)

### 2.1 auth-service

```
auth-service'i oluştur (port 8081, auth_schema).

- Flyway V1__init.sql: users tablosu (mimari bölüm 6).
- POST /api/auth/register: email, password, fullName. Parola BCrypt (cost 12).
  Email zaten varsa 409 EMAIL_EXISTS.
- POST /api/auth/login: JWT üret (HS256, 1 saat, claim'ler: sub=userId, email, role).
  Secret .env'den gelsin.
- GlobalExceptionHandler ile ApiError formatı (mimari bölüm 7).
- springdoc-openapi, Dockerfile, docker-compose girdisi.
- AuthServiceTest: kayıt, duplicate email, başarılı ve başarısız giriş.

JWT secret'ı gateway ile aynı olmalı, .env üzerinden paylaşılsın.
```

**Doğrula:** Gateway üzerinden `POST localhost:8080/api/auth/register` → 201, `login` → token dönüyor.

### 2.2 event-service (kontenjan mantığı dahil)

```
event-service'i oluştur (port 8082, event_schema).

- Flyway: events ve quota_reservations tabloları (mimari bölüm 6).
- GET /api/events: sayfalama + city ve tarih aralığı filtresi (Specification veya
  @Query). GET /api/events/{id}: detay.
- POST/PUT/DELETE /api/events: sadece X-User-Role=ADMIN olan istekler.
- EventService içinde reserveQuota(orderId, eventId, qty) metodu:
  mimarideki koşullu UPDATE'i kullan (UPDATE ... WHERE sold_count + :qty <= total_quota).
  Etkilenen satır 0 ise false dön. Başarılıysa quota_reservations'a orderId ile kayıt at.
  releaseQuota(orderId) metodu: rezervasyon varsa sold_count'u geri düş ve kaydı sil,
  yoksa hiçbir şey yapma.
- V2__seed.sql: 8 örnek etkinlik.
- Testcontainers ile EventQuotaConcurrencyTest: total_quota=5 olan bir etkinlik için
  10 paralel thread reserveQuota(1) çağırsın. Tam 5 tanesi true dönmeli, sold_count
  tam 5 olmalı.

Henüz RabbitMQ listener ekleme, sadece servis metotları olsun.
```

**Doğrula:** `mvn test` → eşzamanlılık testi geçiyor. Gateway üzerinden etkinlik listesi geliyor.

**Commit:** `feat: auth-service ve event-service, kontenjan rezervasyon mantığı`

---

## Faz 3 — Saga'nın ilk turu (Hafta 3)

### 3.1 RabbitMQ topolojisi

```
common-dto'daki sabitleri kullanarak, event-service ve order-service'te paylaşılacak
RabbitMQ konfigürasyonunu yaz:

- eventtix.exchange (TopicExchange, durable)
- eventtix.dlx (dead letter exchange) ve her queue için <queue>.dlq
- Mimarideki bölüm 5'teki routing key / queue tablosunu birebir uygula.
- Her queue: durable, x-dead-letter-exchange ayarlı.
- Jackson2JsonMessageConverter.
- Listener'larda 3 deneme sonrası DLQ'ya düşecek retry politikası
  (RetryInterceptorBuilder veya spring.rabbitmq.listener.simple.retry.*).

Her servis yalnızca kendi tükettiği queue'ları declare etsin, exchange'i ikisi de
declare edebilir.
```

### 3.2 order-service + kontenjan turu

```
order-service'i oluştur (port 8083, order_schema).

- Flyway: orders tablosu (mimari bölüm 6, event_title ve unit_price denormalize alanlar).
- POST /api/orders: gövde {eventId, quantity}. X-User-Id header'ından kullanıcı.
  1. OpenFeign ile event-service'ten etkinlik detayını çek (başlık, fiyat, aktif mi).
  2. orders kaydını PENDING olarak insert et.
  3. Commit SONRASI OrderCreatedEvent yayınla — TransactionSynchronizationManager'ın
     afterCommit kancasını kullan. Bu önemli, rollback olursa mesaj gitmemeli.
  4. 201 ile PENDING siparişi dön.
- GET /api/orders/me ve GET /api/orders/{id} (sadece kendi siparişi).

Sonra event-service'e OrderCreatedEvent listener'ı ekle:
- reserveQuota çağır, sonuca göre QuotaReservedEvent veya QuotaRejectedEvent yayınla.
- Idempotent olsun: quota_reservations'ta o orderId varsa tekrar rezerve etme,
  doğrudan QuotaReservedEvent yayınla.

Son olarak order-service'e iki listener:
- QuotaReservedEvent → status = QUOTA_RESERVED, OrderConfirmedEvent yayınla.
- QuotaRejectedEvent → status = FAILED, failureReason = QUOTA, OrderFailedEvent yayınla.
```

**Doğrula:** Sipariş at → 2 saniye sonra `GET /api/orders/{id}` durumu `QUOTA_RESERVED`. Kontenjanı bitmiş etkinliğe sipariş at → `FAILED`. RabbitMQ arayüzünde queue'larda birikme yok.

**Commit:** `feat: saga birinci tur - sipariş oluşturma ve kontenjan rezervasyonu`

---

## Faz 4 — ticket-service + saga'nın tamamlanması (Hafta 4)

```
ticket-service'i oluştur (port 8084, ticket_schema).

- Flyway: tickets tablosu + (order_id, seq_no) UNIQUE index (mimari bölüm 6).
- OrderConfirmedEvent listener:
  1. Idempotency: bu order_id için bilet varsa TicketGeneratedEvent yayınla ve çık.
  2. quantity kadar bilet üret. ticket_code = UUID.
  3. Her bilet için ZXing ile QR PNG, OpenPDF ile PDF üret; dosyalar /app/tickets
     altına yazılsın (docker volume).
  4. Başarılıysa TicketGeneratedEvent, hata olursa TicketFailedEvent yayınla.
- GET /api/tickets/me: kullanıcının biletleri.
- GET /api/tickets/{code}/pdf: PDF'i indir, sadece bilet sahibi erişebilsin.

order-service'e iki listener daha ekle:
- TicketGeneratedEvent → status = COMPLETED, OrderCompletedEvent yayınla.
- TicketFailedEvent → status = FAILED, failureReason = TICKET,
  OrderCancelledEvent yayınla.

event-service'e bir listener daha:
- OrderCancelledEvent → releaseQuota(orderId). Idempotent olmalı.
```

**Doğrula:** Sipariş at → durum `COMPLETED`, biletler geliyor, PDF iniyor.

**Commit:** `feat: bilet üretimi ve saga tamamlanması`

---

## Faz 5 — Telafi, bildirim, testler (Hafta 5)

### 5.1 notification-service

```
notification-service'i oluştur (port 8085, veritabanı yok).

- spring-boot-starter-mail, MailHog'a bağlan (host: mailhog, port: 1025).
- OrderCompletedEvent listener → "Biletleriniz hazır" maili.
- OrderFailedEvent listener → sebebe göre "kontenjan yetersiz" veya "bilet
  oluşturulamadı" maili.
- Mail gövdeleri basit HTML, Thymeleaf template. Fazla süsleme yok.
```

### 5.2 Telafi testi — projenin en önemli testi

```
order-service'te Testcontainers (PostgreSQL + RabbitMQ) ile uçtan uca saga testleri yaz:

1. SagaHappyPathTest: sipariş oluştur, COMPLETED olmasını bekle (Awaitility),
   bilet sayısı quantity'ye eşit olsun.
2. SagaQuotaRejectedTest: kontenjanı dolu etkinliğe sipariş → FAILED, reason QUOTA.
3. SagaCompensationTest (en kritik): ticket üretimini hata fırlatacak şekilde zorla.
   Beklenen: sipariş FAILED (reason TICKET) VE event'in sold_count değeri
   sipariş öncesi değerine dönmüş olmalı. Kontenjanın geri verildiğini assert et.
4. SagaIdempotencyTest: aynı OrderCreatedEvent'i iki kez yayınla, sold_count
   yalnızca bir kez artmış olsun.

Test isimleri ve assert mesajları açıklayıcı olsun, README'ye çıktısını koyacağım.
```

**Doğrula:** `mvn test` → dördü de geçiyor. Çıktının ekran görüntüsünü al.

**Commit:** `test: uctan uca saga ve telafi testleri`

---

## Faz 6 — Frontend (Hafta 6)

### 6.1 İskelet + etkinlik akışı

```
frontend/ klasöründe React 18 + Vite + Tailwind + react-router-dom projesi kur.
Mimarideki bölüm 8'deki dosya yapısına uy.

- api/client.js: axios, baseURL http://localhost:8080, JWT interceptor (localStorage).
- AuthContext: login, logout, kullanıcı ve token state'i.
- LoginPage, RegisterPage.
- EventListPage: şehir ve tarih filtresi, kart grid, sayfalama.
- EventDetailPage: detay, adet seçimi, "Satın Al" butonu (giriş yoksa login'e yönlendir).
- Navbar, ProtectedRoute, EventCard bileşenleri.

Sade ve temiz bir tasarım yeterli; animasyon, tema değiştirici gibi şeyler ekleme.
```

### 6.2 SagaStepper — demo'nun yıldızı

```
Sipariş sonrası akışı gösteren ekranı yap:

- "Satın Al" → POST /api/orders → dönen PENDING sipariş id'si ile
  /orders/{id} sayfasına git.
- OrderStatusPage: 2 saniyede bir GET /api/orders/{id} ile polling.
- SagaStepper bileşeni: 3 adımlı görsel gösterim
    1. Sipariş alındı          (PENDING'den itibaren tamamlandı)
    2. Kontenjan ayrıldı       (QUOTA_RESERVED)
    3. Biletleriniz hazır      (COMPLETED)
  Aktif adım spinner'lı, tamamlanan adım yeşil tik, FAILED durumunda ilgili adım
  kırmızı ve failureReason gösterilsin.
- COMPLETED olunca 1,5 saniye sonra /tickets'a yönlendir. Polling'i durdur.
- MyTicketsPage: bilet kartları, QR görseli, "PDF indir" butonu.
- admin/EventAdminPage: etkinlik listesi + ekle/düzenle/sil formu (sadece ADMIN).

Polling'in sayfa kapanınca temizlendiğinden emin ol (useEffect cleanup).
```

**Doğrula:** Tarayıcıda uçtan uca akış çalışıyor. SagaStepper'ın adım adım ilerlediği bir GIF kaydet.

**Commit:** `feat: react arayuzu ve saga adim gostergesi`

---

## Faz 7 — README ve son rötuş

```
Mimarideki bölüm 13'e göre kök README.md'yi yaz. Şunlar mutlaka olsun:

1. Bir cümlelik tanım + demo GIF için yer tutucu (docs/demo.gif)
2. Mimari şeması (mimarideki ASCII diyagramı kullan)
3. Servis tablosu: isim, port, sorumluluk, şema
4. Kurulum: docker compose up --build, demo kullanıcı bilgileri
5. Linkler: Swagger, Eureka :8761, RabbitMQ :15672, MailHog :8025
6. "Neden mikroservis? Dağıtık transaction'ı nasıl çözdüm?" başlığı:
   saga akış diyagramı (mermaid), telafi adımları, neden 2PC değil de saga
7. "Aynı mesaj iki kez gelirse?" başlığı: idempotency stratejisi
8. Telafi testinin çıktısı için yer tutucu
9. "v2'de ne eklerdim": config server, distributed tracing, Kubernetes

Türkçe yaz. Pazarlama dili kullanma, teknik ve net olsun.
Ayrıca: repo açıklaması ve topics önerisi ver (spring-boot, microservices,
rabbitmq, saga-pattern, react, docker, postgresql).
```

**Son adımlar (elle):**
- `docs/demo.gif` ve test çıktısı ekran görüntüsünü ekle
- GitHub'da repo açıklaması + topics'i doldur
- Repo'yu pinle

---

## AI ile çalışırken uyulacak kurallar

| Kural | Sebep |
|---|---|
| Her fazı ayrı oturumda çalıştır | Context şişince kalite düşer |
| Faz bitmeden ilerleme | Hatalı temel üstüne inşa en pahalı hata |
| Üretilen kodu **oku** | Mülakatta bu kodu sen savunacaksın |
| Anlamadığın kısmı sor: "bu satır ne yapıyor, neden böyle?" | Anlamadığın kod CV'de yük |
| Commit'leri sen at, mesajı sen yaz | Commit geçmişi profilinin bir parçası |
| AI "şunu da ekleyeyim mi" derse çoğu zaman hayır de | Kapsam şişmesi projeyi bitirtmez |

**Tıkanırsan kullanacağın prompt:**
```
Şu hatayı alıyorum: <hata metni>
İlgili dosya: <dosya yolu>
Ne denedim: <denediklerin>
Önce hatanın sebebini açıkla, sonra en küçük düzeltmeyi öner. Kodu yeniden yazma.
```
