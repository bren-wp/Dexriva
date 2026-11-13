<div align="center">
  <img src="assets/dexrive-banner.svg" alt="DEXRIVE — Android code explorer by Brendigo" width="100%">
  <p><strong>DEXRIVE</strong> · Explore beyond the source.</p>
  <p>
    <a href="https://github.com/bren-wp/DEXRIVA/releases">Download</a> ·
    <a href="https://brendigo.com">Brendigo</a> ·
    <a href="docs/BRAND_GUIDELINES.md">Design system</a> ·
    <a href="docs/MODULE_MIGRATION.md">Compatibility</a>
  </p>
</div>

# An Android explorer built for focus.

**By Brendigo.** DEXRIVE brings package inspection, decompilation and code exploration into a cohesive, local-first desktop workspace, with a lightweight Android companion for inspecting archive metadata directly on your device.

**Published downloads:** [GitHub Releases](https://github.com/bren-wp/DEXRIVA/releases) · **Latest release series:** DEXRIVE 1.4.0 for Windows and Android Inspector 0.5.0. Refer to the Releases page to verify which versions are available. The Android Inspector is a companion app, **not** the full desktop decompiler.

## See the actual applications

<!-- BEGIN DEXRIVE SCREENSHOTS -->

### Windows desktop · Actual running application

![Real DEXRIVE GUI window captured on a Windows runner](docs/screenshots/windows-home.png)

### Android · Actual running companion application

![Real DEXRIVE Inspector home screen captured on Android emulator](docs/screenshots/android-inspector-home.png)

*Both images are genuine captures from running DEXRIVE v1.3.0 applications, not design mockups. The Android image shows the home screen without a selected APK; v1.4.0 adds inspection metrics, so its populated report screen may differ.*

<!-- END DEXRIVE SCREENSHOTS -->

## Why DEXRIVE?

| Explore | Inspect | Stay in control |
| --- | --- | --- |
| Navigate DEX bytecode, decompiled Java classes, references and Android resources on Windows. | Examine APK/AAB/archive structure, native libraries, manifests and heuristic warnings locally. | Use offline inspection without uploading package content to a third-party service. |

The desktop decompilation engine derives from the open-source JADX project. The product identity, visual system and custom enhancements are developed for DEXRIVE by **Brendigo**.

## Made for the way you work

**Windows desktop.** Open a package or previous project from the start screen, explore classes and resources in a tabbed code workspace, and use the built-in APK Inspector to check archive metadata before deeper analysis.

**Android companion.** Open a document through the Android system picker. Review entry counts, DEX/native-library statistics, resource tables, legacy signature-file indicators, compressed-size metadata and archive warnings in accessible cards, then copy the SHA-256 digest for reference. The app processes files locally and temporarily caches the selected archive; it does not send files to a remote service. Inspection is limited to archive metadata and **does not** validate APK signatures or detect malware.

**Automation.** Run `dexrive --help` or `dexrive -d output app.apk`. DEXRIVE-branded environment variables such as `DEXRIVE_CACHE_DIR` and `DEXRIVE_CONFIG_DIR` are supported, while inherited `JADX_*` aliases remain for interoperability.

## Downloads

| Platform | Build | What to expect |
| --- | --- | --- |
| **Windows 10/11** | `DEXRIVE-1.4.0-setup.exe` | Branded installer, per-user setup, shortcuts, uninstall support and bundled Java runtime |
| **Windows 10/11** | `DEXRIVE-1.4.0-portable.exe` | Self-extracting standalone distribution; first run requires a writable folder |
| **Android 7.0+** | `DEXRIVE-APK-Inspector-0.5.0-debug.apk` | Debug-key-signed APK for testing, not a production Play Store build |
| **Android 7.0+** | `DEXRIVE-APK-Inspector-0.5.0-unsigned.aab` | Unsigned release bundle; **requires the legitimate app signing key** for Google Play |

**Availability and integrity:** Check [GitHub Releases](https://github.com/bren-wp/DEXRIVA/releases) for published binaries and SHA-256 checksums. Windows installers currently lack an Authenticode signature. Android debug APKs are test builds and unsigned AABs require the owner's legitimate production upload key.

## Design language

**Midnight** `#101927` · **Slate** `#172A3F` · **Aqua** `#34D9D0` · **Cloud** `#F1F5F9`.

A unified DEXRIVE mark, splash, workspace, installer and Android adaptive icon are built around this system. The UI is responsive, uses scalable text and preserves keyboard-accessible desktop actions. Visual parity is verified only against screenshots of actual running builds.

<details>
<summary>Brand concept illustrations (not screenshots)</summary>

<img src="assets/dexrive-workspace-concept.svg" width="100%" alt="DEXRIVE concept illustration — not a running app screenshot" />

Concept art shows visual direction only; the screenshot gallery above is captured from running applications.

</details>

## Build and development

**Desktop:** JDK 21; `./gradlew :dexrive-gui:test :dexrive-core:test :dexrive-cli:test` and `./gradlew :dexrive-gui:run`.

**Android:** Open `android-inspector/` in Android Studio or run `cd android-inspector && ./gradlew :app:testDebugUnitTest :app:assembleDebug` (JDK 17 plus Android SDK).

**Releases and trust.** CI runs code tests, Windows installer/portable smoke tests and Android APK/AAB builds. UI screenshots have a separate workflow and are not accepted if capture fails. A production Android AAB needs the app owner's signing credentials; a debug APK is **never** a production-signed release.

## Privacy, security and compatibility

The [offline Inspector security model](docs/INSPECTOR_SECURITY.md) documents controls, limits and remaining checks; it is not a security certification.

Archive inputs are untrusted. DEXRIVE's inspector reads ZIP metadata without extracting or executing archive entries; heuristic flags are not malware diagnoses. For suspicious files, use an isolated environment. The decompiler cannot guarantee recreation of original source code.

The internal `jadx.*` packages and saved-project formats remain intentionally compatible with upstream formats; sweeping their identifiers into a new namespace would break existing plug-ins and user projects. See [module migration notes](docs/MODULE_MIGRATION.md).

## Brendigo and legal

**DEXRIVE** · Created and maintained as a branded product by **Brendigo** · [brendigo.com](https://brendigo.com).

Original product design and newly authored DEXRIVE assets © 2026 Brendigo. The included core decompiler contains work by upstream JADX contributors under Apache License 2.0. Their legally required copyright and attribution remain in [LICENSE](LICENSE) and [NOTICE](NOTICE); [BRAND_NOTICE.md](BRAND_NOTICE.md) describes Brendigo additions. This is an independent branded distribution, not an official upstream JADX release.
