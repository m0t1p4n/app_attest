Pod::Spec.new do |s|
  s.name             = 'app_attest'
  s.version          = '0.0.1'
  s.summary          = 'Flutter plugin for Apple App Attest and Android Play Integrity.'
  s.description      = <<-DESC
Flutter plugin for Apple App Attest on iOS and Play Integrity on Android.
                       DESC
  s.homepage         = 'https://example.com/app_attest'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'app_attest' => 'dev@example.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '14.0'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386'
  }
  s.swift_version = '5.0'
end
