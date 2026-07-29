import CryptoKit
import DeviceCheck
import Flutter
import UIKit

public class AppAttestPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "app_attest", binaryMessenger: registrar.messenger())
    let instance = AppAttestPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "isSupported":
      result(isAppAttestSupported())
    case "generateKey":
      generateKey(result: result)
    case "attestKey":
      attestKey(call: call, result: result)
    case "generateAssertion":
      generateAssertion(call: call, result: result)
    case "preparePlayIntegrityTokenProvider",
         "clearPreparedPlayIntegrityTokenProvider",
         "requestStandardPlayIntegrityToken",
         "requestClassicPlayIntegrityToken",
         "requestPlayIntegrityToken":
      result(FlutterError(
        code: "UNSUPPORTED_PLATFORM",
        message: "Play Integrity is only available on Android.",
        details: nil
      ))
    default:
      result(FlutterMethodNotImplemented)
    }
  }

  private func isAppAttestSupported() -> Bool {
    if #available(iOS 14.0, *) {
      return DCAppAttestService.shared.isSupported
    }
    return false
  }

  private func generateKey(result: @escaping FlutterResult) {
    guard isAppAttestSupported() else {
      result(unsupportedAppAttestError())
      return
    }

    if #available(iOS 14.0, *) {
      DCAppAttestService.shared.generateKey { keyId, error in
        self.completeOnMain(result) {
          if let error = error {
            return self.flutterError(code: "GENERATE_KEY_FAILED", error: error)
          }
          guard let keyId = keyId, !keyId.isEmpty else {
            return FlutterError(
              code: "EMPTY_RESULT",
              message: "App Attest did not return a key identifier.",
              details: nil
            )
          }
          return keyId
        }
      }
    }
  }

  private func attestKey(call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard isAppAttestSupported() else {
      result(unsupportedAppAttestError())
      return
    }
    guard let arguments = call.arguments as? [String: Any],
          let keyId = arguments["keyId"] as? String,
          let challenge = arguments["challenge"] as? String,
          !keyId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
          !challenge.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
      result(invalidArgumentsError("Expected non-empty keyId and challenge."))
      return
    }

    if #available(iOS 14.0, *) {
      let clientDataHash = sha256(challenge)
      DCAppAttestService.shared.attestKey(keyId, clientDataHash: clientDataHash) { attestation, error in
        self.completeOnMain(result) {
          if let error = error {
            return self.flutterError(code: "ATTEST_KEY_FAILED", error: error)
          }
          guard let attestation = attestation else {
            return FlutterError(
              code: "EMPTY_RESULT",
              message: "App Attest did not return an attestation object.",
              details: nil
            )
          }
          return [
            "keyId": keyId,
            "attestationObject": attestation.base64EncodedString()
          ]
        }
      }
    }
  }

  private func generateAssertion(call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard isAppAttestSupported() else {
      result(unsupportedAppAttestError())
      return
    }
    guard let arguments = call.arguments as? [String: Any],
          let keyId = arguments["keyId"] as? String,
          let challenge = arguments["challenge"] as? String,
          !keyId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
          !challenge.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
      result(invalidArgumentsError("Expected non-empty keyId and challenge."))
      return
    }

    if #available(iOS 14.0, *) {
      let clientDataHash = sha256(challenge)
      DCAppAttestService.shared.generateAssertion(keyId, clientDataHash: clientDataHash) { assertion, error in
        self.completeOnMain(result) {
          if let error = error {
            return self.flutterError(code: "GENERATE_ASSERTION_FAILED", error: error)
          }
          guard let assertion = assertion else {
            return FlutterError(
              code: "EMPTY_RESULT",
              message: "App Attest did not return an assertion object.",
              details: nil
            )
          }
          return [
            "keyId": keyId,
            "assertionObject": assertion.base64EncodedString()
          ]
        }
      }
    }
  }

  private func sha256(_ value: String) -> Data {
    return Data(SHA256.hash(data: Data(value.utf8)))
  }

  private func completeOnMain(_ result: @escaping FlutterResult, value: @escaping () -> Any) {
    DispatchQueue.main.async {
      result(value())
    }
  }

  private func invalidArgumentsError(_ message: String) -> FlutterError {
    return FlutterError(code: "INVALID_ARGUMENT", message: message, details: nil)
  }

  private func unsupportedAppAttestError() -> FlutterError {
    return FlutterError(
      code: "UNSUPPORTED_PLATFORM",
      message: "App Attest is not supported on this device or OS version.",
      details: nil
    )
  }

  private func flutterError(code: String, error: Error) -> FlutterError {
    let nsError = error as NSError
    return FlutterError(
      code: code,
      message: error.localizedDescription,
      details: [
        "domain": nsError.domain,
        "code": nsError.code
      ]
    )
  }
}
