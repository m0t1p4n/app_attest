package com.motsipan

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.Result

/** Handles the Play Integrity Standard API flow. */
internal class StandardPlayIntegrityClient(
    private val applicationContext: Context
) {
    private var standardIntegrityManager: StandardIntegrityManager? = createManager()
    private var integrityTokenProvider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null
    private var preparedCloudProjectNumber: Long? = null

    fun isAvailable(): Boolean = getManager() != null

    fun prepareTokenProvider(call: MethodCall, result: Result) {
        val cloudProjectNumber = call.argument<Number>("cloudProjectNumber")?.toLong()
        if (cloudProjectNumber == null || cloudProjectNumber <= 0L) {
            result.error("INVALID_ARGUMENT", "cloudProjectNumber must be greater than zero.", null)
            return
        }

        prepareProviderIfNeeded(cloudProjectNumber)
            .addOnSuccessListener { result.success(null) }
            .addOnFailureListener { exception ->
                PlayIntegrityErrors.handleStandardFailure(
                    result = result,
                    exception = exception,
                    fallbackCode = "PREPARE_PLAY_INTEGRITY_FAILED",
                    fallbackMessage = "Failed to prepare the Play Integrity token provider.",
                    onProviderInvalid = ::clearPreparedProvider
                )
            }
    }

    fun requestToken(call: MethodCall, result: Result) {
        val requestHash = call.argument<String>("requestHash")?.trim().orEmpty()
        if (requestHash.isEmpty()) {
            result.error("INVALID_ARGUMENT", "requestHash must not be empty.", null)
            return
        }

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
            .addOnSuccessListener { response -> result.success(response.token()) }
            .addOnFailureListener { exception ->
                PlayIntegrityErrors.handleStandardFailure(
                    result = result,
                    exception = exception,
                    fallbackCode = "REQUEST_STANDARD_PLAY_INTEGRITY_TOKEN_FAILED",
                    fallbackMessage = "Failed to request a Standard Play Integrity token.",
                    onProviderInvalid = ::clearPreparedProvider
                )
            }
    }

    fun clearPreparedProvider() {
        integrityTokenProvider = null
        preparedCloudProjectNumber = null
    }

    private fun prepareProviderIfNeeded(
        cloudProjectNumber: Long
    ): Task<StandardIntegrityManager.StandardIntegrityTokenProvider> {
        val manager = getManager()
            ?: return Tasks.forException(
                IllegalStateException("Standard Play Integrity is not available on this device.")
            )

        val cachedProvider = integrityTokenProvider
        if (preparedCloudProjectNumber == cloudProjectNumber && cachedProvider != null) {
            return Tasks.forResult(cachedProvider)
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
            .addOnFailureListener { clearPreparedProvider() }
    }

    private fun createManager(): StandardIntegrityManager? {
        return try {
            IntegrityManagerFactory.createStandard(applicationContext)
        } catch (_: Throwable) {
            null
        }
    }

    private fun getManager(): StandardIntegrityManager? {
        if (standardIntegrityManager == null) {
            standardIntegrityManager = createManager()
        }
        return standardIntegrityManager
    }
}
