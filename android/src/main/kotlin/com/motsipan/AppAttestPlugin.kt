package com.motsipan

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** Flutter plugin for Android Play Integrity. */
class AppAttestPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "app_attest")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "isSupported" -> result.success(isPlayIntegrityAvailable())
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
    }

    private fun isPlayIntegrityAvailable(): Boolean {
        return try {
            IntegrityManagerFactory.create(applicationContext)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun requestPlayIntegrityToken(call: MethodCall, result: Result) {
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

        val integrityManager = IntegrityManagerFactory.create(applicationContext)
        val request = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(cloudProjectNumber)
            .build()

        integrityManager.requestIntegrityToken(request)
            .addOnSuccessListener { response ->
                result.success(response.token())
            }
            .addOnFailureListener { exception ->
                result.error(
                    "PLAY_INTEGRITY_FAILED",
                    exception.localizedMessage ?: "Play Integrity token request failed.",
                    mapOf("exception" to exception.javaClass.name)
                )
            }
    }
}

