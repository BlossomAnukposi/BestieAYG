package com.bayg.services.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bayg.services.storage.daos.BlockEventDao
import com.bayg.services.storage.daos.DailyUsageDao
import com.bayg.services.storage.daos.StreakDao
import com.bayg.services.storage.daos.UserDao
import com.bayg.services.storage.daos.UserSettingsDao
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.DailyUsage
import com.bayg.services.storage.entities.Streak
import com.bayg.services.storage.entities.User
import com.bayg.services.storage.entities.UserSettings

@Database(
    entities = [
        User::class,
        UserSettings::class,
        BlockEvent::class,
        Streak::class,
        DailyUsage::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun blockEventDao(): BlockEventDao
    abstract fun streakDao(): StreakDao
    abstract fun dailyUsageDao(): DailyUsageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_usage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `firebaseId` TEXT NOT NULL DEFAULT '',
                        `syncedAt` INTEGER,
                        `userId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `usageMinutes` INTEGER NOT NULL DEFAULT 0,
                        `blockCount` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_daily_usage_userId` ON `daily_usage` (`userId`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_usage_userId_date` " +
                            "ON `daily_usage` (`userId`, `date`)"
                )

                db.execSQL(
                    "ALTER TABLE `block_events` ADD COLUMN `label` TEXT NOT NULL DEFAULT 'Daily limit exceeded'"
                )
                db.execSQL(
                    "ALTER TABLE `block_events` ADD COLUMN `severity` TEXT NOT NULL DEFAULT 'RED'"
                )
                db.execSQL(
                    "ALTER TABLE `block_events` ADD COLUMN `detail` TEXT"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bayg.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
