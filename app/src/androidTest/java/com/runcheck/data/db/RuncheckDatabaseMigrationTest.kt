package com.runcheck.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            RuncheckDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrate6To8_validatesSchemaWithExistingData() {
        helper.createDatabase(TEST_DB, 6).apply {
            insertAppBatteryUsageFixture()
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

    @Test
    fun migrate8To9_dropsRedundantPackageNameIndex() {
        helper.createDatabase(TEST_DB, 8).close()

        val database = helper.runMigrationsAndValidate(TEST_DB, 9, true, *DatabaseModule.ALL_MIGRATIONS)
        database.query("PRAGMA index_list(`app_battery_usage`)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                val indexName = cursor.getString(nameColumn)
                check(indexName != "index_app_battery_usage_package_name") {
                    "Redundant single-column package_name index should have been dropped by migration 8→9"
                }
            }
        }
    }

    @Test
    fun migrate6To9_validatesFullSchemaChainWithExistingData() {
        helper.createDatabase(TEST_DB, 6).apply {
            insertAppBatteryUsageFixture()
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 9, true, *DatabaseModule.ALL_MIGRATIONS)
    }

    @Test
    fun migrate6To10_preservesDataAndValidatesFinalSchema() {
        helper.createDatabase(TEST_DB, 6).apply {
            insertFullChainFixture()
            close()
        }

        val database = helper.runMigrationsAndValidate(TEST_DB, 10, true, *DatabaseModule.ALL_MIGRATIONS)

        database.assertTableRowCount("app_battery_usage", 1)
        database.assertTableRowCount("charger_profiles", 1)
        database.assertTableRowCount("charging_sessions", 1)
        database.assertTableRowCount("speed_test_results", 1)
        database.assertTableRowCount("insights", 0)
    }

    @Test
    fun migrate9To10_createsInsightsTableAndIndexes() {
        helper.createDatabase(TEST_DB, 9).close()

        val database = helper.runMigrationsAndValidate(TEST_DB, 10, true, *DatabaseModule.ALL_MIGRATIONS)

        database.query("PRAGMA table_info(`insights`)").use { cursor ->
            check(cursor.count > 0) {
                "Expected insights table columns to exist after migration to version 10"
            }
        }

        database.query("PRAGMA index_list(`insights`)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            var foundGeneratedAtIndex = false
            var foundDismissedPriorityIndex = false
            var foundRuleDedupeIndex = false

            while (cursor.moveToNext()) {
                when (cursor.getString(nameColumn)) {
                    "index_insights_generated_at" -> foundGeneratedAtIndex = true
                    "index_insights_dismissed_expires_at_priority" -> foundDismissedPriorityIndex = true
                    "index_insights_rule_id_dedupe_key" -> foundRuleDedupeIndex = true
                }
            }

            check(foundGeneratedAtIndex) {
                "Expected generated_at index to exist after migration to version 10"
            }
            check(foundDismissedPriorityIndex) {
                "Expected dismissed/expires_at/priority index to exist after migration to version 10"
            }
            check(foundRuleDedupeIndex) {
                "Expected unique rule_id/dedupe_key index to exist after migration to version 10"
            }
        }
    }

    private companion object {
        const val TEST_DB = "runcheck-migration-test"
    }
}

private fun SupportSQLiteDatabase.insertAppBatteryUsageFixture() {
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
        """.trimIndent(),
    )
}

private fun SupportSQLiteDatabase.insertFullChainFixture() {
    insertAppBatteryUsageFixture()
    execSQL(
        """
        INSERT INTO charger_profiles (
            id,
            name,
            created
        ) VALUES (1, 'Desk charger', 1000)
        """.trimIndent(),
    )
    execSQL(
        """
        INSERT INTO charging_sessions (
            id,
            charger_id,
            start_time,
            end_time,
            start_level,
            end_level,
            avg_current_ma,
            max_current_ma,
            avg_voltage_mv,
            avg_power_mw,
            plug_type
        ) VALUES (1, 1, 1000, 2000, 20, 80, 1000, 1500, 5000, 5000, 'USB-C')
        """.trimIndent(),
    )
    execSQL(
        """
        INSERT INTO speed_test_results (
            id,
            timestamp,
            download_mbps,
            upload_mbps,
            ping_ms,
            jitter_ms,
            server_name,
            server_location,
            connection_type,
            network_subtype,
            signal_dbm
        ) VALUES (1, 1000, 100.5, 20.25, 10, NULL, 'M-Lab', 'Helsinki', 'WIFI', NULL, -55)
        """.trimIndent(),
    )
}

private fun SupportSQLiteDatabase.assertTableRowCount(
    tableName: String,
    expectedCount: Int,
) {
    query("SELECT COUNT(*) FROM `$tableName`").use { cursor ->
        check(cursor.moveToFirst()) {
            "Expected row count query for $tableName to return a row"
        }
        check(cursor.getInt(0) == expectedCount) {
            "Expected $tableName to contain $expectedCount row(s) after migration"
        }
    }
}
