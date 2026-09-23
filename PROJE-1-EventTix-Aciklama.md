# EventTix — Proje Açıklaması

Bu dosya mimari değil, **anlama ve anlatma** dosyası. Projeyi neden böyle kurduğunu, nasıl çalıştığını ve mülakatta nasıl savunacağını içerir. README'nin de ham maddesi.

---

## 1. Tek cümlede

Kullanıcıların etkinlik bileti satın aldığı, satın alma akışının dört ayrı servise yayıldığı ve dağıtık transaction probleminin RabbitMQ üzerinden **saga pattern** ile çözüldüğü mikroservis uygulaması.

## 2. Bu proje aslında neyi kanıtlıyor

CV'de "mikroservis biliyorum" yazan çok kişi var; çoğunun projesi aslında "üç tane CRUD servisi ve bir gateway". Ayırt edici soru şudur: **servisleri böldüğünde transaction'ı kaybedersin, bunu nasıl çözdün?**

Bu projede o problem yapay olarak eklenmedi, domainden kendiliğinden çıktı:

- Kontenjan bilgisi `event-service`'in veritabanında
- Sipariş kaydı `order-service`'in veritabanında
- Bilet `ticket-service`'in veritabanında

"Kontenjanı düş, siparişi oluştur, bileti üret" işleminin üç ayrı veritabanına dokunması gerekiyor. Tek bir `@Transactional` bunu kapsayamaz. Çözüm: işi adımlara bölmek ve her adımın **geri alma (telafi)** karşılığını yazmak.

Projenin gerçek çıktısı bu: *dağıtık sistemde tutarlılığı nasıl sağladığını gösteren, çalışan ve test edilmiş bir örnek.*

## 3. Sistem nasıl çalışıyor

### Parçalar

| Parça | Ne yapar |
|---|---|
| `gateway-service` | Dışarıya açılan tek kapı. JWT'yi doğrular, kullanıcı bilgisini header'a koyar, isteği doğru servise yönlendirir. |
| `discovery-service` | Eureka. Servisler nerede çalıştığını buraya bildirir, gateway buradan bulur. |
| `auth-service` | Kayıt, giriş, JWT üretimi. |
| `event-service` | Etkinlikler ve **kontenjan**. Kontenjanı rezerve eder ve gerektiğinde iade eder. |
| `order-service` | Siparişin yaşam döngüsü. Saga'nın durumunu tutan taraf. |
| `ticket-service` | Bilet üretimi: QR kod + PDF. |
| `notification-service` | Sonuç maili. |
| `frontend` | React SPA. Sadece gateway ile konuşur. |

### Sipariş verildiğinde ne oluyor

Kullanıcı "Satın Al"a bastığında sistem **beklemez**. `order-service` siparişi `PENDING` olarak kaydedip hemen cevap döner, gerisi arka planda mesajlarla ilerler:

```
1. order-service   : sipariş PENDING kaydedildi        → "order.created" mesajı
2. event-service   : kontenjanı rezerve etmeyi dener
                     başarılı → "quota.reserved"
                     yetersiz → "quota.rejected"
3. order-service   : QUOTA_RESERVED                    → "order.confirmed"
4. ticket-service  : QR + PDF biletleri üretir         → "ticket.generated"
5. order-service   : COMPLETED                         → "order.completed"
6. notification    : "biletleriniz hazır" maili
```

Arayüzdeki `SagaStepper` bileşeni bu adımları kullanıcıya canlı gösterir (2 saniyede bir sipariş durumunu sorar). Bu, mimariyi demo GIF'inde **görünür** kılar — bir izleyici sistemin dağıtık olduğunu ekranda fark eder.

### Bir şey ters giderse

Asıl mesele bu. İki senaryo var:

**Kontenjan yetersiz:** `event-service` rezervasyonu reddeder, sipariş `FAILED` olur. Henüz hiçbir şey değişmediği için geri alınacak bir şey yok.

**Bilet üretimi patlarsa:** Kontenjan çoktan düşülmüş durumda. Burada telafi devreye girer:

