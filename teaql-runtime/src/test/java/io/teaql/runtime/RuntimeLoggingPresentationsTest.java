package io.teaql.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.teaql.core.DataServiceOperation;
import io.teaql.core.ExecutionMetadata;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class RuntimeLoggingPresentationsTest {
    @Parameterized.Parameters(name = "{0}") public static Object[] formats() { return new Object[]{"json", "human"}; }
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern HEADER = Pattern.compile("^(\\S+) \\[TeaQL\\]\\[(.*)\\]\\[(INFO|WARN|ERROR)\\] (.*) runtime=(\\S+)$");
    private final String format;
    public RuntimeLoggingPresentationsTest(String format) { this.format = format; }
    private RuntimeLogger logger(Path base, Map<String, String> environment) {
        return new RuntimeLogger(new RuntimeLogger.Options(null, List.of(), environment, base, format));
    }
    private static String scalar(String value) throws Exception { return JSON.readValue("\"" + value.replace("\"", "\\\"") + "\"", String.class); }
    private static List<JsonNode> parse(String text, String format) throws Exception {
        assertTrue("complete rendered events", text.endsWith("\n"));
        List<JsonNode> rows = new ArrayList<>(); List<String> sql = null;
        for (String line : text.substring(0, text.length() - 1).split("\n", -1)) {
            if (format.equals("json")) { rows.add(JSON.readTree(line)); continue; }
            assertFalse(line.chars().anyMatch(c -> Character.isISOControl(c) || c == 0x2028 || c == 0x2029));
            var header = HEADER.matcher(line);
            if (header.matches()) {
                assertTrue(header.group(1).endsWith("Z")); Instant.parse(header.group(1));
                ObjectNode row = JSON.createObjectNode().put("timestamp", header.group(1)).put("log_name", scalar(header.group(2)))
                    .put("level", header.group(3)).put("event", scalar(header.group(4))).put("runtime", header.group(5));
                rows.add(row); sql = null;
            } else {
                assertTrue("unindented/forged event continuation", !rows.isEmpty() && line.startsWith("  "));
                ObjectNode row = (ObjectNode) rows.get(rows.size() - 1);
                if (!row.has("message")) row.put("message", scalar(line.substring(2)));
                else if (line.equals("  sql:")) { sql = new ArrayList<>(); row.put("sql", ""); }
                else if (line.startsWith("    ") && sql != null) { sql.add(scalar(line.substring(4))); row.put("sql", String.join("\n", sql)); }
                else { String[] detail = line.substring(2).split("=", 2); assertEquals(2, detail.length);
                    row.set(scalar(detail[0]), JSON.readTree(detail[1])); sql = null; }
            }
        }
        for (JsonNode row : rows) for (String key : List.of("timestamp", "level", "log_name", "runtime", "event", "message")) assertTrue(row.get(key).isTextual());
        return rows;
    }
    private List<JsonNode> read(Path... paths) throws Exception {
        List<JsonNode> rows = new ArrayList<>(); for (Path path : paths) rows.addAll(parse(Files.readString(path), format)); return rows;
    }
    private Path base() { return temporary.getRoot().toPath(); }

    @Test public void selectionDefaultNamesOverrideTwoStartsAndFrozenContexts() throws Exception {
        var defaultLog = new RuntimeLogger(new RuntimeLogger.Options(null, List.of(), Map.of(), temporary.newFolder("default").toPath()));
        assertEquals("json", defaultLog.logFormat());
        int index = 0;
        for (Object[] entry : List.of(new Object[]{"application","domain",List.of("model"),"application"},new Object[]{null,"domain",List.of("model"),"domain"},
            new Object[]{null,"",List.of("model"),"model"},new Object[]{null,"",List.of(),"teaql"},new Object[]{null,"",List.of("one","two"),"teaql"})) {
            Path directory = temporary.newFolder("app" + index++).toPath();
            Map<String,String> env = new HashMap<>(Map.of("TEAQL_DOMAIN",(String)entry[1],"TEAQL_LOG_FORMAT",format));
            @SuppressWarnings("unchecked") var models = (List<String>)entry[2];
            var log = new RuntimeLogger(new RuntimeLogger.Options((String)entry[0],models,env,directory));
            assertEquals(format,log.logFormat());assertEquals(entry[3],log.logName());assertEquals(directory.resolve("logs/"+entry[3]+".log"),log.path());
            env.put("TEAQL_LOG_FORMAT","invalid-unused");env.put("TEAQL_DOMAIN","changed");
            var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).build();
            var context = new DefaultUserContext(runtime);assertSame(log,RuntimeLogger.forContext(context));
            RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(),new RuntimeTelemetry.Operation("query","probe",Map.of())).success();
            new RuntimeLogger(new RuntimeLogger.Options(log.logName(),List.of(),Map.of("TEAQL_LOG_FORMAT","invalid-unused"),directory,format));
            assertEquals(3,read(log.path()).size());
            var override = logger(directory,Map.of("TEAQL_LOG_ENDPOINT","override/custom.log","TEAQL_LOG_FORMAT","invalid-unused"));
            assertEquals(directory.resolve("override/custom.log"),override.path());assertEquals(format,override.logFormat());
        }
        var built = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
            .loggingOptions(new RuntimeLogger.Options(null,List.of(),Map.of(),temporary.newFolder("builder").toPath()))
            .logFormat(format).logName("built").knownModelNames(List.of("model")).build();
        assertEquals(format,built.getLogger().logFormat());
    }

    @Test public void invalidValuesFailWithoutEcho() throws Exception {
        var saved=System.err;var bytes=new ByteArrayOutputStream();
        try (var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {
            System.setErr(output);
            for(String value:List.of("","debug","JSON","human\nFORMAT-CANARY")) for(boolean app:List.of(false,true)) {
                try { new RuntimeLogger(new RuntimeLogger.Options(null,List.of(),app?Map.of():Map.of("TEAQL_LOG_FORMAT",value),base(),app?value:null));fail("invalid format accepted"); }
                catch(IllegalStateException expected) {assertNull(expected.getCause());}
            }
        } finally {System.setErr(saved);}
        String text=bytes.toString(StandardCharsets.UTF_8);assertFalse(text.contains("CANARY"));
        for(JsonNode row:parse(text,"json")) assertEquals("ERROR",row.get("level").asText());
        assertEquals(0,Objects.requireNonNull(temporary.getRoot().list()).length);
    }

    @Test public void streamsKeepTypedEventUnknownKindsAndEscapedSql() throws Exception {
        for(String endpoint:List.of("stdout","stderr")) {
            var saved=endpoint.equals("stdout")?System.out:System.err;var bytes=new ByteArrayOutputStream();
            var fields=new LinkedHashMap<String,Object>();fields.put("category","SQL");fields.put("trace_id","0123456789abcdef0123456789abcdef");
            fields.put("outcome","retrying");fields.put("error_code","connection_timeout");fields.put("entity_id",9007199254740993L);
            fields.put("enabled",true);fields.put("result_count",0);fields.put("omission_reason",null);fields.put("changes",List.of(Map.of("field","password","value","***")));
            fields.put("sql","SELECT id\nFROM customer\n\033\r\u0085\u2028");String message="Readable \\u001b\nFORGED\033\r\u0085\u2029";
            try(var output=new PrintStream(bytes,true,StandardCharsets.UTF_8)) {if(endpoint.equals("stdout"))System.setOut(output);else System.setErr(output);
                var log=logger(base(),Map.of("TEAQL_LOG_ENDPOINT",endpoint));assertTrue(log.write("WARN","unknown.kind",message,fields));
            } finally {if(endpoint.equals("stdout"))System.setOut(saved);else System.setErr(saved);}
            var rows=parse(bytes.toString(StandardCharsets.UTF_8),format);assertEquals(2,rows.size());var got=(ObjectNode)rows.get(1);got.remove("timestamp");
            fields.putAll(Map.of("level","WARN","runtime","java","log_name","teaql","event","unknown.kind","message",message));
            assertEquals(JSON.valueToTree(fields),got);
        }
    }

    private static void privacyEvents(RuntimeLogger log) {
        var metadata=new ExecutionMetadata();metadata.setOperation(DataServiceOperation.QUERY);metadata.setExecutionOutcome("retrying");
        metadata.setParameterizedQuery("SELECT id FROM customer WHERE password = ?");metadata.setParameters(List.of("PASSWORD-CANARY"));
        metadata.setDebugQuery("PASSWORD-CANARY");metadata.setComment("read PASSWORD-CANARY");
        metadata.setSqlExecutionCorrelation(new io.teaql.core.SqlExecutionCorrelation("0123456789abcdef0123456789abcdef","0123456789abcdef"));
        log.writeSql(metadata);log.writeSql(metadata,true);assertEquals(List.of("PASSWORD-CANARY"),metadata.getParameters());
        var event=new RawAuditEvent(MutationAuditKind.UPDATED,"Customer",9007199254740993L,List.of(new AuditFieldChange("password","OLD-CANARY","PASSWORD-CANARY")),List.of(),null,null,null,null,null);
        log.writeAudit(event);assertEquals("PASSWORD-CANARY",event.changes().get(0).newValue());
    }
    @Test public void nativeLevelsProjectionIdentityAndExactAck() throws Exception {
        var log=logger(base(),Map.of());var runtime=TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).build();
        for(Throwable error:Arrays.asList(null,new io.teaql.core.checker.CheckException("CHECK-CANARY"),new java.io.IOException("DRIVER-CANARY"))) {
            var scope=RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(),new RuntimeTelemetry.Operation("query","probe",Map.of()));if(error==null)scope.success();else scope.failure(error);
        }
        assertEquals(List.of("INFO","INFO","WARN","ERROR"),read(log.path()).stream().map(r->r.get("level").asText()).toList());
        assertFalse(Files.readString(log.path()).contains("CANARY"));
        for(String ack:List.of("","true",LogPrivacy.ACKNOWLEDGEMENT)) {
            Path directory=temporary.newFolder().toPath();var command=writer(directory,"privacy",format,"privacy");command.environment().put(LogPrivacy.ENVIRONMENT,ack);
            Process child=command.start();try {assertTrue(child.waitFor(60,TimeUnit.SECONDS));assertEquals(0,child.exitValue());}finally {if(child.isAlive())child.destroyForcibly();child.waitFor();}
            var rows=read(directory.resolve("logs/teaql.log"));assertEquals(4,rows.size());assertEquals("WARN",rows.get(1).get("level").asText());
            assertEquals("0123456789abcdef0123456789abcdef",rows.get(1).get("trace_id").asText());
            assertTrue(rows.get(3).get("entity_id").isIntegralNumber());assertEquals(9007199254740993L,rows.get(3).get("entity_id").asLong());
            assertFalse(Files.readString(directory.resolve("logs/teaql.log")).contains("CANARY"));
        }
    }

    @Test public void positiveRetentionKeepsNumericNewestOrderAndWholeBlocks() throws Exception {
        var log=logger(base(),Map.of("TEAQL_LOG_MAX_SIZE","256","TEAQL_LOG_MAX_FILES","2"));
        for(int i=0;i<5;i++)assertTrue(log.write("INFO","retention","界".repeat(200),Map.of("sequence",i,"sql","SELECT\n"+i)));
        for(int i=0;i<3;i++){var rows=read(Path.of(log.path()+(i==0?"":"."+i)));assertEquals(1,rows.size());assertEquals(4-i,rows.get(0).get("sequence").asInt());assertEquals("SELECT\n"+(4-i),rows.get(0).get("sql").asText());}
        try(var paths=Files.list(log.path().getParent())){assertEquals(3,paths.count());}
        assertFalse(Files.exists(Path.of(log.path()+".3")));
    }

    @Test public void utf8WholeBlocksSizeDayRestartOversizeRetentionAndOverride() throws Exception {
        for(boolean override:List.of(false,true)) {
            Path directory=temporary.newFolder().toPath();var env=new HashMap<>(Map.of("TEAQL_LOG_MAX_SIZE","1K","TEAQL_LOG_MAX_FILES","100"));if(override)env.put("TEAQL_LOG_ENDPOINT","override/custom.log");
            var log=logger(directory,env);for(int index=0;index<40;index++)assertTrue(log.write("INFO","regular","界".repeat(25),Map.of("index",index,"sql","SELECT id\nFROM customer")));
            Path[] files;try(var paths=Files.list(log.path().getParent())){files=paths.toArray(Path[]::new);}for(Path file:files)assertTrue(Files.size(file)>0 && Files.size(file)<=1024);
            var rows=read(files);assertEquals(41,rows.size());assertEquals(40,rows.stream().filter(r->r.has("index")).map(r->r.get("index").asInt()).distinct().count());
            for(var row:rows)if(row.has("index"))assertEquals("SELECT id\nFROM customer",row.get("sql").asText());
            assertTrue(log.write("WARN","oversize","whole",Map.of("sql","界".repeat(2000)+"\nLAST")));byte[] before=Files.readAllBytes(log.path());assertTrue(before.length>1024);assertEquals(1,read(log.path()).size());
            assertTrue(log.write("INFO","after","next",Map.of()));assertArrayEquals(before,Files.readAllBytes(Path.of(log.path()+".1")));
            for(boolean restart:List.of(false,true)){Files.setLastModifiedTime(log.path(),FileTime.from(Instant.now().minusSeconds(172800)));before=Files.readAllBytes(log.path());
                if(restart)logger(directory,env);else assertTrue(log.write("INFO","day","UTC rotation",Map.of()));assertArrayEquals(before,Files.readAllBytes(Path.of(log.path()+".1")));assertEquals(1,read(log.path()).size());}
            env.put("TEAQL_LOG_MAX_FILES","0");var zero=logger(directory,env);assertTrue(zero.write("INFO","last","界".repeat(2000),Map.of()));
            try(var paths=Files.list(log.path().getParent())){assertEquals(1,paths.count());}assertEquals(1,read(log.path()).size());
        }
    }

    private void assertBlocks(Path directory,int count,int starts) throws Exception {
        List<JsonNode> rows;try(var paths=Files.list(directory.resolve("logs"))){rows=read(paths.toArray(Path[]::new));}Set<String> ids=new HashSet<>();
        for(var row:rows)if(row.has("id")){String id=row.get("id").asText();assertTrue(ids.add(id));assertEquals("SELECT\n"+id,row.get("sql").asText());}assertEquals(count,ids.size());assertEquals(count+starts,rows.size());
    }
    @Test public void threadsAppendWholeMultilineEvents() throws Exception {
        var pool=Executors.newFixedThreadPool(4);List<Future<?>> children=new ArrayList<>();
        try {for(int worker=0;worker<4;worker++){int id=worker;children.add(pool.submit(()->{var log=logger(base(),Map.of("TEAQL_LOG_MAX_SIZE","2K","TEAQL_LOG_MAX_FILES","100"));
            for(int i=0;i<30;i++){String key=id+"/"+i;assertTrue(log.write("INFO","parallel","多线程",Map.of("id",key,"sql","SELECT\n"+key)));}}));}for(var child:children)child.get(60,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}assertBlocks(base(),120,4);
    }
    public static class Writer {
        public static void main(String[] args) {var log=new RuntimeLogger(new RuntimeLogger.Options(null,List.of(),Map.of("TEAQL_LOG_MAX_SIZE","2K","TEAQL_LOG_MAX_FILES","100"),Path.of(args[0]),args[2]));
            if(args[3].equals("privacy")){privacyEvents(log);return;}for(int i=0;i<30;i++){String id=args[1]+"/"+i;if(!log.write("INFO","parallel","原生并发",Map.of("id",id,"sql","SELECT\n"+id)))System.exit(1);}}
    }
    private static ProcessBuilder writer(Path directory,String worker,String format,String kind) {
        String classPath=System.getProperty("java.class.path")+java.io.File.pathSeparator+System.getProperty("jdk.module.path","");
        var command=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",classPath,Writer.class.getName(),directory.toString(),worker,format,kind).inheritIO();command.environment().clear();return command;
    }
    @Test public void independentProcessesAndFollowingStartKeepWholeBlocks() throws Exception {
        List<Process> children=new ArrayList<>();try{for(int worker=0;worker<4;worker++)children.add(writer(base(),Integer.toString(worker),format,"write").start());
            for(Process child:children){assertTrue(child.waitFor(60,TimeUnit.SECONDS));assertEquals(0,child.exitValue());}
            var following=writer(base(),"following",format,"write").start();children.add(following);assertTrue(following.waitFor(60,TimeUnit.SECONDS));assertEquals(0,following.exitValue());
        }finally{for(Process child:children){if(child.isAlive())child.destroyForcibly();child.waitFor();}}assertBlocks(base(),150,5);
    }
    @Test public void mixedFormatRejectedBeforeMutationAndIoFailuresPreserveBusiness() throws Exception {
        var log=logger(base(),Map.of());String other=format.equals("json")?"human":"json";Path archive=Path.of(log.path()+".10");Files.writeString(archive,"preserve archive bytes");byte[] before=Files.readAllBytes(log.path());
        try{new RuntimeLogger(new RuntimeLogger.Options(null,List.of(),Map.of("TEAQL_LOG_MAX_SIZE","1","TEAQL_LOG_MAX_FILES","0"),base(),other));fail("mixed startup accepted");}catch(IllegalStateException expected){}
        assertArrayEquals(before,Files.readAllBytes(log.path()));assertEquals("preserve archive bytes",Files.readString(archive));
        var replacement=new RuntimeLogger(new RuntimeLogger.Options(null,List.of(),Map.of(),temporary.newFolder("replacement").toPath(),other));before=Files.readAllBytes(replacement.path());Files.write(log.path(),before);
        var runtime=TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory()).runtimeLogger(log).build();var context=new DefaultUserContext(runtime);context.putToLocalCache("probe",42,60);assertEquals(Integer.valueOf(42),context.getFromLocalCache("probe",Integer.class));
        var original=new java.io.IOException("SECRET-ERROR");RuntimeTelemetry.startSafely(runtime.getOperationTelemetry(),new RuntimeTelemetry.Operation("query","probe",Map.of())).failure(original);assertEquals("SECRET-ERROR",original.getMessage());
        assertFalse(log.write("INFO","later","business completion",Map.of()));assertArrayEquals(before,Files.readAllBytes(log.path()));assertEquals("preserve archive bytes",Files.readString(archive));
        Files.delete(log.path());Files.createDirectory(log.path());assertFalse(log.write("ERROR","later","I/O failure",Map.of()));try{logger(base(),Map.of());fail("startup I/O failure accepted");}catch(IllegalStateException expected){}
    }
}
