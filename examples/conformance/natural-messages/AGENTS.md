# Natural Checker example

The parent conformance safety rules apply, but use this example's model and
entities, not the parent's Work Item table:

```bash
cargo teaql --input examples/conformance/natural-messages/model.xml java-assist-<action>/<entity>
```

Entities: `catalog`, `entry`. Use field-specific Query Assist when needed.
`java-lib-core/` is the current immutable generated library and passive
metadata; `fixture/` preserves the earlier generated input. Neither is an
API-discovery source. Do not install replacement Checkers or hand-built
presentation metadata in the application to make this example pass.
Modify application-owned examples or runtime code; model changes require
evaluation, regeneration and refreshed fixture-origin hashes. Run the local
verifier and retain its exit-zero evidence before claiming acceptance.
