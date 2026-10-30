package io.teaql.web.integration.springmvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.UserContext;
import io.teaql.core.meta.SimpleEntityMetaFactory;
import io.teaql.core.reference.ReferenceDocumentScope;
import io.teaql.core.reference.TrustedReferencePrincipal;
import io.teaql.core.view.AuthorizedResponseGraph;
import io.teaql.core.view.DocumentContext;
import io.teaql.core.view.ViewAuthorization;
import io.teaql.core.view.ViewDocumentContributor;
import io.teaql.core.view.ViewOptions;
import io.teaql.core.view.ViewPreparationException;
import io.teaql.runtime.DefaultUserContext;
import io.teaql.runtime.RuntimeTelemetry;
import io.teaql.runtime.TeaQLRuntime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import static org.junit.Assert.*;

public class SpringMvcViewAdapterTest {
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private static final Clock CLOCK=Clock.fixed(NOW,ZoneOffset.UTC);
    private static final ViewAuthorization AUTHORIZATION=new ViewAuthorization() {
        @Override public Identity identity(UserContext context) {
            return new Identity("test",Objects.toString(context.getAttribute("subject"),"alice"),"tenant-a");
        }
        @Override public boolean canReadEntity(UserContext context,EntityTarget target,String path) { return true; }
        @Override public FieldDecision field(UserContext context,EntityTarget target,String field,String path,ViewOptions.Mode mode) {
            return FieldDecision.show();
        }
    };

    private static TeaQLRuntime.Builder builder() {
        return TeaQLRuntime.builder().metadata(new SimpleEntityMetaFactory())
                .viewAuthorization(AUTHORIZATION).viewClock(CLOCK)
                .telemetry(RuntimeTelemetry.NOOP).executionLogging(false);
    }

    @Test public void responseMatchesCliFrozenValueAndRechecksBeforeEncoding() throws Exception {
        try (var runtime=builder().build()) {
            var context=new DefaultUserContext(runtime);
            var options=new ViewOptions(ViewOptions.Mode.READ_ONLY,ViewOptions.BusinessCompletion.QUERY,
                    null,Duration.ofSeconds(60),false);
            var source=new LinkedHashMap<String,Object>(); source.put("title","before");
            var prepared=runtime.prepareView(context,source,options);
            source.put("title","after");
            var response=new SpringMvcViewAdapter(new ObjectMapper(),CLOCK).renderPrepared(context,prepared,options);
            assertEquals(200,response.getStatusCode().value());
            assertEquals(MediaType.APPLICATION_JSON,response.getHeaders().getContentType());
            assertEquals("private, max-age=60",response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
            assertEquals("before",new ObjectMapper().readTree(response.getBody()).path("title").asText());
            assertEquals(prepared.value(),new ObjectMapper().readValue(response.getBody(),Map.class));
            context.putAttribute("subject","bob");
            var denied=assertThrows(ViewPreparationException.class,() ->
                    new SpringMvcViewAdapter(new ObjectMapper(),CLOCK).renderPrepared(context,prepared,options));
            assertEquals(ViewPreparationException.Code.VIEW_PREPARED_MISMATCH,denied.code());
        }
    }

    @Test public void governedCredentialsAreEncodedOnceAndRevocationFailsClosed() throws Exception {
        var issued=new AtomicInteger();
        var principal=new TrustedReferencePrincipal("test","alice","Tenant",1);
        var scope=new ReferenceDocumentScope("doc1","edit","ViewAsset",42,1);
        ViewDocumentContributor documents=new ViewDocumentContributor() {
            @Override public Preparation prepare(UserContext context,AuthorizedResponseGraph graph,ViewOptions options) {
                issued.incrementAndGet();
                return new Preparation(graph,new DocumentContext(principal,scope,NOW.plusSeconds(30)),
                        Map.of("documentToken","server-issued"));
            }
            @Override public void validateCurrent(UserContext context,AuthorizedResponseGraph graph,
                                                   DocumentContext document,ViewOptions options) {
                if (Boolean.FALSE.equals(context.getAttribute("documentAllowed"))) throw new ViewPreparationException(
                        ViewPreparationException.Code.VIEW_ACCESS_DENIED,options.businessCompletion());
            }
        };
        try (var runtime=builder().viewDocuments(documents).build()) {
            var context=new DefaultUserContext(runtime); context.withTrustedReferencePrincipal(principal);
            var options=new ViewOptions(ViewOptions.Mode.GOVERNED,ViewOptions.BusinessCompletion.QUERY,
                    Duration.ofSeconds(30),Duration.ofSeconds(30),false);
            var adapter=new SpringMvcViewAdapter(new ObjectMapper(),CLOCK);
            var prepared=runtime.prepareView(context,Map.of("title","safe"),options);
            var response=adapter.renderPrepared(context,prepared,options);
            var json=new ObjectMapper().readTree(response.getBody());
            assertEquals("safe",json.path("data").path("title").asText());
            assertEquals("server-issued",json.path("documentCredentials").path("documentToken").asText());
            assertEquals(1,issued.get());
            context.putAttribute("documentAllowed",false);
            var denied=assertThrows(ViewPreparationException.class,() ->
                    adapter.renderPrepared(context,prepared,options));
            assertEquals(ViewPreparationException.Code.VIEW_ACCESS_DENIED,denied.code());
            assertEquals(1,issued.get());
        }
    }

    @Test public void serializationFailureNeverExposesMapperDetailsOrLosesCommittedOutcome() {
        ObjectMapper broken=new ObjectMapper() {
            @Override public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("CANARY_SECRET") {};
            }
        };
        try (var runtime=builder().build()) {
            var context=new DefaultUserContext(runtime);
            var failed=assertThrows(ViewPreparationException.class,() ->
                    new SpringMvcViewAdapter(broken,CLOCK).render(context,Map.of("title","safe"),ViewOptions.afterCommit()));
            assertEquals(ViewPreparationException.Code.VIEW_PREPARATION_FAILED,failed.code());
            assertTrue(failed.businessCommitted());
            assertFalse(failed.toString().contains("CANARY"));
        }
    }
}
