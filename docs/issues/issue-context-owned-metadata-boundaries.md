# Java context-owned metadata boundaries (#134)

This is a call-path audit, not a claim that normal relation queries previously used
the process-global registry. The supported SQL path already carried resolved
descriptors; the remaining work was to verify it against two independent runtimes
with the same entity names.

| Entry point | Classification | Current boundary and executable evidence |
| --- | --- | --- |
| `PortableSQLDataService` relation loading | Active supported Q path | Repositories receive the service's metadata snapshot and `attachRelation` receives a resolved relation descriptor. `PortableSQLDatabaseTest.relationHydrationUsesInvokingRuntimeWhenEntityNamesMatch` runs two SQLite-backed runtimes with `TopNParent`/`TopNChild` in both, but distinct `parent_a`/`parent_b` FK columns. It alternates a wrong and absent global registry; both contexts load only their own child. `TOPN_009_relationHydrationUsesServiceMetadataWithoutGlobalRegistry` covers the absent-global case with the established Top-N fixture. |
| `GenericSQLProperty` / `GenericSQLRelation.setPropertyValue(context, …)` | Public legacy mapper; compatibility-only inside this repository | These context-bearing mappers resolve reference placeholders from the invoking context. `LegacyReferenceHydrationContextTest` alternates two same-name models with wrong/absent global metadata. This is not the normal `PortableSQLDataService` graph assembly path. |
| `PortableSQLRepository.resolveDescriptor` | Active when `streamInternal` hydrates an entity or reference | Service repositories have a metadata snapshot. Schema-only dialect repositories may have none; they can reconcile physical schema but reject cross-entity hydration instead of borrowing global metadata. `PortableSQLDatabaseTest.schemaOnlyRepositoryMustNotHydrateRelationFromGlobalMetadata` registers usable global metadata and verifies that hydration still fails closed. |
| `EntityFactory.forContext` / `forMetadata` | Supported entity construction | The factory is bound to invoking-context or explicit metadata. `EntityFactoryContextTest` uses two runtimes with the same type name and a poisoned global registry. |
| `EntityFactory.defaultFactory()` | Public, deprecated no-context compatibility entry point | It uses an explicitly registered process-global factory and fails with a migration hint when no global factory exists. It cannot guarantee runtime isolation when multiple runtimes coexist; prefer `forContext` or `forMetadata`. |
| `Entity.addRelation(context, …)` | Supported relation attachment | Resolves and validates the relation using the invoking context. `BaseEntityTest` exercises same-name models with wrong/absent global metadata and invalid relation rejection. |
| `Entity.addRelation(name, value)` | Public, deprecated no-context compatibility entry point | Not used by the normal service loader. It uses explicitly registered process-global metadata and fails with a migration hint when none exists. It cannot establish which runtime owns the entity; prefer the context overload. |
| `BaseRequest` unbound metadata fallback | Deprecated context-free JSON search compatibility path | `JsonRequests.findWithJson(context, …)` binds invoking-context metadata before evaluating fields; `JsonRequestsContextTest` alternates same-name models with poisoned global metadata. Context-free callers relying on `EntityMetaFactory.registerGlobal` have no cross-runtime isolation guarantee. |

The two no-context public APIs remain compatibility surfaces, not recommended
multi-runtime APIs. A process-global registration is an explicit single-runtime
opt-in, not a substitute for passing a context. Their final migration policy
(warning versus fail-closed even when a global factory is registered) is a
separate compatibility decision; do not infer that deprecation alone makes
them safe across runtimes.

Verification scope for this audit: the focused two-runtime SQL test, full Maven
tests, Java examples, and the existing live PostgreSQL/MySQL CI gate. The
SQLite test proves descriptor selection on the normal SQL relation path; it
does not claim that every SQL dialect was retested with two simultaneous
runtimes.