```
ticket.failed → order-service: sipariş FAILED  → "order.cancelled"
              → event-service: kontenjanı GERİ VER
              → notification : bilgilendirme maili
```

Yani "rollback" diye bir şey yok; **ters yönde bir iş** yapılıyor. Saga'nın özü bu.

## 4. Anlaman gereken dört kavram

### Saga pattern
Birden fazla servise yayılan bir işlemi, her biri kendi transaction'ı olan adımlara bölmek. Bir adım başarısız olursa, önceki adımların etkisini silen **telafi adımları** çalıştırılır. İki türü var:
- **Choreography** (bu projede): merkezî bir yönetici yok; her servis mesajı dinler, işini yapar, sonucu mesaj olarak yayınlar. Az servis için basit ve doğru seçim.
- **Orchestration**: merkezî bir orkestratör adımları tek tek çağırır. Çok adımlı karmaşık akışlarda tercih edilir.

*Neden choreography seçtim:* akış 4 adım ve doğrusal; merkezî orkestratör eklemek gereksiz bir servis ve tek hata noktası yaratırdı.

### Eventual consistency (nihai tutarlılık)
Sipariş `PENDING` döndüğü an sistem tutarsız durumdadır — sipariş var, bilet yok. Saniyeler içinde tutarlı hâle gelir. Klasik ACID'den farkı budur ve bilinçli bir takastır: karşılığında servisler birbirinden bağımsız çalışabilir, biri çökse bile mesajlar kuyrukta bekler.

