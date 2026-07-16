package com.runcheck.data.storage

import android.app.admin.DevicePolicyManager
import android.os.Environment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.StringReader

class StorageDataSourcePublicInfoTest {
    @Test
    fun `data file system parser accepts mounts whitespace and unusual options`() {
        val mounts =
            sequenceOf(
                "tmpfs /tmp tmpfs rw,nosuid,nodev 0 0",
                "/dev/block/dm-0\t/data\tf2fs\trw,lazytime,nosuid,nodev,noatime,background_gc=on 0 0",
            )

        assertEquals("f2fs", parseDataFileSystemType(mounts))
    }

    @Test
    fun `data file system parser returns null when data mount is missing`() {
        assertEquals(null, parseDataFileSystemType(sequenceOf("tmpfs /tmp tmpfs rw 0 0")))
    }

    @Test
    fun `data file system read returns null when access is denied`() {
        val result =
            readDataFileSystemType(
                openMounts = { throw SecurityException("Access denied") },
            )

        assertEquals(null, result)
    }

    @Test
    fun `data file system read parses provided mounts`() {
        val result =
            readDataFileSystemType(
                openMounts = {
                    BufferedReader(StringReader("/dev/block/dm-0 /data ext4 rw 0 0"))
                },
            )

        assertEquals("ext4", result)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `mapStorageEncryptionStatus maps public Android encryption states`() {
        assertEquals("FBE", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER))
        assertEquals("Encrypted", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE))
        assertEquals(
            "Encrypted (default key)",
            mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY),
        )
        assertEquals("Activating", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING))
        assertEquals("Inactive", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE))
        assertEquals("Unsupported", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED))
        assertEquals(null, mapStorageEncryptionStatus(Int.MAX_VALUE))
    }

    @Test
    fun `countDistinctPackageNames counts only nonblank unique packages`() {
        assertEquals(2, countDistinctPackageNames(listOf("com.alpha", "com.beta", "com.alpha", "")))
        assertEquals(0, countDistinctPackageNames(emptyList()))
        assertEquals(0, countDistinctPackageNames(listOf("", " ")))
    }

    @Test
    fun `aggregate app stats preserve genuine zero values`() {
        val stats =
            queryAggregateAppStats {
                AppStorageByteBreakdown(appBytes = 0L, dataBytes = 0L, cacheBytes = 0L)
            }

        assertEquals(AggregateAppStats(totalBytes = 0L, cacheBytes = 0L), stats)
    }

    @Test
    fun `aggregate app total does not count cache twice`() {
        val stats =
            queryAggregateAppStats {
                AppStorageByteBreakdown(appBytes = 100L, dataBytes = 40L, cacheBytes = 10L)
            }

        assertEquals(AggregateAppStats(totalBytes = 140L, cacheBytes = 10L), stats)
    }

    @Test
    fun `aggregate app stats are unavailable when platform denies query`() {
        val stats = queryAggregateAppStats { throw SecurityException("Usage access denied") }

        assertEquals(null, stats)
    }

    @Test
    fun `portable storage selection excludes primary adopted and unmounted volumes`() {
        val portable = File("portable")
        val candidates =
            listOf(
                storageCandidate(File("primary"), isPrimary = true),
                storageCandidate(File("adopted"), isEmulated = true),
                storageCandidate(File("unmounted"), state = Environment.MEDIA_UNMOUNTED),
                storageCandidate(portable),
                storageCandidate(portable),
            )

        assertEquals(listOf(portable), selectMountedPortableStorageDirectories(candidates))
    }

    @Test
    fun `portable storage selection accepts read only mounted volume`() {
        val readOnly = File("read-only")

        assertEquals(
            listOf(readOnly),
            selectMountedPortableStorageDirectories(
                listOf(storageCandidate(readOnly, state = Environment.MEDIA_MOUNTED_READ_ONLY)),
            ),
        )
    }

    @Test
    fun `storage space aggregation requires every capacity and prevents overflow`() {
        assertEquals(
            StorageSpace(totalBytes = 300L, availableBytes = 100L),
            aggregateStorageSpaces(
                listOf(StorageSpace(100L, 40L), StorageSpace(200L, 60L)),
            ),
        )
        assertEquals(null, aggregateStorageSpaces(listOf(StorageSpace(100L, 40L), null)))
        assertEquals(
            null,
            aggregateStorageSpaces(listOf(StorageSpace(Long.MAX_VALUE, 0L), StorageSpace(1L, 0L))),
        )
    }

    @Test
    fun `primary storage fallback never mixes StorageStats and StatFs values`() {
        val fallback = StorageSpace(totalBytes = 80L, availableBytes = 30L)

        assertEquals(
            fallback,
            queryPrimaryStorageSpace(
                storageStatsQuery = { throw IllegalStateException("free query failed") },
                statFsQuery = { fallback },
            ),
        )
    }

    @Test
    fun `primary storage rejects an inconsistent StorageStats pair`() {
        val fallback = StorageSpace(totalBytes = 80L, availableBytes = 30L)

        assertEquals(
            fallback,
            queryPrimaryStorageSpace(
                storageStatsQuery = { StorageSpace(totalBytes = 100L, availableBytes = 101L) },
                statFsQuery = { fallback },
            ),
        )
    }

    private fun storageCandidate(
        directory: File,
        isPrimary: Boolean = false,
        isRemovable: Boolean = true,
        isEmulated: Boolean = false,
        state: String = Environment.MEDIA_MOUNTED,
    ) = StorageVolumeCandidate(
        directory = directory,
        isPrimary = isPrimary,
        isRemovable = isRemovable,
        isEmulated = isEmulated,
        state = state,
    )
}
