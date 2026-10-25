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
The detected MIME is supplied to the explicitly selected parser as trusted detector
output. Image dimensions and MP3 audio sample rate must actually be parsed; truncated
image/ID3 headers cannot pass just because their magic bytes match.

ZIP preflight reads actual expanded data, rejects duplicate/traversal names and
external XML entities, and bounds entries, expanded bytes and XML/text output.
Only the selected parser runs; there is no `AutoDetectParser`, OCR or embedded
document recursion. The verifier discards document text rather than retaining it.
Default limits are 100 MiB expanded package bytes, 10,000 entries, 1,048,576 text
characters per parsing stage, and two concurrent verifications. Saturation fails
with `BLOB_VERIFICATION_BUSY`; malformed/unsupported content fails with
`BLOB_FORMAT_NOT_ALLOWED`. Resource size limits use `BLOB_SIZE_EXCEEDED`.

The configured staging root must be a private dedicated POSIX directory. Each
verification owns a leased job containing its input and Tika request resources.
PDFBox 3.0.8 (matching Tika 4.1) uses an explicitly configured spill cache in that
job, with at most 8 MiB main cache memory and the expanded-byte limit for total
cache storage. No global JVM or ImageIO cache setting is changed. Ordinary close
removes the job. `stagingMaintenance(permission)` supplies explicit global host
collection after process termination, with active leases preserved. This permission
is independent of tenant catalog management; see `teaql-blob-staging-file`.
The caller retains ownership of the source stream. These
limits do not provide a hard CPU timeout, process isolation or malware scanning;
host collection and scheduling remain explicit.
Applications select their own logging backend.

Tests use unchanged real JPG/JPEG/PNG/MP4/MP3/DOC/DOCX/XLS/XLSX/PDF fixtures and
verify actual parsing, disguised containers, stream bounds, I/O sanitization,
ZIP expansion/path/XML controls, truncated media and concurrent admission. The
PDF cache test actually spills inside the lease and exceeds its storage budget.
The
separate Blob reference module's opt-in `BlobUploadIT` combines this verifier
with real PostgreSQL, private file storage and native business SQL binding.
Source component evidence does not establish generated/native-image or registry
consumer acceptance; those remain separate gates.
