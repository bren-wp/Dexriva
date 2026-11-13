# DEXRIVE 1.4.0 — stronger package inspection on Windows and Android

**Product and interface:** DEXRIVE by Brendigo · https://brendigo.com

DEXRIVE 1.4.0 improves the local ZIP/APK metadata inspection experience on both Windows and Android while preserving the established DEXRIVE visual language, compatibility interfaces, and existing project formats.

## What's improved
- **Android Inspector 0.5.0:** displays resources.arsc presence, legacy META-INF signature-file indicators, known compressed and uncompressed byte counts, archive size and entries exceeding 512 MiB in responsive result cards.
- **Archive safety:** Android and Windows report additional suspicious path patterns (such as Windows drive-relative paths and NUL separators). Flags are heuristic observations, not proof of malware.
- **Responsiveness:** Kotlin coroutine cancellation is checked while copying the chosen Android document and during ZIP metadata enumeration. Temporary files are deleted after analysis or failure.
- **Accessibility:** Windows file selection and inspection report have additional screen-reader descriptions.
- **Regression tests:** expanded synthetic ZIP coverage on both platforms, cancellation behavior and unusual archive names.

## Downloads
- `DEXRIVE-1.4.0-setup.exe` — Windows 10/11 desktop setup with bundled runtime.
- `DEXRIVE-1.4.0-portable.exe` — Windows 10/11 portable self-extracting distribution.
- `DEXRIVE-APK-Inspector-0.5.0-debug.apk` — Android 7.0+ debug-key-signed testing build.
- `DEXRIVE-APK-Inspector-0.5.0-unsigned.aab` — unsigned Play upload bundle, **not a production-signed application**.
- `SHA256SUMS.txt` — hashes for all four binaries.

Both Windows programs currently lack an Authenticode signature. To publish the Android bundle to Play, the app owner must sign it with the authorized upload key; the debug APK is not a substitute.

## Screenshots
The [README](../README.md) includes **real captured Windows and Android application screens** from running DEXRIVE v1.3.0 builds, not mockups. This release changes the populated Android report, while the brand and home screen remain consistent. A newly captured inspection screenshot should be reviewed before advertising it as a v1.4.0 report screen.

## Compatibility and licensing
User-facing product naming remains DEXRIVE by Brendigo. Internal upstream-derived `jadx.*` namespaces remain where needed to avoid breaking integrations. Apache 2.0 LICENSE and NOTICE preserve attribution to the original JADX contributors. Newly created DEXRIVE product designs and enhancements © 2026 Brendigo.

## Inspection limitations
DEXRIVE reports metadata of ZIP/APK/AAB/APKS/APKM/XAPK archives without extracting or executing their entries. It is not a malware scanner, a signing verifier or a guarantee that decompiled Java reflects original source. Untrusted archives should be inspected in an isolated environment.
