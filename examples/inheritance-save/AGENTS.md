# Inherited save example instructions

- Implement application-owned tests and services only. `lib/` is unchanged
  generation output, bound by `generated-files.json`; never edit it manually.
- Discover APIs through targeted `assist/` help or current model-aware Assist,
  not generated library source. Report `MISSING_ASSIST` for missing operations.
- All queries are bounded and carry comment and purpose. Saves carry an audit
  reason and execute with the same UserContext.
- A typed parent view remains a parent object. `runtimeType()` is readonly
  subtype information, not permission to cast or select a child checker.
- Never insert parent rows manually or write business data through test SQL.
  SQL is limited to read-only oracles and maintainer-approved test-fixture DDL:
  reject-only triggers may reject a named test value, never repair or seed rows.
- Run `node verify.cjs --output /absolute/new/evidence-directory`. It requires
  an isolated runtime checkout with no existing Maven targets, verifies local
  class origins and removes only the build outputs created by its own run.
