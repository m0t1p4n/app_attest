Pod::Spec.new do |s|
  s.name             = 'app_attest'
  s.version          = '0.1.0'
  s.summary          = 'Flutter plugin for Apple App Attest and Android Play Integrity.'
  s.description      = <<-DESC
Flutter plugin for Apple App Attest on iOS and Play Integrity on Android.
                       DESC
  s.homepage         = 'https://github.com/m0t1p4n/app_attest'
  s.license          = { :type => 'MIT', :file => '../LICENSE' }
  s.author           = { 'motsipan' => 'https://github.com/m0t1p4n' }
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
