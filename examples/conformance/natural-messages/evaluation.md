model input: examples/conformance/natural-messages/model.xml
using https://api.teaql.io/latest/evaluate
<!-- ephemeral -->
# KSML Evaluation Report
## 📊 Summary
- **Errors**: 0
- **Warnings**: 0
- **Suggestions**: 0
- **Solids**: 9

## ✅ Solids

| Rule ID | Target | Source | Message |
|---------|--------|--------|---------|
| `KSML-UPLOAD-001` | `` | — | The upload contains 1 files. |
| `KSML-UPLOAD-005` | `` | `model.xml:2` | Resolved entrypoint: model.xml |
| `KSML-XML-002` | `` | `model.xml:2` | The XML document model.xml was parsed successfully. |
| `KSML-ROOT-003` | `` | `model.xml:2` | Root name 'checker-natural-message-service' is well-formed. |
| `KSML-ROOT-008` | `` | `model.xml:2` | External JSON business fields use the default camelCase profile. |
| `KSML-OBJECT-001` | `catalog` | `model.xml:4` | Object 'catalog' defines display name, module, and module key metadata. |
| `KSML-OBJECT-001` | `entry` | `model.xml:6` | Object 'entry' defines display name, module, and module key metadata. |
| `KSML-REFERENCE-003` | `entry.catalog` | `model.xml:8` | Reference 'catalog' in 'entry' successfully resolves to target object 'catalog'. |
| `KSML-DOMAIN-ROOT-003` | `` | `model.xml:2` | Exactly one domain root candidate 'catalog' was found. |
