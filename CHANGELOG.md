# Changelog

## 0.32 — 2026-10-19

ON Store 0.32 connects the catalog, application pages and account collections to the Store API.

- Real APK downloads, package version selection, checksums and device compatibility checks.
- Separate Library and Favorites, with Favorites in the profile menu and three main tabs: Apps, Games and Profile.
- Library filters for devices, installed state, price and sorting, behind a collapsible filter button.
- Search history and suggestions, catalog genres and filters.
- Application screenshots, video, descriptions, permissions and per-version ON Account support from the server.
- Public profiles and studios, review avatars, custom four-point rating stars, publisher replies and report controls.
- ON Guardy explanations, offline Retry, pull-to-refresh and maintenance pages.
- Missing-Core detection, dismissible installation notices and download/install notifications.
- Scheduled checks for other installed applications; verified full installations can request root installation.

Requires Android 4.0 (API 14) or newer. This universal APK uses the persistent ON key. Fifteen Store checks passed on Android 5.1. Silent root installation and physical-device compatibility still need testing.

Account features require ON Core Services signed with the same ON key. The corresponding server update must also be deployed. Old debug-signed installations need a one-time migration before using this release.
