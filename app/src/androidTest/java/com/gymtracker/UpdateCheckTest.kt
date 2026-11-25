package com.gymtracker

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gymtracker.di.AppContainer
import com.gymtracker.settings.ThemeMode
import com.gymtracker.ui.GymApp
import com.gymtracker.ui.settings.TAG_UPDATE_ROW
import com.gymtracker.ui.theme.GymTrackerTheme
import com.gymtracker.update.Release
import com.gymtracker.update.UpdateChecker
import com.gymtracker.update.UpdateResult
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Автотесты строки «Проверка обновлений» в настройках D1.
 *
 * Ответ релизной стороны подменяется: приложение в тесте не выходит в сеть,
 * а состояния «есть версия», «актуальна» и «сеть недоступна» задаются
 * фиксированным сценарием. Контейнер собирается на целевом контексте,
 * экран рисуется напрямую — проверка обновлений не зависит от данных журнала.
 *
 * Саму установку автотест не проверяет: системный установщик лежит вне
 * приложения, он проверяется вручную на телефоне.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UpdateCheckTest {

    @get:Rule
    val compose = createComposeRule()

    /** Фиксированный ответ проверки: сеть в тесте недоступна. */
    private class FakeChecker(private val result: UpdateResult) : UpdateChecker {
        override suspend fun check(currentVersion: String): UpdateResult = result
    }

    /** Запустить приложение с заданным ответом проверки обновлений. */
    private fun startWith(result: UpdateResult) {
        val container = AppContainer(
            InstrumentationRegistry.getInstrumentation().targetContext,
            FakeChecker(result),
        )
        compose.setContent {
            GymTrackerTheme(ThemeMode.SYSTEM) {
                GymApp(container)
            }
        }
    }

    /** Перейти на экран настроек. */
    private fun openSettings() {
        compose.onNodeWithContentDescription("Настройки").performClick()
        displayed("Настройки")
    }

    private fun displayed(text: String) {
        compose.waitUntil(30_000) { compose.onNodeWithText(text).isDisplayed() }
    }

    /** Текст как подстрока: сообщение диалога целиком длиннее ожидаемой строки. */
    private fun displayedIn(text: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tapText(text: String) {
        compose.waitUntil(30_000) { compose.onNodeWithText(text).isDisplayed() }
        compose.onNodeWithText(text).performClick()
    }

    /** Текущая версия установленного приложения. */
    private val appVersion: String =
        InstrumentationRegistry.getInstrumentation().targetContext.packageManager
            .getPackageInfo(
                InstrumentationRegistry.getInstrumentation().targetContext.packageName, 0,
            ).versionName!!

    /** s1. Строка активна и показывает текущую версию до проверки. */
    @Test
    fun s1_rowShowsCurrentVersionAndIsClickable() {
        startWith(UpdateResult.UpToDate)
        openSettings()

        compose.onNodeWithTag(TAG_UPDATE_ROW).assertIsDisplayed()
        compose.onNodeWithTag(TAG_UPDATE_ROW).assert(hasClickAction())
        // Текущая версия стоит в строке проверки и в строке «Версия».
        compose.onAllNodesWithText(appVersion).assertCountEquals(2)
    }

    /** s2. Сеть недоступна: диалог с повтором, строка показывает отказ. */
    @Test
    fun s2_networkUnavailableState() {
        startWith(UpdateResult.NetworkError)
        openSettings()

        compose.onNodeWithTag(TAG_UPDATE_ROW).performClick()
        displayed("Сеть недоступна")
        tapText("Закрыть")
        displayed("сеть недоступна")
    }

    /** s3. Версия актуальна: диалог подтверждает, строка показывает «актуальна». */
    @Test
    fun s3_upToDateState() {
        startWith(UpdateResult.UpToDate)
        openSettings()

        compose.onNodeWithTag(TAG_UPDATE_ROW).performClick()
        displayed("Версия актуальна")
        tapText("Закрыть")
        displayed("актуальна")
    }

    /**
     * s4. Есть версия новее: диалог показывает номер, дату и описание релиза;
     * отладочная сборка предупреждает, что релизная её не обновит.
     */
    @Test
    fun s4_newVersionShowsReleaseNotes() {
        startWith(
            UpdateResult.NewVersion(
                Release(
                    tag = "v1.0.3",
                    publishedAt = "2026-11-23T22:12:41Z",
                    notes = "Новые возможности проверки обновлений.",
                    downloadUrl = "https://example.invalid/app.apk",
                ),
            ),
        )
        openSettings()

        compose.onNodeWithTag(TAG_UPDATE_ROW).performClick()
        displayed("Есть версия v1.0.3")
        displayedIn("Новые возможности проверки обновлений.")
        displayed("Скачать и установить")
        // Отладочная сборка: установка невозможна, предупреждение обязано быть.
        displayedIn("ключи подписи разные")

        tapText("Позже")
        displayed("v1.0.3")
    }

    /** s5. Ответ не читается: диалог сообщает о нечитаемом ответе. */
    @Test
    fun s5_unreadableResponseState() {
        startWith(UpdateResult.UnreadableResponse)
        openSettings()

        compose.onNodeWithTag(TAG_UPDATE_ROW).performClick()
        displayed("Ответ не читается")
        tapText("Закрыть")
        displayed("нет ответа")
    }
}
