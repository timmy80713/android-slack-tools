package functions.repo

import functions.api.ClickUpApiClientImpl
import functions.model.RequestResult
import functions.model.clickup.ClickUpSpace
import functions.model.clickup.ClickUpStatus
import functions.model.clickup.ClickUpTask
import functions.model.clickup.request.ClickUpAddCustomFieldRequest
import functions.model.clickup.request.ClickUpUpdateTaskRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClickUpRepoImpl(
    private val clickUpApiClientImpl: ClickUpApiClientImpl,
) {

    suspend fun fetchSpaces(teamId: String): List<ClickUpSpace> {
        return withContext(Dispatchers.IO) {
            clickUpApiClientImpl.fetchSpaces(teamId = teamId)
        }.let {
            when (it) {
                is RequestResult.Success -> it.result.spaces
                is RequestResult.Failure -> throw it.error
            }
        }
    }

    suspend fun fetchTasks(viewId: String): List<ClickUpTask> {
        val slackUsers = try {
            recursiveFetchTasks(emptyList(), viewId)
        } catch (e: Exception) {
            throw e
        }
        return slackUsers
    }

    private suspend fun recursiveFetchTasks(
        list: List<ClickUpTask>,
        viewId: String,
        page: Int = 0,
    ): List<ClickUpTask> {
        val paging = withContext(Dispatchers.IO) {
            clickUpApiClientImpl.fetchTasks(viewId, page)
        }.let {
            when (it) {
                is RequestResult.Success -> it.result
                is RequestResult.Failure -> throw it.error
            }
        }
        val newTasks = list + paging.tasks
        val lastPage = paging.lastPage
        if (lastPage) return newTasks
        return recursiveFetchTasks(newTasks, viewId, page + 1)
    }

    suspend fun addCustomField(
        taskId: String,
        fieldId: String,
        value: String,
    ): RequestResult<Unit> {
        return clickUpApiClientImpl.addCustomField(
            taskId = taskId,
            fieldId = fieldId,
            requestBody = ClickUpAddCustomFieldRequest(value),
        )
    }

    suspend fun updateTask(
        taskId: String,
        targetStatus: ClickUpStatus,
    ): RequestResult<Unit> {
        return clickUpApiClientImpl.updateTask(
            taskId = taskId,
            requestBody = ClickUpUpdateTaskRequest(targetStatus),
        )
    }
}