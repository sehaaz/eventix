-- Servis başına ayrı şema ve kullanıcı. Her kullanıcı sadece kendi şemasının sahibi.
\getenv auth_pw AUTH_DB_PASSWORD
\getenv event_pw EVENT_DB_PASSWORD
\getenv order_pw ORDER_DB_PASSWORD
\getenv ticket_pw TICKET_DB_PASSWORD

REVOKE ALL ON SCHEMA public FROM PUBLIC;

CREATE USER auth_user PASSWORD :'auth_pw';
CREATE USER event_user PASSWORD :'event_pw';
CREATE USER order_user PASSWORD :'order_pw';
CREATE USER ticket_user PASSWORD :'ticket_pw';

CREATE SCHEMA auth_schema AUTHORIZATION auth_user;
CREATE SCHEMA event_schema AUTHORIZATION event_user;
CREATE SCHEMA order_schema AUTHORIZATION order_user;
CREATE SCHEMA ticket_schema AUTHORIZATION ticket_user;
