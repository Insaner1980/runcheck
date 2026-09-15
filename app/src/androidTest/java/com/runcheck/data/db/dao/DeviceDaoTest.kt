package com.runcheck.data.db.dao

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.data.db.RuncheckDatabase
import com.runcheck.data.db.entity.DeviceEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceDaoTest {
    private lateinit var database: RuncheckDatabase
    private lateinit var dao: DeviceDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    RuncheckDatabase::class.java,
                ).build()
        dao = database.deviceDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun replaceCurrent_initializesEmptyTable() =
        runBlocking {
            val current = device("a")

            dao.replaceCurrent(current)

            assertEquals(listOf(current), allDevices())
        }

    @Test
    fun replaceCurrent_replacesSameIdentity() =
        runBlocking {
            val old = device("a")
            dao.insertOrUpdate(old)
            val current =
                old.copy(
                    manufacturer = "Updated manufacturer",
                    model = "Updated model",
                    apiLevel = 36,
                    firstSeen = 200L,
                    profileJson = "{\"updated\":true}",
                )

            dao.replaceCurrent(current)

            assertEquals(listOf(current), allDevices())
        }

    @Test
    fun replaceCurrent_removesAllOtherProfiles() =
        runBlocking {
            dao.insertOrUpdate(device("a"))
            dao.insertOrUpdate(device("b"))
            val current = device("c")

            dao.replaceCurrent(current)

            assertEquals(listOf(current), allDevices())
        }

    @Test
    fun replaceCurrent_rollsBackInsertWhenCleanupFails() =
        runBlocking {
            val previous = listOf(device("a"), device("b"))
            previous.forEach { dao.insertOrUpdate(it) }
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_device_cleanup BEFORE DELETE ON devices
                BEGIN
                    SELECT RAISE(ABORT, 'device cleanup failed');
                END
                """.trimIndent(),
            )

            val failure = runCatching { dao.replaceCurrent(device("c")) }.exceptionOrNull()

            assertTrue(failure is SQLiteException)
            assertTrue(failure?.message.orEmpty().contains("device cleanup failed"))
            assertEquals(previous, allDevices())
        }

    private fun allDevices(): List<DeviceEntity> =
        database.openHelper.readableDatabase.query("SELECT * FROM devices ORDER BY id").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DeviceEntity(
                            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                            manufacturer = cursor.getString(cursor.getColumnIndexOrThrow("manufacturer")),
                            model = cursor.getString(cursor.getColumnIndexOrThrow("model")),
                            apiLevel = cursor.getInt(cursor.getColumnIndexOrThrow("api_level")),
                            firstSeen = cursor.getLong(cursor.getColumnIndexOrThrow("first_seen")),
                            profileJson = cursor.getString(cursor.getColumnIndexOrThrow("profile_json")),
                        ),
                    )
                }
            }
        }

    private fun device(id: String): DeviceEntity =
        DeviceEntity(
            id = id,
            manufacturer = "Manufacturer $id",
            model = "Model $id",
            apiLevel = 35,
            firstSeen = 100L,
            profileJson = "{\"model\":\"Model $id\"}",
        )
}
