package au.gov.health.covidsafe.streetpass.persistence

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import au.gov.health.covidsafe.LocalBlobV2
import au.gov.health.covidsafe.logging.CentralLog
import au.gov.health.covidsafe.status.persistence.StatusRecord
import au.gov.health.covidsafe.status.persistence.StatusRecordDao
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.concurrent.thread


const val CURRENT_DB_VERSION = 3

@Database(
        entities = [StreetPassRecord::class, StatusRecord::class],
        version = CURRENT_DB_VERSION,
        exportSchema = true
)
abstract class StreetPassRecordDatabase : RoomDatabase() {

    abstract fun recordDao(): StreetPassRecordDao
    abstract fun statusDao(): StatusRecordDao

    companion object {

        private val TAG = this.javaClass.simpleName

        private const val ID_COLUMN_INDEX = 0
        private const val TIMESTAMP_COLUMN_INDEX = 1
        private const val VERSION_COLUMN_INDEX = 2
        private const val MESSAGE_COLUMN_INDEX = 3
        private const val ORG_COLUMN_INDEX = 4
        private const val MODELP_COLUMN_INDEX = 5
        private const val MODELC_COLUMN_INDEX = 6
        private const val RSSI_COLUMN_INDEX = 7
        private const val TX_POWER_COLUMN_INDEX = 8

        private const val EMPTY_DICT = "{}"
        private val EMPTY_DICT_BYTE_ARRAY = EMPTY_DICT.toByteArray(Charsets.UTF_8)

        val ENCRYPTED_EMPTY_DICT = Encryption.encryptPayload(EMPTY_DICT_BYTE_ARRAY)

        const val VERSION_ONE = 1
        const val VERSION_TWO = 2

        const val DUMMY_DEVICE = ""
        const val DUMMY_RSSI = 999
        const val DUMMY_TXPOWER = 999

        var migrationCallback: MigrationCallBack? = null

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: StreetPassRecordDatabase? = null

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                migrationCallback?.migrationFinished()
            }
        }

        fun getDatabase(context: Context, migrationCallBack: MigrationCallBack? = null): StreetPassRecordDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            this.migrationCallback = migrationCallBack
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context,
                        StreetPassRecordDatabase::class.java,
                        "record_database"
                )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .addCallback(CALLBACK)
                        .build()
                INSTANCE = instance
                return instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //adding of status table
                database.execSQL("CREATE TABLE IF NOT EXISTS `status_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `msg` TEXT NOT NULL)")

                if (!isFieldExist(database, "record_table", "v")) {
                    database.execSQL("ALTER TABLE `record_table` ADD COLUMN `v` INTEGER NOT NULL DEFAULT 1")
                }

                if (!isFieldExist(database, "record_table", "org")) {
                    database.execSQL("ALTER TABLE `record_table` ADD COLUMN `org` TEXT NOT NULL DEFAULT 'AU_DTA'")
                }

            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrationCallback?.migrationStarted()
                //adding a temporary encrypted encounters table for the migration of old data
                database.execSQL("CREATE TABLE IF NOT EXISTS `encrypted_record_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `v` INTEGER NOT NULL, `org` TEXT NOT NULL, `localBlob` TEXT NOT NULL, `remoteBlob` TEXT NOT NULL)")

                encryptExistingRecords(database)

                database.execSQL("DROP TABLE `record_table`")

                database.execSQL("ALTER TABLE `encrypted_record_table` RENAME TO `record_table`")
            }
        }

        fun encryptExistingRecords(db: SupportSQLiteDatabase) {


            val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

            val allRecs = db.query("SELECT * FROM record_table")
            CentralLog.d(TAG, "starting encryption of ${allRecs.count} records")
            if (allRecs.moveToFirst()) {
                do {
                    val contentValues = ContentValues()
                    val id = allRecs.getInt(ID_COLUMN_INDEX)
                    val version = allRecs.getInt(VERSION_COLUMN_INDEX)
                    val timestamp = allRecs.getLong(TIMESTAMP_COLUMN_INDEX)
                    val msg = allRecs.getString(MESSAGE_COLUMN_INDEX)
                    val org = allRecs.getString(ORG_COLUMN_INDEX)
                    val modelP = allRecs.getString(MODELP_COLUMN_INDEX)
                    val modelC = allRecs.getString(MODELC_COLUMN_INDEX)
                    val rssi = allRecs.getInt(RSSI_COLUMN_INDEX)
                    val txPower = allRecs.getInt(TX_POWER_COLUMN_INDEX)
                    val plainRecord = gson.toJson(EncryptedRecord(modelP, modelC, rssi, txPower, msg)).toByteArray(Charsets.UTF_8)
                    val remoteBlob: String = if (version == 1) {
                        Encryption.encryptPayload(plainRecord)
                    } else {
                        msg
                    }
                    val localBlob: String = if (version == 1) {
                        ENCRYPTED_EMPTY_DICT
                    } else {
                        val modelP = if (DUMMY_DEVICE == modelP) null else modelP
                        val modelC = if (DUMMY_DEVICE == modelC) null else modelC
                        val rssi = if (DUMMY_RSSI == rssi) null else rssi
                        val txPower = if (DUMMY_TXPOWER == txPower) null else txPower
                        val plainRecord = gson.toJson(LocalBlobV2(modelP, modelC, rssi, txPower)).toByteArray(Charsets.UTF_8)
                        Encryption.encryptPayload(plainRecord)
                    }
                    contentValues.put("v", VERSION_TWO)
                    contentValues.put("org", org)
                    contentValues.put("localBlob", localBlob)
                    contentValues.put("remoteBlob", remoteBlob)
                    contentValues.put("id", id)
                    contentValues.put("timestamp", timestamp)
                    db.insert("encrypted_record_table", CONFLICT_REPLACE, contentValues)
                } while (allRecs.moveToNext())
            }
            CentralLog.d(TAG, "encryption done")
        }

        class EncryptedRecord(var modelP: String, var modelC: String, var rssi: Int, var txPower: Int?, var msg: String)

        // This method will check if column exists in your table
        fun isFieldExist(db: SupportSQLiteDatabase, tableName: String, fieldName: String): Boolean {
            var isExist = false
            val res =
                    db.query("PRAGMA table_info($tableName)", null)
            res.moveToFirst()
            do {
                val currentColumn = res.getString(1)
                if (currentColumn == fieldName) {
                    isExist = true
                }
            } while (res.moveToNext())
            return isExist
        }
    }
}

interface MigrationCallBack {
    fun migrationStarted()
    fun migrationFinished()
}