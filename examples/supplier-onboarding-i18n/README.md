# Supplier onboarding in 15 locales with TeaQL Java

The [KSML model](model.xml) describes a warehouse with suppliers. The [semantic model](semantics.json) sends two registrations through the generated workspace. One lacks a supplier name. The other submits 3 as the minimum order quantity, below the allowed value of 10. The application attempts audited saves and uses generated Q to confirm that SQLite stores neither invalid supplier.

## Run and select a locale

JDK 21, Maven 3.9, Git, and Python 3.9 or newer. This example uses local Java SDK 6.3.0 source. The runner builds the local Java SDK, generated library and shaded console in temporary directories. Maven settings are supplied by the runner and resolve third-party dependencies through Maven Central.

```sh
bash examples/supplier-onboarding-i18n/run.sh
bash examples/supplier-onboarding-i18n/run.sh --locale fr
bash examples/supplier-onboarding-i18n/run.sh --all
bash examples/supplier-onboarding-i18n/run.sh --edge-cases
bash examples/supplier-onboarding-i18n/run.sh --verify
```

The commands run from the runtime repository root. The output is UTF-8 JSON Lines. Each validation row gives the effective locale, stable message key, canonical field, native rule, arguments and localized message. `--all` uses the 15 locales from the semantic model in one process. The final command runs the full set twice against one SQLite file and runs the locale boundary checks.

The app selects the locale with `context.setLocaleCode(code)` and reads `context.locale().code()`. A new context defaults to `en`. The parser trims, normalizes underscores and case, and resolves known aliases such as `ZH_hant` to `zh-TW`. An empty or unsupported code raises `UnsupportedLocaleException` and preserves the previous locale. Single-language mode prints the rejection and exits 2. Locale selection comes from the trusted CLI and context. Environment variables and business input do not set it.

`JAVA_HOME` selects JDK 21. `MAVEN` defaults to `mvn`. `TEAQL_JAVA_EXAMPLE_SOURCE_ROOT` defaults to the enclosing repository. Empty tool or source overrides use the defaults.

## How the message is produced

1. The console builds its `UserContext`, installs generated model metadata, and explicitly initializes SQLite. It sets the locale before the generated business call.
2. The real model check rejects the invalid registration. Use `CheckException.getViolates()` to inspect the original `REQUIRED` or `MIN` rule. The model field path is read before serialization. A bounded query reads the existing warehouse as a generated `Warehouse` before it is passed to `updateWarehouse`.
3. The renderer chooses the locale and a template for the field's location shape. It binds the rendered field, model path and applicable checker parameters, including the lower bound.
4. The sample attaches the generated application catalog through `context.installI18nCatalog(I18nCatalog.fromJson(input))`. Its Vietnamese message entries are absent. Built-in Vietnamese supplies the same two messages after attachment. For this natural checker message, selection tries the application's shape-specific override, the application legacy template, and the built-in natural template at the active locale, then repeats that order for English. The generic invalid rule can supply the final natural template. Direct lookup through the legacy message catalog selects application and built-in entries at the current locale first, then application and built-in English, then the stable key.

The independent French context remains French through the main context's Japanese, Arabic and Japanese sequence. Domain labels without an available translation can remain in English. The original rule enum is uppercase. The example exposes the language-neutral keys alongside it. [Locale source](../../teaql-core/src/main/java/io/teaql/core/i18n/Locale.java) · [Message lookup source](../../teaql-core/src/main/java/io/teaql/core/i18n/I18nCatalog.java) · [Natural templates source](../../teaql-core/src/main/java/io/teaql/core/i18n/CheckerMessageCatalog.java)

## Run the same scene in another runtime

Keep the same KSML and semantic inputs when switching to its sibling checkout. Each runtime uses its own setup and call style, while the error keys, field paths and minimum bound stay stable.

[Java](./README.md) · [Rust](../../../rust/examples/supplier-onboarding-i18n/README.md) · [TypeScript](../../../typescript/examples/supplier-onboarding-i18n/README.md) · [Go](../../../go/examples/supplier-onboarding-i18n/README.md) · [Python](../../../python/examples/supplier-onboarding-i18n/README.md) · [C#/.NET](../../../dotnet/examples/supplier-onboarding-i18n/README.md) · [Swift](../../../swift/Examples/SupplierOnboardingI18n/README.md)

## Add a locale

Add an enum constant with code `it` in `Locale.java` and register any accepted aliases in its parser. Update the built-in resources `builtin-messages-v1.json`, `checker-messages-v1.json` and `checker-presentation-v1.json` under `teaql-core/src/main/resources/io/teaql/core/i18n`. Supply natural forms for the supported shapes and add presentation words, such as the name for the current object and the measurement units. For a legacy required-message entry the identifier and placeholder stay the same.

```json
{
  "it": {
    "messages": {"checker.required": "Il campo {location} è obbligatorio"},
    "vocabulary": {}
  }
}
```

Keep placeholders such as `{field}`, `{location}` and `{system}` aligned with their respective catalog schemas. Add the locale to the generator and shared semantic model, regenerate the workspaces, then run the same save, alias, fallback and isolation checks. To customize text for an already supported locale, attach an explicit application catalog.
