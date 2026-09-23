# Main branch CI ruleset — 2026-09-23

Tracking issue: [#133](https://github.com/teaql/teaql-java/issues/133).

The active repository ruleset [Protect default branch](https://github.com/teaql/teaql-java/rules/21070374) targets `~DEFAULT_BRANCH`. It preserves deletion and non-fast-forward protection and requires these exact GitHub Actions check contexts (app ID `15368`):

| Check context | Workflow | Coverage |
| --- | --- | --- |
| `Build and Test` | CI | Maven tests, SpotBugs, trusted OWASP Dependency-Check snapshot, CVSS >= 7 gate, and retained HTML/JSON reports |
| `examples` | Example Gate | Generated/example application checks |
| `postgres-mysql` | Live SQL dialects | PostgreSQL and MySQL dialect checks |

The source of each context was verified from the successful check runs on Java [PR #160](https://github.com/teaql/teaql-java/pull/160), commit `28d05bb04f16c5aca0f498d261676d7b0e82135d`. A missing, pending, or failing required check must prevent merge. A passing check must be from the current PR head; manual discipline is not the gate.

## Policy choice

- `strict_required_status_checks_policy=false`: the PR head needs passing checks, but is not forced to rerun the full suite after an unrelated main-branch merge. This is an explicit *loose* policy to bound CI latency. It does **not** guarantee that the exact post-merge tree was tested; reconsider if incompatible concurrent merges become frequent.
- The ruleset has **no bypass actors**. In particular, no administrator bypass is configured. Repository administrators can still deliberately edit the ruleset, which is a separate auditable governance action, not a silent check bypass.
- No requirement for an approving review is added here; the scope of #133 is CI enforcement. A review policy is a separate decision.

The ruleset lives in GitHub settings, not this Markdown file. Verify effective state with `GET /repos/teaql/teaql-java/rules/branches/main` after any settings change. The issue records a controlled PR test of pending and passing states.
