# DEXRIVE offline archive inspection: security model

This document describes a **scoped source-review** of the Windows and Android archive inspectors, not a comprehensive external penetration test, SBOM assessment, or security certification.

## Trust boundary

- **Untrusted input:** an archive selected by the user, ZIP filenames, entry headers and reported size/compression metadata.
- **Android:** selected document data comes from the Android document provider and is copied into app-private temporary storage before ZIP enumeration. The copy is capped at 512 MiB and SHA-256 is computed during the copy. The temporary file is removed in a `finally` block.
- **Windows:** `ZipFile` reads local archive metadata in a background Swing worker to keep the interface responsive; on dialog close, enumeration is cancelled cooperatively via thread interruption.
- **No extraction/execution:** neither inspector runs, writes out or decompresses ZIP entries as part of the metadata report.
- **No malicious-content verdict:** unusual path names, compression ratios and size counts are **heuristics only**. Signature-file *presence* is not APK signature verification.

## Safeguards and limits

| Control | Windows | Android |
| --- | --- | --- |
| Enumerate at most 100,000 ZIP entries | Yes | Yes |
| Detect traversal-like names | Absolute, drive-prefixed, NUL, `..` segments | Same classes |
| Flag high compression ratio | Greater than 100:1 | Greater than 100:1 |
| Flag oversized entry metadata | Greater than 512 MiB | Greater than 512 MiB |
| Read ZIP entry contents | No | No |
| Cancellation | SwingWorker/interrupted thread | Coroutine context checked during copy and scan |
| App-private copy cap | Not needed for direct-file inspection | 512 MiB |
| Verify v2/v3/v4 APK signature blocks | No | No |

ZIP metadata may be crafted or inconsistent. The aggregate sizes are computed from entries with known nonnegative sizes and saturate on overflow. These numbers **must not** be used to estimate the memory needed to extract an archive.

## Remaining production checks

- Fuzz malformed ZIP headers and filename encodings in an isolated environment.
- Validate Android 7–current system document providers and lifecycle cancellation on physical devices, including very large files.
- Run dependency vulnerability review and generate a provenance-aware SBOM in a controlled build.
- Verify actual Windows DPI and Android TalkBack/font scaling against approved UI references.
- Complete code signing using the authorized keys; never claim signed binaries without signatures.
