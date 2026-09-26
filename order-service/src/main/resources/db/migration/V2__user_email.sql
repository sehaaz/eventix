-- Bildirim mailleri için sipariş anındaki e-posta (JWT'den) kopyalanır.
-- Mevcut satırlar için geçici boş default, sonra kaldırılır.
ALTER TABLE orders ADD COLUMN user_email VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE orders ALTER COLUMN user_email DROP DEFAULT;
