package com.gymtracker.util

/** Русская плюрализация: (одна, две, много). */
fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && !(mod100 in 12..14) -> few
        else -> many
    }
}

fun workoutsWord(n: Int): String = plural(n, "тренировка", "тренировки", "тренировок")

fun exercisesWord(n: Int): String = plural(n, "упражнение", "упражнения", "упражнений")

fun photosWord(n: Int): String = plural(n, "изображение", "изображения", "изображений")

/** «12 тренировок», «1 тренировка». */
fun workoutsLabel(n: Int): String = "$n ${workoutsWord(n)}"

/** «3 упражнения». */
fun exercisesLabel(n: Int): String = "$n ${exercisesWord(n)}"

/** Число с группировкой разрядов через неразрывный пробел: 1 495. */
fun groupNumber(n: Int): String {
    val s = n.toString()
    return buildString {
        s.reversed().forEachIndexed { i, c ->
            if (i > 0 && i % 3 == 0) append(' ')
            append(c)
        }
    }.reversed()
}

/** Формат подхода: «5 × 85 кг» (повторы, затем вес) или «12 повт.» для собственного веса. */
fun formatSet(weightKg: Double, reps: Int, isBodyweight: Boolean): String {
    return if (isBodyweight) "$reps повт." else "$reps × ${formatWeight(weightKg)} кг"
}

/** Вес: целые без знака, дробные — с одним знаком («40», «22.5»). */
fun formatWeight(kg: Double): String {
    val rounded = Math.round(kg * 10) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        rounded.toString().replace('.', ',')
    }
}
