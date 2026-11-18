package io.teaql.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.teaql.core.ExecutionMetadata;
import io.teaql.core.UserContext;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Startup-owned structured destination, independent of optional OTel SDKs. */
public final class RuntimeLogger {
    public record Options(String logName, List<String> modelNames,
            Map<String, String> environment, Path workingDirectory, String logFormat) {
        public Options {
            modelNames = modelNames == null ? List.of() : List.copyOf(modelNames);
            environment = environment == null ? null : Map.copyOf(environment);
        }
        public Options(String logName, List<String> modelNames, Map<String, String> environment, Path workingDirectory) {
            this(logName, modelNames, environment, workingDirectory, null);
        }
        public Options() { this(null, List.of(), null, null, null); }
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<Path, Object> GATES = new ConcurrentHashMap<>();
    private static final Map<String, RuntimeLogger> DEFAULTS = new HashMap<>();
    private static final List<String> SETTINGS = List.of("TEAQL_DOMAIN", "TEAQL_LOG_ENDPOINT", "TEAQL_LOG_MAX_SIZE", "TEAQL_LOG_MAX_FILES", "TEAQL_LOG_FORMAT");
    private static final Pattern DEVICE = Pattern.compile("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])");
    private static final Pattern SIZE = Pattern.compile("([0-9]+)\\s*(K|KB|M|MB|G|GB)?");
    private static final Pattern ARCHIVE = Pattern.compile("[1-9][0-9]*");
    private static final Pattern HUMAN_HEADER = Pattern.compile("^\\d{4}-\\d\\d-\\d\\dT\\S+(?:Z|\\+00:00) \\[TeaQL\\]\\[.*\\]\\[(?:INFO|WARN|ERROR)\\] .+ runtime=\\S+$");
    private static final Set<String> EVENT_FIELDS = Set.of("timestamp", "level", "log_name", "runtime", "event", "message");
    private final String logName;
    private final String logFormat;
    private final Path path;
    private final long maxSize;
    private final int maxFiles;
    private final String stream;
    private final Object gate;

    public RuntimeLogger() { this(new Options()); }

    public RuntimeLogger(Options options) {
        String validatedName = "teaql";
        try {
            Map<String, String> env = options.environment() == null ? loggingEnvironment() : options.environment();
            String domain = env.get("TEAQL_DOMAIN");
            String name = options.logName() != null ? options.logName()
                    : domain != null && !domain.isEmpty() ? domain
                    : options.modelNames().size() == 1 ? options.modelNames().get(0) : "teaql";
            validateName(name);
            validatedName = name;
            this.logName = name;
            this.logFormat = options.logFormat() != null ? options.logFormat() : env.getOrDefault("TEAQL_LOG_FORMAT", "json");
            if (!Set.of("json", "human").contains(logFormat)) throw new IllegalArgumentException();
            this.maxSize = parseSize(env.getOrDefault("TEAQL_LOG_MAX_SIZE", "50M"));
            String retention = env.getOrDefault("TEAQL_LOG_MAX_FILES", "7");
            if (!retention.matches("[0-9]+")) throw new IllegalArgumentException();
            this.maxFiles = Integer.parseInt(retention);
            String endpoint = env.get("TEAQL_LOG_ENDPOINT");
            if (endpoint == null || endpoint.isEmpty()) endpoint = "./logs/" + name + ".log";
            if (Set.of("stdout", "stderr", "-").contains(endpoint)) {
                this.stream = endpoint;
                this.path = null;
                this.gate = new Object();
            } else {
                if (endpoint.startsWith("file:")) {
                    URI uri = URI.create(endpoint);
                    if (uri.getPath() == null || uri.getPath().isEmpty() || uri.getQuery() != null || uri.getFragment() != null
                            || uri.getHost() != null && !uri.getHost().equals("localhost")) throw new IllegalArgumentException();
                    endpoint = uri.getPath();
                } else if (endpoint.contains("://")) throw new IllegalArgumentException();
                Path base = options.workingDirectory() == null ? Path.of("").toAbsolutePath() : options.workingDirectory().toAbsolutePath();
                Path destination = base.resolve(endpoint).normalize();
                Files.createDirectories(destination.getParent());
                destination = destination.getParent().toRealPath().resolve(destination.getFileName());
                try { destination = destination.toRealPath(); }
                catch (NoSuchFileException absentOrRotated) { /* Reopened under ownership below. */ }
                this.path = destination;
                this.stream = null;
                this.gate = GATES.computeIfAbsent(destination, ignored -> new Object());
            }
            emit(record("INFO", "runtime.start", "TeaQL runtime started", Map.of()));
        } catch (IOException | RuntimeException unavailable) {
            failure(validatedName, "log_destination_unavailable");
            throw new IllegalStateException("Runtime logging initialization failed");
        }
    }

    private static Map<String, String> loggingEnvironment() {
        Map<String, String> result = new TreeMap<>();
        for (String setting : SETTINGS) {
            String value = System.getenv(setting);
            if (value != null) result.put(setting, value);
        }
        return result;
    }

    public static synchronized RuntimeLogger defaultLogger() {
        Map<String, String> env = loggingEnvironment();
        Path cwd = Path.of("").toAbsolutePath().normalize();
        String key = cwd + "\n" + env;
        return DEFAULTS.computeIfAbsent(key, ignored -> new RuntimeLogger(new Options(null, List.of(), env, cwd)));
    }

    public static RuntimeLogger forContext(UserContext context) {
        if (context != null) {
            RuntimeLogger logger = context.capability(RuntimeLogger.class);
            if (logger != null) return logger;
        }
        return defaultLogger();
    }

    public String logName() { return logName; }
    public String logFormat() { return logFormat; }
    public Path path() { return path; }
    public long maxSize() { return maxSize; }
    public int maxFiles() { return maxFiles; }

    private static void validateName(String name) throws IOException {
        if (name == null || name.isEmpty() || name.equals(".") || name.equals("..") || name.endsWith(" ") || name.endsWith(".")
                || DEVICE.matcher(name.split("\\.", -1)[0]).matches()
                || name.chars().anyMatch(c -> Character.isISOControl(c) || "<>:\"/\\|?*".indexOf(c) >= 0)) throw new IllegalArgumentException();
        StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).encode(CharBuffer.wrap(name));
    }

