---
name: git-workflow
description: "Git workflow for GymTracker: issue, branch, pre-commit checks, commit, PR, merge, cleanup, release. Use for any commit, push, PR, issue, merge, branch cleanup, release."
---

# Git-workflow GymTracker

Порядок работы с git: от согласования изменения до мержа и уборки.
Репозиторий: `github.com/Narendin/GymTracker`, базовая ветка `Master`.
Выпуск релиза — `docs/RELEASE.md`, сборка — `docs/BUILD.md`.

## Железные правила

1. **Никто не коммитит в Master.** Всё — через ветку от Master и PR.
   Прямой коммит в Master — исключение, о котором пользователь сказал заранее.
2. **Коммит — только по явному указанию пользователя.** Не «по завершении
   вехи», не из вежести. Подготовь сообщение и спроси.
3. **PR никогда не мержи сам.** Создай PR, дай ссылку — мержит пользователь.
4. **Каждая задача — по GitHub issue.** PR без issue не создаются: сначала
   issue, потом ветка, потом PR.
5. **Сообщение коммита — английский, императив, ≤ 20 слов**
   («Add primary muscles to journal rows»). Русский — в коде, документации,
   обсуждениях и текстах issue, PR и релизов.
6. **Документация по задаче — в PR этой задачи:** правятся все файлы
   документации, описывающие то, что задача меняет (перечень файлов —
   раздел «Документация» `AGENTS.md`). PR без своей документации не мержат.
7. **Секреты в коммит не попадают.** Проверь staged перед коммитом.

## Шаг 1. Issue (для изменений кода)

- Согласуй текст по-русски; **название и тело issue — на русском**.
- После подтверждения пользователя:
  `& "C:\Program Files\GitHub CLI\gh.exe" issue create -t "<Title>" -b "<body>"`
- Запиши номер issue — он нужен в теле PR.

## Шаг 2. Ветка

- `git checkout Master; git pull` — старт с актуального Master.
- `git checkout -b <branch>` — имя по сути изменения
  (`journal-muscles`, `fix-delete-confirm`). Одна ветка = одна тема.

## Шаг 3. Подготовка перед коммитом (обязательный чек-лист)

Из корня проекта `F:\Pets\GymTracker`. JDK:
`$env:JAVA_HOME='C:\Users\Владимир\.jdks\jbr-21.0.11'` (Java 25 Gradle 8.7
не понимает).

1. **Сборка отладочной версии:**
   `./gradlew.bat :app:assembleDebug` — 0 ошибок.
2. **Юнит-тесты:** `./gradlew.bat :app:test`. В изолированной среде они
   могут падать на `ClassNotFoundException: GradleWorkerMain` — это среда,
   не код; в обычной среде тесты должны проходить.
3. **Комментарии в коде — по-русски, по сути.** Ссылки на `docs/...`
   допустимы, если они указывают источник (например палитра из
   `docs/mockups-mvp.html` в `ui/theme/Color.kt`).
4. **Секреты.** Проверить, что в коммит не идут:
   - `secrets.properties` (значение `applicationId`, пароли, путь к ключу),
   - `*.jks`, `*.keystore`, `keystore/`,
   - `local.properties`, `build/`, `shots/`, `.idea`.
   Контроль:
   `git status --short` и
   `git grep -I -n <значение applicationId>` — 0 совпадений в файлах,
   которые попадают в репозиторий.
5. `git status --short` — в diff только то, что ожидается; временные файлы
   (дампы uiautomator, снимки экранов, отчёты проверок) удалить, в коммит не брать.
6. **Изменение поведения приложения проверено на эмуляторе/телефоне**
   (`emulator-5554`, установка `adb install -r`, снимок экрана).
7. **Проверка на чистом эмуляторе — обязательна перед коммитом.**
   Проверка поверх установленного приложения не считается: она не проверяет
   создание базы и посев каталога. Падение при чистой установке (проверено на
   примере: посев INSERT без поля `isHidden` — `NOT NULL constraint failed`,
   приложение закрывалось через пару секунд) проявляетcя только на пустом месте.
   - `adb -s <device> shell pm list packages | Select-String 'gymtracker'` —
     приложения с id `dev.vlads.gymtracker.a74f7c77` быть не должно; если оно
     есть, удалить: `adb -s <device> shell pm uninstall dev.vlads.gymtracker.a74f7c77`
     (данные удаляются).
   - Ставить без `-r`: `adb -s <device> install app\build\outputs\apk\release\app-release.apk`.
   - Запуск: `adb -s <device> shell am start -n "dev.vlads.gymtracker.a74f7c77/com.gymtracker.MainActivity"`.
   - Проверить, что приложение живое: `adb -s <device> shell pidof dev.vlads.gymtracker.a74f7c77`
     дважды с интервалом (значения совпадают).
   - Проверить лог на падение: `adb -s <device> logcat -d | Select-String 'FATAL EXCEPTION|SQLiteConstraint'`
     — 0 совпадений.
   - Снимок экрана: экран «Тренировка» с датой, счётчиками и вкладками.
   - Релизный APK проверять тем же способом: он собирается тем же кодом,
     и чистая установка — путь, которым его получит пользователь.

## Шаг 4. Коммит и push

- `git add <файлы>; git commit -m "<английская строка>"`
- `git push -u origin <branch>`
- Предупреждения CRLF→LF — норма Windows, игнорировать.

## Шаг 5. PR

- `& "C:\Program Files\GitHub CLI\gh.exe" pr create -B Master -H <branch> `
  `-t "<название issue>" -b "Closes #N + краткое описание по-русски"`
  (первая строка — `Closes #N`: GitHub связывает issue и закрывает её при
  мерже только по английскому ключевому слову; остальной текст по-русски).
- Дать пользователю ссылку на PR. **Не мержить.**

## Шаг 6. Мерж и уборка

- Мержит пользователь. При мерже в UI GitHub есть галочка
  **«Delete branch after merging»** — попросить оставить её. Глобально
  репозиторий включает Settings → General → «Automatically delete head branches».
- Если ветка после мержа осталась:
  `git push origin --delete <branch>`
- Локально после мержа: `git checkout Master; git pull` и
  `git branch -d <branch>`.

## Релиз

Релиз — отдельная процедура, она описана в `docs/RELEASE.md`: версия
(`versionCode` строго растёт), секция `docs/CHANGELOG.md`, подписанный APK
из `secrets.properties`, тег `v1.0.1`, GitHub Release с приложенным APK.
Текст GitHub Release — по-русски. Релизный коммит идёт по тому же правилу:
ветка → PR → мерж пользователем.

## Практические детали

- **gh CLI лежит в `C:\Program Files\GitHub CLI\gh.exe` и не прописан в PATH.**
  Проверка входа: `& "C:\Program Files\GitHub CLI\gh.exe" auth status`
  (аккаунт `Narendin`, протокол ssh, токен с правом `repo`).
- Состояние PR: `gh pr view N --json state,mergedAt,headRefName`.
  Удалять ветку безопасно только после `state: MERGED`.
- Локальная ветка удаляется через `git branch -d` (не `-D`, если не сливалась).
- Имя ветки в репозитории — `Master` с большой буквы; `main` нет.
- Перед отправкой смотреть, что `git add -A` берёт — список файлов коммита.
