package com.gymtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MuscleArea::class,
        Muscle::class,
        Exercise::class,
        ExerciseMuscleLink::class,
        WorkoutSession::class,
        WorkoutExercise::class,
        SetEntry::class,
        ChangeGroup::class,
        ChangePhoto::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(GymConverters::class)
abstract class GymDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun catalogDao(): CatalogDao
    abstract fun changeDao(): ChangeDao

    companion object {
        private const val DB_NAME = "gymtracker.db"

        /** v2: скрытые упражнения экрана «Мышцы». */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercise ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): GymDatabase =
            Room.databaseBuilder(context.applicationContext, GymDatabase::class.java, DB_NAME)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        Seed.statements().forEach { db.execSQL(it) }
                    }
                })
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
