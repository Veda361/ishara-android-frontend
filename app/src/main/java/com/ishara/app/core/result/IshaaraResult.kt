package com.ishara.app.core.result

/**
 * Standard Result wrapper for all domain, repository, and use case operations.
 */
sealed class IshaaraResult<out T> {
    data class Success<out T>(val data: T) : IshaaraResult<T>()
    data class Failure(val error: IshaaraError) : IshaaraResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Failure -> defaultValue
    }

    inline fun <R> map(transform: (T) -> R): IshaaraResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> Failure(error)
    }

    inline fun <R> flatMap(transform: (T) -> IshaaraResult<R>): IshaaraResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> Failure(error)
    }

    inline fun onSuccess(action: (T) -> Unit): IshaaraResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onFailure(action: (IshaaraError) -> Unit): IshaaraResult<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        fun <T> success(data: T): IshaaraResult<T> = Success(data)
        fun failure(error: IshaaraError): IshaaraResult<Nothing> = Failure(error)
    }
}
