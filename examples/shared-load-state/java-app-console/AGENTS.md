<!-- DISCARD_BLOCK: phase_modeling -->

# TeaQL Java Agent Instructions

## TeaQL workspace rules

- Work in application-owned files. Keep the generated domain library read-only.
- Discover API names and signatures through model-aware Assist only.
  Do not read or search generated library source for API discovery. Use the
  original evaluated KSML file or directory and its exact entity and field names.
- Ask for the current entity and action first. Request field-level query help
  only for the fields you use. If Assist lacks the operation, report
  `MISSING_ASSIST` with the entity, action, and relevant field or diagnostic.

```text
cargo teaql --input [model] java-assist-[action]/[entity]
cargo teaql --input [model] java-assist-query/[entity].[field]
```

Replace the bracketed values with the actual model path, action, entity, and
field. Actions include `query`, `list-page`, `create`, `update`, `delete`, and
`expression`. Validate API usage by compiling and testing the application.

- Use the trusted `UserContext` as the only runtime argument for generated query
  execution and saves. Resolve tenant, actor, authorization, and provider
  resources from trusted application setup.
- Keep queries bounded and provide a non-empty purpose and comment. Supply a
  non-empty audit reason for writes through the API shown by Assist.

> [!WARNING]
> **IGNORE GENERIC ORM EXPERIENCE**
>
> Do **not** use pre-trained habits from data-access frameworks, ORMs, or database integration libraries.
>
> Do **not** use MyBatis, JPA, Hibernate, Spring Data, MyBatis-Plus, or similar frameworks.
>
> Do **not** write raw SQL, DAOs, Repository implementations, or custom persistence layers.
>
> Do **not** guess TeaQL method names.

## Model entities

| entity-name | display-name |
|-------------|--------------|
| platform | platform |
| school_type | school_type |
| school | school |
| school_capacity_summary | School Capacity Summary |


## Application entry and context

Start in `App.java` under `src/main/java`. The console assembles the
`TeaQLRuntime` and creates its `UserContext` with `new CustomUserContext(runtime)`.

## Additional References

Read these only when the task requires them:

* **`TOOL_API_GUIDE.md`**
  Framework runtime references and context handling.

* **`RUNTIME_CUSTOM_GUIDE.md`**
  Runtime setup, debugging, and project-specific restrictions.

## Optional standard telemetry

See [STANDARD_TELEMETRY.md](STANDARD_TELEMETRY.md) for the selected runtime's optional module, trusted activation variables and safe startup diagnostics.
