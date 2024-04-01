package functions.repo

import functions.api.BitriseApiClientImpl
import functions.model.RequestResult
import functions.model.bitrise.BitriseResponse
import functions.model.bitrise.BitriseTriggerRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BitriseRepoImpl(private val bitriseApiClientImpl: BitriseApiClientImpl) {
    suspend fun triggerBuild(requestBody: BitriseTriggerRequest): RequestResult<BitriseResponse> {
        return withContext(Dispatchers.IO) {
            bitriseApiClientImpl.triggerBuild(requestBody)
        }
    }
}