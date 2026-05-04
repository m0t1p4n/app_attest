import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'app_attest_platform_interface.dart';

/// MethodChannel-backed implementation of [AppAttestPlatform].
class MethodChannelAppAttest extends AppAttestPlatform {
  /// Native channel used by this plugin.
  @visibleForTesting
  final methodChannel = const MethodChannel('app_attest');

  @override
  Future<bool> isSupported() async {
    return await methodChannel.invokeMethod<bool>('isSupported') ?? false;
  }

  @override
  Future<String> generateKey() async {
    final keyId = await methodChannel.invokeMethod<String>('generateKey');
    if (keyId == null || keyId.isEmpty) {
      throw PlatformException(
        code: 'NULL_RESULT',
        message: 'Native generateKey returned an empty key identifier.',
      );
    }
    return keyId;
  }

  @override
  Future<Map<dynamic, dynamic>> attestKey({
    required String keyId,
    required String challenge,
  }) async {
    final result = await methodChannel.invokeMapMethod<dynamic, dynamic>(
      'attestKey',
      <String, dynamic>{'keyId': keyId, 'challenge': challenge},
    );
    if (result == null) {
      throw PlatformException(
        code: 'NULL_RESULT',
        message: 'Native attestKey returned null.',
      );
    }
    return result;
  }

  @override
  Future<Map<dynamic, dynamic>> generateAssertion({
    required String keyId,
    required String challenge,
  }) async {
    final result = await methodChannel.invokeMapMethod<dynamic, dynamic>(
      'generateAssertion',
      <String, dynamic>{'keyId': keyId, 'challenge': challenge},
    );
    if (result == null) {
      throw PlatformException(
        code: 'NULL_RESULT',
        message: 'Native generateAssertion returned null.',
      );
    }
    return result;
  }

  @override
  Future<void> preparePlayIntegrityTokenProvider({
    required int cloudProjectNumber,
  }) {
    return methodChannel.invokeMethod<void>(
      'preparePlayIntegrityTokenProvider',
      <String, dynamic>{'cloudProjectNumber': cloudProjectNumber},
    );
  }

  @override
  Future<void> clearPreparedPlayIntegrityTokenProvider() {
    return methodChannel.invokeMethod<void>(
      'clearPreparedPlayIntegrityTokenProvider',
    );
  }

  @override
  Future<String> requestStandardPlayIntegrityToken({
    required String requestHash,
  }) async {
    final token = await methodChannel.invokeMethod<String>(
      'requestStandardPlayIntegrityToken',
      <String, dynamic>{'requestHash': requestHash},
    );
    if (token == null || token.isEmpty) {
      throw PlatformException(
        code: 'NULL_RESULT',
        message:
            'Native requestStandardPlayIntegrityToken returned an empty token.',
      );
    }
    return token;
  }

  @override
  Future<String> requestPlayIntegrityToken({
    required String nonce,
    required int cloudProjectNumber,
  }) async {
    await preparePlayIntegrityTokenProvider(
      cloudProjectNumber: cloudProjectNumber,
    );
    return requestStandardPlayIntegrityToken(requestHash: nonce);
  }
}
