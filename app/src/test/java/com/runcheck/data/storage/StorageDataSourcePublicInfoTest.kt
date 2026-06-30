package com.runcheck.data.storage

import android.app.admin.DevicePolicyManager
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageDataSourcePublicInfoTest {
    @Test
    fun `mapStorageEncryptionStatus maps public Android encryption states`() {
        assertEquals("FBE", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER))
        assertEquals("Encrypted", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE))
        assertEquals("Inactive", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE))
        assertEquals("Unsupported", mapStorageEncryptionStatus(DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED))
        assertEquals(null, mapStorageEncryptionStatus(-1))
    }

    @Test
    fun `countDistinctPackageNames counts only nonblank unique packages`() {
        assertEquals(2, countDistinctPackageNames(listOf("com.alpha", "com.beta", "com.alpha", "")))
        assertEquals(null, countDistinctPackageNames(emptyList()))
        assertEquals(null, countDistinctPackageNames(listOf("", " ")))
    }
}
