package com.gymtracker.data

/**
 * Встроенный каталог: 6 областей, 15 мышц, 46 базовых упражнений.
 * Разметка первичных/вторичных мышц — часть данных каталога (спецификация, раздел 5).
 * Точный список — открытый вопрос спецификации; здесь рабочий стартовый набор,
 * он расширяется без миграции (посев выполняется при создании базы version 1).
 */
object Seed {
    private val areas = listOf(
        1L to "Грудь",
        2L to "Спина",
        3L to "Плечи",
        4L to "Руки",
        5L to "Ноги",
        6L to "Корпус",
    )

    private val muscles = listOf(
        Triple(1L, "Грудь", 1L),
        Triple(2L, "Широчайшие", 2L),
        Triple(3L, "Большая круглая", 2L),
        Triple(4L, "Поясница", 2L),
        Triple(5L, "Передняя дельта", 3L),
        Triple(6L, "Средняя дельта", 3L),
        Triple(7L, "Задняя дельта", 3L),
        Triple(8L, "Бицепс", 4L),
        Triple(9L, "Трицепс", 4L),
        Triple(10L, "Предплечья", 4L),
        Triple(11L, "Квадратичные", 5L),
        Triple(12L, "Бицепс бедра", 5L),
        Triple(13L, "Икры", 5L),
        Triple(14L, "Ягодицы", 5L),
        Triple(15L, "Пресс", 6L),
    )

    private data class CatalogEntry(
        val id: Long,
        val name: String,
        val primary: Long,
        val secondary: List<Long>,
    )

    private val exercises = listOf(
        // Грудь
        CatalogEntry(1L, "Жим штанги лёжа", 1L, listOf(5L, 9L)),
        CatalogEntry(2L, "Жим гантелей лёжа", 1L, listOf(5L, 9L)),
        CatalogEntry(3L, "Жим гантелей на наклонной", 1L, listOf(5L, 9L)),
        CatalogEntry(4L, "Разведение гантелей лёжа", 1L, listOf(5L)),
        CatalogEntry(5L, "Разведение гантелей на наклонной", 1L, listOf(5L)),
        CatalogEntry(6L, "Отжимания на брусьях", 1L, listOf(9L, 5L)),
        CatalogEntry(7L, "Отжимания от пола", 1L, listOf(5L, 9L)),
        CatalogEntry(8L, "Кроссовер", 1L, listOf()),
        CatalogEntry(9L, "Пуловер", 1L, listOf(2L, 3L)),
        // Спина
        CatalogEntry(10L, "Подтягивания", 2L, listOf(8L, 10L, 3L)),
        CatalogEntry(11L, "Тяга верхнего блока", 2L, listOf(8L, 10L)),
        CatalogEntry(12L, "Тяга горизонтального блока сидя", 2L, listOf(8L, 10L, 7L)),
        CatalogEntry(13L, "Тяга штанги в наклоне", 2L, listOf(7L, 8L)),
        CatalogEntry(14L, "Шраги", 6L, listOf(10L)),
        CatalogEntry(15L, "Становая тяга", 4L, listOf(14L, 11L, 15L)),
        CatalogEntry(16L, "Гиперэкстензии", 4L, listOf(14L, 12L)),
        CatalogEntry(17L, "Румынская тяга", 12L, listOf(4L, 14L)),
        // Плечи
        CatalogEntry(18L, "Жим штанги сидя", 5L, listOf(6L, 9L)),
        CatalogEntry(19L, "Жим гантелей сидя", 5L, listOf(6L, 9L)),
        CatalogEntry(20L, "Разведение гантелей стоя", 6L, listOf(5L)),
        CatalogEntry(21L, "Тяга к подбородку", 6L, listOf(8L)),
        CatalogEntry(22L, "Разведение гантелей в наклоне", 7L, listOf(3L)),
        CatalogEntry(23L, "Подъём гантелей перед собой", 5L, listOf(6L)),
        // Руки
        CatalogEntry(24L, "Подъём гантелей на бицепс", 8L, listOf(10L)),
        CatalogEntry(25L, "Подъём штанги на бицепс", 8L, listOf(10L)),
        CatalogEntry(26L, "Подъём гантелей на скамье Скотта", 8L, listOf(10L)),
        CatalogEntry(27L, "Сгибания «молот»", 8L, listOf(10L)),
        CatalogEntry(28L, "Французский жим", 9L, listOf()),
        CatalogEntry(29L, "Разгибание на блоке", 9L, listOf()),
        CatalogEntry(30L, "Разгибание гантели из-за головы", 9L, listOf()),
        CatalogEntry(31L, "Отжимания узким хватом", 9L, listOf(1L)),
        CatalogEntry(32L, "Сгибание запястий", 10L, listOf(8L)),
        // Ноги
        CatalogEntry(33L, "Приседания со штангой", 11L, listOf(14L, 15L)),
        CatalogEntry(34L, "Фронтальные приседания", 11L, listOf(15L, 14L)),
        CatalogEntry(35L, "Жим ногами", 11L, listOf(14L, 12L)),
        CatalogEntry(36L, "Выпады", 11L, listOf(14L, 12L)),
        CatalogEntry(37L, "Сгибание ног в тренажёре", 12L, listOf()),
        CatalogEntry(38L, "Разгибание ног в тренажёре", 11L, listOf()),
        CatalogEntry(39L, "Подъёмы на носки стоя", 13L, listOf()),
        CatalogEntry(40L, "Подъёмы на носки сидя", 13L, listOf()),
        CatalogEntry(41L, "Ягодичный мостик", 14L, listOf(15L)),
        CatalogEntry(42L, "Отведение ноги в тренажёре", 14L, listOf()),
        // Корпус
        CatalogEntry(43L, "Скручивания", 15L, listOf()),
        CatalogEntry(44L, "Подъём ног в висе", 15L, listOf()),
        CatalogEntry(45L, "Планка", 15L, listOf()),
        CatalogEntry(46L, "Скручивания на блоке", 15L, listOf()),
    )

    private val presetGroups = listOf("Вид спереди", "Вид сзади", "Вид слева", "Вид справа")

    private fun esc(value: String) = value.replace("'", "''")

    fun statements(): List<String> = buildList {
        areas.forEach { (id, name) ->
            add("INSERT INTO muscle_area (id, name) VALUES ($id, '${esc(name)}')")
        }
        muscles.forEach { (id, name, areaId) ->
            add("INSERT INTO muscle (id, name, areaId) VALUES ($id, '${esc(name)}', $areaId)")
        }
        exercises.forEach { e ->
            // Поле isHidden (версия 2 базы) обязательно имеет значение: схема Room
            // создаёт колонку без DEFAULT, поэтому посев задаёт его явно.
            add(
                "INSERT INTO exercise (id, name, type, imagePath, isDeleted, isHidden) " +
                    "VALUES (${e.id}, '${esc(e.name)}', 'CATALOG', NULL, 0, 0)"
            )
            add(
                "INSERT INTO exercise_muscle (exerciseId, muscleId, role) " +
                    "VALUES (${e.id}, ${e.primary}, 'PRIMARY')"
            )
            e.secondary.forEach { mid ->
                add(
                    "INSERT INTO exercise_muscle (exerciseId, muscleId, role) " +
                        "VALUES (${e.id}, $mid, 'SECONDARY')"
                )
            }
        }
        presetGroups.forEachIndexed { i, name ->
            add("INSERT INTO change_group (id, name, createdAt, isPreset) VALUES (${i + 1}, '${esc(name)}', 0, 1)")
        }
    }
}
