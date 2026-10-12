# DungeonTasks Backend

Каркас modular monolith для DungeonTasks. Сейчас существует только стандартный
класс запуска Spring Boot. Прикладные API, модели, сервисы и тесты ещё не реализованы.

## Стек

- Java 21; Spring Boot 4.1.1; Gradle Wrapper 9.7.1 (Groovy DSL).
- Spring MVC (REST), Security, OAuth2 Client, OAuth2 Resource Server с JWT/JOSE.
- jOOQ, PostgreSQL 17, Liquibase, Bean Validation.
- springdoc-openapi 3.1.1 (OpenAPI / Swagger UI).
- Стандартные тестовые зависимости Spring Boot и JUnit Platform.

Версии Spring-зависимостей, jOOQ, JDBC-драйвера и Liquibase управляются Spring Boot BOM.

## Запуск в Docker

```sh
cp .env.example .env
docker compose up --build
```

Backend: `http://localhost:8080`. PostgreSQL: `localhost:5432`.
При занятом порте измените `BACKEND_PORT` или `POSTGRES_PORT` в `.env`.
С физического iPhone backend доступен по LAN-адресу компьютера и `BACKEND_PORT`.

```sh
docker compose logs -f backend
docker compose down
```

Данные PostgreSQL хранятся в named volume и сохраняются после `down`.
Пароль в `.env.example` предназначен только для локальной разработки.

## Запуск из IDE / Gradle

Нужен установленный JDK 21. Сначала запустите БД:

```sh
docker compose up -d postgres
```

В настройках запуска IDE задайте `DB_PASSWORD`, совпадающий с `POSTGRES_PASSWORD`
из `.env`. При изменении параметров БД задайте также `DB_URL` и `DB_USERNAME`.
Для терминала с настройками из `.env.example`:

```sh
DB_PASSWORD=dungeontasks-local-only ./gradlew bootRun
```

Gradle и приложение сами не читают `.env`; этот файл читает Docker Compose.

```sh
./gradlew build
```

## Согласованные решения и текущие ограничения

- Миграции: Liquibase; упоминание Flyway в Phase 1 исходного описания устарело.
- Вход: Google + Apple; приложение будет получать собственные access/refresh JWT.
  Логин с паролем из исходного описания не входит в согласованный вариант.
- Сейчас подключены только зависимости авторизации. Регистрации OAuth-провайдеров,
  ключи JWT, refresh tokens и правила доступа будут настроены отдельной задачей.
  Пока действует стандартная конфигурация Spring Security: HTTP-запрос может вернуть
  `401` или перенаправление на форму входа. Это не готовый OAuth/JWT flow.
- Подключена документация `/swagger-ui.html` и `/v3/api-docs`, но её доступность
  будет настроена вместе с Security. Прикладных endpoints пока нет.
- Liquibase changelog пуст: таблиц предметной области нет. Liquibase создаёт
  только свои служебные таблицы.
- jOOQ code generation будет настроен после появления схемы БД.
- Optional WebSocket и Testcontainers пока не добавлены.
- Скилл, `AGENTS.md` и `codestyle.md` для Swift и Spring будут согласованы позднее.
- Разработка функциональности идёт в порядке прямых команд пользователя.
  Фазы из `description.txt` — описание плана, а не команда начать реализацию.
