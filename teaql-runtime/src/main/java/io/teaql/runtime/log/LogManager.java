package io.teaql.runtime.log;

import io.teaql.runtime.config.TeaQLEnv;
import io.teaql.core.log.TraceNode;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
import java.io.FileInputStream;

public class LogManager {

    private static final LogManager INSTANCE = new LogManager();
    private static final String EXTREME_TEST_FLAG = "__i_agree_to_disable_runtime_trace_only_for_extreme_performance_testing";

    private final String endpoint;
    private final long maxSize;
    private final int maxFiles;

    private final BlockingQueue<Runnable> queue;
    private final Thread workerThread;

    private FileChannel currentChannel;
    private final AtomicLong currentSize = new AtomicLong(0);
    private final Object fileLock = new Object();
    private long nextMidnightMillis;
    private volatile boolean headerWritten = false;

    private String determineEndpoint() {
        String mode = TeaQLEnv.get("TEAQL_TRACE_MODE");
        if ("off".equals(mode)) {
            String ack = TeaQLEnv.get("TEAQL_TRACE_OFF_ACK");
            if (EXTREME_TEST_FLAG.equals(ack)) {
                return "off";
            }
        }
        String val = TeaQLEnv.get("TEAQL_LOG_ENDPOINT");
        if (val != null && !val.trim().isEmpty()) {
            return val;
        }
        String domain = TeaQLEnv.get("TEAQL_DOMAIN");
        if (domain != null && !domain.trim().isEmpty()) {
            return domain.trim() + ".log";
        }
        String command = System.getProperty("sun.java.command");
        String exeName = "teaql";
        if (command != null && !command.trim().isEmpty()) {
            exeName = command.split(" ")[0];
            int lastDot = exeName.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < exeName.length() - 1) {
                exeName = exeName.substring(lastDot + 1);
            }
        }
        return exeName + ".log";
    }

    private LogManager() {
        this.endpoint = determineEndpoint();
        this.maxSize = TeaQLEnv.getSizeInBytes("TEAQL_LOG_MAX_SIZE", 50 * 1024 * 1024L); // default 50MB
        this.maxFiles = TeaQLEnv.getInt("TEAQL_LOG_MAX_FILES", 7);
        
        this.queue = new ArrayBlockingQueue<>(10000);

        if (!"off".equals(this.endpoint) && !"stdout".equals(this.endpoint)) {
            initFileChannel();
        }

        this.workerThread = new Thread(this::processQueue, "TeaQL-LogWriter-Thread");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    public static LogManager getInstance() {
        return INSTANCE;
    }

    private void calculateNextMidnight() {
        LocalDateTime tomorrowMidnight = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        this.nextMidnightMillis = tomorrowMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void initFileChannel() {
        synchronized (fileLock) {
            try {
                Path path = Paths.get(this.endpoint);
                if (path.getParent() != null) {
                    Files.createDirectories(path.getParent());
                }
                FileOutputStream fos = new FileOutputStream(path.toFile(), true);
                this.currentChannel = fos.getChannel();
                this.currentSize.set(this.currentChannel.size());
                calculateNextMidnight();
            } catch (Exception e) {
                System.err.println("TeaQL LogManager Failed to initialize file channel: " + e.getMessage());
            }
        }
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Runnable task = queue.take();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("TeaQL LogManager Exception in worker thread: " + e.getMessage());
            }
        }
    }

    private void writeHeaderIfNeeded() {
        if (headerWritten || "off".equals(endpoint)) return;
        synchronized (fileLock) {
            if (headerWritten) return;
            try {
                java.net.URL url = getClass().getResource("/log_header.txt");
                String header = "";
                if (url != null) {
                    try (java.io.InputStream is = url.openStream();
                         java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
                        header = s.hasNext() ? s.next() : "";
                    }
                } else {
                    header = "================================================================================\n" +
                             "🚀 TEAQL Holographic Trace Log\n" +
                             "================================================================================";
                }
                byte[] bytes = (header + "\n").getBytes(StandardCharsets.UTF_8);
                if ("stdout".equals(endpoint)) {
                    System.out.print(new String(bytes, StandardCharsets.UTF_8));
                } else if (currentChannel != null) {
                    currentChannel.write(ByteBuffer.wrap(bytes));
                    currentSize.addAndGet(bytes.length);
                }
            } catch (Exception e) {
                System.err.println("TeaQL LogManager Failed to write header: " + e.getMessage());
            } finally {
                headerWritten = true;
            }
        }
    }

    private void asyncWrite(String content, io.teaql.core.log.CustomLogSink customSink) {
        if (!queue.offer(() -> syncWrite(content, customSink))) {
            // Queue is full, drop or print to standard error to prevent blocking main business logic
            System.err.println("TeaQL LogManager queue full, dropped log.");
        }
    }

    private void syncWrite(String content, io.teaql.core.log.CustomLogSink customSink) {
        if (content == null || content.isEmpty()) return;
        if (customSink != null) {
            customSink.onLog(content);
        }
        
        if ("off".equals(endpoint)) return;
        writeHeaderIfNeeded();

        byte[] bytes = (content + "\n").getBytes(StandardCharsets.UTF_8);

        if ("stdout".equals(endpoint)) {
            System.out.print(new String(bytes, StandardCharsets.UTF_8));
            return;
        }

        synchronized (fileLock) {
            if (currentChannel == null) return;
            
            try {
                boolean timeToRotate = System.currentTimeMillis() >= nextMidnightMillis;
                boolean sizeToRotate = currentSize.get() + bytes.length > maxSize;
                
                if (timeToRotate || sizeToRotate) {
                    rotateLogFile();
                    if (timeToRotate) {
                        calculateNextMidnight();
                    }
                }
                
                currentChannel.write(ByteBuffer.wrap(bytes));
                currentSize.addAndGet(bytes.length);
            } catch (Exception e) {
                System.err.println("TeaQL LogManager Failed to write to log file: " + e.getMessage());
            }
        }
    }

    private void rotateLogFile() {
        try {
            if (currentChannel != null) {
                currentChannel.close();
            }

            File currentFile = new File(endpoint);
            File backupFile = null;
            if (currentFile.exists()) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                backupFile = new File(endpoint + "." + timestamp);
                Files.move(currentFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            initFileChannel();
            cleanupOldFiles();
            
            if (backupFile != null && backupFile.exists()) {
                compressAsync(backupFile);
            }

        } catch (Exception e) {
            System.err.println("TeaQL LogManager Failed to rotate log file: " + e.getMessage());
        }
    }

    private void compressAsync(File source) {
        CompletableFuture.runAsync(() -> {
            File target = new File(source.getAbsolutePath() + ".gz");
            try (FileInputStream fis = new FileInputStream(source);
                 FileOutputStream fos = new FileOutputStream(target);
                 GZIPOutputStream gos = new GZIPOutputStream(fos)) {
                
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    gos.write(buffer, 0, len);
                }
                gos.finish();
                source.delete();
            } catch (Exception e) {
                System.err.println("TeaQL LogManager Failed to compress log file: " + e.getMessage());
            }
        });
    }

    private void cleanupOldFiles() {
        File currentFile = new File(endpoint);
        File parentDir = currentFile.getParentFile();
        if (parentDir == null) {
            parentDir = new File(".");
        }

        final String baseName = currentFile.getName();
        File[] files = parentDir.listFiles((dir, name) -> name.startsWith(baseName + "."));
        
        if (files != null && files.length > maxFiles) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            int filesToDelete = files.length - maxFiles;
            for (int i = 0; i < filesToDelete; i++) {
                if (!files[i].delete()) {
                    System.err.println("TeaQL LogManager Failed to delete old log file: " + files[i].getName());
                }
            }
        }
    }

    public void writeExecutionLog(io.teaql.core.UserContext ctx, io.teaql.core.ExecutionMetadata metadata) {
        if (!LogConfig.getInstance().shouldLogSql(metadata.getDebugQuery())) {
            return;
        }
        String content = LogFormatterFactory.getFormatter().formatExecutionLog(metadata);
        io.teaql.core.log.CustomLogSink customSink = ctx != null ? ctx.getCustomSink() : null;
        asyncWrite(content, customSink);
    }

    public void writeAuditLog(io.teaql.core.UserContext ctx, List<TraceNode> traceChain, AuditEvent event) {
        if (!LogConfig.getInstance().shouldLogAudit(event.getEntityType())) {
            return;
        }
        String content = LogFormatterFactory.getFormatter().formatAuditLog(traceChain, event);
        io.teaql.core.log.CustomLogSink customSink = ctx != null ? ctx.getCustomSink() : null;
        asyncWrite(content, customSink);
    }
}
