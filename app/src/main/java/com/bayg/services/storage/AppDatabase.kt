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
import com.bayg.services.storage.daos.DailyUsageDao
import com.bayg.services.storage.daos.UserDao
import com.bayg.services.storage.daos.UserSettingsDao
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.DailyUsage
import com.bayg.services.storage.entities.User
import com.bayg.services.storage.entities.UserSettings
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

@Database(
    entities = [
        User::class,
        UserSettings::class,
        BlockEvent::class,
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
    abstract fun dailyUsageDao(): DailyUsageDao

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
                .addMigrations(MIGRATION_1_2)
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
