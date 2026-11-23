package com.gymtracker

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.gymtracker.ui.TAG_TAB
import com.gymtracker.ui.exercise.TAG_CARD_HISTORY_ROW
import com.gymtracker.ui.exercise.TAG_DELETE
import com.gymtracker.ui.exercise.TAG_EDIT
import com.gymtracker.ui.exercise.TAG_FORM_CHIP
import com.gymtracker.ui.exercise.TAG_FORM_NAME
import com.gymtracker.ui.exercise.TAG_ADD_PRIMARY
import com.gymtracker.ui.exercise.TAG_ADD_SECONDARY
import com.gymtracker.ui.exercise.TAG_SAVE
import com.gymtracker.ui.history.TAG_ROW
import com.gymtracker.ui.muscles.TAG_ADD_EXERCISE
import com.gymtracker.ui.muscles.TAG_EYE
import com.gymtracker.ui.muscles.TAG_HIDDEN_HEADER
import com.gymtracker.ui.muscles.TAG_MUSCLE
import com.gymtracker.ui.muscles.TAG_MUSCLE_ROW
import com.gymtracker.ui.workout.TAG_LAST
import com.gymtracker.ui.workout.TAG_MONTH
import com.gymtracker.ui.workout.TAG_PICK_ROW
import com.gymtracker.ui.workout.TAG_REPS
import com.gymtracker.ui.workout.TAG_SEARCH
import com.gymtracker.ui.workout.TAG_TOTAL
import com.gymtracker.ui.workout.TAG_WEIGHT
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatDateShort
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Автотесты на весь функционал приложения (issue #11).
 *
 * Один сквозной прогон: методы идут по нарастанию имён, состояние приложения
 * накапливается — пустая база, потом одна тренировка, две, три. Каждый шаг
 * опирается на результат предыдущего, поэтому порядок задаёт сам класс.
 *
 * Перед прогоном данные приложения очищают (`adb shell pm clear <applicationId>`),
 * первый метод стартует с пустой базы. Правку и удаление подходов (issue #6)
 * здесь не проверяем — функция ещё не реализована.
 *
 * Элементы находятся по тексту, `testTag` и `contentDescription`, координаты не
 * используются. Экраны прокручиваются (`performScrollTo`): элемент под сгибом
 * экрана не считается видимым. Лист (`ModalBottomSheet`) не прокручивается,
 * поэтому тап берут по элементу из его видимой части. Состояние ViewModel
 * асинхронно (Room на фоновом потоке) и живёт между методами, поэтому шаги
 * ждут появления элемента (30 секунд) и явно выставляют нужное состояние.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AppFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /** Пакет приложения — из целевого контекста, id задаёт secrets.properties. */
    private val appPackage: String =
        InstrumentationRegistry.getInstrumentation().targetContext.packageName

    /** a1. Пустая база: счётчики, некликабельная «Последняя тренировка», пустой журнал. */
    @Test
    fun a1_emptyStateAndEmptyJournal() {
        compose.onNodeWithTag(TAG_LAST).assertIsDisplayed()
        compose.onNodeWithTag(TAG_LAST).assert(hasClickAction().not())
        displayed("—")
        compose.onNodeWithTag(TAG_MONTH).assert(hasClickAction())
        compose.onNodeWithTag(TAG_TOTAL).assert(hasClickAction())

        tapTab("Журнал")
        displayed("Журнал")
        displayed("Пока нет ни одной записанной тренировки.")
        compose.onNodeWithText("Всё").assert(isSelected())
        compose.onNodeWithText("Неделя").assert(isNotSelected())

        device.pressBack()
        displayed("Начать тренировку")
    }

    /** b2. Полный цикл первой тренировки: старт, упражнение из каталога, подход, завершение. */
    @Test
    fun b2_firstWorkoutFullCycle() {
        tapText("Начать тренировку")
        displayed("Пока пусто. Добавьте первое упражнение.")

        tapText("Добавить упражнение")
        displayed("← Тренировка")
        compose.onNodeWithText("Все").assert(isSelected())
        pickExercise("Жим штанги лёжа")

        // Активный блок раскрыт: повторы подставлены 10, вес пуст.
        displayed("Жим штанги лёжа")
        fieldHas(TAG_REPS, "10")
        fieldHas(TAG_WEIGHT, "")

        typeInto(TAG_WEIGHT, "40")
        tapText("Добавить подход")
        displayed("40 кг × 10")
        // Следующий подход подставляет значение этого подхода.
        fieldHas(TAG_WEIGHT, "40")

        tapText("Завершить упражнение")
        // Блок свёрнут: поля ввода исчезли, чип остался.
        fieldGone(TAG_REPS)
        displayed("40 кг × 10")

        tapText("Завершить тренировку")
        displayed("Завершить тренировку?")
        tapText("Завершить")

        compose.onNodeWithTag(TAG_LAST).assert(hasClickAction())
        counterValue(TAG_LAST, todayShort())
        counterValue(TAG_MONTH, "1 тренировка")
        counterValue(TAG_TOTAL, "1")
    }

    /** c3. Счётчик «Последняя тренировка» ведёт в детали: состав подходов и итоги. */
    @Test
    fun c3_lastCounterOpensWorkoutDetails() {
        tapTag(TAG_LAST)

        displayed("← Журнал")
        displayed(todayFull())
        displayed("1 упражнение · 400 кг")
        displayed("40 кг × 10")
        displayed("Удалить тренировку")

        tapText("← Журнал")
        // Строка журнала: основная мышца, число упражнений, тоннаж.
        displayed("грудь")
        displayed("1 упражнение")
        displayed("400 кг")

        device.pressBack()
        compose.onNodeWithTag(TAG_LAST).assertIsDisplayed()
    }

    /** d4. Журнал: фильтры «Неделя/Месяц/Год/Всё» — выбранный отмечен семантически. */
    @Test
    fun d4_journalPeriodFilters() {
        val labels = listOf("Неделя", "Месяц", "Год", "Всё")
        tapTab("Журнал")
        for (label in labels) {
            tapText(label)
            compose.onNodeWithText(label).assert(isSelected())
            for (other in labels - label) {
                compose.onNodeWithText(other).assert(isNotSelected())
            }
            // Тренировка сегодня — попадает в каждый период.
            displayed("400 кг")
        }

        device.pressBack()
        displayed("Начать тренировку")
    }

    /** e5. Вторая тренировка: два упражнения, подставленный вес, завершение с подтверждением. */
    @Test
    fun e5_secondWorkoutTwoExercises() {
        tapText("Начать тренировку")

        tapText("Добавить упражнение")
        pickExercise("Приседания со штангой")
        typeInto(TAG_WEIGHT, "60")
        tapText("Добавить подход")
        displayed("60 кг × 10")
        tapText("Завершить упражнение")

        // A3a: активного блока нет — нижние кнопки возвращаются.
        tapText("Добавить упражнение")
        pickExercise("Жим гантелей лёжа")
        typeInto(TAG_WEIGHT, "20")
        typeInto(TAG_REPS, "12")
        tapText("Добавить подход")
        displayed("20 кг × 12")
        tapText("Завершить упражнение")

        tapText("Завершить тренировку")
        displayed("Завершить тренировку?")
        tapText("Завершить")

        counterValue(TAG_MONTH, "2 тренировки")
        counterValue(TAG_TOTAL, "2")
    }

    /** f6. Журнал после двух тренировок: две строки, свежая сверху, детали второй. */
    @Test
    fun f6_journalRowsAndSecondWorkoutDetails() {
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).assertCountEquals(2)
        displayed("2 упражнения")
        displayed("840 кг")

        // Порядок строк — по дате, свежая сверху: первая строка — вторая тренировка.
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayed("← Журнал")
        displayed("2 упражнения · 840 кг")
        displayed("60 кг × 10")
        displayed("20 кг × 12")

        tapText("← Журнал")
        device.pressBack()
        compose.onNodeWithTag(TAG_LAST).assertIsDisplayed()
    }

    /** g7. Счётчики после двух тренировок: «за 30 дней» и «всего» считают записи. */
    @Test
    fun g7_countersAfterTwoWorkouts() {
        counterValue(TAG_LAST, todayShort())
        counterValue(TAG_MONTH, "2 тренировки")
        counterValue(TAG_TOTAL, "2")
    }

    /** h8. Пустая сессия: завершение без подтверждения и без записи в журнал. */
    @Test
    fun h8_emptySessionIsNotRecorded() {
        tapText("Начать тренировку")
        tapText("Завершить тренировку")

        compose.onNodeWithText("Завершить тренировку?").assertDoesNotExist()
        displayed("Начать тренировку")
        counterValue(TAG_TOTAL, "2")
    }

    /** i9. A6 «Подходов нет»: «Продолжить» оставляет блок активным, запись появляется после подхода. */
    @Test
    fun i9_noSetsDialogKeepsExercise() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Подтягивания")

        tapText("Завершить упражнение")
        displayed("Подходов нет")
        tapText("Продолжить упражнение")

        compose.waitUntil(30_000) { compose.onNodeWithTag(TAG_REPS).isDisplayed() }
        typeInto(TAG_WEIGHT, "0")
        tapText("Добавить подход")
        displayed("0 кг × 10")

        tapText("Завершить упражнение")
        tapText("Завершить тренировку")
        tapText("Завершить")

        counterValue(TAG_TOTAL, "3")
        counterValue(TAG_MONTH, "3 тренировки")
    }

    /** j10. Данные остаются после перезапуска приложения (Room). */
    @Test
    fun j10_dataSurvivesAppRestart() {
        // Экран пересоздаётся (recreate): процесс тот же, состояние ViewModel — новое,
        // данные читаются из Room.
        compose.activityRule.scenario.onActivity { activity -> activity.recreate() }
        compose.waitForIdle()

        displayed("Тренировка")
        counterValue(TAG_TOTAL, "3")
        compose.onNodeWithTag(TAG_LAST).assert(hasClickAction())

        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).assertCountEquals(3)
        device.pressBack()
    }

    /** k11. Двойной «Назад» на верхнем экране сворачивает приложение. */
    @Test
    fun k11_doubleBackMinimizesApp() {
        device.pressBack()
        displayed("Повторите действие для сворачивания приложения")

        device.pressBack()
        compose.waitUntil(30_000) { device.currentPackageName != appPackage }
    }

    /** l12. «Мышцы»: аккордеон, выбор мышцы, скрытие и возврат упражнения. */
    @Test
    fun l12_musclesAccordionSelectionAndHiddenExercise() {
        tapTab("Мышцы")
        displayed("Мышцы")
        // При запуске все аккордеоны свёрнуты: кнопки «Добавить упражнение» нет.
        countTag(TAG_ADD_EXERCISE, 0)

        openArea("Спина")
        countTag(TAG_MUSCLE, 3)
        compose.onNodeWithText("Широчайшие").assert(isSelected())
        compose.onNodeWithText("Большая круглая").assert(isNotSelected())
        // Строки упражнений выбранной мышцы (основная и вторичная), по алфавиту.
        countTag(TAG_MUSCLE_ROW, 5)

        // Смена мышцы: у «Большая круглая» нет упражнений с основной ролью,
        // но есть вторичные — Пуловер, Подтягивания, Разведение гантелей в наклоне.
        tapText("Большая круглая")
        compose.onNodeWithText("Большая круглая").assert(isSelected())
        countTag(TAG_MUSCLE_ROW, 3)

        tapText("Широчайшие")
        countTag(TAG_MUSCLE_ROW, 5)

        // Иконка глаза первой строки («Подтягивания») скрывает упражнение.
        tapTagIndex(TAG_EYE, 0)
        countTag(TAG_MUSCLE_ROW, 4)
        displayed("Скрытое")
        displayed("(1)")
        countTag(TAG_HIDDEN_HEADER, 1)

        // Блок «Скрытое» свёрнут по умолчанию: тап по слову раскрывает список.
        tapTag(TAG_HIDDEN_HEADER)
        displayed("Подтягивания")
        // Скрытая строка идёт после видимых: её глаз — пятый.
        tapTagIndex(TAG_EYE, 4)
        countTag(TAG_MUSCLE_ROW, 5)
        countTag(TAG_HIDDEN_HEADER, 0)

        // Повторный тап по заголовку сворачивает область.
        tapText("Спина")
        countTag(TAG_MUSCLE, 0)
        countTag(TAG_ADD_EXERCISE, 0)
    }

    /** m13. Карточка упражнения: история, детали, карандаш, мягкое удаление. */
    @Test
    fun m13_exerciseCardHistoryEditAndSoftDelete() {
        tapTab("Мышцы")
        openArea("Спина")
        tapText("Широчайшие")
        countTag(TAG_MUSCLE_ROW, 5)

        tapText("Подтягивания")
        displayed("← Мышцы")
        displayed("Подтягивания")
        displayed("Фото")
        displayed("История упражнения")
        // Подход из третьей тренировки (i9).
        countTag(TAG_CARD_HISTORY_ROW, 1)
        displayed("0 кг × 10")

        // Строка истории ведёт в детали тренировки.
        tapTag(TAG_CARD_HISTORY_ROW)
        displayed("← Журнал")
        displayed("0 кг × 10")
        tapText("← Журнал")
        displayed("Мышцы")
        countTag(TAG_MUSCLE_ROW, 5)

        // Карандаш: форма редактирования, название уже заполнено.
        tapText("Подтягивания")
        tapTag(TAG_EDIT)
        displayed("Редактировать упражнение")
        fieldHas(TAG_FORM_NAME, "Подтягивания")
        typeInto(TAG_FORM_NAME, "Подтягивания широким хватом")
        tapTag(TAG_SAVE)
        // Форма редактирования открыта из карточки: сохранение возвращает на карточку.
        displayed("← Мышцы")
        displayed("Подтягивания широким хватом")

        // Удаление: «Отмена» оставляет карточку, «Удалить» скрывает упражнение.
        tapTag(TAG_DELETE)
        displayed("Удалить упражнение?")
        tapText("Отмена")
        displayed("← Мышцы")

        tapTag(TAG_DELETE)
        displayed("Удалить упражнение?")
        tapText("Удалить")
        // Карточка закрывается: возврат на экран «Мышцы».
        displayed("Мышцы")
        countTag(TAG_MUSCLE_ROW, 4)
    }

    /** n14. Форма нового упражнения: основная мышца из «Мышцы», «+ добавить», сохранение. */
    @Test
    fun n14_exerciseFormCreate() {
        tapTab("Мышцы")
        openArea("Спина")
        tapText("Большая круглая")
        // После m13 «Подтягивания» удалено: остаются Пуловер и Разведение гантелей в наклоне.
        countTag(TAG_MUSCLE_ROW, 2)

        tapTag(TAG_ADD_EXERCISE)
        displayed("← Мышцы")
        displayed("Новое упражнение")
        displayed("Основные мышцы")
        displayed("Вторичные мышцы")
        // Основная мышца приходит из выбранной на экране «Мышцы».
        countTag(TAG_FORM_CHIP, 1)
        displayed("Большая круглая")

        typeInto(TAG_FORM_NAME, "Тяга блока сверху")

        // «+ добавить» в основных: лист мышц, тап добавляет чип.
        tapTag(TAG_ADD_PRIMARY)
        displayed("Основная мышца")
        tapText("Широчайшие")
        countTag(TAG_FORM_CHIP, 2)
        displayed("Широчайшие")

        tapTag(TAG_ADD_SECONDARY)
        displayed("Вторичная мышца")
        // Лист мышц не прокручивается: берём мышцу из видимой части списка.
        tapText("Грудь")
        displayed("Грудь")

        tapTag(TAG_SAVE)
        displayed("Мышцы")
        // Упражнение создано с основной «Большая круглая» — оно в списке этой мышцы.
        countTag(TAG_MUSCLE_ROW, 3)
        displayed("Тяга блока сверху")
    }

    /** o15. Карточка созданного упражнения: мышцы из формы, история пуста. */
    @Test
    fun o15_createdExerciseCard() {
        tapTab("Мышцы")
        openArea("Спина")
        tapText("Большая круглая")
        countTag(TAG_MUSCLE_ROW, 3)

        tapText("Тяга блока сверху")
        displayed("← Мышцы")
        displayed("Тяга блока сверху")
        displayed("Большая круглая")
        displayed("Широчайшие")
        displayed("Грудь")
        displayed("История упражнения")
        countTag(TAG_CARD_HISTORY_ROW, 0)

        // Возврат на верхний экран: k11 проверяет двойной «Назад» с него.
        tapText("← Мышцы")
        displayed("Мышцы")
        device.pressBack()
        displayed("Начать тренировку")
    }

    /** Есть ли элемент в дереве (не обязательно виден). */
    private fun exists(text: String): Boolean =
        compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    /** Число элементов с тегом: ждём, пока состояние придёт из ViewModel. */
    private fun countTag(tag: String, n: Int) {
        compose.waitUntil(30_000) {
            try {
                compose.onAllNodesWithTag(tag).assertCountEquals(n)
                true
            } catch (_: Throwable) {
                false
            }
        }
        compose.onAllNodesWithTag(tag).assertCountEquals(n)
    }

    /** Тап по элементу с тегом по порядку (ряд одинаковых элементов). */
    private fun tapTagIndex(tag: String, index: Int) {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size > index
        }
        compose.onAllNodesWithTag(tag).get(index).performClick()
    }

    /** Значение счётчика: проверка внутри строки с тегом, а не по всей экрану. */
    private fun counterValue(tag: String, value: String) {
        compose.waitUntil(30_000) {
            try {
                compose.onNodeWithTag(tag).assert(hasText(value))
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /** Поле ввода содержит значение: ждём, пока значение придёт из ViewModel. */
    private fun fieldHas(tag: String, value: String) {
        compose.waitUntil(30_000) {
            try {
                compose.onNodeWithTag(tag).assertTextEquals(value)
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /** Поле ввода исчезло (блок свёрнут). */
    private fun fieldGone(tag: String) {
        compose.waitUntil(30_000) {
            try {
                compose.onNodeWithTag(tag).assertDoesNotExist()
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /** Элемент на экране: при необходимости прокручиваем список до него. */
    private fun displayed(text: String) {
        compose.waitUntil(30_000) { exists(text) }
        val node = compose.onNodeWithText(text)
        try {
            node.performScrollTo()
        } catch (_: Throwable) {
            // Прокрутка не нужна: элемент вне прокручиваемого контейнера.
        }
        compose.waitUntil(30_000) { node.isDisplayed() }
        node.assertIsDisplayed()
    }

    /**
     * Открыть область аккордеона «Мышцы». Открытая область и выбранная мышца
     * живут всю сессию приложения (ViewModel), поэтому первый тап может
     * свернуть уже открытую область — тогда открываем повторно.
     */
    private fun openArea(name: String) {
        tapText(name)
        val chips = { compose.onAllNodesWithTag(TAG_MUSCLE).fetchSemanticsNodes().isNotEmpty() }
        try {
            compose.waitUntil(5_000) { chips() }
        } catch (_: Throwable) {
            tapText(name)
            compose.waitUntil(30_000) { chips() }
        }
    }

    /** Тап по табу таб-панели: у иконки есть подпись-описание, узел с описанием кликабелен. */
    private fun tapTab(label: String) {
        compose.waitUntil(30_000) { compose.onNodeWithContentDescription(label).isDisplayed() }
        compose.onNodeWithContentDescription(label).performClick()
    }

    /** Поиск по названию и тап по первой строке списка выбора (A4). */
    private fun pickExercise(name: String) {
        // Запрос в ViewModel живёт между заходами в выбор: поле очищаем перед вводом.
        compose.onNodeWithTag(TAG_SEARCH).performTextClearance()
        compose.onNodeWithTag(TAG_SEARCH).performTextInput(name)
        compose.waitUntil(30_000) { compose.onAllNodesWithTag(TAG_PICK_ROW).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithTag(TAG_PICK_ROW).get(0).performClick()
        displayed(name)
    }

    /** Ввод в числовое поле: поле очищается, затем вводится значение. */
    private fun typeInto(tag: String, text: String) {
        compose.onNodeWithTag(tag).performTextClearance()
        compose.onNodeWithTag(tag).performTextInput(text)
    }

    private fun tapText(text: String) {
        compose.waitUntil(30_000) { exists(text) }
        val node = compose.onNodeWithText(text)
        try {
            node.performScrollTo()
        } catch (_: Throwable) {
            // Прокрутка не нужна: элемент вне прокручиваемого контейнера.
        }
        compose.waitUntil(30_000) { node.isDisplayed() }
        node.performClick()
    }

    private fun tapTag(tag: String) {
        compose.waitUntil(30_000) { compose.onNodeWithTag(tag).isDisplayed() }
        compose.onNodeWithTag(tag).performClick()
    }

    private fun todayFull(): String = formatDateFull(System.currentTimeMillis())

    private fun todayShort(): String = formatDateShort(System.currentTimeMillis())
}
