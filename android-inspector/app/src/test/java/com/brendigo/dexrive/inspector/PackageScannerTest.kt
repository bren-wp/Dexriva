package com.brendigo.dexrive.inspector

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class PackageScannerTest {
    @Test fun sanitizeUntrustedDisplayName() {
        assertEquals("Android package", PackageScanner.sanitizeDisplayName("  "))
        assertEquals("sample.apk", PackageScanner.sanitizeDisplayName("  sample.apk  "))
        assertEquals(160, PackageScanner.sanitizeDisplayName("x".repeat(250)).length)
        assertEquals("demo.apk", PackageScanner.sanitizeDisplayName("demo.apk\n\t"))
    }

    @Test fun traversalPathExamples() {
        for (name in listOf("../escape", "folder\\..\\escape", "/absolute", "C:relative.txt",
                "C:/absolute", "\\\\server\\share", "bad\u0000name")) {
            assertTrue("Expected risky ZIP path: $name", PackageScanner.isTraversalLike(name))
        }
        for (name in listOf("lib/arm64-v8a/liba.so", "subdir/normal.txt",
                "folder/a..b/ok.txt", "AndroidManifest.xml")) {
            assertFalse("Expected safe ZIP path: $name", PackageScanner.isTraversalLike(name))
        }
    }

    @Test fun interruptedScanStopsBeforeEnumeratingEntries() {
        val file = File.createTempFile("dexrive-cancel", ".apk")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("classes.dex"))
                zip.write(1)
                zip.closeEntry()
            }
            assertThrows(IllegalStateException::class.java) {
                PackageScanner.inspectZip(file) { throw IllegalStateException("cancelled") }
            }
        } finally { file.delete() }
    }

    @Test fun inspectSyntheticApk() {
        val file = File.createTempFile("dexrive-test", ".apk")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                for (name in listOf("classes.dex", "AndroidManifest.xml", "lib/arm64-v8a/libx.so", "../escape", "resources.arsc", "META-INF/CERT.RSA", "C:relative.txt")) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(1)
                    zip.closeEntry()
                }
            }
            val r = PackageScanner.inspectZip(file)
            assertEquals(7, r.entries)
            assertEquals(1, r.dex)
            assertEquals(1, r.native)
            assertEquals(2, r.traversal)
            assertTrue(r.manifest)
            assertTrue(r.resources)
            assertTrue(r.legacySignatureFiles)
            assertEquals(0, r.largeEntries)
            assertEquals(7L, r.knownUncompressedBytes)
            assertTrue(r.knownCompressedBytes > 0L)
        } finally { file.delete() }
    }
}
