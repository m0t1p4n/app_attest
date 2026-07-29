// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "app_attest",
    platforms: [
        .iOS("14.0")
    ],
    products: [
        // The library name is hyphen separated because Swift Package Manager uses it as the
        // CFBundleIdentifier when linked dynamically, and that cannot contain underscores.
        .library(name: "app-attest", targets: ["app_attest"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "app_attest",
            dependencies: [],
            resources: [
                // App Attest (DeviceCheck) does not use any required reason APIs, so no
                // PrivacyInfo.xcprivacy is bundled. If that changes, add the manifest here:
                // https://developer.apple.com/documentation/bundleresources/privacy_manifest_files
            ]
        )
    ]
)
