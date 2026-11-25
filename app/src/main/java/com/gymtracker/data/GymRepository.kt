package com.gymtracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class GymRepository(
    private val sessionDao: SessionDao,
    private val catalogDao: CatalogDao,
    private val changeDao: ChangeDao,
    private val photosDir: File,
) {
    /** Результат выбора упражнения в экране A4: id выбранного упражнения. */
    val pickerResult = MutableStateFlow<Long?>(null)

    fun clearPickerResult() {
        pickerResult.value = null
    }

    // ——— Экран A1: счётчики ———
    fun lastSession(): Flow<WorkoutSession?> = sessionDao.lastSession()
    fun countSince(sinceMillis: Long): Flow<Int> = sessionDao.countSince(sinceMillis)
    fun countAll(): Flow<Int> = sessionDao.countAll()

    // ——— Сессия ———
    fun activeSession(): Flow<WorkoutSession?> = sessionDao.activeSession()
    fun blocksForSession(sessionId: Long): Flow<List<BlockWithExercise>> =
        sessionDao.blocksForSession(sessionId)

    suspend fun startSession(): Long =
        sessionDao.insertSession(WorkoutSession(startMillis = System.currentTimeMillis()))

    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long): Long {
        val order = (sessionDao.maxBlockOrder(sessionId) ?: 0) + 1
        return sessionDao.insertBlock(
            WorkoutExercise(
                sessionId = sessionId,
                exerciseId = exerciseId,
                orderInSession = order,
                status = BlockStatus.ACTIVE,
            )
        )
    }

    suspend fun lastWeightForExercise(exerciseId: Long): Double? =
        sessionDao.lastWeightForExercise(exerciseId)

    suspend fun lastRepsForExercise(exerciseId: Long): Int? =
        sessionDao.lastRepsForExercise(exerciseId)

    suspend fun addSet(blockId: Long, reps: Int, weightKg: Double) {
        val order = (sessionDao.maxSetOrder(blockId) ?: 0) + 1
        sessionDao.insertSet(
            SetEntry(
                workoutExerciseId = blockId,
                orderInExercise = order,
                weightKg = weightKg,
                reps = reps,
            )
        )
    }

    /** Правка подхода в текущей сессии. */
    suspend fun editSet(setId: Long, weightKg: Double, reps: Int) =
        sessionDao.updateSet(setId, weightKg, reps)

    /** Удаление подхода из текущей сессии. */
    suspend fun removeSet(setId: Long) = sessionDao.deleteSet(setId)

    suspend fun finishBlock(blockId: Long) =
        sessionDao.setBlockStatus(blockId, BlockStatus.DONE)

    suspend fun resumeBlock(blockId: Long) =
        sessionDao.setBlockStatus(blockId, BlockStatus.ACTIVE)

    suspend fun deleteBlock(blockId: Long) = sessionDao.deleteBlock(blockId)

    suspend fun completeSession(sessionId: Long) =
        sessionDao.finishSession(sessionId, System.currentTimeMillis())

    suspend fun deleteSession(sessionId: Long) = sessionDao.deleteSessionById(sessionId)

    suspend fun activeSessionOnce(): WorkoutSession? = sessionDao.activeSessionOnce()

    // ——— Журнал ———
    fun finishedSessions(): Flow<List<SessionRow>> = sessionDao.finishedSessions()
    fun sessionById(sessionId: Long): Flow<SessionRow> = sessionDao.sessionById(sessionId)
    /** Основные мышцы каждой завершённой тренировки — подписи строк журнала. */
    fun sessionPrimaryMuscles(): Flow<List<SessionPrimaryMuscle>> = sessionDao.sessionPrimaryMuscles()

    // ——— Каталог ———
    fun areas(): Flow<List<MuscleArea>> = catalogDao.areas()
    fun musclesForArea(areaId: Long): Flow<List<Muscle>> = catalogDao.musclesForArea(areaId)
    fun activeExercises(): Flow<List<Exercise>> = catalogDao.activeExercises()
    fun exercisesByArea(areaId: Long, query: String): Flow<List<Exercise>> =
        catalogDao.exercisesByArea(areaId, query)
    /** A4: фильтр «Все» — упражнения всех областей. */
    fun exercisesAll(query: String): Flow<List<Exercise>> = catalogDao.exercisesAll(query)
    fun muscleLabels(): Flow<List<MuscleLabel>> = catalogDao.muscleLabels()
    /** C1: видимые/скрытые упражнения выбранной мышцы — отдельными потоками. */
    fun exercisesForMuscle(muscleId: Long, hidden: Boolean): Flow<List<Exercise>> =
        catalogDao.exercisesForMuscle(muscleId, hidden)

    /** Иконка глаза: скрыть упражнение или вернуть его в общий список. */
    suspend fun setExerciseHidden(id: Long, hidden: Boolean) =
        catalogDao.setExerciseHidden(id, hidden)
    fun exerciseById(id: Long): Flow<Exercise?> = catalogDao.exerciseById(id)
    suspend fun exerciseByIdOnce(id: Long): Exercise? = catalogDao.exerciseByIdOnce(id)

    suspend fun createExercise(
        name: String,
        primaryMuscleIds: List<Long>,
        secondaryMuscleIds: List<Long>,
        imagePath: String?,
        isBodyweight: Boolean = false,
    ): Long {
        val id = catalogDao.insertExercise(
            Exercise(name = name, type = ExerciseType.CUSTOM, imagePath = imagePath, isBodyweight = isBodyweight)
        )
        primaryMuscleIds.forEach {
            catalogDao.insertLink(ExerciseMuscleLink(id, it, MuscleRole.PRIMARY))
        }
        secondaryMuscleIds.forEach {
            catalogDao.insertLink(ExerciseMuscleLink(id, it, MuscleRole.SECONDARY))
        }
        return id
    }

    /** C3c: форма редактирования — те же поля, что при создании. */
    suspend fun updateExercise(
        id: Long,
        name: String,
        primaryMuscleIds: List<Long>,
        secondaryMuscleIds: List<Long>,
        imagePath: String?,
        isBodyweight: Boolean,
    ) {
        val current = catalogDao.exerciseByIdOnce(id) ?: return
        catalogDao.updateExercise(current.copy(name = name, imagePath = imagePath, isBodyweight = isBodyweight))
        catalogDao.deleteLinks(id)
        primaryMuscleIds.forEach {
            catalogDao.insertLink(ExerciseMuscleLink(id, it, MuscleRole.PRIMARY))
        }
        secondaryMuscleIds.forEach {
            catalogDao.insertLink(ExerciseMuscleLink(id, it, MuscleRole.SECONDARY))
        }
    }

    suspend fun setExercisePhoto(id: Long, imagePath: String?) {
        val current = catalogDao.exerciseByIdOnce(id) ?: return
        catalogDao.updateExercise(current.copy(imagePath = imagePath))
    }

    suspend fun softDeleteExercise(id: Long) = catalogDao.softDeleteExercise(id)

    fun allMuscles(): Flow<List<Muscle>> = catalogDao.allMuscles()
    fun musclesForExercise(exerciseId: Long): Flow<List<MuscleWithRole>> =
        catalogDao.musclesForExercise(exerciseId)
    fun linksForExercise(exerciseId: Long): Flow<List<ExerciseMuscleLink>> =
        catalogDao.linksForExercise(exerciseId)

    // ——— График и история упражнения (карточка C3) ———
    fun maxWeightPoints(exerciseId: Long): Flow<List<WeightPoint>> =
        sessionDao.maxWeightPoints(exerciseId)

    fun exerciseSessions(exerciseId: Long): Flow<List<ExerciseSessionRow>> =
        sessionDao.exerciseSessions(exerciseId)

    fun exerciseHistorySets(exerciseId: Long): Flow<List<ExerciseHistorySet>> =
        sessionDao.exerciseHistorySets(exerciseId)

    // ——— Изменения ———
    fun changeGroups(): Flow<List<ChangeGroupRow>> = changeDao.groups()
    fun photosForGroup(groupId: Long): Flow<List<ChangePhoto>> = changeDao.photosForGroup(groupId)

    suspend fun createChangeGroup(name: String): Long =
        changeDao.insertGroup(
            ChangeGroup(name = name, createdAt = System.currentTimeMillis(), isPreset = false)
        )

    suspend fun addChangePhoto(groupId: Long, sourcePath: String): Long {
        val target = copyPhotoIntoAppCatalog(sourcePath)
        return changeDao.insertPhoto(
            ChangePhoto(groupId = groupId, imagePath = target.absolutePath, takenAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteChangePhoto(id: Long) = changeDao.deletePhoto(id)

    /**
     * Копирование снимка из галереи/камеры (content-URI) в каталог приложения.
     * Возвращает путь сохранённого файла.
     */
    suspend fun importPhoto(resolver: android.content.ContentResolver, uri: android.net.Uri): String {
        photosDir.mkdirs()
        val target = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Не открыт поток для $uri" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target.absolutePath
    }

    /** Файл приложения не удаляется при обновлении версии; в базу идёт только путь. */
    private fun copyPhotoIntoAppCatalog(sourcePath: String): File {
        val source = File(sourcePath)
        photosDir.mkdirs()
        val target = File(photosDir, "photo_${System.currentTimeMillis()}_${source.name}")
        source.inputStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        return target
    }
}
