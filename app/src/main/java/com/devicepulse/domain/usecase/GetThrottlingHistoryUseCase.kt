package com.devicepulse.domain.usecase

import com.devicepulse.data.db.dao.ThrottlingEventDao
import com.devicepulse.data.db.entity.ThrottlingEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThrottlingHistoryUseCase @Inject constructor(
    private val throttlingEventDao: ThrottlingEventDao
) {
    operator fun invoke(): Flow<List<ThrottlingEventEntity>> =
        throttlingEventDao.getRecentEvents()
}
