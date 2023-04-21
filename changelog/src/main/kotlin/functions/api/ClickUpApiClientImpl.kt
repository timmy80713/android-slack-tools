package functions.api

import functions.env.Env
import functions.http.generateHttpClient
import functions.model.RequestResult
import functions.model.clickup.ClickUpTasksResponse
import functions.model.clickup.request.ClickUpAddCustomFieldRequest
import functions.model.clickup.request.ClickUpUpdateTaskRequest
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

class ClickUpApiClientImpl {

    private val httpClient = generateHttpClient {
        it.defaultRequest {
            url("https://api.clickup.com")
            header("Authorization", System.getenv(Env.CLICKUP_API_TOKEN))
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    suspend fun fetchTasks(viewId: String, page: Int): RequestResult<ClickUpTasksResponse> {
        val response = try {
            httpClient.get {
                url {
                    path("api", "v2", "view", viewId, "task")
                    parameter("page", page)
                }
            }
        } catch (e: Exception) {
            return RequestResult.Failure(e)
        }
        return RequestResult.Success(response.body())
    }

    suspend fun addCustomField(
        taskId: String,
        fieldId: String,
        requestBody: ClickUpAddCustomFieldRequest,
    ): RequestResult<Unit> {
        val response = try {
            httpClient.post {
                url {
                    path("api", "v2", "task", taskId, "field", fieldId)
                    setBody(requestBody)
                }
            }
        } catch (e: Exception) {
            return RequestResult.Failure(e)
        }
        return RequestResult.Success(response.body())
    }

    suspend fun updateTask(
        taskId: String,
        requestBody: ClickUpUpdateTaskRequest,
    ): RequestResult<Unit> {
        val response = try {
            httpClient.put {
                url {
                    path("api", "v2", "task", taskId)
                    setBody(requestBody)
                }
            }
        } catch (e: Exception) {
            return RequestResult.Failure(e)
        }
        return RequestResult.Success(response.body())
    }
}