### Idempotency (aynı işi iki kez yapmama)
RabbitMQ "en az bir kez teslim" garantisi verir — yani aynı mesaj iki kez gelebilir. Kontenjanın iki kez düşmesi kabul edilemez. Çözüm: her mesajda `sagaId` (= sipariş id'si) taşınır ve her consumer işi yapmadan önce "bunu daha önce yaptım mı?" diye bakar:
- `event-service`: `quota_reservations` tablosunda `order_id` **UNIQUE**
- `ticket-service`: `tickets` tablosunda `(order_id, seq_no)` **UNIQUE**

Veritabanı kısıtı, kod kontrolünden daha güvenilirdir — yarış durumunda da tutar.

### Dead Letter Queue (DLQ)
Bir mesaj 3 denemede de işlenemezse sonsuz döngüye girip kuyruğu tıkamaması için ayrı bir "ölü mektup" kuyruğuna alınır. Orada incelenir, düzeltilir, gerekirse elle yeniden işlenir.

## 5. Bilinçli olarak yapmadıkların (ve sebepleri)

Mülakatta "neden şunu yapmadın" sorusu gelir. Cevabı hazır olsun:

| Yapmadığın | Sebep |
|---|---|
| 2PC / XA distributed transaction | Servisleri birbirine kilitler, bir servis çökerse hepsi bloklanır. Mikroservis dünyasında pratikte kullanılmaz. |
| Merkezî saga orchestrator | 4 adımlık doğrusal akış için fazla ağır; ek servis ve tek hata noktası. |
| Servis başına ayrı PostgreSQL container | Şema ve kullanıcı ayrımı mantıksal izolasyonu zaten sağlıyor; 4 container lokal geliştirmeyi ağırlaştırırdı. README'de bu not var. |
| Kubernetes | Docker Compose kapsamı için yeterli. K8s öğrenme eğrisi projeyi bitirmeyi geciktirirdi. |
| Distributed tracing (Zipkin) | v2 notu olarak README'de duruyor — farkındalığı gösteriyor, kapsamı şişirmiyor. |
| Gerçek ödeme entegrasyonu | Domain'e bir şey katmıyor; sahte `PaymentService` yeterli. |

## 6. Mülakat soruları ve cevapları

**S: Neden mikroservis? Monolith daha basit olmaz mıydı?**
Bu ölçekte olurdu. Bunu mikroservis yapmamın sebebi dağıtık sistem problemlerini çözebildiğimi göstermek. Nitekim ikinci projemde (CoreBank) tam tersini savundum: para transferi tek ACID transaction'a sığdığı için orada modular monolith seçtim. Mimariyi domaine göre seçiyorum.

**S: Sipariş verildi ama bilet üretimi çöktü. Ne oluyor?**
`ticket-service` `ticket.failed` yayınlıyor. `order-service` siparişi `FAILED` yapıp `order.cancelled` yayınlıyor. `event-service` bunu dinleyip kontenjanı geri veriyor. Kullanıcıya bilgilendirme maili gidiyor. Bunun için özel bir test yazdım: telafi sonrası `sold_count` sipariş öncesi değerine dönüyor.

**S: Aynı mesaj iki kez gelirse?**
Her mesajda `sagaId` var. Consumer'lar veritabanı UNIQUE kısıtı ile korunuyor — `quota_reservations.order_id` ve `tickets(order_id, seq_no)`. İkinci mesaj işi tekrar yapmaz, sadece sonuç mesajını tekrar yayınlar. `SagaIdempotencyTest` bunu doğruluyor.

**S: RabbitMQ çökerse?**
Mesajlar persistent ve queue'lar durable, disk'te duruyor. RabbitMQ geri geldiğinde consumer'lar kaldığı yerden devam eder. O sırada gelen yeni siparişlerde publish başarısız olur ve kullanıcı hata alır — mesaj kaybolmaz. Gerçek üretimde bunun için transactional outbox pattern eklerdim; README'de v2 notu olarak var.

**S: Mesajı commit'ten önce yayınlasan ne olurdu?**
Sipariş rollback olsa bile `order.created` mesajı gitmiş olurdu; `event-service` var olmayan bir sipariş için kontenjan düşerdi. Bu yüzden publish'i `afterCommit` kancasına aldım.

**S: Kontenjan yarışını nasıl çözdün?**
`sold_count`'u okuyup sonra yazmıyorum — tek bir koşullu UPDATE:
`UPDATE events SET sold_count = sold_count + :qty WHERE id = :id AND sold_count + :qty <= total_quota`
Etkilenen satır 0 ise kontenjan yetersiz. Veritabanı satır kilidi işi çözüyor, uygulama tarafında kilit gerekmiyor. 10 paralel thread ve kontenjan 5 ile test ettim, tam 5 rezervasyon başarılı oluyor.

**S: Gateway JWT'yi doğruluyorsa servisler neden doğrulamıyor?**
Servisler compose ağının içinde, dışarıya port açmıyorlar — tek giriş gateway. Gateway doğruladıktan sonra `X-User-Id` ve `X-User-Role` header'larını ekliyor. Üretimde bu yeterli değil; servis-servis mTLS veya her serviste token doğrulaması gerekir. Bu takası README'de açıkça yazdım.

## 7. Projeyi CV'de nasıl yazacaksın

> **EventTix — Mikroservis Bilet Satış Platformu**
> Java 21, Spring Boot 3, Spring Cloud Gateway, Eureka, PostgreSQL, RabbitMQ, React, Docker
> Sipariş akışının 4 servise yayıldığı, dağıtık transaction probleminin choreography saga ile çözüldüğü platform. Telafi adımları, idempotent consumer'lar ve DLQ ile hata toleransı; Testcontainers ile uçtan uca saga ve telafi testleri.

Tek satırlık versiyon (LinkedIn başlığı vb.):
*"RabbitMQ üzerinde saga pattern ile dağıtık transaction çözen 6 servisli Spring Boot uygulaması."*

## 8. Bitirme kontrol listesi

- [ ] `docker compose up --build` temiz bir makinede tek seferde çalışıyor
- [ ] README'de mimari şeması ve saga akış diyagramı var
- [ ] Demo GIF'inde SagaStepper'ın adım adım ilerlediği görülüyor
- [ ] Telafi testinin çıktısı README'de ekran görüntüsü olarak var
- [ ] "Neden mikroservis / dağıtık transaction" başlığı README'de yazılı
- [ ] Repo açıklaması + topics dolu (`spring-boot`, `microservices`, `rabbitmq`, `saga-pattern`, `react`, `docker`)
- [ ] CoreBank repo'suna karşılıklı link verilmiş
- [ ] Repo pinlenmiş
- [ ] Commit sayısı 30+ ve mesajlar anlamlı
