package com.ruru.practice.core.util

suspend inline fun <T> runCatchingSuspend(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (throwable: kotlin.coroutines.cancellation.CancellationException) {
        throw throwable
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
