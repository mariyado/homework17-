# Обратная связь по архитектуре проекта

Дата: 2026-10-21. Основано на фактическом состоянии репозитория (`git ls-files`, `build.gradle`, прогон `.\gradlew.bat test`).

## Что есть сейчас

Один Gradle-модуль `homework17`, только тестовый код (`src/test/java`), два независимо развивавшихся набора тестов:

```
src/test/java/
  serve_rest_test/
    ServeRestTest.java        # 8 API-тестов, RestAssured, https://serverest.dev
    dto/UserRequest.java      # появился при рефакторинге shouldCreateNewUser
  ru/alfabank/day19/
    pages/                    # Selenide page objects (4 класса)
    tests/LoginTest.java      # 2 UI-теста, https://the-internet.herokuapp.com
```

Плюс `build.gradle`, `gradle/wrapper`, `AGENTS.md`. CI, README, конфигурации линтеров и форматтеров нет.

## Что сделано хорошо

1. **Разделение API и UI по пакетам** уже есть, и оба набора запускаются одной командой `.\gradlew.bat test`.
2. **Page Object pattern** в UI-части выдержан: `StartPage` / `LoginPage` / `SecurePage` / `ErrorLoginPage` без `WebDriver` в тестах, локаторы спрятаны.
3. **Стиль RestAssured** — канонический `given().when().then()` с Hamcrest-матчерами, читается как спецификация.
4. **`enableLoggingOfRequestAndResponseIfValidationFails()`** — правильный дефолт, при падении в логе есть запрос/ответ.
5. `AGENTS.md` фиксирует неочевидное (порядок тестов, дрейф данных, версии зависимостей) — это экономит время следующим сессиям.

## Проблемы по убыванию влияния

### 1. Тесты не изолированы: скрытое состояние между тестами

`ServeRestTest` — это по сути один сценарий, разрезанный на 8 методов через `static String userId` / `static String token` и `@TestMethodOrder(OrderAnnotation)`:

- нельзя запустить `shouldUpdateUser` отдельно — упадёт NPE/400, потому что `userId` задаётся в тесте №3;
- падение теста №3 каскадно ломает №4–6, диагноз по отчёту становится ложным («сломан логин», хотя сломалось создание);
- порядок в JUnit — это зависимость, а не требование продукта: `OrderAnnotation` маскирует отсутствие фикстур.

**Рекомендация.** JUnit 5 по умолчанию создаёт новый экземпляр класса на каждый тест — статику можно убрать полностью:

```java
private String userId;   // обычное поле, не static
private String token;

@BeforeEach
void createUser() {
    userId = usersClient.create(uniqueUser()).id();
}

@AfterEach
void deleteUser() {
    usersClient.deleteQuietly(userId, token);  // try/finally, 200/400
}
```

Сквозной сценарий «создал → изменил → залогинился → удалил», если он нужен как демонстрация, оставить одним тестом с `@Tag("e2e")`.

### 2. Данные теста захардкожены и живут дольше теста

- Email `kj232d43ie8hgyge@qa.com` создаётся в №3 и удаляется в №6. Прерванный прогон (Ctrl+C, таймаут, первое падение до №6) оставляет пользователя на публичном инстансе, и следующий `POST /usuarios` получает 400 «Este email já está sendo usado». Тест начинает падать «на пустом месте», лечится только ручным удалением.
- Пароль и email дублируются строкой в трёх тестах (`shouldCreateNewUser`, `shouldUpdateUser`, `shouldLogin`) — изменение в одном месте не доедет до остальных.
- `shouldGetAllProducts` проверяет наличие продукта `"Awesome Plastic Gloves"` в общем каталоге. Этого продукта больше нет — тест падает по причине, не связанной с кодом.

**Рекомендация:**

- email — `"qa-" + UUID.randomUUID() + "@qa.com"` на прогон, а не константа;
- один объект-фикстура `UserRequest` на тест, переиспользуемый в create/update/login;
- проверять **инварианты API**, а не конкретные записи чужой БД: `quantidade == produtos.size()`, `preco > 0`, `nome` непустой, `_id` уникальны. Проверки на сид-данные — только если у проекта есть свой инстанс ServeRest, который вы контролируете (тогда `docker run` локально, а не `serverest.dev`).

