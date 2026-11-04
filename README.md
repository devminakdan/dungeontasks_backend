# DungeonTasks Backend

Backend modular monolith для DungeonTasks. Реализованы Google ID token exchange,
собственные access/refresh JWT, независимые device sessions и профиль текущего пользователя.

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
из `.env`, и `JWT_SECRET` длиной не менее 32 UTF-8 байт. При изменении параметров БД
задайте также `DB_URL` и `DB_USERNAME`.
Для терминала с настройками из `.env.example`:

```sh
DB_PASSWORD=dungeontasks-local-only \
JWT_SECRET=replace-with-a-random-local-secret-at-least-32-bytes \
./gradlew bootRun
```

Gradle и приложение сами не читают `.env`; этот файл читает Docker Compose.

Для локальной компиляции и сборки executable JAR без запуска интеграционных тестов:

```sh
./gradlew bootJar
```

## Google Sign-In и JWT

`GOOGLE_WEB_CLIENT_ID` — Web OAuth Client ID того же Google Cloud project, который iOS
передаёт как server client ID. Без него backend безопасно запускается, но
`POST /api/v1/auth/google` возвращает `503 application/problem+json`. Проверка ID token
использует Google JWKS, только RS256, оба допустимых issuer (`accounts.google.com` и
`https://accounts.google.com`), ожидаемый audience, обязательный expiry и verified email.
Production flow не использует `tokeninfo` и не содержит test bypass.

`JWT_SECRET` обязателен при каждом запуске и должен содержать не менее 32 UTF-8 байт.
Значения из `.env.example` предназначены только для локальной разработки. Для реального
окружения создайте отдельный случайный секрет и не сохраняйте его в Git. Дополнительные настройки:

- `JWT_ISSUER` (default `dungeontasks-backend`);
- `JWT_AUDIENCE` (default `dungeontasks-ios`);
- `GOOGLE_JWK_SET_URI` (default официальный Google JWKS; меняется только для изолированных тестов).

Access JWT живёт до 15 минут и дополнительно ограничен абсолютным сроком session. Refresh JWT
одноразовый: успешный refresh заменяет его, не продлевая 30-дневный срок. Повторное использование
старого refresh отзывает всю session этого устройства. Sign out также отзывает только текущую session.
В PostgreSQL хранится SHA-256 hash текущего refresh JWT, а не сам токен.

## API фазы 2

- `POST /api/v1/auth/google` — обмен Google ID token на собственную пару токенов;
- `POST /api/v1/auth/refresh` — атомарная ротация refresh token;
- `POST /api/v1/auth/logout` — отзыв текущей bearer-session, ответ `204`;
- `GET /api/v1/users/me` — профиль текущего пользователя;
- `PUT /api/v1/users/me/username` — сохранить или изменить уникальный username;
- `POST /api/v1/users/me/username-suggestions` — получить предложение без сохранения.

Swagger UI доступен на `/swagger-ui.html`, OpenAPI JSON — на `/v3/api-docs`.
Ошибки прикладного API, MVC validation и Security возвращаются как
`application/problem+json`. Username имеет формат `^[a-z0-9]{3,24}$`; уникальность
обеспечивает PostgreSQL.

## Интеграционные тесты

`AuthProfileIntegrationTest` требует отдельный disposable PostgreSQL 17. Перед каждым тестом он выполняет
`TRUNCATE` таблиц `auth_session`, `external_identity` и `app_user`. Никогда не направляйте
`TEST_DB_URL` на локальную application database или БД с нужными данными.

Следующий сценарий создаёт отдельный контейнер со случайно уникализированным именем, ждёт готовности БД,
запускает полный `build`, а затем останавливает только созданный контейнер:

```sh
set -eu
DUNGEONTASKS_TEST_DB_CONTAINER="dungeontasks-phase2-test-postgres-$$"
trap 'docker rm -f "$DUNGEONTASKS_TEST_DB_CONTAINER" >/dev/null 2>&1 || true' EXIT

docker run --rm -d \
  --name "$DUNGEONTASKS_TEST_DB_CONTAINER" \
  -p 127.0.0.1:55432:5432 \
  -e POSTGRES_DB=dungeontasks \
  -e POSTGRES_USER=dungeontasks \
  -e POSTGRES_PASSWORD=phase2-test-db \
  postgres:17

until docker exec "$DUNGEONTASKS_TEST_DB_CONTAINER" \
  pg_isready -U dungeontasks -d dungeontasks; do
  sleep 1
done

TEST_DB_URL=jdbc:postgresql://localhost:55432/dungeontasks \
TEST_DB_USERNAME=dungeontasks \
TEST_DB_PASSWORD=phase2-test-db \
./gradlew build

docker stop "$DUNGEONTASKS_TEST_DB_CONTAINER"
trap - EXIT
```

Указанные `TEST_DB_URL`, `TEST_DB_USERNAME` и `TEST_DB_PASSWORD` совпадают с test defaults;
их можно опустить только при использовании точно такой же отдельной БД на `localhost:55432`.
Тест поднимает локальный JWKS HTTP endpoint и подписывает RS256 Google test tokens; это проверка verifier,
а не настоящий Google-вход.

## Решения и текущие ограничения

- Миграции: Liquibase; упоминание Flyway в Phase 1 исходного описания устарело.
- В текущей фазе реализован Google; Apple отложен. Парольного входа нет.
- Google account связывается только по стабильному `sub`; одинаковый email не объединяет аккаунты.
- Реальный сквозной Google-вход остаётся непроверенным до получения iOS и Web OAuth Client ID.
- Liquibase создаёт `app_user`, `external_identity` и `auth_session`. Игровых таблиц пока нет.
- Репозитории используют типизированные jOOQ `Table`/`Field`, объявленные в коде. Code generation
  не добавлен: текущей небольшой схеме не требуется отдельный доступ к БД на Gradle configuration stage.
- Optional WebSocket и Testcontainers пока не добавлены.
- Скилл координации и инструкции трёх ролей согласованы и находятся в общей и проектных `.codex`.
  Обязательные кодстайлы: `backend/.codex/codestyle.md` и `client/.codex/codestyle.md`
  относительно корня `bi-ios`; эти локальные файлы исключены из Git.
- Разработка функциональности идёт в порядке прямых команд пользователя.
  Фазы из `description.txt` — описание плана, а не команда начать реализацию.
