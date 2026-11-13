package jadx.gui.ui.dialog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies local, non-executing archive metadata analysis for the DEXRIVE desktop inspector. */
public class DexriveInspectorTest {

    @TempDir
    Path dir;

    @Test
    public void reportsMetadataAndTraversalWarnings() throws IOException {
        Path archive = dir.resolve("sample.apk");
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(archive))) {
            for (String name : new String[] {
                    "classes.dex", "AndroidManifest.xml", "lib/arm64-v8a/libdemo.so",
                    "../escape.txt", "C:relative.txt", "META-INF/CERT.RSA", "resources.arsc" }) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(new byte[] {1, 2, 3});
                zip.closeEntry();
            }
        }

        String report = DexriveInspector.inspect(archive);
        assertTrue(report.contains("DEXRIVE | LOCAL ARCHIVE INSPECTION"));
        assertTrue(report.contains("ZIP entries inspected: 7"));
        assertTrue(report.contains("DEX files: 1"));
        assertTrue(report.contains("Native libraries: 1"));
        assertTrue(report.contains("Android manifest found: true"));
        assertTrue(report.contains("Entries with traversal-like names: 2"));
        assertTrue(report.contains("resources.arsc found: true"));
        assertTrue(report.contains("META-INF legacy signature files present: true"));
        assertTrue(report.contains("Known uncompressed bytes: 21"));
        assertTrue(report.contains("No archive entries were extracted or executed."));
        assertTrue(report.contains("NOT a malware verdict"));
    }

    @Test
    public void rejectsMissingFile() {
        assertThrows(IOException.class, () -> DexriveInspector.inspect(dir.resolve("not-found.apk")));
    }
}