    public static long parseSize(String value) {
        var match = SIZE.matcher(value.trim().toUpperCase(Locale.ROOT));
        if (!match.matches()) throw new IllegalArgumentException("Invalid log size");
        long amount = Long.parseLong(match.group(1));
        String suffix = match.group(2);
        long multiplier = suffix == null ? 1 : switch (suffix) {
            case "K", "KB" -> 1024;
            case "M", "MB" -> 1024 * 1024;
            case "G", "GB" -> 1024 * 1024 * 1024;
            default -> throw new IllegalArgumentException("Invalid log size");
        };
        if (amount <= 0) throw new IllegalArgumentException("Invalid log size");
        return Math.multiplyExact(amount, multiplier);
    }

    private Map<String, Object> record(String level, String event, String message, Map<String, ?> fields) {
        Map<String, Object> record = new LinkedHashMap<>(fields);
        record.put("timestamp", Instant.now().toString()); record.put("level", level);
        record.put("log_name", logName); record.put("runtime", "java"); record.put("event", event); record.put("message", message);
        return record;
    }

    /** Accepts already-projected fields. Destination failures never escape this method. */
    public boolean write(String level, String event, String message, Map<String, ?> safeFields) {
        try {
            if (!Set.of("INFO", "WARN", "ERROR").contains(level)) throw new IllegalArgumentException();
            emit(record(level, event, message, safeFields));
            return true;
        } catch (Exception unavailable) { failure(logName, "log_write_failed"); return false; }
    }

