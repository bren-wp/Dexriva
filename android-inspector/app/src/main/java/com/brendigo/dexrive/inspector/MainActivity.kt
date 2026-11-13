package com.brendigo.dexrive.inspector

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object Brand {
    val midnight = Color(0xFF101927)
    val slate = Color(0xFF172A3F)
    val elevated = Color(0xFF1A2B40)
    val aqua = Color(0xFF34D9D0)
    val cloud = Color(0xFFF1F5F9)
    val muted = Color(0xFFA7B7C9)
    val warning = Color(0xFFFFCA80)
    val error = Color(0xFFFFB39F)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DexriveTheme {
                InspectorScreen { uri -> PackageScanner.inspect(this@MainActivity, uri) }
            }
        }
    }
}

@Composable
private fun DexriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Brand.aqua,
            onPrimary = Brand.midnight,
            background = Brand.midnight,
            onBackground = Brand.cloud,
            surface = Brand.midnight,
            onSurface = Brand.cloud,
            surfaceVariant = Brand.elevated,
            onSurfaceVariant = Brand.muted
        ),
        content = content
    )
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(Brand.slate, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(53.dp)
                .semantics { contentDescription = "DEXRIVE brand mark" }
        ) {
            val scale = size.width / 53f
            drawLine(Brand.aqua, Offset(11f * scale, 10f * scale),
                Offset(29f * scale, 26f * scale), 6f * scale, StrokeCap.Round)
            drawLine(Brand.aqua, Offset(29f * scale, 26f * scale),
                Offset(11f * scale, 42f * scale), 6f * scale, StrokeCap.Round)
            drawLine(Brand.cloud, Offset(32f * scale, 42f * scale),
                Offset(45f * scale, 42f * scale), 4f * scale, StrokeCap.Round)
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BrandMark()
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("DEXRIVE", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
                color = Brand.cloud, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ANDROID APK INSPECTOR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = Brand.aqua, letterSpacing = 0.8.sp)
            Text("by Brendigo  •  brendigo.com", fontSize = 12.sp, color = Brand.muted)
        }
    }
}

@Composable
private fun InspectorScreen(inspect: suspend (Uri) -> PackageReport) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var report by remember { mutableStateOf<PackageReport?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !loading) {
            loading = true
            report = null
            error = null
            scope.launch {
                try {
                    report = withContext(Dispatchers.IO) { inspect(uri) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = "Could not inspect this file. Select a valid Android package or ZIP archive."
                } finally {
                    loading = false
                }
            }
        }
    }

    Scaffold(
        containerColor = Brand.midnight,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { insets ->
        Surface(color = Brand.midnight, modifier = Modifier.fillMaxSize().padding(insets)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Header()
                    HorizontalDivider(color = Brand.slate)
                    Text("Explore beyond the source.", fontSize = 23.sp,
                        fontWeight = FontWeight.Bold, color = Brand.cloud)
                    Text(
                        "Inspect Android package structure and archive metadata locally. No upload, extraction or execution.",
                        color = Brand.muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val compact = maxWidth < 480.dp
                        if (compact) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { picker.launch(arrayOf("*/*")) },
                                    enabled = !loading,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                ) { Text("Open Android package") }
                                if (report != null) {
                                    OutlinedButton(
                                        onClick = { report = null; error = null },
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) { Text("Clear report") }
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { picker.launch(arrayOf("*/*")) },
                                    enabled = !loading,
                                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                                ) { Text("Open Android package") }
                                if (report != null) {
                                    OutlinedButton(
                                        onClick = { report = null; error = null },
                                        modifier = Modifier.heightIn(min = 52.dp)
                                    ) { Text("Clear report") }
                                }
                            }
                        }
                    }
                    if (loading) {
                        Row(
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Inspecting package"
                            },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(26.dp),
                                color = Brand.aqua, strokeWidth = 3.dp)
                            Text("Inspecting package…", color = Brand.muted)
                        }
                    }
                    if (error != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = Brand.elevated),
                            modifier = Modifier.fillMaxWidth()) {
                            Text(error ?: "", color = Brand.error,
                                modifier = Modifier.padding(16.dp))
                        }
                    }
                    report?.let { item ->
                        ReportCard(item) {
                            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            manager.setPrimaryClip(ClipData.newPlainText("DEXRIVE SHA-256", item.sha256))
                            scope.launch { snackbar.showSnackbar("SHA-256 copied") }
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Brand.slate),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("PRIVATE BY DESIGN", color = Brand.aqua,
                                style = MaterialTheme.typography.labelLarge)
                            Text("Only local archive metadata is inspected. No network or storage permissions.",
                                color = Brand.cloud, style = MaterialTheme.typography.bodyMedium)
                            Text("This is not a malware scan or APK signature verification.",
                                color = Brand.muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ReportCard(item: PackageReport, onCopyHash: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Brand.elevated),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("INSPECTION COMPLETE", color = Brand.aqua,
                style = MaterialTheme.typography.labelLarge)
            Text(item.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                color = Brand.cloud)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val metrics = listOf(
                    "ZIP entries" to item.entries.toString(),
                    "Archive size" to readableBytes(item.archiveBytes),
                    "DEX files" to item.dex.toString(),
                    "Native libraries" to item.native.toString(),
                    "Manifest" to (if (item.manifest) "Found" else "Not found"),
                    "resources.arsc" to (if (item.resources) "Found" else "Absent"),
                    "Legacy signature files" to (if (item.legacySignatureFiles) "Found" else "Absent"),
                    "Known unpacked" to readableBytes(item.knownUncompressedBytes),
                    "Known compressed" to readableBytes(item.knownCompressedBytes),
                    "Traversal-like paths" to item.traversal.toString(),
                    "High compression" to item.highCompression.toString(),
                    "Over 512 MiB" to item.largeEntries.toString()
                )
                val columns = if (maxWidth < 520.dp) 2 else 3
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { metric ->
                                StatTile(metric.first, metric.second,
                                    modifier = Modifier.weight(1f))
                            }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (item.truncated) {
                Text("Entry limit reached. Results are partial.", color = Brand.warning)
            }
            if (item.traversal > 0 || item.highCompression > 0 || item.largeEntries > 0) {
                Text("Heuristic flags are not evidence of malware.", color = Brand.warning,
                    style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider(color = Brand.slate)
            Text("SHA-256", color = Brand.aqua, fontWeight = FontWeight.SemiBold)
            SelectionContainer {
                Text(item.sha256, color = Brand.cloud, fontSize = 12.sp,
                    lineHeight = 18.sp)
            }
            OutlinedButton(onClick = onCopyHash,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Copy SHA-256")
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Brand.slate, RoundedCornerShape(14.dp))
            .heightIn(min = 82.dp)
            .padding(12.dp)
            .semantics(mergeDescendants = true) { contentDescription = label + ": " + value },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = Brand.cloud, maxLines = 1)
        Text(label, fontSize = 11.sp, color = Brand.muted,
            maxLines = 2, lineHeight = 14.sp)
    }
}

/** Display-only formatting. Byte counts remain exact in [PackageReport]. */
private fun readableBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KiB", "MiB", "GiB", "TiB")
    var value = bytes.toDouble()
    var unit = -1
    do {
        value /= 1024.0
        unit++
    } while (value >= 1024.0 && unit < units.lastIndex)
    return "%.1f %s".format(Locale.ROOT, value, units[unit])
}
