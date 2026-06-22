# Logistics Delivery Management System

Микросервисная система управления логистическими доставками.

**Стек:** Java 25 (LTS) · Spring Boot 4.1 · Spring Cloud 2025.x · Keycloak 26 · Kafka · PostgreSQL · Redis · ELK (
Elasticsearch + Logstash + Kibana)

---

## Архитектура

```
     ┌──────────────────┐          ┌──────────────────────────────────────────┐
     │    Keycloak      │  :9090   │               API Gateway                │  :8080
     │ Identity Provider│◄─────────│            Spring Cloud GW               │
     │  (OAuth2 / OIDC) │  JWKS    │            JWT validation                │
     └──────────────────┘          └───┬──────────┬──────────────┬────────────┘
                                       │ HTTP     │ HTTP         │ HTTP    │ HTTP
              ┌────────────────────────┘  ┌───────┘              │         └─────────────────┐
              │                           │                      │                           │
     ┌────────▼────────┐       ┌──────────▼──────────┐  ┌────────▼─────────┐  ┌──────────────▼───┐
     │  cargo-service  │       │  transport-service  │  │delivery-service  │  │reporting-service │
     │      :8081      │       │       :8082         │  │     :8083        │  │     :8085        │
     └────────▲────────┘       └──────────▲──────────┘  └────────┬─────────┘  └──────────▲───────┘
              │  HTTP                     │  HTTP                │                       │
              └───────────────────────────┘◄─────────────────────┤ Kafka                 │ Kafka
                                                                 ├───────────────────────┘
                                                                 │ Kafka
                                                  ┌──────────────▼──────────────┐
                                                  │    notification-service     │
                                                  │           :8084             │
                                                  └─────────────────────────────┘

     ┌─────────────────────────────────────────────────────────────────────────────────┐
     │                               Infrastructure                                    │
     │            PostgreSQL · Redis · Kafka · Eureka · Config Server · ELK            │
     └─────────────────────────────────────────────────────────────────────────────────┘
```

### Инфраструктурные сервисы

| Сервис              | Порт(ы)    | Описание                                            |
|---------------------|------------|-----------------------------------------------------|
| `keycloak`          | 9090       | Identity Provider — OAuth2/OIDC, пользователи, роли |
| `elasticsearch`     | 9200       | Хранилище логов ELK                                 |
| `logstash`          | 5000, 5044 | Сбор и обработка логов (TCP JSON + Beats)           |
| `kibana`            | 5601       | UI для поиска и анализа логов                       |
| `discovery-service` | 8761       | Eureka Server                                       |
| `config-service`    | 8888       | Spring Cloud Config — конфигурация                  |
| `api-gateway`       | 8080       | Единая точка входа, маршрутизация, JWT-валидация    |

### Бизнес-сервисы

| Сервис                 | Порт | БД         | Ответственность                                  |
|------------------------|------|------------|--------------------------------------------------|
| `cargo-service`        | 8081 | PostgreSQL | CRUD грузов, валидация веса/размеров             |
| `transport-service`    | 8082 | PostgreSQL | CRUD транспорта, управление статусом доступности |
| `delivery-service`     | 8083 | PostgreSQL | Доставки, расчёт стоимости                       |
| `notification-service` | 8084 | —          | Email/SMS уведомления через Kafka                |
| `reporting-service`    | 8085 | PostgreSQL | Агрегация отчётов, статистика                    |

---

## Технологический стек

### Core

| Компонент         | Версия / Технология                           |
|-------------------|-----------------------------------------------|
| Java              | 25 LTS                                        |
| Spring Boot       | 4.1                                           |
| Spring Framework  | 7 (Jakarta EE 11)                             |
| Spring Cloud      | 2025 (Gateway, Config, Eureka, OpenFeign)     |
| Spring Security   | 7 (OAuth2 Resource Server — JWT от Keycloak)  |
| Keycloak          | 26 (Identity Provider, OAuth2/OIDC, Admin UI) |
| Spring Data JPA   | 4                                             |
| Spring Data Redis | 4                                             |

### Инфраструктура

| Компонент      | Назначение                                         |
|----------------|----------------------------------------------------|
| PostgreSQL 18  | Реляционная БД (отдельная бд на каждый сервис)     |
| Apache Kafka   | Event streaming (доменные события между сервисами) |
| Redis 8        | кэш                                                |
| Docker         | Контейнеризация всех сервисов                      |
| Docker Compose | Локальный запуск всего стека                       |

