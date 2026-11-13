package com.brendigo.dexrive.inspector

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

data class PackageReport(
    val name: String, val entries: Int, val dex: Int, val native: Int,
    val manifest: Boolean, val traversal: Int, val highCompression: Int,
    val truncated: Boolean, val sha256: String,
    val resources: Boolean, val legacySignatureFiles: Boolean, val largeEntries: Int,
    val knownUncompressedBytes: Long, val knownCompressedBytes: Long, val archiveBytes: Long
)
internal data class ZipStats(
    val entries: Int, val dex: Int, val native: Int,
    val manifest: Boolean, val traversal: Int, val highCompression: Int,
    val truncated: Boolean, val resources: Boolean, val legacySignatureFiles: Boolean,
    val largeEntries: Int, val knownUncompressedBytes: Long, val knownCompressedBytes: Long
)
object PackageScanner {
    private const val LIMIT_BYTES = 512L * 1024 * 1024
    // The document provider controls DISPLAY_NAME. Keep its label single-line and bounded.
    internal fun sanitizeDisplayName(value: String?): String {
        val cleaned = value?.filterNot { Character.isISOControl(it) }?.trim()?.take(160)
        return if (cleaned.isNullOrEmpty()) "Android package" else cleaned
    }
    internal suspend fun inspect(context: Context, uri: Uri): PackageReport {
        val inspectionContext = currentCoroutineContext()
        val name = sanitizeDisplayName(runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull())
        val tmp = File.createTempFile("dexrive-", ".zip", context.cacheDir)
        try {
            val hash = MessageDigest.getInstance("SHA-256")
            var size = 0L
            context.contentResolver.openInputStream(uri).use { input ->
                if (input == null) throw IOException("Cannot open document")
                tmp.outputStream().use { output ->
                    val buf = ByteArray(65536)
                    while (true) {
                        inspectionContext.ensureActive()
                        val read = input.read(buf)
                        if (read < 0) break
                        if (read == 0) continue
                        if (read > LIMIT_BYTES - size) throw IOException("File exceeds 512 MiB safety limit")
                        size += read
                        output.write(buf, 0, read)
                        hash.update(buf, 0, read)
                    }
                }
            }
            val stat = inspectZip(tmp) { inspectionContext.ensureActive() }
            return PackageReport(
                name, stat.entries, stat.dex, stat.native, stat.manifest,
                stat.traversal, stat.highCompression, stat.truncated,
                hash.digest().joinToString("") { "%02x".format(it) },
                stat.resources, stat.legacySignatureFiles, stat.largeEntries,
                stat.knownUncompressedBytes, stat.knownCompressedBytes, size
            )
        } finally {
            tmp.delete()
        }
    }
    private const val MAX_ENTRIES = 100_000
    private const val LARGE_ENTRY = 512L * 1024 * 1024

    // ZIP paths are never extracted; these indicators only flag suspicious metadata.
    internal fun isTraversalLike(name: String): Boolean {
        val normalized = name.replace('\\', '/')
        return normalized.startsWith('/') ||
            (normalized.length >= 2 && normalized[0].isLetter() && normalized[1] == ':') ||
            normalized.indexOf('\u0000') >= 0 ||
            normalized.split('/').contains("..")
    }

    private fun addWithoutOverflow(a: Long, b: Long): Long =
        if (b > Long.MAX_VALUE - a) Long.MAX_VALUE else a + b

    internal fun inspectZip(file: File, checkActive: () -> Unit = {}): ZipStats {
        var entries = 0
        var dex = 0
        var native = 0
        var manifest = false
        var traversal = 0
        var ratios = 0
        var truncated = false
        var resources = false
        var legacySignatureFiles = false
        var largeEntries = 0
        var knownUncompressedBytes = 0L
        var knownCompressedBytes = 0L
        ZipFile(file).use { zip ->
            val iter = zip.entries()
            while (iter.hasMoreElements()) {
                checkActive()
                if (entries >= MAX_ENTRIES) {
                    truncated = true
                    break
                }
                val e = iter.nextElement()
                entries++
                val lower = e.name.replace('\\', '/').lowercase(Locale.ROOT)
                if (lower.endsWith(".dex")) dex++
                if (lower.endsWith(".so") && (lower.startsWith("lib/") || lower.contains("/lib/"))) native++
                if (lower == "androidmanifest.xml" || lower.endsWith("/manifest/androidmanifest.xml")) manifest = true
                if (lower == "resources.arsc") resources = true
                if (lower.startsWith("meta-inf/") &&
                    (lower.endsWith(".rsa") || lower.endsWith(".dsa") || lower.endsWith(".ec"))
                ) legacySignatureFiles = true
                if (isTraversalLike(e.name)) traversal++
                val raw = e.size
                val packed = e.compressedSize
                if (raw >= 0L) {
                    knownUncompressedBytes = addWithoutOverflow(knownUncompressedBytes, raw)
                    if (raw > LARGE_ENTRY) largeEntries++
                    if (raw > 0L && (packed == 0L ||
                            (packed > 0L && raw.toDouble() / packed > 100.0))) ratios++
                }
                if (packed >= 0L) {
                    knownCompressedBytes = addWithoutOverflow(knownCompressedBytes, packed)
                }
            }
        }
        return ZipStats(
            entries, dex, native, manifest, traversal, ratios, truncated,
            resources, legacySignatureFiles, largeEntries, knownUncompressedBytes,
            knownCompressedBytes
        )
    }
}
