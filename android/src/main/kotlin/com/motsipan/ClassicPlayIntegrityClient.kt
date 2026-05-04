package com.motsipan

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.Result

/** Handles the original Play Integrity Classic API flow. */
internal class ClassicPlayIntegrityClient(
    private val applicationContext: Context
) {
    private var integrityManager: IntegrityManager? = createManager()

    fun isAvailable(): Boolean = getManager() != null

    fun requestToken(call: MethodCall, result: Result) {
        val nonce = call.argument<String>("nonce")?.trim().orEmpty()
        val cloudProjectNumber = call.argument<Number>("cloudProjectNumber")?.toLong()

        if (nonce.isEmpty()) {
            result.error("INVALID_ARGUMENT", "nonce must not be empty.", null)
            return
        }
        if (cloudProjectNumber == null || cloudProjectNumber <= 0L) {
            result.error("INVALID_ARGUMENT", "cloudProjectNumber must be greater than zero.", null)
            return
        }

        val manager = getManager()
        if (manager == null) {
            result.error(
                "API_NOT_AVAILABLE",
                "Classic Play Integrity is not available on this device.",
                null
            )
            return
        }

        val request = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(cloudProjectNumber)
            .build()

        manager.requestIntegrityToken(request)
            .addOnSuccessListener { response -> result.success(response.token()) }
            .addOnFailureListener { exception ->
                PlayIntegrityErrors.handleClassicFailure(
                    result = result,
                    exception = exception,
                    fallbackCode = "REQUEST_CLASSIC_PLAY_INTEGRITY_TOKEN_FAILED",
                    fallbackMessage = "Failed to request a Classic Play Integrity token."
                )
            }
    }

    private fun createManager(): IntegrityManager? {
        return try {
            IntegrityManagerFactory.create(applicationContext)
        } catch (_: Throwable) {
            null
        }
    }

    private fun getManager(): IntegrityManager? {
        if (integrityManager == null) {
            integrityManager = createManager()
        }
        return integrityManager
    }
}
