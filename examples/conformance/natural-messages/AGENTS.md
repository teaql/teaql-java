# Natural Checker example

The parent conformance safety rules apply, but use this example's model and
entities, not the parent's Work Item table:

```bash
cargo teaql --input examples/conformance/natural-messages/model.xml java-assist-<action>/<entity>
```

Entities: `catalog`, `entry`. Use field-specific Query Assist when needed.
`fixture/lib/**` is an immutable generated library, not an API-discovery source.
Modify application-owned examples or runtime code; model changes require
evaluation, regeneration and refreshed fixture-origin hashes. Run the local
verifier and retain its exit-zero evidence before claiming acceptance.
