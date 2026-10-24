# Changelog

## 0.35 — 2026-10-24

Store 0.35 connects catalogue and review features to the updated Store API.

- Curated categories and Store banner links open inside the app.
- Reviews support like, dislike, share and report actions. Own reviews appear as cards with an Edit pencil; edits are saved locally per account. New ratings start at zero and require a star selection before publishing.
- Additional report details are optional when choosing Other.
- Package permissions use readable descriptions and neutral icons. Selecting another APK version updates the permission list, including explicitly empty lists.
- An arrow replaces the upper Download control while package details are expanded.
- Maintainers use their public nickname and personal avatar. Own applications use the studio identity. Original creators have their own catalogue pages.
- The catalogue refreshes after five minutes when returning to Store. Saved package metadata retains ABI and permission information.

Install the matching server update to enable the new category, review and public-profile endpoints. Android 4.0 / API 14 and later; universal APK, signed with the persistent ON certificate. Eleven UI and compatibility checks passed on an isolated Android 5.1 emulator, including a live read-only catalogue check.

## 0.34 — 2026-10-19

Store 0.34 adds a separate Update button beside Open.

- Update appears only when a newer compatible APK exists. It selects the newest compatible version, independently of the version currently selected in the list. Open stays available.
- After installing the latest version, Update disappears and Open remains.
- Downloaded updates must match the selected package and version, and the installed application's signing certificate. Downgrades are rejected.
- Automatic updates recheck the auto-update and network settings throughout downloads and before installation. Wi-Fi only remains enforced when the network changes. Manual Update remains available with auto-update disabled.
- Optional update summary notifications follow the notification preference. Android still requires the ongoing notification while a foreground download is active.

Android 4.0 / API 14 or newer; universal APK. Signed with the persistent ON key. Update/Open behaviour and disabled-auto-update behaviour were checked on Android 5.1. Silent installation requires full setup.

## 0.33 — 2026-10-19

Store 0.33 fixes installation progress, account gating and developer attribution.

- Automatic installation shows an animated Installing bar. The downloaded-APK button stays hidden during installation, and the cancel control uses the Store style.
- On a full installation, Store uses Android's INSTALL_PACKAGES permission when it is granted. Root is used only as a fallback when direct privileged installation is unavailable.
- Removing or disabling Core clears Store's saved login and account token. An account name alone can no longer establish a session.
- Library requires sign-in; guest users no longer see a cached or local library.
- A null reply from the server is treated as no reply.
- Developer labels use the saved studio name. Studio logos no longer fall back to the publisher's personal avatar.

Android 4.0 / API 14 or newer; universal APK. Signed with the same ON key as 0.32. The accompanying server update is needed for canonical studio data. System permission grants and root behaviour depend on the device and installation method.

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
