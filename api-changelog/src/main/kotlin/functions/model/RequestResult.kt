package functions.model

sealed class RequestResult<out T> {
    class Success<T>(val result: T) : RequestResult<T>()
    class Failure(val error: Throwable) : RequestResult<Nothing>()
}

fun <T> RequestResult<T>.resultOrNull(): T? {
    return when (this) {
        is RequestResult.Success -> result
        is RequestResult.Failure -> null
    }
}