### Наблюдаемость

| Компонент                | Назначение                                            |
|--------------------------|-------------------------------------------------------|
| Spring Boot Actuator     | Health, info, metrics эндпоинты                       |
| Elasticsearch 8          | Хранилище структурированных логов                     |
| Logstash 8               | Сбор логов по TCP, парсинг JSON, отправка в ES        |
| Kibana 8                 | Поиск, фильтрация, дашборды по логам                  |
| logstash-logback-encoder | JSON-форматирование логов, MDC-поля (traceId, spanId) |

### Сборка и качество

| Компонент      | Версия              |
|----------------|---------------------|
| Gradle         | 9 (Kotlin DSL)      |
| Lombok         | 1.18                |
| MapStruct      | 1.6                 |
| OpenAPI 3.1    | springdoc-openapi 3 |
| JUnit 5        | 5.12                |
| Testcontainers | 1.21                |

---

## Доменные события (Kafka)

Событийно-ориентированное взаимодействие через Apache Kafka.

### Топики

| Топик                      | Producer          | Consumer(s)                                                |
|----------------------------|-------------------|------------------------------------------------------------|
| `delivery.created`         | delivery-service  | notification-service, reporting-service                    |
| `delivery.status-changed`  | delivery-service  | notification-service, reporting-service, transport-service |
| `delivery.cancelled`       | delivery-service  | notification-service, transport-service                    |
| `transport.status-changed` | transport-service | delivery-service                                           |

---

## REST API

### Аутентификация — Keycloak

Keycloak предоставляет стандартные OIDC-эндпоинты. Клиенты взаимодействуют напрямую с Keycloak:

| Действие              | URL                                                                         |
|-----------------------|-----------------------------------------------------------------------------|
| Получить токен        | `POST http://localhost:9090/realms/logistics/protocol/openid-connect/token` |
| Обновить токен        | `POST .../token` с `grant_type=refresh_token`                               |
| Выйти                 | `POST .../logout`                                                           |
| JWKS (публичный ключ) | `GET .../certs`                                                             |

Регистрация пользователей — через Keycloak Admin Console (`http://localhost:9090/admin`) или Admin REST API.

### cargo-service `/api/v1/warehouse`

| Метод  | Путь    | Описание       |
|--------|---------|----------------|
| POST   | `/`     | Создать склад  |
| GET    | `/`     | Список складов |
| GET    | `/{id}` | Склад по id    |
| DELETE | `/{id}` | Удалить склад  |

### cargo-service `/api/v1/cargo`

| Метод  | Путь    | Описание      |
|--------|---------|---------------|
| POST   | `/`     | Создать груз  |
| GET    | `/`     | Список грузов |
| GET    | `/{id}` | Груз по id    |
| DELETE | `/{id}` | Удалить груз  |

### transport-service `/api/v1/transport`

| Метод | Путь           | Описание                    |
|-------|----------------|-----------------------------|
| POST  | `/`            | Создать транспорт           |
| GET   | `/`            | Список транспорта           |
| GET   | `/{id}`        | Транспорт по id             |
| PATCH | `/{id}/status` | Изменить статус доступности |

### delivery-service `/api/v1/delivery`

| Метод  | Путь           | Описание          |
|--------|----------------|-------------------|
| POST   | `/`            | Создать доставку  |
| GET    | `/`            | Список доставок   |
| GET    | `/{id}`        | Доставка по id    |
| PATCH  | `/{id}/status` | Обновить статус   |
| DELETE | `/{id}/cancel` | Отменить доставку |

### reporting-service `/api/v1/report`

| Метод | Путь         | Описание                                |
|-------|--------------|-----------------------------------------|
| GET   | `/by-status` | Количество доставок по статусам         |
| GET   | `/completed` | Список завершённых доставок             |
| GET   | `/revenue`   | Сумма доставок за период (`?from=&to=`) |

---

## Бизнес-правила

### Статус-машина доставки

```
CREATED ──► STORED ──► IN_TRANSIT ──► DELIVERED
   │           │             │
   └───────────┴─────────────┴──► CANCELLED
```

- `CANCELLED` → переход в другой статус запрещён
- Назначение транспорта: только если статус `AVAILABLE`
- Вес груза не может превышать грузоподъёмность транспорта
- После назначения транспорта: статус транспорта → `IN_USE`
- После завершения доставки: статус транспорта → `AVAILABLE`

### Расчёт стоимости

```
стоимость = 100 + (вес × 10) + (расстояние × 5)
```

