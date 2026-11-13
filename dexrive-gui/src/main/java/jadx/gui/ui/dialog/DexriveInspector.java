package jadx.gui.ui.dialog;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import jadx.gui.utils.UiUtils;

/**
 * Offline archive metadata inspection. Does not extract, load, or execute APK contents.
 * Not a malware scanner or cryptographic signature verifier.
 */
public final class DexriveInspector {
    private static final int MAX_ENTRIES = 100_000;
    private static final long LARGE_ENTRY = 512L * 1024 * 1024;
    private static final double HIGH_RATIO = 100.0;
    private static final Color AQUA = new Color(0x34D9D0);
    private static final Color MUTED = new Color(0xA7B7C9);
    private static final Color NAVY = new Color(0x101927);
    private static final Color PANEL = new Color(0x172A3F);

    private DexriveInspector() {
    }

    /**
     * Scalable, non-modal inspector workspace. ZIP enumeration runs off the Swing event thread.
     */
    public static void open(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "DEXRIVE | APK Inspector", Dialog.ModalityType.MODELESS);
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        content.setBackground(NAVY);

        JLabel title = new JLabel("APK INSPECTOR");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 23f));
        title.setForeground(AQUA);
        JLabel subtitle = new JLabel("Local package analysis • by Brendigo");
        subtitle.setForeground(MUTED);
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        content.add(header, BorderLayout.NORTH);

        JTextArea report = new JTextArea("Choose an APK, AAB, APKS, APKM, XAPK or ZIP to inspect.\n"
                + "ZIP metadata is read locally and no files are extracted or uploaded.");
        report.setEditable(false);
        report.getAccessibleContext().setAccessibleName("DEXRIVE archive inspection report");
        report.getAccessibleContext().setAccessibleDescription(
                "Read-only ZIP metadata, heuristic warnings and package inspection limitations.");
        report.setLineWrap(true);
        report.setWrapStyleWord(true);
        report.setBackground(PANEL);
        report.setForeground(Color.WHITE);
        report.setCaretColor(AQUA);
        report.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        report.setMargin(new java.awt.Insets(14, 14, 14, 14));
        JScrollPane scroll = new JScrollPane(report);
        content.add(scroll, BorderLayout.CENTER);

        JButton choose = new JButton("Open package");
        choose.getAccessibleContext().setAccessibleDescription("Choose a local Android package or ZIP archive.");
        JButton copy = new JButton("Copy report");
        copy.setEnabled(false);
        JButton close = new JButton("Close");
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
        controls.setOpaque(false);
        controls.add(choose);
        controls.add(copy);
        controls.add(close);
        content.add(controls, BorderLayout.SOUTH);

        AtomicReference<SwingWorker<String, Void>> activeWorker = new AtomicReference<>();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                SwingWorker<String, Void> worker = activeWorker.getAndSet(null);
                if (worker != null) {
                    worker.cancel(true);
                }
            }
        });

        choose.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("DEXRIVE | Choose Android package");
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Android packages (*.apk, *.aab, *.apks, *.apkm, *.xapk, *.zip)",
                    "apk", "aab", "apks", "apkm", "xapk", "zip"));
            if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            Path path = chooser.getSelectedFile().toPath();
            report.setText("Inspecting " + path.getFileName() + "...");
            choose.setEnabled(false);
            copy.setEnabled(false);
            SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws IOException {
                    return inspect(path);
                }

                @Override
                protected void done() {
                    activeWorker.compareAndSet(this, null);
                    if (!dialog.isDisplayable()) {
                        return;
                    }
                    choose.setEnabled(true);
                    if (isCancelled()) {
                        return;
                    }
                    try {
                        report.setText(get());
                        report.setCaretPosition(0);
                        copy.setEnabled(true);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        report.setText("Inspection interrupted.");
                    } catch (java.util.concurrent.ExecutionException e) {
                        report.setText("Cannot inspect archive: " + e.getCause().getMessage());
                    }
                }
            };
            activeWorker.set(worker);
            worker.execute();
        });

        copy.addActionListener(event -> {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(report.getText()), null);
            } catch (IllegalStateException | SecurityException e) {
                JOptionPane.showMessageDialog(dialog, "Clipboard is unavailable. Please try again.",
                        "DEXRIVE", JOptionPane.WARNING_MESSAGE);
            }
        });
        close.addActionListener(event -> dialog.dispose());

        dialog.setContentPane(content);
        UiUtils.setWindowIcons(dialog);
        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = Math.max(320, usable.width - 48);
        int height = Math.max(260, usable.height - 48);
        dialog.setMinimumSize(new Dimension(Math.min(420, width), Math.min(330, height)));
        dialog.setSize(Math.min(820, width), Math.min(600, height));
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    public static String inspect(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Not a regular file: " + path);
        }
        int entries = 0;
        int dexFiles = 0;
        int nativeLibraries = 0;
        int pathTraversalNames = 0;
        int largeEntries = 0;
        int highCompressionRatios = 0;
        boolean manifest = false;
        boolean resources = false;
        boolean v1SignatureFile = false;
        boolean limitReached = false;
        long uncompressed = 0;
        long compressed = 0;
        try (ZipFile zip = new ZipFile(path.toFile())) {
            Enumeration<? extends ZipEntry> iterator = zip.entries();
            while (iterator.hasMoreElements()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new IOException("Inspection cancelled");
                }
                if (entries == MAX_ENTRIES) {
                    limitReached = true;
                    break;
                }
                ZipEntry entry = iterator.nextElement();
                entries++;
                String entryName = entry.getName().replace('\\', '/');
                String lower = entryName.toLowerCase(Locale.ROOT);
                if (lower.endsWith(".dex")) {
                    dexFiles++;
                }
                if (lower.endsWith(".so") && (lower.startsWith("lib/") || lower.contains("/lib/"))) {
                    nativeLibraries++;
                }
                if (lower.equals("androidmanifest.xml") || lower.endsWith("/manifest/androidmanifest.xml")) {
                    manifest = true;
                }
                if (lower.equals("resources.arsc")) {
                    resources = true;
                }
                if (lower.startsWith("meta-inf/")
                        && (lower.endsWith(".rsa") || lower.endsWith(".dsa") || lower.endsWith(".ec"))) {
                    v1SignatureFile = true;
                }
                if (entryName.startsWith("/") || entryName.matches("^[a-zA-Z]:.*")
                        || entryName.indexOf('\0') >= 0) {
                    pathTraversalNames++;
                } else {
                    for (String part : entryName.split("/")) {
                        if ("..".equals(part)) {
                            pathTraversalNames++;
                            break;
                        }
                    }
                }
                long raw = entry.getSize();
                long packed = entry.getCompressedSize();
                if (raw >= 0) {
                    uncompressed = saturatedAdd(uncompressed, raw);
                    if (raw > LARGE_ENTRY) {
                        largeEntries++;
                    }
                    if (packed > 0 && ((double) raw / packed) > HIGH_RATIO) {
                        highCompressionRatios++;
                    }
                    if (packed == 0 && raw > 0) {
                        highCompressionRatios++;
                    }
                }
                if (packed >= 0) {
                    compressed = saturatedAdd(compressed, packed);
                }
            }
        }
        StringBuilder out = new StringBuilder();
        out.append("DEXRIVE | LOCAL ARCHIVE INSPECTION\n");
        out.append("=================================\n");
        out.append("File: ").append(path.getFileName()).append('\n');
        out.append("Archive bytes: ").append(Files.size(path)).append('\n');
        out.append("ZIP entries inspected: ").append(entries).append('\n');
        out.append("DEX files: ").append(dexFiles).append('\n');
        out.append("Native libraries: ").append(nativeLibraries).append('\n');
        out.append("Android manifest found: ").append(manifest).append('\n');
        out.append("resources.arsc found: ").append(resources).append('\n');
        out.append("META-INF legacy signature files present: ").append(v1SignatureFile).append('\n');
        out.append("Known uncompressed bytes: ").append(uncompressed).append('\n');
        out.append("Known compressed entry bytes: ").append(compressed).append("\n\n");
        out.append("HEURISTIC WARNINGS (not proof of maliciousness)\n");
        out.append("Entries with traversal-like names: ").append(pathTraversalNames).append('\n');
        out.append("Entries larger than 512 MiB: ").append(largeEntries).append('\n');
        out.append("Entries with compression ratio over 100:1: ").append(highCompressionRatios).append('\n');
        if (limitReached) {
            out.append("WARNING: Entry count limit reached; summary is incomplete.\n");
        }
        out.append("\nNo archive entries were extracted or executed.\n");
        out.append("APK Signing Block v2/v3/v4 is NOT verified.\n");
        out.append("This report is NOT a malware verdict or a signature verification.\n");
        return out.toString();
    }

    private static long saturatedAdd(long a, long b) {
        return Long.MAX_VALUE - a < b ? Long.MAX_VALUE : a + b;
    }
}
