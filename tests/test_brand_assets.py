"""DEXRIVE module, branding and legal retention smoke tests."""
import struct
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class BrandIntegrationTest(unittest.TestCase):
    def test_original_license_and_notice(self):
        text = (ROOT / "LICENSE").read_text(encoding="utf-8")
        self.assertTrue(text.lstrip().startswith("Apache License"))
        self.assertTrue((ROOT / "NOTICE").is_file())

    def test_about_upstream_attribution(self):
        content = (ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/dialog/AboutDialog.java").read_text()
        self.assertIn("DEXRIVE", content)
        self.assertNotIn("Based on open-source JADX", content)
        self.assertNotIn("JADX logo", content)

    def test_splash_and_workspace_are_branded(self):
        splash = (ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/DexriveSplash.java").read_text()
        home = (ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/startpage/StartPagePanel.java").read_text()
        self.assertIn("DEXRIVE", splash)
        self.assertIn("PREPARING WORKSPACE", splash)
        self.assertIn("Open package", home)
        self.assertIn("APK Inspector", home)
        self.assertIn("componentResized", home)

    def test_upstream_logo_assets_not_shipped(self):
        logos = ROOT / "dexrive-gui/src/main/resources/logos"
        self.assertFalse(list(logos.glob("jadx-logo*")))
        self.assertFalse((ROOT / "dexrive-gui/dist/windows/jadx-logo.ico").exists())
        self.assertFalse((ROOT / "dexrive-gui/dist/macos/jadx-logo.icns").exists())
        self.assertTrue((ROOT / "dexrive-gui/dist/macos/dexrive-logo.icns").exists())

    def test_no_auto_upstream_update_or_issue_links(self):
        main = (ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/MainWindow.java").read_text()
        error = (ROOT / "dexrive-gui/src/main/java/jadx/gui/report/ExceptionDialog.java").read_text()
        self.assertNotIn("checkForUpdate();", main)
        self.assertNotIn("github.com/", error)
        self.assertNotIn("new JadxUpdate()", main)


    def test_product_assets_and_attribution(self):
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        self.assertIn("By Brendigo", readme)
        self.assertIn("brendigo.com", readme)
        for path in ["assets/dexrive-mark.svg", "assets/dexrive-banner.svg",
                     "assets/dexrive-workspace-concept.svg",
                     "assets/feature-inspect.svg", "assets/feature-code.svg",
                     "assets/feature-privacy.svg", "BRAND_NOTICE.md"]:
            self.assertTrue((ROOT / path).is_file(), path)

    def test_native_inspector_in_tools_menu(self):
        content = (ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/MainWindow.java").read_text()
        self.assertIn("DexriveInspector.open(this)", content)
        self.assertIn('DEFAULT_TITLE = "DEXRIVE • Brendigo"', content)

    def test_all_locales_use_new_product_name(self):
        root = ROOT / "dexrive-gui/src/main/resources/i18n"
        for path in root.glob("Messages_*.properties"):
            data = path.read_text(encoding="utf-8")
            self.assertNotIn("DEXRIVA", data, str(path))
            self.assertNotIn("Jadx is running low", data, str(path))
            self.assertIn("start_page.title=DEXRIVE Workspace", data, str(path))


    def test_installer_artwork_matches_generated_names(self):
        import re
        script = (ROOT / ".dexrive/make_setup_artwork.py").read_text(encoding="utf-8")
        installer = (ROOT / "packaging/windows/dexrive-setup.nsi").read_text(encoding="utf-8")
        declared = ("dexrive-header.bmp", "dexrive-welcome.bmp")
        self.assertGreaterEqual(len(declared), 2)
        for name in declared:
            self.assertIn('"' + name + '"', script)
            self.assertIn(name, installer)
        self.assertNotIn("dexriva-", installer)

    def test_release_pipeline_has_all_platform_packages(self):
        workflow = (ROOT / ".github/workflows/windows-release.yml").read_text(encoding="utf-8")
        self.assertIn("needs: [windows, android]", workflow)
        self.assertIn("DEXRIVE-1.4.0-setup.exe", workflow)
        self.assertIn("DEXRIVE-1.4.0-portable.exe", workflow)
        self.assertIn("DEXRIVE-APK-Inspector-0.5.0-debug.apk", workflow)
        self.assertIn("DEXRIVE-APK-Inspector-0.5.0-unsigned.aab", workflow)
        self.assertIn("testDebugUnitTest :app:assembleDebug :app:bundleRelease", workflow)
        self.assertIn("SHA256SUMS.txt", workflow)

    def test_1_3_release_and_real_screen_capture_pipeline(self):
        release = (ROOT / ".github/workflows/windows-release.yml").read_text(encoding="utf-8")
        capture = (ROOT / ".github/workflows/capture-ui.yml").read_text(encoding="utf-8")
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        desktop = (ROOT / "dexrive-gui/build.gradle.kts").read_text(encoding="utf-8")
        android = (ROOT / "android-inspector/app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('version.set("1.4.0")', desktop)
        self.assertIn('versionCode = 4', android)
        self.assertIn('versionName = "0.5.0"', android)
        self.assertIn('gh release create v1.4.0', release)
        self.assertIn('reactivecircus/android-emulator-runner@v2', capture)
        self.assertIn('capture_windows_ui.ps1', capture)
        self.assertIn('GetWindowRect', (ROOT / "scripts/capture_windows_ui.ps1").read_text(encoding="utf-8"))
        self.assertIn('<!-- BEGIN DEXRIVE SCREENSHOTS -->', readme)
        self.assertIn('<!-- END DEXRIVE SCREENSHOTS -->', readme)
        self.assertIn('Concept art', readme)
        self.assertTrue((ROOT / "docs/RELEASE_1.4.0.md").is_file())

    def test_android_layout_is_width_constrained(self):
        activity = (ROOT / "android-inspector/app/src/main/java/com/brendigo/dexrive/inspector/MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("widthIn(max = 720.dp)", activity)
        self.assertIn("verticalScroll(rememberScrollState())", activity)

    def test_renamed_gradle_modules(self):
        settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('rootProject.name = "dexrive"', settings)
        for module in ("dexrive-cli", "dexrive-core", "dexrive-gui",
                       "dexrive-gui-api", "dexrive-plugins-tools"):
            self.assertTrue((ROOT / module / "build.gradle.kts").is_file(), module)
            self.assertIn('include("' + module + '")', settings)
        for original in ("jadx-cli", "jadx-core", "jadx-gui", "jadx-gui-api",
                         "jadx-plugins", "jadx-commons", "jadx-plugins-tools"):
            self.assertFalse((ROOT / original).exists(), original)
        self.assertTrue((ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/DexriveSplash.java").exists())
        self.assertTrue((ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/dialog/DexriveInspector.java").exists())
        self.assertTrue((ROOT / ".dexrive/make_icons.py").exists())
        self.assertFalse((ROOT / ".dexriva").exists())

    def test_distribution_uses_dexrive_executable_and_keeps_notices(self):
        build = (ROOT / "build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('path == "bin/dexrive"', build)
        self.assertNotIn('path == "bin/jadx"', build)
        self.assertIn('group = "dexrive"', build)
        self.assertIn('include("BRAND_NOTICE.md")', build)
        self.assertIn('include("NOTICE")', build)
        cli = (ROOT / "dexrive-cli/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('applicationName = "dexrive"', cli)

    def test_windows_installer_uses_generated_dexrive_branding(self):
        installer = (ROOT / "packaging/windows/dexrive-setup.nsi").read_text(encoding="utf-8")
        self.assertIn('dexrive-welcome.bmp', installer)
        self.assertIn('dexrive-header.bmp', installer)
        self.assertNotIn('dexriva-welcome.bmp', installer)
        self.assertNotIn('dexriva-header.bmp', installer)

    def test_cli_help_uses_brendigo_identity(self):
        cli = (ROOT / "dexrive-cli/src/main/java/jadx/cli/JCommanderWrapper.java").read_text(encoding="utf-8")
        self.assertIn("DEXRIVE by Brendigo", cli)
        self.assertIn("usage: dexrive [command] [options]", cli)
        self.assertIn("DEXRIVE_DISABLE_XML_SECURITY", cli)
        self.assertNotIn('out.println("  jadx ', cli)
        args = (ROOT / "dexrive-cli/src/main/java/jadx/cli/JadxCLIArgs.java").read_text(encoding="utf-8")
        self.assertIn("print DEXRIVE version", args)

    def test_branded_env_alias_preserves_existing_integrations(self):
        helper = (ROOT / "dexrive-commons/dexrive-app-commons/src/main/java/jadx/commons/app/JadxCommonEnv.java").read_text(encoding="utf-8")
        self.assertIn('varName.startsWith("JADX_")', helper)
        self.assertIn('"DEXRIVE_" + varName.substring', helper)
        self.assertIn("return System.getenv(varName);", helper)

    def test_android_responsive_ui_and_accessibility(self):
        activity = (ROOT / "android-inspector/app/src/main/java/com/brendigo/dexrive/inspector/MainActivity.kt").read_text(encoding="utf-8")
        for label in (
            "DEXRIVE", "by Brendigo", "brendigo.com",
            "Open Android package", "Copy SHA-256", "INSPECTION COMPLETE",
            "widthIn(max = 720.dp)", "verticalScroll(rememberScrollState())",
            "BoxWithConstraints", "SnackbarHost",
            "semantics(mergeDescendants = true)", "Cancel",
        ):
            # The app must handle cancellation as well as readable, scalable cards.
            self.assertIn(label if label != "Cancel" else "CancellationException", activity)

    def test_windows_responsive_ui_and_cancelable_inspection(self):
        start = (ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/startpage/StartPagePanel.java").read_text(encoding="utf-8")
        inspector = (ROOT / "dexrive-gui/src/main/java/jadx/gui/ui/dialog/DexriveInspector.java").read_text(encoding="utf-8")
        self.assertIn("width < 900", start)
        self.assertIn("width < 490", start)
        self.assertIn("KeyEvent.VK_ENTER", start)
        self.assertIn("openRecentProject", start)
        self.assertIn("worker.cancel(true)", inspector)
        self.assertIn("Thread.currentThread().isInterrupted()", inspector)
        self.assertIn("UiUtils.setWindowIcons(dialog)", inspector)
        self.assertIn("getMaximumWindowBounds()", inspector)

    def test_adaptive_android_launcher_brand(self):
        root = ROOT / "android-inspector/app/src/main"
        manifest = (root / "AndroidManifest.xml").read_text(encoding="utf-8")
        self.assertIn('android:icon="@mipmap/ic_launcher"', manifest)
        self.assertIn('android:roundIcon="@mipmap/ic_launcher"', manifest)
        self.assertTrue((root / "res/mipmap-anydpi/ic_launcher.xml").is_file())
        adaptive = (root / "res/mipmap-anydpi-v26/ic_launcher.xml").read_text(encoding="utf-8")
        self.assertIn("@drawable/ic_dexrive_foreground", adaptive)
        self.assertIn("@color/dexrive_icon_background", adaptive)
        foreground = (root / "res/drawable/ic_dexrive_foreground.xml").read_text(encoding="utf-8")
        self.assertIn("#34D9D0", foreground)
        self.assertIn("#F1F5F9", foreground)

    def test_android_startup_theme_matches_brand(self):
        res = ROOT / "android-inspector/app/src/main/res"
        manifest = (ROOT / "android-inspector/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
        theme = (res / "values/styles.xml").read_text(encoding="utf-8")
        splash = (res / "values-v31/styles.xml").read_text(encoding="utf-8")
        self.assertIn('@style/Theme.Dexrive', manifest)
        self.assertIn('@color/dexrive_icon_background', theme)
        self.assertIn('windowSplashScreenBackground', splash)
        self.assertIn('windowSplashScreenAnimatedIcon', splash)

    def test_legal_provenance_and_brand_credit(self):
        brand_notice = (ROOT / "BRAND_NOTICE.md").read_text(encoding="utf-8")
        self.assertIn("Brendigo", brand_notice)
        self.assertTrue((ROOT / "LICENSE").exists())
        self.assertTrue((ROOT / "NOTICE").exists())

    def test_generated_png_icons(self):
        logos = ROOT / "dexrive-gui/src/main/resources/logos"
        sizes = [(16, "dexrive-logo-16px.png"), (32, "dexrive-logo-32px.png"), (48, "dexrive-logo-48px.png"), (256, "dexrive-logo.png")]
        for size, filename in sizes:
            with self.subTest(filename=filename):
                data = (logos / filename).read_bytes()
                self.assertEqual(data[:8], b"\x89PNG\r\n\x1a\n")
                self.assertEqual(struct.unpack(">II", data[16:24]), (size, size))

if __name__ == "__main__":
    unittest.main()