    public static void failure(String name, String code) {
        try {
            System.err.println(JSON.writeValueAsString(Map.of("timestamp", Instant.now().toString(), "level", "ERROR", "log_name", name,
                    "runtime", "java", "event", "logging.failure", "message", "Runtime logging failed", "error_code", code)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException unavailable) {
            // Reporting a logging failure must remain safe even if JSON
            // serialization itself fails. Never disclose the original error.
            System.err.println("{\"timestamp\":\"" + Instant.now() + "\",\"level\":\"ERROR\",\"log_name\":\"teaql\","
                    + "\"runtime\":\"java\",\"event\":\"logging.failure\",\"message\":\"Runtime logging failed\","
                    + "\"error_code\":\"log_reporting_failed\"}");
        }
    }

    private SortedMap<Integer, Path> archives() throws IOException {
        SortedMap<Integer, Path> entries = new TreeMap<>(Comparator.reverseOrder());
        String prefix = path.getFileName() + ".";
        try (var files = Files.list(path.getParent())) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (!name.startsWith(prefix)) continue;
                String suffix = name.substring(prefix.length());
                if (!ARCHIVE.matcher(suffix).matches()) continue;
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException();
                entries.put(Integer.parseInt(suffix), file);
            }
        }
        return entries;
    }

    private void emit(Map<String, Object> record) throws IOException {
        String encoded = JSON.writeValueAsString(record);
        byte[] data = (logFormat.equals("human") ? humanRecord(JSON.readTree(encoded)) : encoded + "\n").getBytes(StandardCharsets.UTF_8);
        synchronized (gate) {
            if (stream != null) {
                var output = stream.equals("stderr") ? System.err : System.out;
                output.write(data); output.flush();
                if (output.checkError()) throw new IOException();
                return;
            }
            Path lockPath = Path.of(path + ".lock");
            FileChannel ownership;
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
            while (true) {
                try { ownership = FileChannel.open(lockPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE); break; }
                catch (FileAlreadyExistsException busy) {
                    if (System.nanoTime() >= deadline) throw new IOException();
                    try { Thread.sleep(5); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException(); }
                }
            }
            try (FileChannel acquired = ownership) {
                checkPresentation(); // Before pruning or rotating any existing file.
                var archives = archives();
                for (var archive : archives.entrySet()) if (archive.getKey() > maxFiles) Files.delete(archive.getValue());
                BasicFileAttributes active = null;
                try { active = Files.readAttributes(path, BasicFileAttributes.class); }
                catch (NoSuchFileException absent) { }
                if (active != null) {
                    if (!active.isRegularFile()) throw new IOException();
                    LocalDate day = active.lastModifiedTime().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                    if (active.size() > 0 && (data.length > maxSize - active.size() || !day.equals(LocalDate.now(ZoneOffset.UTC)))) {
                        for (var archive : archives.entrySet()) {
                            int number = archive.getKey();
                            if (number > maxFiles) continue;
                            if (number == maxFiles) Files.delete(archive.getValue());
                            else Files.move(archive.getValue(), Path.of(path + "." + (number + 1)));
                        }
                        if (maxFiles == 0) Files.delete(path); else Files.move(path, Path.of(path + ".1"));
                    }
                }
                // Always reopen the active pathname after another writer rotates.
                try (var output = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                    ByteBuffer bytes = ByteBuffer.wrap(data);
                    while (bytes.hasRemaining()) if (output.write(bytes) == 0) throw new IOException();
                }
            } finally { Files.delete(lockPath); }
        }
    }