---

## Безопасность

### Keycloak — настройка Realm

```
Realm: logistics
Clients:
  - logistics-gateway   (confidential, backend)
  - logistics-frontend  (public, SPA/Postman)
Roles:
  - ROLE_ADMIN
  - ROLE_MANAGER
  - ROLE_CLIENT
```

### Как работает аутентификация

```
Client ──► Keycloak ──► JWT ──► API Gateway ──► Microservice
                                   │
                            проверяет подпись
                            по JWKS Keycloak
```

### Правила

- JWT Bearer токен обязателен на всех эндпоинтах
- API Gateway валидирует токен через JWKS до маршрутизации
- Сервисы общаются между собой внутри Docker-сети без токена
- Отзыв токенов — через Keycloak logout endpoint

---

## Документация API (Swagger)

После запуска:

| Сервис            | URL                                   |
|-------------------|---------------------------------------|
| cargo-service     | http://localhost:8081/swagger-ui.html |
| transport-service | http://localhost:8082/swagger-ui.html |
| delivery-service  | http://localhost:8083/swagger-ui.html |
| reporting-service | http://localhost:8085/swagger-ui.html |
| Keycloak Admin    | http://localhost:9090/admin           |
| Kibana (логи)     | http://localhost:5601                 |
| Eureka Dashboard  | http://localhost:8761                 |

---

## Схема баз данных

Каждый бизнес-сервис — отдельная бд в одном PostgreSQL-инстансе.

### cargo-service → схема `cargo`

**Таблица `warehouse`**

| Колонка       | Тип            | Ограничения             |
|---------------|----------------|-------------------------|
| `id`          | UUID           | PK, NOT NULL            |
| `name`        | VARCHAR(200)   | NOT NULL                |
| `address`     | TEXT           | NOT NULL                |
| `city`        | VARCHAR(100)   | NOT NULL                |
| `capacity_kg` | DECIMAL(10, 2) | NOT NULL, CHECK (> 0)   |
| `created_at`  | TIMESTAMP      | NOT NULL, DEFAULT NOW() |

**Таблица `cargo`**

| Колонка        | Тип            | Ограничения                   |
|----------------|----------------|-------------------------------|
| `id`           | UUID           | PK, NOT NULL                  |
| `warehouse_id` | UUID           | NOT NULL, FK → `warehouse.id` |
| `name`         | VARCHAR(200)   | NOT NULL                      |
| `description`  | VARCHAR(500)   | NULLABLE                      |
| `quantity`     | INT            | NOT NULL, CHECK (> 0)         |
| `weight_kg`    | DECIMAL(10, 2) | NOT NULL, CHECK (> 0)         |
| `created_at`   | TIMESTAMP      | NOT NULL, DEFAULT NOW()       |

---

### transport-service → схема `transport`

**Таблица `transport`**

| Колонка       | Тип            | Ограничения                         |
|---------------|----------------|-------------------------------------|
| `id`          | UUID           | PK, NOT NULL                        |
| `type`        | VARCHAR(50)    | NOT NULL (TRUCK / VAN / MOTORCYCLE) |
| `model`       | VARCHAR(200)   | NOT NULL                            |
| `notes`       | VARCHAR(300)   | NULLABLE                            |
| `capacity_kg` | DECIMAL(10, 2) | NOT NULL, CHECK (> 0)               |
| `status`      | ENUM           | NOT NULL, DEFAULT 'AVAILABLE'       |
| `created_at`  | TIMESTAMP      | NOT NULL, DEFAULT NOW()             |

Enum `TransportStatus`: `AVAILABLE`, `IN_USE`, `MAINTENANCE`

---

### delivery-service → схема `delivery`

**Таблица `delivery`**

| Колонка               | Тип            | Ограничения                            |
|-----------------------|----------------|----------------------------------------|
| `id`                  | UUID           | PK, NOT NULL                           |
| `cargo_id`            | UUID           | NOT NULL (ссылка на cargo-service)     |
| `transport_id`        | UUID           | NULLABLE (ссылка на transport-service) |
| `recipient_name`      | VARCHAR(200)   | NOT NULL                               |
| `recipient_email`     | VARCHAR(200)   | NOT NULL                               |
| `recipient_phone`     | VARCHAR(20)    | NULLABLE                               |
| `origin_address`      | TEXT           | NOT NULL                               |
| `destination_address` | TEXT           | NOT NULL                               |
| `distance_km`         | DECIMAL(10, 2) | NOT NULL, CHECK (> 0)                  |
| `cost`                | DECIMAL(19, 2) | NOT NULL, DEFAULT 0                    |
| `status`              | ENUM           | NOT NULL, DEFAULT 'CREATED'            |
| `created_at`          | TIMESTAMP      | NOT NULL, DEFAULT NOW()                |
| `updated_at`          | TIMESTAMP      | NOT NULL                               |

