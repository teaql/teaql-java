package io.teaql.runtime;

import io.teaql.core.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.Test;
import static org.junit.Assert.*;

/** #202: query provenance belongs to its request, including while execution overlaps. */
public class QueryTraceIsolationTest {
    private static class Request extends BaseRequest<TeaQLRuntimeTest.DummyEntity> {
        Request(String comment, String purpose) {
            super(TeaQLRuntimeTest.DummyEntity.class);
            changeIntent(comment, purpose);
        }
        void changeIntent(String comment, String purpose) { internalComment(comment); internalPurpose(purpose); }
        @Override public String getTypeName() { return "Dummy"; }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue("query must reach the controlled checkpoint", latch.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    @Test public void overlappingListAndAggregateNeverWriteTheContextTraceStack() throws Exception {
        var firstEntered = new CountDownLatch(1);
        var bothEntered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var captured = new CopyOnWriteArrayList<QueryRequest>();
        var provider = new TeaQLRuntimeTest.DummyQueryExecutor() {
            @Override public QueryResult query(UserContext caller, QueryRequest request) {
                captured.add(request);
                firstEntered.countDown(); bothEntered.countDown(); await(release);
                return new DefaultQueryResult(new SmartList<>(), new AggregationResult());
            }
        };
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider).build();
        var context = new DefaultUserContext(runtime);
        context.pushTrace("unrelated application diagnostic");
        var baseline = context.getTraceChain();
        var workers = Executors.newFixedThreadPool(2);
        try {
            var first = workers.submit(() -> runtime.executeForList(context, new Request("read first", "render first")));
            await(firstEntered);
            var second = workers.submit(() -> runtime.aggregation(context, new Request("count second", "render second")));
            await(bothEntered);
            assertEquals("two live queries must not append operation, intent or relation frames to Context",
                    baseline, context.getTraceChain());
            assertEquals(List.of("read first", "count second"), captured.stream().map(QueryRequest::comment).toList());
            assertEquals(List.of("render first", "render second"), captured.stream().map(QueryRequest::purpose).toList());
            release.countDown();
            first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
            assertEquals(baseline, context.getTraceChain());
        } finally {
            release.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test public void reentrantInternalQueryUsesExplicitIntentWithoutAmbientFrames() {
        var captured = new CopyOnWriteArrayList<QueryRequest>();
        var provider = new TeaQLRuntimeTest.DummyQueryExecutor() {
            @Override public QueryResult query(UserContext caller, QueryRequest request) {
                assertTrue("root and nested execution must leave Context untouched", caller.getTraceChain().isEmpty());
                captured.add(request);
                if (captured.size() == 1) {
                    var child = new Request(null, null) {
                        @Override public QueryIntent inheritedQueryIntent() { return request.intent(); }
                    };
                    caller.internalExecuteForList(child);
                }
                return super.query(caller, request);
            }
        };
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider).build();
        var context = new DefaultUserContext(runtime);
        runtime.executeForList(context, new Request("load graph", "render graph"));
        assertEquals(2, captured.size());
        assertSame(captured.get(0).intent(), captured.get(1).intent());
        assertTrue(context.getTraceChain().isEmpty());
    }

    @Test public void failedProviderCannotAddOrPopAnApplicationDiagnostic() {
        var contextRef = new AtomicReference<DefaultUserContext>();
        var baseline = new AtomicReference<List<TraceNode>>();
        var failure = new TeaQLRuntimeException("intentional query failure");
        var provider = new TeaQLRuntimeTest.DummyQueryExecutor() {
            @Override public QueryResult query(UserContext caller, QueryRequest request) {
                assertSame(contextRef.get(), caller);
                assertEquals(baseline.get(), caller.getTraceChain());
                throw failure;
            }
        };
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider).build();
        var context = new DefaultUserContext(runtime); contextRef.set(context);
        context.pushTrace("application diagnostic"); baseline.set(context.getTraceChain());
        assertSame(failure, assertThrows(TeaQLRuntimeException.class,
                () -> runtime.executeForList(context, new Request("fail read", "verify failure cleanup"))));
        assertEquals(baseline.get(), context.getTraceChain());
    }

    private static final class StreamProvider implements StreamingQueryExecutor {
        String comment;
        String purpose;
        @Override public String name() { return "dummy"; }
        @Override public DataServiceCapabilities capabilities() { return new DataServiceCapabilities(); }
        @Override public <T extends Entity> Stream<T> queryForStream(UserContext context, QueryRequest request) {
            comment = request.comment(); purpose = request.purpose();
            assertTrue(context.getTraceChain().isEmpty());
            return Stream.empty();
        }
    }

    @Test public void streamingProviderReceivesTheIntentCapturedBeforePolicy() {
        var provider = new StreamProvider();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider).queryPolicy(new QueryPolicy() {
                    @Override public void enforceSelect(UserContext context, SearchRequest<?> request) {
                        ((Request) request).changeIntent("later builder comment", "later builder purpose");
                    }
                }).build();
        try (var stream = runtime.executeForStream(new DefaultUserContext(runtime),
                new Request("original stream comment", "original stream purpose"))) {
            assertEquals(0, stream.count());
        }
        assertEquals("original stream comment", provider.comment);
        assertEquals("original stream purpose", provider.purpose);
    }

    @Test public void internalStreamDoesNotRequireRepeatedRootIntentOnTheChildBuilder() {
        var provider = new StreamProvider();
        var runtime = TeaQLRuntime.builder().metadata(new TeaQLRuntimeTest.DummyMetaFactory())
                .dataService("dummy", provider).build();
        var intent = QueryIntent.of("root stream comment", "root stream purpose");
        var request = new Request(null, null) {
            @Override public QueryIntent inheritedQueryIntent() { return intent; }
        };
        try (var stream = runtime.internalExecuteForStream(new DefaultUserContext(runtime), request)) {
            assertEquals(0, stream.count());
        }
        assertEquals(intent.comment(), provider.comment);
        assertEquals(intent.purpose(), provider.purpose);
    }
}
