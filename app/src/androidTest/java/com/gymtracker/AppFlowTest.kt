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
import com.gymtracker.ui.components.TAG_COMMENT_COUNTER
import com.gymtracker.ui.components.TAG_COMMENT_FIELD
import com.gymtracker.ui.exercise.TAG_BODYWEIGHT_TOGGLE
import com.gymtracker.ui.exercise.TAG_CARD_HISTORY_ROW
import com.gymtracker.ui.exercise.TAG_DELETE
import com.gymtracker.ui.exercise.TAG_EDIT
import com.gymtracker.ui.exercise.TAG_FORM_CHIP
import com.gymtracker.ui.exercise.TAG_FORM_NAME
import com.gymtracker.ui.exercise.TAG_ADD_PRIMARY
import com.gymtracker.ui.exercise.TAG_ADD_SECONDARY
import com.gymtracker.ui.exercise.TAG_SAVE
import com.gymtracker.ui.history.TAG_DETAIL_BLOCK_COMMENT
import com.gymtracker.ui.history.TAG_DETAIL_COMMENT
import com.gymtracker.ui.history.TAG_ROW_COMMENT
import com.gymtracker.ui.history.TAG_ROW
import com.gymtracker.ui.muscles.TAG_ADD_EXERCISE
import com.gymtracker.ui.muscles.TAG_EYE
import com.gymtracker.ui.muscles.TAG_HIDDEN_HEADER
import com.gymtracker.ui.muscles.TAG_MUSCLE
import com.gymtracker.ui.muscles.TAG_MUSCLE_ROW
import com.gymtracker.ui.workout.TAG_BLOCK_COMMENT
import com.gymtracker.ui.workout.TAG_BLOCK_ROW
import com.gymtracker.ui.workout.TAG_EDIT_REPS
import com.gymtracker.ui.workout.TAG_EDIT_WEIGHT
import com.gymtracker.ui.workout.TAG_INFO
import com.gymtracker.ui.workout.TAG_INFO_CHIP
import com.gymtracker.ui.workout.TAG_INFO_COMMENT
import com.gymtracker.ui.workout.TAG_INFO_SESSION
import com.gymtracker.ui.workout.TAG_INFO_SHEET
import com.gymtracker.ui.workout.TAG_LAST
import com.gymtracker.ui.workout.TAG_MONTH
import com.gymtracker.ui.workout.TAG_PICK_ROW
import com.gymtracker.ui.workout.TAG_REPS
import com.gymtracker.ui.workout.TAG_SEARCH
import com.gymtracker.ui.workout.TAG_TOTAL
import com.gymtracker.ui.workout.TAG_WEIGHT
import com.gymtracker.ui.workout.TAG_WORKOUT_COMMENT
import com.gymtracker.util.formatDateFull
import com.gymtracker.util.formatDateShort
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * Автотесты на весь функционал приложения.
 *
 * Один сквозной прогон: методы идут по нарастанию имён, состояние приложения
 * накапливается — пустая база, потом одна тренировка, две, три. Каждый шаг
 * опирается на результат предыдущего, поэтому порядок задаёт сам класс.
 *
 * Перед прогоном данные приложения очищают (`adb shell pm clear <applicationId>`),
 * первый метод стартует с пустой базы.
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
        displayed("10 × 40 кг")
        // Следующий подход подставляет значение этого подхода.
        fieldHas(TAG_WEIGHT, "40")

        tapText("Завершить упражнение")
        // Блок свёрнут в одну строку: поля ввода исчезли, чипы скрыты.
        fieldGone(TAG_REPS)
        compose.onNodeWithText("10 × 40 кг").assertDoesNotExist()
        // Тап по строке раскрывает блок: bubble-подход появляется.
        tapTag(TAG_BLOCK_ROW)
        displayed("10 × 40 кг")
        // Тап по строке сворачивает блок обратно: чипы исчезают.
        tapTag(TAG_BLOCK_ROW)
        compose.onNodeWithText("10 × 40 кг").assertDoesNotExist()

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
        displayed("10 × 40 кг")
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
        displayed("10 × 60 кг")
        tapText("Завершить упражнение")

        // A3a: активного блока нет — нижние кнопки возвращаются.
        tapText("Добавить упражнение")
        pickExercise("Жим гантелей лёжа")
        typeInto(TAG_WEIGHT, "20")
        typeInto(TAG_REPS, "12")
        tapText("Добавить подход")
        displayed("12 × 20 кг")
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
        displayed("10 × 60 кг")
        displayed("12 × 20 кг")

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
        pickExercise("Тяга верхнего блока")

        tapText("Завершить упражнение")
        displayed("Подходов нет")
        tapText("Продолжить упражнение")

        compose.waitUntil(30_000) { compose.onNodeWithTag(TAG_REPS).isDisplayed() }
        typeInto(TAG_WEIGHT, "0")
        tapText("Добавить подход")
        displayed("10 × 0 кг")

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

        tapText("Тяга верхнего блока")
        displayed("← Мышцы")
        displayed("Тяга верхнего блока")
        displayed("Фото")
        displayed("История упражнения")
        // Подход из третьей тренировки (i9).
        countTag(TAG_CARD_HISTORY_ROW, 1)
        displayed("10 × 0 кг")

        // Строка истории ведёт в детали тренировки.
        tapTag(TAG_CARD_HISTORY_ROW)
        displayed("← Журнал")
        displayed("10 × 0 кг")
        tapText("← Журнал")
        displayed("Мышцы")
        countTag(TAG_MUSCLE_ROW, 5)

        // Карандаш: форма редактирования, название уже заполнено.
        tapText("Тяга верхнего блока")
        tapTag(TAG_EDIT)
        displayed("Редактировать упражнение")
        fieldHas(TAG_FORM_NAME, "Тяга верхнего блока")
        typeInto(TAG_FORM_NAME, "Тяга блока сверху")
        tapTag(TAG_SAVE)
        // Форма редактирования открыта из карточки: сохранение возвращает на карточку.
        displayed("← Мышцы")
        displayed("Тяга блока сверху")

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
        // После m13 «Тяга верхнего блока» удалена: Пуловер, Подтягивания, Разведение гантелей в наклоне.
        countTag(TAG_MUSCLE_ROW, 3)

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
        countTag(TAG_MUSCLE_ROW, 4)
        displayed("Тяга блока сверху")
    }

    /** o15. Карточка созданного упражнения: мышцы из формы, история пуста. */
    @Test
    fun o15_createdExerciseCard() {
        tapTab("Мышцы")
        openArea("Спина")
        tapText("Большая круглая")
        countTag(TAG_MUSCLE_ROW, 4)

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

    /** p16. Правка подхода: тап по чипу открывает диалог, «Сохранить» обновляет чип и тоннаж. */
    @Test
    fun p16_editSetUpdatesChipAndTonnage() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Жим штанги лёжа")
        typeInto(TAG_WEIGHT, "50")
        typeInto(TAG_REPS, "8")
        tapText("Добавить подход")
        displayed("8 × 50 кг")

        // Тап по чипу открывает диалог правки с текущими значениями.
        tapText("8 × 50 кг")
        displayed("Правка подхода")
        fieldHas(TAG_EDIT_WEIGHT, "50")
        fieldHas(TAG_EDIT_REPS, "8")

        // Изменить вес на 60, сохранить.
        // Поле должно показать набранное: диалог читает наблюдаемое состояние
        // экрана, а не vm.uiState.value (иначе набитый текст не появляется).
        typeInto(TAG_EDIT_WEIGHT, "60")
        fieldHas(TAG_EDIT_WEIGHT, "60")
        tapText("Сохранить")

        // Чип обновлён, диалог закрыт.
        displayed("8 × 60 кг")
        compose.onNodeWithText("Правка подхода").assertDoesNotExist()

        // Завершить упражнение и тренировку — тоннаж пересчитан (60×8=480).
        tapText("Завершить упражнение")
        tapText("Завершить тренировку")
        tapText("Завершить")

        counterValue(TAG_TOTAL, "4")
        // Проверка тоннажа в журнале: 480 кг.
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayedInLabel("480 кг")
        tapText("← Журнал")
        device.pressBack()
        displayed("Начать тренировку")
    }

    /** p17. Удаление подхода из завершённого блока: чип исчезает, тоннаж пересчитывается. */
    @Test
    fun p17_deleteSetFromCompletedBlock() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Приседания со штангой")
        typeInto(TAG_WEIGHT, "80")
        typeInto(TAG_REPS, "5")
        tapText("Добавить подход")
        displayed("5 × 80 кг")

        typeInto(TAG_WEIGHT, "90")
        tapText("Добавить подход")
        displayed("5 × 90 кг")

        // Завершить блок — он свёрнут в одну строку, чипы скрыты.
        tapText("Завершить упражнение")

        // Раскрыть блок тапом по строке: чипы появляются, тап по чипу — диалог правки.
        tapTag(TAG_BLOCK_ROW)

        // Тап по первому чипу (5 × 80 кг) — диалог правки.
        tapText("5 × 80 кг")
        displayed("Правка подхода")
        fieldHas(TAG_EDIT_WEIGHT, "80")
        fieldHas(TAG_EDIT_REPS, "5")

        // Удалить подход.
        tapText("Удалить")

        // Чип «5 × 80 кг» исчез, «5 × 90 кг» остался.
        compose.onNodeWithText("5 × 80 кг").assertDoesNotExist()
        displayed("5 × 90 кг")

        tapText("Завершить тренировку")
        tapText("Завершить")

        // Тоннаж: 90×5=450.
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayedInLabel("450 кг")
        tapText("← Журнал")
        device.pressBack()
        displayed("Начать тренировку")
    }

    /** p18. Удаление последнего подхода блока — показывается A6 «Подходов нет». */
    @Test
    fun p18_deleteLastSetShowsNoSetsDialog() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Жим гантелей лёжа")
        typeInto(TAG_WEIGHT, "30")
        typeInto(TAG_REPS, "10")
        tapText("Добавить подход")
        displayed("10 × 30 кг")

        // Удалить единственный подход через диалог.
        tapText("10 × 30 кг")
        displayed("Правка подхода")
        tapText("Удалить")

        // A6: «Подходов нет».
        displayed("Подходов нет")
        tapText("Завершить без записи")

        // Блок удалён, сессия пуста — завершение без подтверждения.
        tapText("Завершить тренировку")
        compose.onNodeWithText("Завершить тренировку?").assertDoesNotExist()
        displayed("Начать тренировку")
        // Тренировка не записана: счётчик не вырос.
        counterValue(TAG_TOTAL, "5")
    }

    /** p19. Журнал read-only: чипы подходов в деталях прошлой тренировки не кликабельны. */
    @Test
    fun p19_journalChipsAreReadOnly() {
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayed("← Журнал")

        // Чип «5 × 90 кг» из p17 — есть, но не кликабельный.
        val chip = compose.onNodeWithText("5 × 90 кг")
        compose.waitUntil(30_000) { chip.isDisplayed() }
        chip.assertIsDisplayed()
        chip.assert(hasClickAction().not())

        tapText("← Журнал")
        device.pressBack()
        displayed("Начать тренировку")
    }

    /** q20. Удаление тренировки из деталей: нижняя панель возвращается, навигация корректна. */
    @Test
    fun q20_deleteWorkoutFromDetailsRestoresBottomBar() {
        // Часть 1: удаление из деталей, открытых тапом по строке журнала.
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayed("← Журнал")
        // В деталях панель не показывается: табы отсутствуют в дереве.
        compose.onNodeWithContentDescription("Журнал").assertDoesNotExist()

        tapText("Удалить тренировку")
        tapText("Удалить")

        // После удаления: список журнала, панель видна.
        displayed("Журнал")
        compose.onNodeWithContentDescription("Журнал").assertIsDisplayed()
        compose.onAllNodesWithTag(TAG_ROW).assertCountEquals(4)

        // Часть 2: удаление из деталей, открытых из счётчика «Последняя тренировка».
        tapTab("Тренировка")
        tapTag(TAG_LAST)
        displayed("← Журнал")

        tapText("Удалить тренировку")
        tapText("Удалить")

        // Возврат на журнал, панель видна, активен таб «Журнал».
        displayed("Журнал")
        compose.onNodeWithContentDescription("Журнал").assertIsDisplayed()
        compose.onNodeWithContentDescription("Тренировка").assertIsDisplayed()
        compose.onAllNodesWithTag(TAG_ROW).assertCountEquals(3)

        // Часть 3: удаление из деталей, открытых из карточки упражнения.
        tapTab("Мышцы")
        openArea("Грудь")
        tapText("Жим штанги лёжа")
        displayed("← Мышцы")
        // Строка истории ведёт в детали тренировки (карточка снимается со стека).
        tapTag(TAG_CARD_HISTORY_ROW)
        displayed("← Журнал")

        tapText("Удалить тренировку")
        tapText("Удалить")

        // Возврат на «Мышцы»: панель видна (top — Tab).
        displayed("Мышцы")
        compose.onNodeWithContentDescription("Мышцы").assertIsDisplayed()

        // Часть 4: «Назад» — возврат на Журнал, затем на Тренировку.
        device.pressBack()
        displayed("Журнал")
        device.pressBack()
        displayed("Начать тренировку")
    }

    /** r21. Упражнение без веса: чип по повторам, поле «Вес» отсутствует, тоннаж 0. */
    @Test
    fun r21_bodyweightExerciseChipAndNoWeightField() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Подтягивания")

        // Поле «Вес» отсутствует — только «Повторы».
        compose.onNodeWithTag(TAG_WEIGHT).assertDoesNotExist()
        compose.onNodeWithTag(TAG_REPS).assertIsDisplayed()

        typeInto(TAG_REPS, "12")
        tapText("Добавить подход")
        displayed("12 повт.")

        tapText("Завершить упражнение")
        tapText("Завершить тренировку")
        tapText("Завершить")

        // Тоннаж: упражнения без веса дают 0 — тренировка показывает 0 кг.
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayed("← Журнал")
        displayed("12 повт.")
        displayedInLabel("0 кг")

        tapText("← Журнал")
        device.pressBack()
        displayed("Начать тренировку")
    }

    /** s22. Активный блок сворачивается в одну строку и раскрывается тапом по строке. */
    @Test
    fun s22_activeBlockCollapseAndExpand() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Жим гантелей лёжа")

        // Активный блок по умолчанию раскрыт: поля ввода на месте.
        compose.onNodeWithTag(TAG_REPS).assertIsDisplayed()
        typeInto(TAG_REPS, "10")
        typeInto(TAG_WEIGHT, "25")
        tapText("Добавить подход")
        displayed("10 × 25 кг")
        // Добавление подхода раскрытие не меняет — блок остаётся открытым.
        compose.onNodeWithTag(TAG_REPS).assertIsDisplayed()

        // Тап по строке сворачивает активный блок: поля и кнопки исчезают, чипы скрыты.
        tapTag(TAG_BLOCK_ROW)
        fieldGone(TAG_REPS)
        fieldGone(TAG_WEIGHT)
        compose.onNodeWithText("10 × 25 кг").assertDoesNotExist()
        compose.onNodeWithText("Добавить подход").assertDoesNotExist()
        // Свёрнутый активный блок — правило A3: нижние кнопки скрыты, «↻» нет.
        compose.onNodeWithText("Добавить упражнение").assertDoesNotExist()
        compose.onNodeWithText("Завершить тренировку").assertDoesNotExist()
        compose.onNodeWithText("↻").assertDoesNotExist()

        // Тап по строке активного блока раскрывает его, подход
        // не добавляет; тап по строке раскрытого блока сворачивает обратно.
        tapTag(TAG_BLOCK_ROW)
        fieldHas(TAG_REPS, "10")
        fieldHas(TAG_WEIGHT, "25")
        displayed("10 × 25 кг")
        tapTag(TAG_BLOCK_ROW)
        fieldGone(TAG_REPS)
        compose.onNodeWithText("Добавить подход").assertDoesNotExist()

        // Тап по строке раскрывает блок: значения полей не сбросились.
        tapTag(TAG_BLOCK_ROW)
        fieldHas(TAG_REPS, "10")
        fieldHas(TAG_WEIGHT, "25")
        displayed("10 × 25 кг")

        // В раскрытом блоке подход добавляется.
        typeInto(TAG_REPS, "12")
        tapText("Добавить подход")
        displayed("12 × 25 кг")

        // Перезапуск приложения: состояние ViewModel новое, активный блок по
        // умолчанию снова раскрыт — чипы, поля и кнопки на месте, «↻» нет.
        compose.activityRule.scenario.onActivity { activity -> activity.recreate() }
        compose.waitForIdle()
        compose.onNodeWithTag(TAG_REPS).assertIsDisplayed()
        displayed("12 × 25 кг")
        textGone("↻")
        textGone("Добавить упражнение")

        tapText("Завершить упражнение")
        // Завершённый блок свёрнут: чипы скрыты; состояние приходит асинхронно,
        // поэтому ждем исчезновения, а не проверяем мгновенно.
        textGone("12 × 25 кг")

        // Второе упражнение: активный блок раскрыт, свёрнутый завершённый
        // раскрывают тапом по строке; «↻» не появляется, пока есть активное упражнение.
        tapText("Добавить упражнение")
        pickExercise("Приседания со штангой")
        tapTagIndex(TAG_BLOCK_ROW, 0)
        displayed("12 × 25 кг")
        textGone("↻")

        typeInto(TAG_WEIGHT, "70")
        tapText("Добавить подход")
        displayed("10 × 70 кг")

        tapText("Завершить упражнение")
        // A3a: активного упражнения нет — нижние кнопки доступны, «↻» появляется
        // на последнем завершённом блоке в раскрытом виде. Тап по «↻» делает блок
        // активным и раскрывает, поля подставлены из последнего подхода.
        compose.waitForIdle()
        compose.onNodeWithText("Добавить упражнение").assertIsDisplayed()
        tapTagIndex(TAG_BLOCK_ROW, 1)
        displayed("↻")
        tapText("↻")
        // «↻» делает блок активным: нижние кнопки снова скрыты; ждем обновления состояния.
        textGone("Добавить упражнение")
        fieldHas(TAG_REPS, "10")
        fieldHas(TAG_WEIGHT, "70")
        displayed("10 × 70 кг")
        tapText("Завершить упражнение")

        tapText("Завершить тренировку")
        displayed("Завершить тренировку?")
        tapText("Завершить")

        // Тренировка записана: q20 удалил три, r21 и этот шаг добавили по одной.
        counterValue(TAG_TOTAL, "4")
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).assertCountEquals(4)
        device.pressBack()
    }

    /** t23. Комментарий тренировки над блоками, комментарий упражнения под чипами. */
    @Test
    fun t23_workoutAndExerciseComments() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Жим штанги лёжа")
        typeInto(TAG_WEIGHT, "40")
        tapText("Добавить подход")
        displayed("10 × 40 кг")

        // Строка комментария тренировки — над блоками: пустый комментарий показан
        // подсказом, текста комментария строка не показывает.
        tagHas(TAG_WORKOUT_COMMENT, "Добавить комментарий")
        tapTag(TAG_WORKOUT_COMMENT)
        displayed("Комментарий тренировки")
        fieldHas(TAG_COMMENT_FIELD, "")
        typeInto(TAG_COMMENT_FIELD, "Было сложно")
        fieldHas(TAG_COMMENT_FIELD, "Было сложно")
        tapText("Сохранить")
        displayed("Было сложно")

        // Комментарий упражнения — под чипами раскрытого блока.
        tapTag(TAG_BLOCK_COMMENT)
        displayed("Комментарий упражнения")
        typeInto(TAG_COMMENT_FIELD, "Левая рука отставала")
        tapText("Сохранить")
        displayed("Левая рука отставала")

        // Тап по строке сворачивает блок: комментарий упражнения скрыт, комментарий
        // тренировки над блоками остаётся.
        tapTag(TAG_BLOCK_ROW)
        tagGone(TAG_BLOCK_COMMENT)
        displayed("Было сложно")

        // Раскрыть блок тапом по строке, завершить упражнение — свёрнутый завершённый
        // блок комментария не показывает; нижние кнопки появляются (A3a).
        tapTag(TAG_BLOCK_ROW)
        displayed("Левая рука отставала")
        tapText("Завершить упражнение")
        tagGone(TAG_BLOCK_COMMENT)

        tapText("Завершить тренировку")
        displayed("Завершить тренировку?")
        tapText("Завершить")
        counterValue(TAG_TOTAL, "5")

        // B2: комментарий тренировки над блоками, комментарий упражнения под чипами.
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayed("← Журнал")
        displayed("Было сложно")
        displayed("Левая рука отставала")

        // B1: комментарий тренировки в строке журнала под датой.
        tapText("← Журнал")
        tagHas(TAG_ROW_COMMENT, "Было сложно")

        device.pressBack()
        displayed("Начать тренировку")
    }

    /** u24. Лимит 250 знаков, правка комментария из деталей журнала, «Удалить» очищает. */
    @Test
    fun u24_commentLimitHistoryEditAndClear() {
        tapTab("Журнал")
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        displayed("← Журнал")

        // Диалог комментария упражнения подставляет сохранённый текст: первый блок,
        // у блоков без комментария строка показывает подсказ под тем же тегом.
        tapTagIndex(TAG_DETAIL_BLOCK_COMMENT, 0)
        fieldHas(TAG_COMMENT_FIELD, "Левая рука отставала")
        tapText("Отмена")

        // Ввод 250 знаков: поле принимает, счётчик показывает «250 / 250».
        // 251-й знак отклоняется: поле остаётся 250 знаков.
        tapTag(TAG_DETAIL_COMMENT)
        typeInto(TAG_COMMENT_FIELD, longText(250))
        fieldHas(TAG_COMMENT_FIELD, longText(250))
        tagHas(TAG_COMMENT_COUNTER, "250 / 250")
        compose.onNodeWithTag(TAG_COMMENT_FIELD).performTextInput("!")
        fieldHas(TAG_COMMENT_FIELD, longText(250))
        tagHas(TAG_COMMENT_COUNTER, "250 / 250")
        tapText("Сохранить")

        // Строка журнала показывает длинный комментарий (обрывается многоточием).
        tapText("← Журнал")
        tagHas(TAG_ROW_COMMENT, longText(250))

        // «Удалить» очищает комментарий: строка деталей снова с подсказом,
        // строка комментария в журнале исчезает.
        compose.onAllNodesWithTag(TAG_ROW).get(0).performClick()
        tapTag(TAG_DETAIL_COMMENT)
        fieldHas(TAG_COMMENT_FIELD, longText(250))
        tapText("Удалить")
        tagHas(TAG_DETAIL_COMMENT, "Добавить комментарий")
        tapText("← Журнал")
        tagGone(TAG_ROW_COMMENT)

        device.pressBack()
        displayed("Начать тренировку")
    }

    /** v25. Иконка «i» в раскрытом активном блоке: лист истории упражнения. */
    @Test
    fun v25_infoIconOpensHistorySheet() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Жим штанги лёжа")
        typeInto(TAG_REPS, "10")
        typeInto(TAG_WEIGHT, "40")
        tapText("Добавить подход")
        displayed("10 × 40 кг")

        // Иконка «i» в раскрытом активном блоке: тап открывает лист истории.
        tapTag(TAG_INFO)
        // Заголовок листа в слитом дереве сливается с содержимым — ищем подстроку.
        displayedInLabel("История упражнения")
        countTag(TAG_INFO_SHEET, 1)
        // После q20 удалили все тренировки с этим упражнением; в листе —
        // завершённая тренировка из t23 (текущая сессия в лист не попадает):
        // дата, чип, комментарий упражнения. Строка листа не кликабельная, дата
        // и чипы лежат дочерними текстами: в слитом дереве они подстроки листа.
        countTag(TAG_INFO_SESSION, 1)
        displayedInLabel(todayFull())
        countTag(TAG_INFO_CHIP, 1)
        displayedInLabel("10 × 40 кг")
        tagHas(TAG_INFO_COMMENT, "Левая рука отставала")

        // Свёрнутый активный блок: иконки «i» нет; тап по строке раскрывает — она появляется.
        tapTag(TAG_BLOCK_ROW)
        tagGone(TAG_INFO)
        tapTag(TAG_BLOCK_ROW)
        compose.waitUntil(30_000) { compose.onNodeWithTag(TAG_INFO).isDisplayed() }

        // «Назад» закрывает лист, приложение остаётся на экране.
        device.pressBack()
        tagGone(TAG_INFO_SHEET)
        displayed("Тренировка")
        compose.waitUntil(30_000) { device.currentPackageName == appPackage }

        tapText("Завершить упражнение")
        tapText("Завершить тренировку")
        tapText("Завершить")
        counterValue(TAG_TOTAL, "6")
    }

    /** v26. Упражнение без истории: лист показывает пустое состояние. */
    @Test
    fun v26_infoSheetEmptyState() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Отжимания от пола")
        typeInto(TAG_REPS, "10")
        tapText("Добавить подход")
        displayed("10 повт.")

        tapTag(TAG_INFO)
        // Заголовок листа в слитом дереве сливается с содержимым — ищем подстроку.
        displayedInLabel("История упражнения")
        displayedInLabel("Данных о прошлых тренировках нет")
        countTag(TAG_INFO_SHEET, 1)
        countTag(TAG_INFO_SESSION, 0)

        device.pressBack()
        tagGone(TAG_INFO_SHEET)

        tapText("Завершить упражнение")
        tapText("Завершить тренировку")
        tapText("Завершить")
        counterValue(TAG_TOTAL, "7")
    }

    /** w27. Дата строки листа — дата тренировки, не id сессии. */
    @Test
    fun w27_infoSheetDateIsTrainingDate() {
        tapText("Начать тренировку")
        tapText("Добавить упражнение")
        pickExercise("Жим штанги лёжа")
        typeInto(TAG_REPS, "10")
        typeInto(TAG_WEIGHT, "40")
        tapText("Добавить подход")
        tapTag(TAG_INFO)
        displayedInLabel("История упражнения")
        // Строки листа: завершённые тренировки с этим упражнением (t23 и эта);
        // дата каждой — дата тренировки, не id сессии (1970 — признак подмены).
        countTag(TAG_INFO_SESSION, 2)
        displayedInLabel(todayFull())
        displayedInLabel("10 × 40 кг")
        device.pressBack()
        tapText("Завершить упражнение")
        tapText("Завершить тренировку")
        tapText("Завершить")
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

    /** Строка длиной n знаков для проверки лимита комментария. */
    private fun longText(n: Int): String = "0123456789".repeat(30).substring(0, n)

    /**
     * Строка с тегом показывает текст (ждёт: состояние приходит из Room). Дерево
     * берётся неразбитым: строка комментария в журнале лежит внутри кликабельной
     * карточки и в слитом дереве сливается с ней.
     */
    private fun tagHas(tag: String, text: String) {
        compose.waitUntil(30_000) {
            try {
                compose.onNodeWithTag(tag, useUnmergedTree = true).assert(hasText(text))
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    /** Строка с тегом исчезла из дерева (блок свёрнут, комментария нет). */
    private fun tagGone(tag: String) {
        compose.waitUntil(30_000) {
            try {
                compose.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(0)
                true
            } catch (_: Throwable) {
                false
            }
        }
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

    /** Текст исчез из дерева (свернутый блок, закрытый диалог): ждёт, а не проверяет мгновенно. */
    private fun textGone(text: String) {
        compose.waitUntil(30_000) { !exists(text) }
    }

    /**
     * Текст на экране как часть подписи: `hasText` требует точного совпадения,
     * а тоннаж в деталях тренировки идёт в одной строке с числом упражнений
     * («1 упражнение · 480 кг»), поэтому здесь ищем подстроку.
     */
    private fun displayedInLabel(text: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
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
