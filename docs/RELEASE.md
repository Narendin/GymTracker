# Как выпустить релиз и отправить его в гит

Релиз = подписанный APK + коммит с номером версии +
тег + запись GitHub Release. Бинарники в репозиторий не коммитятся:
`build/` в `.gitignore`, файл прикладывается к GitHub Release.

Репозиторий: `github.com/Narendin/GymTracker`, ветка `Master`.

## 1. Подготовить содержимое

1. Убедиться, что фичи этого релиза работают на эмуляторе или телефоне.
2. Поднять номер в `app\build.gradle.kts`:
   - `versionCode` — целое, строго больше предыдущего (по нему телефон
     решает, можно ли установить обновление);
   - `versionName` — то, что видит человек, например `1.0.1`.
3. Добавить секцию в `docs\CHANGELOG.md` по формату Keep a Changelog:
   `## [1.0.1] — 2026-11-22`, разделы `Добавлено`, `Изменено`, `Удалено`.
   Описание для пользователя, не по файлам.
4. Обновить `docs\STATE.md`, если менялось описание состояния проекта.
5. Прогнать тесты: `./gradlew.bat :app:test` (в изолированной среде они
   могут падать на `GradleWorkerMain` — это среда, не код).

## 2. Собрать

Из корня проекта, с рабочим JDK (Java 25 Gradle 8.7 не понимает):

```
$env:JAVA_HOME='C:\Users\Владимир\.jdks\jbr-21.0.11'
./gradlew.bat :app:assembleRelease   # релизный APK
```

Результат:
- `app\build\outputs\apk\release\app-release.apk`

Подпись подставляется из `secrets.properties` (`signing.keystore`,
`signing.storePassword`, `signing.keyAlias`, `signing.keyPassword`).
Без этих строк APK получится `app-release-unsigned.apk` и на телефон
не встанет.

## 3. Проверить

```
$env:JAVA_HOME='C:\Users\Владимир\.jdks\jbr-21.0.11'; ./gradlew.bat :app:signingReport
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs `
  app\build\outputs\apk\release\app-release.apk
```

Проверить, что:
- имя пакета `<applicationId>` (значение из `secrets.properties`);
- `versionCode` и `versionName` — новые;
- SHA-1 подписи — релизного ключа, а не debug (отпечаток debug-ключа в
  документации не пишется, он берётся из `./gradlew.bat :app:signingReport`).

Поставить на устройство:

```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 install -r `
  app\build\outputs\apk\release\app-release.apk
```

Релизный APK поверх debug-приложения не установится — подписи разные.
Нужно удалить приложение, данные при этом теряются. Это правило спецификации:
отладочную сборку нельзя обновить релизной.

## 4. Отправить в гит

```
git add -A
git commit -m "chore: release GymTracker 1.0.1"   # сообщение на английском
git push origin Master
git tag -a v1.0.1 -m "GymTracker 1.0.1"
git push origin v1.0.1
```

Отправка на GitHub требует аутентификации: по HTTPS GitHub не принимает
пароль от аккаунта, нужен личный токен (Settings → Developer settings →
Personal access tokens, с правом `repo`) или SSH-ключ. Первый запрос
графическая оболочка GitHub покажет.

## 5. Запись GitHub Release

1. Открыть `github.com/Narendin/GymTracker` → **Releases** → **New release**.
2. Tag: создать `v1.0.1` из последнего коммита ветки `Master`
   (тег из шага 4 уже будет в списке).
3. Заголовок: `GymTracker 1.0.1`.
4. Описание: текст секции `CHANGELOG.md` этой версии. Кнопка
   *Generate automatic release notes* генерирует из коммитов и pull request'ов
   — это список кода, ручной текст полезнее.
5. Приложить файл: `app-release.apk` (прямая установка).
6. Снять «Set as a pre-release», опубликовать (**Publish release**).

## Правила

- `versionCode` только растёт; задним числом не правится.
- Тег релиза — `v` + `versionName` (`v1.0.1`): приложение сравнивает номер из
  тега последней записи релизов с текущей версией, другой формат тег сравнение
  ломает. APK в релизе обязан быть единственным приложенным файлом с расширением
  `.apk` — приложение берёт ссылку на него.
- Релизный ключ (файл `<.jks>`, путь из `secrets.properties`) —
  пожизненно: без него обновления того же приложения выпустить нельзя.
  Он не в репозитории, не передаётся, пароли в `secrets.properties`.
- В репозиторий не попадают: `build/`, `local.properties`,
  `secrets.properties`, `keystore/`, `*.jks`, `shots/`.
- Перед отправкой смотреть, что `git add -A` берёт:
  `git status --short`, и `git grep -I -n <значение applicationId>` —
  идентификатор в публичных файлах быть не должен.