    private static String humanText(String value, boolean escapeBackslash) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escapeBackslash && c == '\\') output.append("\\\\");
            else if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029')
                output.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else output.append(c);
        }
        return output.toString();
    }

    private static String humanRecord(JsonNode record) {
        StringBuilder output = new StringBuilder();
        output.append(humanText(record.get("timestamp").asText(), true)).append(" [TeaQL][")
                .append(humanText(record.get("log_name").asText(), true)).append("][")
                .append(record.get("level").asText()).append("] ")
                .append(humanText(record.get("event").asText(), true)).append(" runtime=")
                .append(humanText(record.get("runtime").asText(), true)).append("\n  ")
                .append(humanText(record.get("message").asText(), true)).append('\n');
        SortedSet<String> keys = new TreeSet<>();
        record.fieldNames().forEachRemaining(keys::add);
        keys.removeAll(EVENT_FIELDS);
        for (String key : keys) {
            JsonNode value = record.get(key);
            if (key.equals("sql") && value.isTextual()) {
                output.append("  sql:\n");
                for (String line : value.asText().split("\n", -1)) output.append("    ").append(humanText(line, true)).append('\n');
            } else output.append("  ").append(humanText(key, true)).append('=')
                    .append(humanText(value.toString(), false)).append('\n');
        }
        return output.toString();
    }

    private void checkPresentation() throws IOException {
        BasicFileAttributes active;
        try { active = Files.readAttributes(path, BasicFileAttributes.class); }
        catch (NoSuchFileException absent) { return; }
        if (!active.isRegularFile()) throw new IOException();
        if (active.size() == 0) return;
        String first;
        try (var input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { first = input.readLine(); }
        if (first != null && logFormat.equals("human") && HUMAN_HEADER.matcher(first).matches()) return;
        if (first != null && logFormat.equals("json")) {
            try {
                JsonNode record = JSON.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(first);
                boolean valid = record != null && record.isObject();
                for (String key : EVENT_FIELDS) valid &= record != null && record.has(key) && record.get(key).isTextual();
                if (valid && Set.of("INFO", "WARN", "ERROR").contains(record.get("level").asText())) return;
            } catch (IOException invalidRecord) { /* Reject before touching any archive. */ }
        }
        throw new IOException("incompatible_log_format");
    }

    public void writeSql(ExecutionMetadata metadata) {
        writeSql(metadata, false);
    }

    public void writeSql(ExecutionMetadata metadata, boolean sensitive) {
        try {
            var safe = LogPrivacy.sql(metadata, sensitive && LogPrivacy.plaintextEnabled());
            String outcome = Objects.requireNonNullElse(safe.getExecutionOutcome(), "unknown");
            String level = switch (outcome) { case "failure", "error", "cancelled" -> "ERROR"; case "retrying" -> "WARN"; default -> "INFO"; };
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("category", "SQL"); fields.put("outcome", outcome); fields.put("batch_outcome", safe.getBatchOutcome());
            fields.put("sql", safe.getDebugQuery()); fields.put("comment", safe.getComment()); fields.put("purpose", safe.getPurpose());
            fields.put("audit_reason", safe.getAuditReason()); fields.put("trace_chain", safe.getRequestTraceChain());
            fields.put("trace_path", safe.getTracePath()); fields.put("mutation_lineage", safe.getMutationLineage());
            fields.put("request_tree", safe.getRequestTree()); fields.put("elapsed_us", safe.getElapsedUs());
            fields.put("result_count", safe.getResultCount()); fields.put("affected_rows", safe.getAffectedRows());
            if (safe.getSqlExecutionCorrelation() != null) fields.put("trace_id", safe.getSqlExecutionCorrelation().traceId());
            write(level, "sql." + outcome, "TeaQL SQL execution", fields);
        } catch (Exception unavailable) { failure(logName, "log_projection_failed"); }
    }

    public void writeAudit(RawAuditEvent event) {
        try {
            var safe = LogPrivacy.audit(event, false);
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("category", "AUDIT"); fields.put("outcome", "success"); fields.put("entity", safe.entityType());
            fields.put("entity_id", safe.entityId()); fields.put("fields", safe.changes()); fields.put("trace_chain", safe.traceChain());
            fields.put("audit_reason", safe.reason());
            write("INFO", "audit.event", "TeaQL audit event", fields);
        } catch (Exception unavailable) { failure(logName, "log_projection_failed"); }
    }
}
