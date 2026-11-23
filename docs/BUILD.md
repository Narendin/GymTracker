# Как собирать GymTracker

Порядок выпуска релиза и отправки в гит — `docs/RELEASE.md`.

## Что нужно

- Windows, PowerShell.
- Android SDK с API 35 (Android Studio поставит). Путь к SDK пишется в
  `local.properties` — файл создаёт Android Studio, он в `.gitignore`.
- JDK. Gradle 8.7, который использует проект, работает с Java 8–22.
  Java 25 (её ставит Android Studio) Gradle 8.7 не понимает: сборка падает
  сообщением `What went wrong: 25.0.4`. Рабочий JDK на этом компьютере —
  `C:\Users\Владимир\.jdks\jbr-21.0.11`.
- Обёртка Gradle лежит в репозитории (`gradlew.bat` и `gradle\wrapper\`),
  устанавливать Gradle не нужно: он скачается сам.

## secrets.properties

В корне проекта, в `.gitignore`. Без него сборка не идёт. Шаблон —
`secrets.properties.example`.

```
applicationId=<applicationId>
signing.keystore=<путь к ключу>
signing.storePassword=…
signing.keyAlias=<alias>
signing.keyPassword=…
```

- `applicationId` обязателен: без него сборка падает с текстом
  «Не найден applicationId…».
- Значение `applicationId` нигде не публикуется: не попадает в код,
  документацию, тексты issue, PR и релизов. В файлах репозитория пишется
  `<applicationId>`, значение известно только из `secrets.properties`.
- Четыре строки `signing.*` — для подписи релиза. Без них релизный APK
  собирается неподписанным и на телефон не ставится.
- Путь писать прямыми слэшами, файл читается как UTF-8 (иначе кириллица
  в пути превращается в недопустимые символы).

## Команды сборки

Все — из корня проекта `F:\Pets\GymTracker`. Для командной строки задай JDK:

```
$env:JAVA_HOME='C:\Users\Владимир\.jdks\jbr-21.0.11'
```

| Что собрать | Команда | Результат |
|---|---|---|
| Отладочный APK | `./gradlew.bat :app:assembleDebug` | `app\build\outputs\apk\debug\app-debug.apk` |
| Релизный APK | `./gradlew.bat :app:assembleRelease` | `app\build\outputs\apk\release\app-release.apk` |
| Юнит-тесты | `./gradlew.bat :app:test` | отчёт `app\build\reports\tests` |
| Отпечатки ключей | `./gradlew.bat :app:signingReport` | debug и release SHA1 |
| Очистка сборки | `./gradlew.bat :app:clean` | удаляет `app\build` |

Проверка, что релиз подписан:

```
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs `
  app\build\outputs\apk\release\app-release.apk
```

Должно быть: имя пакета `<applicationId>` (значение из `secrets.properties`),
SHA-1 релизного ключа. У неподписанного APK — `DOES NOT VERIFY`.

## Установка на эмулятор или телефон

```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r `
  app\build\outputs\apk\debug\app-debug.apk
```

Запуск:

```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 shell `
  am start -n "<applicationId>/com.gymtracker.MainActivity"
```

Имя компонента состоит из `applicationId` и пакета кода: код лежит в
`com.gymtracker`, а приложение на телефоне — `<applicationId>`.

Правила:
- `-r` ставит поверх, сохраняя данные: тот же `applicationId` и та же подпись.
- Релизный APK поверх debug-приложения не встанет (подпись другая): нужно
  удалить приложение, данные потеряются. Это правило спецификации.
- Обновление принимается только при `versionCode` больше установленного.

## Перед релизом

1. Поднять `versionCode` и `versionName` в `app\build.gradle.kts`.
2. Собрать APK и AAB.
3. Закоммитить изменения, поставить тег:
   `git tag -a v1.0.0 -m "GymTracker 1.0.0"`, отправить:
   `git push origin Master --tags`.
4. Описания изменений — `docs/CHANGELOG.md`, оно идёт в текст GitHub Release.
5. APK и AAB прикладываются к GitHub Release; в репозиторий бинарники не
   коммитятся (`build/` в `.gitignore`).

## Известные ловушки

- **Java 25**: сборка падает с Gradle 8.7. Ставь `JAVA_HOME` на JDK 21.
- **Кириллица в `secrets.properties`**: свойства читаются ISO-8859-1, путь
  ломается; файл читается как UTF-8, путь проверен.
- **Кракозябры в выводе консоли**: Gradle печатает UTF-8, консоль показывает
  палочками. Путь при этом правильный. Перед командой:
  `[console]::OutputEncoding=[System.Text.Encoding]::UTF8`.
- **Юнит-тесты в ограниченной среде** (песочница, CI с изоляцией):
  `:app:test` может падать на `ClassNotFoundException:
  GradleWorkerMain` — это среда, не код.
- **Gradle-демон завис**: `./gradlew.bat --stop`.
