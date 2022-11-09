package functions.model

sealed class RequestResult<out T> {
    class Success<T>(val result: T) : RequestResult<T>()
    class Failed(val error: Throwable) : RequestResult<Nothing>()
}

inline fun <T> RequestResult<T>.doOnSuccess(block: (T) -> Unit): RequestResult<T> {
    if (this is RequestResult.Success) {
        block(result)
    }
    return this
}

inline fun <T> RequestResult<T>.doOnFailure(block: (Throwable) -> Unit): RequestResult<T> {
    if (this is RequestResult.Failed) {
        block(error)
    }
    return this
}