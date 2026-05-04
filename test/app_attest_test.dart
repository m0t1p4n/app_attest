import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:app_attest/app_attest.dart';
import 'package:app_attest/src/method_channel_app_attest.dart';

class FakeAppAttestPlatform extends AppAttestPlatform {
  @override
  Future<bool> isSupported() async => true;

  @override
  Future<String> generateKey() async => 'key-id';

  @override
  Future<Map<dynamic, dynamic>> attestKey({
    required String keyId,
    required String challenge,
  }) async {
    return <String, dynamic>{
      'keyId': keyId,
      'attestationObject': 'attestation-base64',
    };
  }

  @override
  Future<Map<dynamic, dynamic>> generateAssertion({
    required String keyId,
    required String challenge,
  }) async {
    return <String, dynamic>{
      'keyId': keyId,
      'assertionObject': 'assertion-base64',
    };
  }

  @override
  Future<String> requestPlayIntegrityToken({
    required String nonce,
    required int cloudProjectNumber,
  }) async {
    return 'play-integrity-token';
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  tearDown(() {
    AppAttestPlatform.instance = MethodChannelAppAttest();
  });

  test('public API forwards to platform implementation', () async {
    AppAttestPlatform.instance = FakeAppAttestPlatform();

    expect(await AppAttest.isSupported(), isTrue);
    expect(await AppAttest.generateKey(), 'key-id');

    final attestation = await AppAttest.attestKey(
      keyId: 'key-id',
      challenge: 'server-challenge',
    );
    expect(attestation.keyId, 'key-id');
    expect(attestation.attestationObject, 'attestation-base64');

    final assertion = await AppAttest.generateAssertion(
      keyId: 'key-id',
      challenge: 'server-challenge',
    );
    expect(assertion.keyId, 'key-id');
    expect(assertion.assertionObject, 'assertion-base64');

    expect(
      await AppAttest.requestPlayIntegrityToken(
        nonce: 'nonce',
        cloudProjectNumber: 123,
      ),
      'play-integrity-token',
    );
  });

  test('public API validates empty arguments', () {
    AppAttestPlatform.instance = FakeAppAttestPlatform();

    expect(
      () => AppAttest.attestKey(keyId: '', challenge: 'challenge'),
      throwsArgumentError,
    );
    expect(
      () => AppAttest.generateAssertion(keyId: 'key-id', challenge: ' '),
      throwsArgumentError,
    );
    expect(
      () => AppAttest.requestPlayIntegrityToken(
        nonce: 'nonce',
        cloudProjectNumber: 0,
      ),
      throwsArgumentError,
    );
  });

  test('method channel maps native calls and arguments', () async {
    final implementation = MethodChannelAppAttest();
    final calls = <MethodCall>[];

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(implementation.methodChannel, (call) async {
          calls.add(call);
          switch (call.method) {
            case 'isSupported':
              return true;
            case 'generateKey':
              return 'key-id';
            case 'attestKey':
              return <String, dynamic>{
                'keyId': (call.arguments as Map<dynamic, dynamic>)['keyId'],
                'attestationObject': 'attestation-base64',
              };
            case 'generateAssertion':
              return <String, dynamic>{
                'keyId': (call.arguments as Map<dynamic, dynamic>)['keyId'],
                'assertionObject': 'assertion-base64',
              };
            case 'requestPlayIntegrityToken':
              return 'play-integrity-token';
          }
          return null;
        });

    expect(await implementation.isSupported(), isTrue);
    expect(await implementation.generateKey(), 'key-id');
    expect(
      await implementation.attestKey(keyId: 'key-id', challenge: 'challenge'),
      containsPair('attestationObject', 'attestation-base64'),
    );
    expect(
      await implementation.generateAssertion(
        keyId: 'key-id',
        challenge: 'challenge',
      ),
      containsPair('assertionObject', 'assertion-base64'),
    );
    expect(
      await implementation.requestPlayIntegrityToken(
        nonce: 'nonce',
        cloudProjectNumber: 123,
      ),
      'play-integrity-token',
    );

    expect(calls.map((call) => call.method), <String>[
      'isSupported',
      'generateKey',
      'attestKey',
      'generateAssertion',
      'requestPlayIntegrityToken',
    ]);
    expect(calls.last.arguments, <String, dynamic>{
      'nonce': 'nonce',
      'cloudProjectNumber': 123,
    });

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(implementation.methodChannel, null);
  });
}