Enum `DeliveryStatus`: `CREATED`, `STORED`, `IN_TRANSIT`, `DELIVERED`, `CANCELLED`

---

### reporting-service → схема `reporting`

**Таблица `delivery_snapshot`** (проекция событий из Kafka)

| Колонка           | Тип            | Ограничения      |
|-------------------|----------------|------------------|
| `id`              | UUID           | PK, NOT NULL     |
| `delivery_id`     | UUID           | NOT NULL, UNIQUE |
| `status`          | VARCHAR(20)    | NOT NULL         |
| `cost`            | DECIMAL(19, 2) | NOT NULL         |
| `recipient_email` | VARCHAR(200)   | NULLABLE         |
| `created_at`      | TIMESTAMP      | NOT NULL         |
| `completed_at`    | TIMESTAMP      | NULLABLE         |

> Данные приходят через Kafka-события `delivery.created` / `delivery.status-changed`.
> reporting-service не вызывает delivery-service по HTTP.

---

### notification-service

Stateless Kafka consumer. Получает событие → отправляет письмо → done.

---

## Межсервисное взаимодействие

### Синхронное (OpenFeign) — в рамках delivery-service

| Вызывающий сервис | Цель              | Когда                                                            |
|-------------------|-------------------|------------------------------------------------------------------|
| delivery-service  | cargo-service     | POST /deliveries — проверить вес груза                           |
| delivery-service  | transport-service | POST /deliveries — проверить статус транспорта                   |
| delivery-service  | transport-service | PATCH /deliveries/{id}/status → DELIVERED — освободить транспорт |

Все остальные взаимодействия — **асинхронные через Kafka**.

### Асинхронное (Kafka)

```
delivery-service ──► delivery.created          ──► notification-service
                                                ──► reporting-service

delivery-service ──► delivery.status-changed   ──► notification-service
                                                ──► reporting-service
                                                ──► transport-service (освобождение)

delivery-service ──► delivery.cancelled        ──► notification-service
                                                ──► transport-service

transport-service ──► transport.status-changed ──► delivery-service
```

---

## Роли пользователей

Keycloak realm `logistics` содержит 3 роли:

| Роль           | Кто                            |
|----------------|--------------------------------|
| `ROLE_ADMIN`   | Администратор системы          |
| `ROLE_MANAGER` | Логист / менеджер по доставкам |
| `ROLE_CLIENT`  | Клиент (получатель груза)      |

### Матрица прав

| Эндпоинт                              | CLIENT | MANAGER | ADMIN |
|---------------------------------------|--------|---------|-------|
| `GET /api/v1/warehouse`               | ✓      | ✓       | ✓     |
| `GET /api/v1/warehouse/{id}`          | ✓      | ✓       | ✓     |
| `POST /api/v1/warehouse`              |        |         | ✓     |
| `DELETE /api/v1/warehouse/{id}`       |        |         | ✓     |
| `GET /api/v1/cargo`                   | ✓      | ✓       | ✓     |
| `GET /api/v1/cargo/{id}`              | ✓      | ✓       | ✓     |
| `POST /api/v1/cargo`                  |        | ✓       | ✓     |
| `DELETE /api/v1/cargo/{id}`           |        | ✓       | ✓     |
| `GET /api/v1/transport`               | ✓      | ✓       | ✓     |
| `GET /api/v1/transport/{id}`          | ✓      | ✓       | ✓     |
| `POST /api/v1/transport`              |        | ✓       | ✓     |
| `PATCH /api/v1/transport/{id}/status` |        | ✓       | ✓     |
| `GET /api/v1/delivery`                | ✓      | ✓       | ✓     |
| `GET /api/v1/delivery/{id}`           | ✓      | ✓       | ✓     |
| `POST /api/v1/delivery`               | ✓      | ✓       | ✓     |
| `PATCH /api/v1/delivery/{id}/status`  |        | ✓       | ✓     |
| `DELETE /api/v1/delivery/{id}/cancel` | ✓      | ✓       | ✓     |
| `GET /api/v1/report/**`               |        | ✓       | ✓     |
