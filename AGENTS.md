# AGENTS.md

## Project

Gradle repo `homework17` (group `ru.alfabank`, version `1.0-SNAPSHOT`). It contains two unrelated black-box test suites, both hitting **live public sites** — no mocks, no local server, no `src/main`.

- API: `src/test/java/serve_rest_test/ServeRestTest.java` — RestAssured against https://serverest.dev
- UI: `src/test/java/ru/alfabank/day19/` — Selenide page objects in `pages/`, tests in `tests/LoginTest.java`, against https://the-internet.herokuapp.com

## Commands

Run from the repo root with PowerShell (`.\gradlew.bat`, not `gradle`):

- All tests: `.\gradlew.bat test`
- One class: `.\gradlew.bat test --tests "serve_rest_test.ServeRestTest"`
- One method: `.\gradlew.bat test --tests "ru.alfabank.day19.tests.LoginTest.successfulLoginTest"`
- HTML report: `build/reports/tests/test/index.html`; raw XML in `build/test-results/test/`

Gotcha: on JDK 25 the wrapper writes JVM native-access warnings to stderr, which PowerShell surfaces as `NativeCommandError` / exit code 1 even when the build prints `BUILD SUCCESSFUL`. Judge the result by the build log and test results, not by the shell exit code.

## Requirements

- JDK 25 (the only installed JDK; `build.gradle` declares no toolchain, so Gradle uses whatever JDK it runs under — do not add a lower toolchain).
- Network is required; every test calls a public endpoint on each run.
- UI tests need a real browser; Selenide's driver manager downloads chromedriver on first run.

## Testing gotchas (verified)

- `ServeRestTest` is order-dependent (`@TestMethodOrder(OrderAnnotation)`): `userId` / `token` are threaded from create → update → login → delete, so a single `@Order` method cannot be run in isolation.
- It creates a user with a hardcoded email `kj232d43ie8hgyge@qa.com` and deletes it in `@Order(6)`. A run that aborts before delete leaves the email registered, and the next `POST /usuarios` returns 400, failing tests 3–5. Free that email via the API instead of changing the test data.
- Assertions depend on data seeded in shared public instances and drift. Last run: `shouldGetAllProducts` fails at `ServeRestTest.java:152` (expects product "Awesome Plastic Gloves"); the other 6 API tests and both `LoginTest` methods pass. Treat such failures as environment data, not as bugs to patch blindly.
- `serve_rest_test` is a package directly under the source root; keep the package declaration when moving or renaming the file.

## Dependencies

- Declared versions are not the resolved ones: the JUnit BOM 6.0.0 wins over the explicit `junit-jupiter:5.10.2`, and `allure-commandline:2.46.0` upgrades `jackson-databind` from 2.17.0 to 2.22.2. Check with `.\gradlew.bat dependencies --configuration testRuntimeClasspath` before assuming a version.
- Selenide and Allure are declared as `implementation` rather than `testImplementation`, which is why `compileJava`/`classes` exist despite having no main sources. Add new test-only libraries as `testImplementation`.
