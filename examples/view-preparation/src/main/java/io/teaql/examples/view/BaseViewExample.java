package io.teaql.examples.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.teaql.core.*;
import io.teaql.core.meta.*;
import io.teaql.core.view.*;
import io.teaql.runtime.*;
import java.util.*;

/** Plain java main: no Web server, DI, storage or secure-document implementation. */
public final class BaseViewExample {
    public static void main(String[] arguments) throws Exception {
        var metadata=new SimpleEntityMetaFactory(); var descriptor=new EntityDescriptor(); descriptor.setType("Notice");
        descriptor.setTargetType(Notice.class); descriptor.setEntitySupplier(Notice::new);
        descriptor.addSimpleProperty("id",Long.class); descriptor.addSimpleProperty("version",Long.class);
        descriptor.addSimpleProperty("title",String.class); descriptor.addSimpleProperty("privateNote",String.class); metadata.register(descriptor);
        ViewAuthorization policy=new ViewAuthorization() {
            @Override public Identity identity(UserContext context) { return new Identity("example","reader","example"); }
            @Override public boolean canReadEntity(UserContext context,EntityTarget target,String path) { return true; }
            @Override public FieldDecision field(UserContext context,EntityTarget target,String field,String path,ViewOptions.Mode mode) {
                return field.equals("privateNote")?FieldDecision.hide():FieldDecision.show();
            }
        };
        try (var runtime=TeaQLRuntime.builder().metadata(metadata).viewAuthorization(policy).telemetry(RuntimeTelemetry.NOOP).executionLogging(false).build()) {
            runtime.install(RuntimeModule.of().withRequiredCapabilities(ViewPreparationService.CAPABILITY)); runtime.validateReadiness();
            var actor=new DefaultUserContext(runtime); var notice=new Notice(); notice.__internalSet("id",42L); notice.__internalSet("version",1L);
            notice.__internalSet("title","Ready for rendering"); notice.__internalSet("privateNote","not for rendering");
            var options=ViewOptions.readOnly(); var prepared=actor.capability(ViewPreparationService.class).prepare(actor,Map.of("data",List.of(notice)),options);
            if (notice.__internalHasMutationLedger() || prepared!=runtime.prepareView(actor,prepared,options)) throw new IllegalStateException("Presentation state changed");
            String json=new ObjectMapper().writeValueAsString(prepared.value());
            if (json.contains("privateNote") || json.contains("not for rendering")) throw new IllegalStateException("Private field escaped");
            System.out.println(json);
            System.out.println("PASS base view: authorized immutable copy; no Blob/document provider or keys");
        }
    }
    public static final class Notice extends BaseEntity {
        private String title,privateNote;
        @Override public String typeName() { return "Notice"; }
        @Override public void __internalSet(String field,Object value) {
            switch (field) { case "title" -> { title=(String)value; markPropertyLoaded(field); } case "privateNote" -> { privateNote=(String)value; markPropertyLoaded(field); } default -> super.__internalSet(field,value); }
        }
        @Override public Object __internalGet(String field) {
            return switch (field) { case "title" -> title; case "privateNote" -> throw new IllegalStateException("Hidden field was read"); default -> super.__internalGet(field); };
        }
    }
}
