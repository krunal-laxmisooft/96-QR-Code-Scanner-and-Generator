package com.app.vv.infscan.core

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val reason: FailureReason) : AppResult<Nothing>
}

enum class FailureReason { OFFLINE, NOT_FOUND, SERVICE, UNKNOWN }
