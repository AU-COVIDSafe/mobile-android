package au.gov.health.covidsafe.streetpass.persistence

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * This test class is used as a util to revert the actual db version and to populate it with version one record in order to test the migrations
 */
class DBUtilityTest {
    private val ACTUAL_DB = "record_database"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            StreetPassRecordDatabase::class.java.canonicalName,
            FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun revertDbToVersion1() {
        helper.createDatabase(ACTUAL_DB, 1).apply {
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun populateVersion1Db() {
        var db = helper.createDatabase(ACTUAL_DB, 1).apply {
            // db has schema version 1. insert some data using SQL queries.
            // We cannot use DAO classes because they expect the latest schema.
            for (i in 1..1000) {
                val insertSql = """INSERT INTO record_table values (?,?,?,?,?,?,?,?,?)""".trimIndent()

                execSQL(insertSql, arrayOf(i, System.currentTimeMillis(), 1, "testMessage", "AU_DTA", "modelP", "modelC", i, i))

            }
            close()
        }

    }
}