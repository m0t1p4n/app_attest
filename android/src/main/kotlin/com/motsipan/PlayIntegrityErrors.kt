package com.motsipan

import com.google.android.play.core.integrity.IntegrityServiceException
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.model.IntegrityErrorCode
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import io.flutter.plugin.common.MethodChannel.Result

internal object PlayIntegrityErrors {
    fun handleClassicFailure(
        result: Result,
        exception: Exception,
        fallbackCode: String,
        fallbackMessage: String
    ) {
        val errorCode = if (exception is IntegrityServiceException) {
            mapClassicErrorCode(exception.errorCode, fallbackCode)
        } else {
            fallbackCode
        }

        result.error(
            errorCode,
            exception.localizedMessage ?: fallbackMessage,
            details(exception)
        )
    }

    fun handleStandardFailure(
        result: Result,
        exception: Exception,
        fallbackCode: String,
        fallbackMessage: String,
        onProviderInvalid: () -> Unit = {}
    ) {
        if (exception is StandardIntegrityException &&
            exception.errorCode == StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID
        ) {
            onProviderInvalid()
        }

        val errorCode = if (exception is StandardIntegrityException) {
            mapStandardErrorCode(exception.errorCode, fallbackCode)
        } else {
            fallbackCode
        }

        result.error(
            errorCode,
            exception.localizedMessage ?: fallbackMessage,
            details(exception)
        )
    }

    private fun details(exception: Exception): Map<String, Any> {
        val details = mutableMapOf<String, Any>(
            "exception" to exception.javaClass.name
        )

        when (exception) {
            is IntegrityServiceException -> details["errorCode"] = exception.errorCode
            is StandardIntegrityException -> details["errorCode"] = exception.errorCode
        }

        return details
    }

    private fun mapClassicErrorCode(errorCode: Int, fallbackCode: String): String {
        return when (errorCode) {
            IntegrityErrorCode.API_NOT_AVAILABLE -> "API_NOT_AVAILABLE"
            IntegrityErrorCode.PLAY_STORE_NOT_FOUND -> "PLAY_STORE_NOT_FOUND"
            IntegrityErrorCode.NETWORK_ERROR -> "NETWORK_ERROR"
            IntegrityErrorCode.PLAY_STORE_ACCOUNT_NOT_FOUND -> "PLAY_STORE_ACCOUNT_NOT_FOUND"
            IntegrityErrorCode.APP_NOT_INSTALLED -> "APP_NOT_INSTALLED"
            IntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> "PLAY_SERVICES_NOT_FOUND"
            IntegrityErrorCode.APP_UID_MISMATCH -> "APP_UID_MISMATCH"
            IntegrityErrorCode.TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS"
            IntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> "CANNOT_BIND_TO_SERVICE"
            IntegrityErrorCode.NONCE_TOO_SHORT -> "NONCE_TOO_SHORT"
            IntegrityErrorCode.NONCE_TOO_LONG -> "NONCE_TOO_LONG"
            IntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> "GOOGLE_SERVER_UNAVAILABLE"
            IntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> "PLAY_STORE_VERSION_OUTDATED"
            IntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> "PLAY_SERVICES_VERSION_OUTDATED"
            IntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> "CLOUD_PROJECT_NUMBER_IS_INVALID"
            IntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> "CLIENT_TRANSIENT_ERROR"
            IntegrityErrorCode.INTERNAL_ERROR -> "INTERNAL_ERROR"
            else -> fallbackCode
        }
    }

    private fun mapStandardErrorCode(errorCode: Int, fallbackCode: String): String {
        return when (errorCode) {
            StandardIntegrityErrorCode.API_NOT_AVAILABLE -> "API_NOT_AVAILABLE"
            StandardIntegrityErrorCode.PLAY_STORE_NOT_FOUND -> "PLAY_STORE_NOT_FOUND"
            StandardIntegrityErrorCode.NETWORK_ERROR -> "NETWORK_ERROR"
            StandardIntegrityErrorCode.APP_NOT_INSTALLED -> "APP_NOT_INSTALLED"
            StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND -> "PLAY_SERVICES_NOT_FOUND"
            StandardIntegrityErrorCode.APP_UID_MISMATCH -> "APP_UID_MISMATCH"
            StandardIntegrityErrorCode.TOO_MANY_REQUESTS -> "TOO_MANY_REQUESTS"
            StandardIntegrityErrorCode.CANNOT_BIND_TO_SERVICE -> "CANNOT_BIND_TO_SERVICE"
            StandardIntegrityErrorCode.GOOGLE_SERVER_UNAVAILABLE -> "GOOGLE_SERVER_UNAVAILABLE"
            StandardIntegrityErrorCode.PLAY_STORE_VERSION_OUTDATED -> "PLAY_STORE_VERSION_OUTDATED"
            StandardIntegrityErrorCode.PLAY_SERVICES_VERSION_OUTDATED -> "PLAY_SERVICES_VERSION_OUTDATED"
            StandardIntegrityErrorCode.CLOUD_PROJECT_NUMBER_IS_INVALID -> "CLOUD_PROJECT_NUMBER_IS_INVALID"
            StandardIntegrityErrorCode.REQUEST_HASH_TOO_LONG -> "REQUEST_HASH_TOO_LONG"
            StandardIntegrityErrorCode.CLIENT_TRANSIENT_ERROR -> "CLIENT_TRANSIENT_ERROR"
            StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID -> "INTEGRITY_TOKEN_PROVIDER_INVALID"
            StandardIntegrityErrorCode.INTERNAL_ERROR -> "INTERNAL_ERROR"
            else -> fallbackCode
        }
    }
}
