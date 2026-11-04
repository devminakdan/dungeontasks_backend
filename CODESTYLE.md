# DungeonTasks — Backend Code Style (Java 21 / Spring Boot)

Данный документ содержит обязательные правила оформления серверного кода проекта DungeonTasks.
Смотри также общепроектный [../CODESTYLE.md](../CODESTYLE.md) и [../AGENTS.md](../AGENTS.md).

---

## 1. Стек и архитектура
- **Стек**: Java 21, Spring Boot, jOOQ, Liquibase, PostgreSQL 17, JUnit 5.
- **ЗАПРЕЩЕНО**: Использовать Lombok, JPA / Hibernate, Spring Data JPA.
- **Слои**: Feature-based modular monolith:
  ```text
  com.dungeontasks.<feature>
  ├── controller/   // REST, @Operation, @ApiResponses, DTO mapping
  ├── service/      // Бизнес-логика, @Transactional, Server-authoritative logic
  ├── repository/   // jOOQ DSLContext queries
  ├── domain/       // Внутренние рекорды и модели
  └── dto/          // request/ и response/
  ```

---

## 2. Именование идентификаторов: строго `*ID`
- Всегда используйте суффикс `ID` заглавными буквами в Java: `questID`, `userID`, `userItemID`, `raidID`.
- В JSON API ключи строго совпадают: `"questID"`, `"userID"`.
- В PostgreSQL (`snake_case`): `quest_id`, `user_id`.
- Никаких `questId`, `userId`, `id` в собственном коде.

---

## 3. Ошибки и RFC 7807 ProblemDetail
- Ошибки возвращаются как `ProblemDetail` со статусом и телом `application/problem+json`:
  - 400: `VALIDATION_FAILED`
  - 401: `UNAUTHORIZED`
  - 403: `FORBIDDEN`
  - 404: `RESOURCE_NOT_FOUND`
  - 409: `CONFLICT`
  - 500: `INTERNAL_ERROR`

---

## 4. База данных и Liquibase
- Любые изменения таблиц оформляются исключительно через XML-миграции в `backend/src/main/resources/db/changelog/changes/NNN-<feature>.xml`.
- Все внешние ключи обязаны иметь индексы.

---

## 5. Тестирование
- Перед коммитом всегда запускать:
  ```bash
  ./gradlew test
  ```
