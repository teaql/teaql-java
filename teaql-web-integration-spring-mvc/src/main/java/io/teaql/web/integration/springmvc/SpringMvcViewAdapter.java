package io.teaql.web.integration.springmvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.UserContext;
import io.teaql.core.view.PreparedView;
import io.teaql.core.view.ViewOptions;
import io.teaql.core.view.ViewPreparationException;
import io.teaql.core.view.ViewPreparationService;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Explicit Spring MVC response boundary; the host supplies its trusted UserContext. */
public final class SpringMvcViewAdapter {
    private final ObjectMapper mapper;
    private final Clock clock;

    public SpringMvcViewAdapter(ObjectMapper mapper) {
        this(mapper, Clock.systemUTC());
    }

    public SpringMvcViewAdapter(ObjectMapper mapper, Clock clock) {
        this.mapper=Objects.requireNonNull(mapper, "mapper");
        this.clock=Objects.requireNonNull(clock, "clock");
    }

    /** Prepare after business completion, then encode before Spring can serialize the response. */
    public ResponseEntity<byte[]> render(UserContext context,Object result,ViewOptions options) {
        ViewPreparationService views=views(context,options);
        return renderPrepared(context,views.prepare(context,result,options),options);
    }

    /** Recheck an already prepared result at the actual response boundary without re-signing it. */
    public ResponseEntity<byte[]> renderPrepared(UserContext context,PreparedView prepared,ViewOptions options) {
        ViewPreparationService views=views(context,options);
        PreparedView current=views.prepare(context,Objects.requireNonNull(prepared,"prepared"),options);
        Object body=current.value();
        if (!current.documentCredentials().isEmpty()) {
            var envelope=new LinkedHashMap<String,Object>();
            envelope.put("data",body);
            envelope.put("documentCredentials",current.documentCredentials());
            body=envelope;
        }
        byte[] json;
        try {
            json=mapper.writeValueAsBytes(body);
        } catch (Exception failed) {
            throw new ViewPreparationException(
                    ViewPreparationException.Code.VIEW_PREPARATION_FAILED,
                    options.businessCompletion());
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL,current.cachePolicyAt(clock.instant()))
                .body(json);
    }

    private static ViewPreparationService views(UserContext context,ViewOptions options) {
        Objects.requireNonNull(options,"options");
        if (context==null) throw missing(options);
        ViewPreparationService views=context.capability(ViewPreparationService.class);
        if (views==null) throw missing(options);
        return views;
    }

    private static ViewPreparationException missing(ViewOptions options) {
        return new ViewPreparationException(
                ViewPreparationException.Code.VIEW_CAPABILITY_MISSING,
                options.businessCompletion());
    }
}
