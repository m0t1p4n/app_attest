package com.motsipan

import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** Flutter plugin for Android Play Integrity. */
class AppAttestPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private var standardPlayIntegrityClient: StandardPlayIntegrityClient? = null
    private var classicPlayIntegrityClient: ClassicPlayIntegrityClient? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val applicationContext: Context = binding.applicationContext
        standardPlayIntegrityClient = StandardPlayIntegrityClient(applicationContext)
        classicPlayIntegrityClient = ClassicPlayIntegrityClient(applicationContext)
        channel = MethodChannel(binding.binaryMessenger, "app_attest")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "isSupported" -> result.success(isPlayIntegrityAvailable())
            "preparePlayIntegrityTokenProvider" -> standardClient().prepareTokenProvider(call, result)
            "clearPreparedPlayIntegrityTokenProvider" -> {
                standardClient().clearPreparedProvider()
                result.success(null)
            }
            "requestStandardPlayIntegrityToken" -> standardClient().requestToken(call, result)
            "requestClassicPlayIntegrityToken", "requestPlayIntegrityToken" -> {
                classicClient().requestToken(call, result)
            }
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
        standardPlayIntegrityClient?.clearPreparedProvider()
        standardPlayIntegrityClient = null
        classicPlayIntegrityClient = null
    }

    private fun isPlayIntegrityAvailable(): Boolean {
        return standardClient().isAvailable() || classicClient().isAvailable()
    }

    private fun standardClient(): StandardPlayIntegrityClient {
        return standardPlayIntegrityClient
            ?: throw IllegalStateException("StandardPlayIntegrityClient is not attached.")
    }

    private fun classicClient(): ClassicPlayIntegrityClient {
        return classicPlayIntegrityClient
            ?: throw IllegalStateException("ClassicPlayIntegrityClient is not attached.")
    }
}

