package com.teaql.supplieronboardingservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teaql.supplieronboardingservice.warehouse.Warehouse;
import io.teaql.core.UserContext;
import io.teaql.core.checker.CheckException;
import io.teaql.core.checker.JsonFieldNamingProfile;
import io.teaql.core.i18n.I18nCatalog;
import io.teaql.core.i18n.UnsupportedLocaleException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

final class SupplierOnboarding {
    private static final ObjectMapper JSON = new ObjectMapper();

    private SupplierOnboarding() {}

    static void run(JsonNode scenario, UserContext primary, UserContext independent) throws IOException {
        var warehouse = Q.warehousesWithMinimalFields().withIdIs(scenario.path("warehouse").path("id").asLong())
                .limit(1).comment("Load the initialized receiving warehouse")
                .purpose("Register a supplier for warehouse purchasing").executeForList(primary).first();
        Objects.requireNonNull(warehouse, "Initial receiving warehouse is missing");
        var contexts = Map.of("main", primary, "independent", independent);
        for (var step : scenario.required("steps")) {
            var context = Objects.requireNonNull(contexts.get(step.required("context").asText()), "Unknown context");
            switch (step.required("operation").asText()) {
                case "select" -> select(step, context);
                case "validate" -> verifyFailure(step, context, warehouse);
                case "install-catalog" -> {
                    try (var input = Files.newInputStream(Path.of(step.required("path").asText()))) {
                        context.installI18nCatalog(I18nCatalog.fromJson(input));
                    }
                    emit(step, context, "catalog-installed", Map.of());
                }
                case "verify-unchanged" -> {
                    var rows = Q.suppliersWithMinimalFields().limit(1)
                            .comment("Check the rejected registrations").purpose("Verify no invalid supplier was stored")
                            .executeForList(context);
                    emit(step, context, "storage", Map.of("supplierCount", rows.size()));
                }
                default -> throw new IllegalArgumentException("Unknown example operation");
            }
        }
    }

    private static void select(JsonNode step, UserContext context) throws IOException {
        String code = step.required("locale").asText();
        try {
            context.setLocaleCode(code);
            emit(step, context, "locale", Map.of("requestedLocale", code, "accepted", true));
        } catch (UnsupportedLocaleException error) {
            emit(step, context, "locale", Map.of("requestedLocale", error.getLocaleCode(), "accepted", false,
                    "nativeError", error.getClass().getSimpleName()));
        }
    }

    private static void verifyFailure(JsonNode step, UserContext context, Warehouse warehouse) throws IOException {
        var values = step.required("input");
        var supplier = Q.suppliers().comment("Prepare supplier registration")
                .purpose("Register a supplier for warehouse purchasing").newEntity(context);
        supplier.updateWarehouse(warehouse);
        supplier.updateName(values.required("name").asText(null));
        supplier.updateMinimumOrderQuantity(values.required("minimum_order_quantity").asInt());
        try {
            supplier.auditAs("Register the supplier for purchasing").save(context);
        } catch (CheckException failure) {
            if (failure.getViolates().size() != 1) throw failure;
            var violation = failure.getViolates().getFirst();
            var wire = violation.toWire(JsonFieldNamingProfile.CAMEL_CASE);
            var rule = violation.getRuleId().name().toLowerCase(Locale.ROOT);
            Map<String, Integer> arguments = switch (rule) {
                case "required" -> Map.of();
                case "min" -> Map.of("minimum", Integer.parseInt(String.valueOf(wire.systemValue())));
                default -> throw failure;
            };
            var nativeArguments = new LinkedHashMap<String, Object>();
            nativeArguments.put("inputValue", wire.inputValue());
            nativeArguments.put("systemValue", wire.systemValue());
            emit(step, context, "validation", Map.of("case", step.required("case").asText(),
                    "messageKey", "checker." + rule, "nativeRule", violation.getRuleId().name(),
                    "location", violation.getLocation().modelPath(), "arguments", arguments,
                    "message", wire.message(), "nativeArguments", nativeArguments));
            return;
        }
        throw new IllegalStateException("Invalid supplier registration was stored");
    }

    private static void emit(JsonNode step, UserContext context, String type, Map<String, ?> details) throws IOException {
        var event = new LinkedHashMap<String, Object>(details);
        event.put("type", type);
        event.put("context", step.required("context").asText());
        event.put("effectiveLocale", context.locale().code());
        System.out.println("TEAQL_I18N_SHOWCASE " + JSON.writeValueAsString(event));
    }
}
