package com.bayg.services.storage

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bayg.security.DbPassphraseProvider
import com.bayg.services.storage.daos.BlockEventDao
import com.bayg.services.storage.daos.UserDao
import com.bayg.services.storage.daos.UserSettingsDao
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.User
import com.bayg.services.storage.entities.UserSettings
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

@Database(
    entities = [
        User::class,
        UserSettings::class,
        BlockEvent::class,
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DB_NAME = "bayg.db"

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Streak is no longer a stored/synced entity, it's computed on
                // demand by using data from BlockEvent now.
                db.execSQL("DROP TABLE IF EXISTS `streak`")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE block_events_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firebaseId TEXT,
                        syncedAt INTEGER,
                        userId TEXT NOT NULL,
                        triggeredAt INTEGER NOT NULL,
                        blockDurationMinutes INTEGER NOT NULL,
                        label TEXT NOT NULL DEFAULT 'Daily limit exceeded',
                        severity TEXT NOT NULL DEFAULT 'RED',
                        detail TEXT,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Collapse existing duplicates from the bug: one row per real
                // firebaseId, and every never-synced ('' firebaseId) row kept as-is.
                db.execSQL(
                    """
                        INSERT INTO block_events_new
                        SELECT id, NULLIF(firebaseId, ''), syncedAt, userId, triggeredAt,
                               blockDurationMinutes, label, severity, detail
                        FROM block_events
                        GROUP BY CASE WHEN firebaseId = '' THEN id ELSE firebaseId END
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE block_events")
                db.execSQL("ALTER TABLE block_events_new RENAME TO block_events")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_block_events_userId ON block_events(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_block_events_triggeredAt ON block_events(triggeredAt)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_block_events_firebaseId ON block_events(firebaseId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Stats now reads live from UsageStatsManager instead of caching
                // daily Instagram totals in Room. The table was never written to
                // by any code path, so dropping it is safe.
                db.execSQL("DROP TABLE IF EXISTS `daily_usage`")
                db.execSQL("DROP INDEX IF EXISTS `index_daily_usage_userId`")
                db.execSQL("DROP INDEX IF EXISTS `index_daily_usage_userId_date`")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // block_events.userId is a Firebase UID (TEXT), but the historical
                // schema (introduced in MIGRATION_3_4) carried a foreign key
                // referencing users.id (INTEGER). That FK rejects every insert
                // because no users row matches a Firebase UID string. SQLite has no
                // DROP CONSTRAINT, so rebuild the table without the FK while
                // carrying all existing rows forward unchanged.
                db.execSQL(
                    """
                    CREATE TABLE block_events_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firebaseId TEXT,
                        syncedAt INTEGER,
                        userId TEXT NOT NULL,
                        triggeredAt INTEGER NOT NULL,
                        blockDurationMinutes INTEGER NOT NULL,
                        label TEXT NOT NULL DEFAULT 'Daily limit exceeded',
                        severity TEXT NOT NULL DEFAULT 'RED',
                        detail TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO block_events_new
                    SELECT id, firebaseId, syncedAt, userId, triggeredAt,
                           blockDurationMinutes, label, severity, detail
                    FROM block_events
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE block_events")
                db.execSQL("ALTER TABLE block_events_new RENAME TO block_events")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_block_events_userId ON block_events(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_block_events_triggeredAt ON block_events(triggeredAt)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_block_events_firebaseId ON block_events(firebaseId)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildEncryptedDatabase(context.applicationContext)
                    .also { INSTANCE = it }
            }

        /**
         * Build the Room database backed by SQLCipher. The passphrase is
         * provided by [DbPassphraseProvider] (256-bit random, stored in
         * EncryptedSharedPreferences which is itself protected by the
         * Android Keystore).
         *
         * On first launch with this version we may find a leftover
         * plaintext `bayg.db` from a previous install. SQLCipher cannot
         * open a plaintext DB with a key, so we delete the old file. The
         * SyncWorker re-pulls user settings from Firestore on next login,
         * so the user does not lose any data that was already synced to
         * the cloud.
         */
        private fun buildEncryptedDatabase(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")

            val passphrase = DbPassphraseProvider.create(context).loadOrCreate()
            deletePlaintextDbIfPresent(context, passphrase)

            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
        }

        /**
         * If the database file on disk starts with the literal magic
         * string `SQLite format 3\0`, it was created by the pre-SQLCipher
         * build and SQLCipher cannot open it with the new passphrase.
         * Delete it so the SQLCipher path can create a fresh encrypted DB.
         *
         * SQLCipher-encrypted databases start with the 16-byte random
         * salt, not the magic string, so this check correctly leaves
         * already-encrypted databases alone.
         */
        private fun deletePlaintextDbIfPresent(context: Context, passphrase: ByteArray) {
            val dbFile: File = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists() || dbFile.length() < SQLITE_MAGIC.size) return
            // Read the first 16 bytes the API-24-compatible way. The length
            // check above guarantees the file has at least that many bytes,
            // so a single `read` call is sufficient and we do not need
            // `InputStream.readNBytes`, which only exists on API 33+.
            val firstBytes = ByteArray(SQLITE_MAGIC.size)
            dbFile.inputStream().use { stream ->
                var offset = 0
                while (offset < firstBytes.size) {
                    val n = stream.read(firstBytes, offset, firstBytes.size - offset)
                    if (n <= 0) break
                    offset += n
                }
            }
            if (firstBytes.contentEquals(SQLITE_MAGIC)) {
                Log.w(TAG, "Plaintext bayg.db detected, deleting so SQLCipher can recreate.")
                context.deleteDatabase(DB_NAME)
            }
            // Reference the passphrase so the JIT cannot reorder this method
            // ahead of the caller having loaded the key, even though we do
            // not use it directly here.
            @Suppress("UNUSED_VARIABLE")
            val unused = passphrase
        }

        // "SQLite format 3" + null terminator. Present at offset 0 of any
        // unencrypted SQLite v3 database file.
        private val SQLITE_MAGIC: ByteArray = byteArrayOf(
            0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66,
            0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00,
        )
    }
}
