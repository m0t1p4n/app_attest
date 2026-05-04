package com.motsipan

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** Flutter plugin for Android Play Integrity. */
class AppAttestPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context
    private var standardIntegrityManager: StandardIntegrityManager? = null
    private var integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null
    private var preparedCloudProjectNumber: Long? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        standardIntegrityManager = createStandardIntegrityManager()
        channel = MethodChannel(binding.binaryMessenger, "app_attest")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "isSupported" -> result.success(isPlayIntegrityAvailable())
            "preparePlayIntegrityTokenProvider" -> preparePlayIntegrityTokenProvider(call, result)
            "clearPreparedPlayIntegrityTokenProvider" -> {
                clearPreparedProvider()
                result.success(null)
            }
            "requestStandardPlayIntegrityToken" -> requestStandardPlayIntegrityToken(call, result)
            "requestPlayIntegrityToken" -> requestPlayIntegrityToken(call, result)
            "generateKey", "attestKey", "generateAssertion" -> result.error(
                "UNSUPPORTED_PLATFORM",
                "Apple App Attest is only available on iOS.",
                null
            )
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        clearPreparedProvider()
        standardIntegrityManager = null
    }

    private fun isPlayIntegrityAvailable(): Boolean {
        return getStandardIntegrityManager() != null
    }

    private fun createStandardIntegrityManager(): StandardIntegrityManager? {
        return try {
            IntegrityManagerFactory.createStandard(applicationContext)
        } catch (_: Throwable) {
            null
        }
    }

    private fun getStandardIntegrityManager(): StandardIntegrityManager? {
        if (standardIntegrityManager != null) {
            return standardIntegrityManager
        }

        standardIntegrityManager = createStandardIntegrityManager()
        return standardIntegrityManager
    }

    private fun preparePlayIntegrityTokenProvider(call: MethodCall, result: Result) {
        val cloudProjectNumber = call.argument<Number>("cloudProjectNumber")?.toLong()

        if (cloudProjectNumber == null || cloudProjectNumber <= 0L) {
            result.error("INVALID_ARGUMENT", "cloudProjectNumber must be greater than zero.", null)
            return
        }

        prepareProviderIfNeeded(cloudProjectNumber)
            .addOnSuccessListener {
                result.success(null)
            }
            .addOnFailureListener { exception ->
                handleStandardIntegrityFailure(
                    result = result,
                    exception = exception,
                    fallbackCode = "PREPARE_PLAY_INTEGRITY_FAILED",
                    fallbackMessage = "Failed to prepare the Play Integrity token provider."
                )
            }
    }

    private fun requestStandardPlayIntegrityToken(call: MethodCall, result: Result) {
        val requestHash = call.argument<String>("requestHash")?.trim().orEmpty()

        if (requestHash.isEmpty()) {
            result.error("INVALID_ARGUMENT", "requestHash must not be empty.", null)
            return
        }

        requestTokenFromPreparedProvider(requestHash, result)
    }

    private fun requestPlayIntegrityToken(call: MethodCall, result: Result) {
        val requestHash = call.argument<String>("nonce")?.trim().orEmpty()
        val cloudProjectNumber = call.argument<Number>("cloudProjectNumber")?.toLong()

        if (requestHash.isEmpty()) {
            result.error("INVALID_ARGUMENT", "nonce must not be empty.", null)
            return
        }
        if (cloudProjectNumber == null || cloudProjectNumber <= 0L) {
            result.error("INVALID_ARGUMENT", "cloudProjectNumber must be greater than zero.", null)
            return
        }

        prepareProviderIfNeeded(cloudProjectNumber)
            .addOnSuccessListener {
                requestTokenFromPreparedProvider(requestHash, result)
            }
            .addOnFailureListener { exception ->
                handleStandardIntegrityFailure(
                    result = result,
                    exception = exception,
                    fallbackCode = "PREPARE_PLAY_INTEGRITY_FAILED",
                    fallbackMessage = "Failed to prepare the Play Integrity token provider."
                )
            }
    }

    private fun prepareProviderIfNeeded(
        cloudProjectNumber: Long
    ): com.google.android.gms.tasks.Task<StandardIntegrityManager.StandardIntegrityTokenProvider> {
        val manager = getStandardIntegrityManager()
            ?: return com.google.android.gms.tasks.Tasks.forException(
                IllegalStateException("Standard Play Integrity is not available on this device.")
            )

        if (preparedCloudProjectNumber == cloudProjectNumber && integrityTokenProvider != null) {
            return com.google.android.gms.tasks.Tasks.forResult(integrityTokenProvider!!)
        }

        clearPreparedProvider()

        val request = StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
            .setCloudProjectNumber(cloudProjectNumber)
            .build()

        return manager.prepareIntegrityToken(request)
            .addOnSuccessListener { provider ->
                integrityTokenProvider = provider
                preparedCloudProjectNumber = cloudProjectNumber
            }
            .addOnFailureListener {
                clearPreparedProvider()
            }
    }

    private fun requestTokenFromPreparedProvider(requestHash: String, result: Result) {
        val provider = integrityTokenProvider
        if (provider == null) {
            result.error(
                "NOT_PREPARED",
                "Play Integrity token provider is not prepared. Call preparePlayIntegrityTokenProvider first.",
                null
            )
            return
        }

        val request = StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
            .setRequestHash(requestHash)
            .build()

        provider.request(request)
            .addOnSuccessListener { response ->
                result.success(response.token())
            }
            .addOnFailureListener { exception ->
                handleStandardIntegrityFailure(
                    result = result,
                    exception = exception,
                    fallbackCode = "REQUEST_STANDARD_PLAY_INTEGRITY_TOKEN_FAILED",
                    fallbackMessage = "Failed to request a Standard Play Integrity token."
                )
            }
    }

    private fun clearPreparedProvider() {
        integrityTokenProvider = null
        preparedCloudProjectNumber = null
    }

    private fun handleStandardIntegrityFailure(
        result: Result,
        exception: Exception,
        fallbackCode: String,
        fallbackMessage: String
    ) {
        if (exception is StandardIntegrityException &&
            exception.errorCode == StandardIntegrityErrorCode.INTEGRITY_TOKEN_PROVIDER_INVALID
        ) {
            clearPreparedProvider()
        }

        val errorCode = if (exception is StandardIntegrityException) {
            mapStandardIntegrityErrorCode(exception.errorCode, fallbackCode)
        } else {
            fallbackCode
        }

        val details = mutableMapOf<String, Any>(
            "exception" to exception.javaClass.name
        )
        if (exception is StandardIntegrityException) {
            details["errorCode"] = exception.errorCode
        }

        result.error(
            errorCode,
            exception.localizedMessage ?: fallbackMessage,
            details
        )
    }

    private fun mapStandardIntegrityErrorCode(errorCode: Int, fallbackCode: String): String {
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

