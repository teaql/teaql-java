
# TeaQL Tool & Runtime API Reference

> [!WARNING]
> **DO NOT GUESS FRAMEWORK APIS**
> Do not guess how to use `UserContext`, `SmartList`, `WebResponse`, or how to manage transactions.

> [!IMPORTANT]
> **USE TEAQL'S OWN TOOLING FIRST**
> Always use `cargo teaql` to retrieve assist content. Never bypass the CLI or invoke the generator endpoint directly.

To get the exact API usage and examples for Framework-level APIs (UserContext, SmartList, WebResponse, Entity Expression, Checkers, etc.), you must fetch the dynamically generated prompt directly from the code generation server. Use your tools to execute the following command:

```bash
cargo teaql --input models/school-management-service.xml java-assist-tool-api
```

Once the command succeeds, read its output. Use the printed code as a template to write your logic.

## Domain Object Assist APIs

If you need reference code or tool APIs specifically tailored for your domain objects (e.g., `user`, `order`), TeaQL provides code generators that yield perfect, ready-to-copy Java code snippets.

You can query these assist APIs for any object defined in your `models/school-management-service.xml`:

| Target | Description | Example Command |
|--------|-------------|-----------------|
| `java-assist-query/[object]` | How to query and filter `[object]` | `cargo teaql --input models/school-management-service.xml java-assist-query/school` |
| `java-assist-create/[object]` | How to insert/create `[object]` | `cargo teaql --input models/school-management-service.xml java-assist-create/school` |
| `java-assist-update/[object]` | How to update `[object]` | `cargo teaql --input models/school-management-service.xml java-assist-update/school` |
| `java-assist-delete/[object]` | How to delete `[object]` | `cargo teaql --input models/school-management-service.xml java-assist-delete/school` |