### 3. Нет слоя доступа к API: HTTP-вызовы, ассерты и данные в одном методе

`ServeRestTest` — 180 строк, где на один метод приходится: сборка payload, вызов, 3–5 ассертов, извлечение значения в статик. Отсюда:

- `RestAssured.baseURI` и логирование задаются глобально в `@BeforeAll` — при переходе на параллельный запуск или второй стенд это ломается молча;
- португальские сообщения (`"Cadastro realizado com sucesso"`) разбросаны по ассертам без единого места правки;
- нет `RequestSpecification`/`ResponseSpecification`, поэтому `contentType(ContentType.JSON)` повторяется в каждом тесте;
- ответы читаются через `path("...")` и magic-строки вместо типизированных DTO — `UserRequest` появился только для запроса.

**Рекомендация — минимальная слоистость, без фабрик и абстракций ради абстракций:**

```
ru/alfabank/serverest/
  config/ApiConfig.java        # baseURI из -Dapi.url, дефолт https://serverest.dev
  spec/Specs.java              # requestSpec(): baseUri, contentType, log().ifValidationFails()
  client/UsersClient.java      # create/update/delete/findById -> Response/UserResponse
  client/LoginClient.java
  client/ProductsClient.java
  dto/UserRequest, UserResponse, LoginRequest, LoginResponse
  tests/...                    # только ассерты и сценарий
```

Тест тогда читается как требование, а не как HTTP-трассировка:

```java
UserResponse created = usersClient.create(request);
assertThat(created.id()).isNotBlank();
```

Это же снимает дублирование `contentType` и позволяет один раз поменять базовый URL через `-Dapi.url=http://localhost:3000`.

### 4. Смешение ответственности в одном тесте (и в одном page object)

- `shouldDeleteUser` проверяет две вещи: успешное удаление и то, что GET после удаления отдаёт `400`. Это два теста (или один, но с явным вторым шагом в названии).
- `LoginPage.assertComponentsVisible()` проверяет ссылку «Elemental Selenium» — это футер/сайдбар сайта, не относящийся к логину. Такой ассерт падает при редизайне страницы и ничего не говорит о функциональности логина.
- `ErrorLoginPage.assertFlashHasText()` держит ожидаемый текст внутри page object. Page object должен отдавать состояние (`flashText()`), а ожидание — жить в тесте; иначе «что проверяем» размазано по двум файлам.
- В `LoginTest` оба теста дублируют 5 строк навигации — просится `@BeforeEach`.

### 5. `build.gradle` не отражает реальность

- JUnit объявлен дважды: BOM `6.0.0` и явный `junit-jupiter:5.10.2`. Побеждает BOM (проверено `dependencies`), то есть вторая строка — мусор, вводящий в заблуждение.
- `jackson-databind` объявлен как `2.17.0`, но `allure-commandline` тянет `2.22.2`; реальная версия не та, что написана.
- `selenide` и `allure-commandline` подключены как `implementation`, а не `testImplementation` — из-за этого Gradle создаёт `compileJava`/`classes` для несуществующих main-исходников и тянет тестовые библиотеки в «продакшн»-конфигурацию.
- `allure-commandline` — это CLI-упаковка, а не интеграция. Allure нигде не используется: нет `allure-junit5`, нет `@Step`, нет вложений скриншотов. Зависимость весит десятки мегабайт и не даёт ничего.
- Нет `testLogging`, нет `systemProperty`-проброса, нет разделения быстрых API-тестов и медленных UI-тестов: `.\gradlew.bat test` всегда поднимает браузер.

**Рекомендация:**

