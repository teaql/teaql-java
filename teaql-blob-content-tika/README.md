# Optional Blob content verifier

Tracking [Java #272](https://github.com/teaql/teaql-java/issues/272) and
[conformance #237](https://github.com/teaql/teaql-conformance/issues/237).

`TikaBlobContentVerifier` implements the core `BlobContentVerifier` SPI using
Apache Tika 4.1.0 and explicit image, audio/video, PDF and Microsoft Office parsers.
The optional artifact is assembled by the host; core and the base runtime have no
Tika dependency. Its constructor performs no I/O and starts no background work.

`verify(source, expectedExtension, maximumBytes, allowEmpty)` stages at most the
permitted input bytes, computes the exact size/SHA-256 and recognizes actual
content without a client filename or Content-Type hint. It checks the expected
suffix against the actual registered type, including OLE/OOXML Word-versus-Excel
containers. Generic ISO BMFF/QuickTime detection is refined by parsing the MP4
brands and requiring an actual video track with positive dimensions. A bare MP4
container header is rejected. Empty bytes must still identify as an allowed format.

ZIP preflight reads actual expanded data, rejects duplicate/traversal names and
external XML entities, and bounds entries, expanded bytes and XML/text output.
Only the selected parser runs; there is no `AutoDetectParser`, OCR or embedded
document recursion. The verifier discards document text rather than retaining it.
Default limits are 100 MiB expanded package bytes, 10,000 entries, 1,048,576 text
characters per parsing stage, and two concurrent verifications. Saturation fails
with `BLOB_VERIFICATION_BUSY`; malformed/unsupported content fails with
`BLOB_FORMAT_NOT_ALLOWED`. Resource size limits use `BLOB_SIZE_EXCEEDED`.

The configured staging root must be a private POSIX directory. Temporary files,
including Tika-managed resources, are scoped there and closed/deleted on ordinary
success and failure. The caller retains ownership of the source stream. These
limits do not provide a hard CPU timeout, process isolation or malware scanning;
crash-left staging reconciliation belongs to the host maintenance lifecycle.
Applications select their own logging backend.

Tests use unchanged real JPG/JPEG/PNG/MP4/MP3/DOC/DOCX/XLS/XLSX/PDF fixtures and
verify actual parsing, disguised containers, stream bounds, I/O sanitization,
ZIP expansion/path/XML controls, truncated MP4 and concurrent admission. The
separate Blob reference module's opt-in `BlobUploadIT` combines this verifier
with real PostgreSQL, private file storage and native business SQL binding.
Source component evidence does not establish generated/native-image or registry
consumer acceptance; those remain separate gates.
