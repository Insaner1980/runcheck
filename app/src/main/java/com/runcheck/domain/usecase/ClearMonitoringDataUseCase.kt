package com.runcheck.domain.usecase

import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.SpeedTestRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.ThrottlingRepository
import javax.inject.Inject

class ClearMonitoringDataUseCase @Inject constructor(
    private val transactionRunner: DatabaseTransactionRunner,
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository,
    private val thermalRepository: ThermalRepository,
    private val storageRepository: StorageRepository,
    private val throttlingRepository: ThrottlingRepository,
    private val appBatteryUsageRepository: AppBatteryUsageRepository,
    private val speedTestRepository: SpeedTestRepository
) {
    suspend operator fun invoke() {
        transactionRunner.runInTransaction {
            batteryRepository.deleteAll()
            networkRepository.deleteAll()
            thermalRepository.deleteAll()
            storageRepository.deleteAll()
            throttlingRepository.deleteAll()
            appBatteryUsageRepository.deleteAll()
            speedTestRepository.deleteAll()
        }
    }
}