```groovy
dependencies {
    testImplementation platform('org.junit:junit-bom:6.0.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testImplementation 'io.rest-assured:rest-assured:5.5.0'
    testImplementation 'com.fasterxml.jackson.core:jackson-databind:2.17.0'
    testImplementation 'com.codeborne:selenide:7.18.0'
    // allure-commandline убрать или заменить на allure-junit5 + aspectjweaver
}

tasks.named('test') {
    useJUnitPlatform { excludeTags 'ui' }        // быстрый дефолт
    systemProperty 'api.url', System.getProperty('api.url', 'https://serverest.dev')
    testLogging { events 'failed'; exceptionFormat 'full' }
}

tasks.register('uiTest', Test) {                  // отдельная задача для браузера
    useJUnitPlatform { includeTags 'ui' }
}
```

### 6. Публичный общий стенд как единственное окружение

`serverest.dev` и `the-internet.herokuapp.com` — чужие инстансы с общими данными: другие люди создают и удаляют записи параллельно с вами. Любой тест, опирающийся на содержимое БД (наличие продукта, количество пользователей), нестабилен by design. Плюс внешняя сеть: офлайн/флапающий DNS = красный прогон.

**Рекомендация:** для API поднять ServeRest локально в Docker (`POST /usuarios` без ограничений, чистая БД, детерминизм), публичный стенд оставить как smoke. Для UI зафиксировать версию сайта или использовать локальный `the-internet` контейнер, если он есть.

### 7. Именование и структура пакетов

- `serve_rest_test` — snake_case в корневом пакете, рядом с `ru.alfabank.day19` (компания + номер дня курса). Два разных стиля в одном модуле.
- `day19`, `homework17` привязывают код к расписанию занятий: через месяц непонятно, что внутри, а перенос «дня» в другой модуль ломает пути.
- `ServeRestTest` — свалка из 8 несвязанных сценариев (users, login, products); при росте это будет 500 строк.

**Рекомендация:** `ru.alfabank.serverest.tests.users`, `.tests.login`, `.tests.products`; UI — `ru.alfabank.theinternet.pages/.tests`. Классы по теме: `UserCrudTest`, `UserSearchTest`, `LoginTest`, `ProductSearchTest`.

### 8. Нет автоматизации качества и документации

- Нет CI: ни один коммит не проверяется, красный/зелёный статус существует только на машине автора.
- Нет README: как запустить, какие стенды нужны, что означают падения — всё это сейчас только в `AGENTS.md`, который читают агенты, а не люди.
- Нет `.editorconfig`/форматтера: отступы в `ServeRestTest` уже «пляшут» (`.when()` то с отступом, то без; пустая строка между `@Order(4)` и сигнатурой метода).

**Рекомендация:** GitHub Actions с JDK 25, `./gradlew test` + `./gradlew uiTest` в отдельных job'ах (UI — headless Chrome); README на 15 строк; `.editorconfig` с 4 пробелами.

## Приоритетный план

| Приоритет | Действие | Эффект |
|-----------|----------|--------|
| 1 | Убрать порядок и статику: `@BeforeEach` создаёт пользователя, `@AfterEach` удаляет; email — UUID | тесты запускаются по одному, нет каскадных падений и «залипших» email |
| 2 | Заменить проверки сид-данных на инварианты (`shouldGetAllProducts`, `shouldFindProductsByName`) | прогон перестаёт быть красным из-за чужих данных |
| 3 | Ввести `client/` + `dto/` + `Specs` для API | тесты читаются как требования, base URL меняется флагом |
| 4 | Почистить `build.gradle`: дубликаты, `testImplementation`, теги `ui`, `testLogging` | быстрый дефолтный прогон, честные версии |
| 5 | Локальный ServeRest (Docker) + `-Dapi.url` | детерминизм, независимость от публичного стенда |
| 6 | CI + README + `.editorconfig` | регрессии видны сразу, проект понятен новому человеку |

## Чего делать не стоит

- Тянуть за собой фреймворки (TestNG, Cucumber, свой DSL) — при 10 тестах это удвоит объём кода без выгоды.
- Делать «универсальные» page objects с фабриками и наследованием на 4 страницах.
- Оставлять `allure-commandline` «на будущее»: либо интеграция с `@Step`/скриншотами, либо удаление.
- Чинить дрейф данных правкой ожидаемого значения (`"Awesome Plastic Gloves"` → актуальное имя): через неделю имя снова изменится, а причины падения никто не поймёт.
