package com.runcheck.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.di.DatabaseModule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuncheckDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RuncheckDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate6To8_preservesDataAndValidatesSchema() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """
                INSERT INTO app_battery_usage (
                    id,
                    timestamp,
                    package_name,
                    app_label,
                    foreground_time_ms,
                    estimated_drain_mah
                ) VALUES (1, 1000, 'com.runcheck.test', 'runcheck', 1234, NULL)
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 8, true, *DatabaseModule.ALL_MIGRATIONS)
    }

    @Test
    fun migrate7To8_addsCompositeAppUsageIndex() {
        helper.createDatabase(TEST_DB, 7).close()

        val database = helper.runMigrationsAndValidate(TEST_DB, 8, true, *DatabaseModule.ALL_MIGRATIONS)
        database.query("PRAGMA index_list(`app_battery_usage`)").use { cursor ->
            var foundCompositeIndex = false
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == "index_app_battery_usage_package_name_timestamp") {
                    foundCompositeIndex = true
                    break
                }
            }
            check(foundCompositeIndex) {
                "Expected composite app_battery_usage index to exist after migration to version 8"
            }
        }
    }

    private companion object {
        const val TEST_DB = "runcheck-migration-test"
    }
}
