# TeaQL Error Catalog

`catalog.yml` is the authoritative database of stable TeaQL error types. Error
messages, exception classes, HTTP statuses, and source locations may change; a
published error ID does not.

## Identifier shape

```text
TQL-<SPACE>-<NUMBER>
```

Each space allocates numbers from `1001` through `9999`, giving it 8,999 IDs.
Spaces are stable product domains rather than Maven modules. Numbers are opaque
sequences: they do not encode severity, retryability, protocol status, or a
release number.

Examples of the intended shape (not allocated IDs):

```text
TQL-QRY-1001
TQL-MUT-1001
TQL-DYN-1001
```

## Discovery workflow

Error discovery is deliberately separate from allocation:

1. Scan production Java sources and collect explicit `throw new ...` sites and
   existing symbolic codes.
2. Review candidates by user-visible condition, not by source statement.
   Multiple throw sites may represent one error; one broad throw site may need
   several errors.
3. Decide whether callers, operators, or coding agents can take a distinct
   action for the condition. If not, prefer an existing error or leave it as an
   implementation exception.
4. Choose the stable product space, allocate its next unused number, and record
   the error in `catalog.yml`.
5. Add Cause, Action, structured parameters, compatibility metadata, and tests
   before changing runtime code to emit the ID.

Generate a review inbox without changing the catalog:

```bash
python3 tools/error_catalog.py scan --output /tmp/teaql-error-candidates.yml
```

Validate allocations and numbering rules:

```bash
python3 tools/error_catalog.py validate errors/catalog.yml
```

The scanner is intentionally conservative and dependency-free. It is a source
discovery aid, not a Java parser and not an allocator. Reviewers should also use
call-chain and reference tools when deciding whether candidates are the same
externally observable condition.

## Entry shape

Once reviewed, an entry should have this form:

```yaml
- id: TQL-QRY-1001
  name: QUERY_PURPOSE_REQUIRED
  status: active
  since: 1.x
  title: Query purpose is required
  category: failed_precondition
  retry: never
  cause: A query reached execution without a declared purpose.
  action: Declare the purpose before executing the query.
  parameters:
    - operation
    - entity_type
  legacy_names:
    - PURPOSE_REQUIRED
```

`name` is a stable, language-neutral symbolic alias. `title`, `cause`, and
`action` are human-readable and may gain translations. Per-occurrence IDs and
trace IDs are recorded by observability layers and are not catalog IDs.

## Allocation rules

- Allocate monotonically within a space, beginning at `1001`.
- Never renumber or reuse an ID, including retired IDs.
- Do not allocate one ID per exception class or one ID per throw statement.
- Do not branch on localized message text.
- Preserve upstream identifiers such as SQLSTATE and `ORA-xxxxx` as structured
  cause data rather than replacing them.
- Keep sensitive values out of public messages and catalog examples.
- Map HTTP, gRPC, retry, and severity as metadata; they are not part of the ID.
