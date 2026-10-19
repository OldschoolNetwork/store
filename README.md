# ON Store

The Oldschool Network application store for classic Android devices.

## Download

Current build: **alpha 0.34**, version code **10**.

- [Download ON Store](https://github.com/OldschoolNetwork/store/releases/download/alpha-0.34/ON-Store-0.34.apk)
- [Release notes and checksums](https://github.com/OldschoolNetwork/store/releases/tag/alpha-0.34)
- [All ON downloads](https://oldschoolnetwork.online/downloads/)

Android **4.0 / API 14 or newer**. This is a universal APK. It uses the persistent Oldschool Network signing key.

## What it does

Store provides applications, games, server-hosted media and reviews. Library keeps applications acquired by your account, including free ones, across your devices. Favorites is a separate collection in the profile menu.

Search, genres, device filters, package version selection and compatibility checks use the shared Store catalog. Core is needed for ON Account and ON Auth features. With ordinary installation, Android asks you to approve package installation. Full installation uses the privileged package installer when available and root as a fallback. Newer compatible versions have an Update button beside Open. Auto-update and network preferences control background updates.

## Alpha status

These builds are under development. Android 5.1 checks have passed, but that does not establish compatibility with every device or ROM. Full root installation, Google component removal, Magisk overlays and silent updates still need testing on physical devices. Keep a backup and recovery access before using system installation.

The ON release key differs from the old Android debug key. Android cannot update those old development packages in place; plan a one-time migration and keep any data you need.

## Help and problems

- [Oldschool Network](https://oldschoolnetwork.online/)
- [Documentation](https://oldschoolnetwork.online/downloads/docs/)
- [Changelog](CHANGELOG.md)

Include your device model, Android version, application version and the steps that led to a problem. Remove account tokens and private information from logs before posting them.
