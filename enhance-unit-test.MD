# Unit Test Enhancement Workflow and Progress

## Document Status

- Repository: `teaql/teaql-java`
- Base branch: `main`
- Last updated: July 17, 2026
- Current checkpoint: Work is paused after Issue #24

## Purpose

This document defines the maintenance workflow for incrementally improving unit test coverage. The project has completed its main development phase, so each change should be small, traceable, independently reviewable, and protected by automated verification.

All issues, branch descriptions, commit messages, pull requests, test names, code comments, and maintenance documentation must be written in English.

## Working Rules

1. Create or select a focused GitHub issue before changing code.
2. Keep one issue, one branch, and one pull request per change.
3. Use small issues with explicit scope and acceptance criteria.
4. Start every issue from an up-to-date `main` branch.
5. Name branches `issue/<number>-<short-description>`.
6. Never create a branch whose name starts with `codex`.
7. Target pull requests at `main` and include `Fixes #<number>` in the description.
8. Add production changes only when a focused test exposes a real defect.
9. Do not mix unrelated cleanup, formatting, generated files, or refactoring into a unit test pull request.
10. Merge only after all required CI checks pass.
11. After merging, delete the issue branch locally and remotely, update local `main`, and begin the next issue from that clean state.

## Standard Issue Lifecycle

### 1. Synchronize the Base Branch

Switch to `main`, pull with fast-forward only, and confirm that the working tree is clean.

```shell
git switch main
git pull --ff-only origin main
git status --short --branch
```

### 2. Confirm the Issue Scope

Before implementation, verify that the issue contains:

- a concise English title;
- the behavior or class under test;
- the important cases to cover;
- explicit acceptance criteria;
- exclusions that prevent scope expansion.

If the behavior is already fixed on `main`, document the resolving commit on the issue and close it without creating a redundant pull request.

### 3. Create the Issue Branch

Create a branch from the updated `main` branch.

```shell
git switch -c issue/<number>-<short-description>
```

Example:

```shell
git switch -c issue/25-base-entity-change-tracking
```

### 4. Add Focused Tests

Add the smallest useful test set for the issue. Tests should:

- describe observable behavior rather than implementation details;
- cover normal behavior, boundaries, and important error cases;
- remain deterministic and independent;
- avoid unnecessary fixtures and broad integration setup;
- use a regression-first approach when fixing a defect.

For a regression, first demonstrate that the new test fails for the expected reason. Then make the smallest production change needed to pass it.

### 5. Validate Locally

Run the narrowest relevant test command first, then widen validation in proportion to the change.

```shell
mvn -q -pl teaql-core -am test
git diff --check
git status --short
```

For higher-risk or cross-module changes, run the complete build before opening the pull request:

```shell
mvn -B install
```

Review generated or modified files before committing. In particular, local tests may modify `teaql-sqlite/teaql_test.db`; that generated change must not be included unless the issue explicitly requires it.

### 6. Commit and Push

Use a concise English commit message tied to the issue scope.

```shell
git add <relevant-files>
git commit -m "test(core): <focused description>"
git push -u origin issue/<number>-<short-description>
```

### 7. Open the Pull Request

The pull request should contain:

- a short English summary;
- the behaviors covered;
- any production fix and why it was necessary;
- the local verification performed;
- `Fixes #<number>`.

### 8. Wait for Required CI Checks

The pull request must remain unmerged until all repository checks pass:

1. Maven build: `mvn -B install`
2. SpotBugs: `mvn spotbugs:check`
3. OWASP dependency check: `mvn org.owasp:dependency-check-maven:check`

GitHub native auto-merge is not enabled for this repository. The maintenance automation therefore monitors the checks and explicitly performs the merge only after every required check is green.

### 9. Merge and Clean Up

Use a squash merge, delete the remote issue branch, return the local repository to `main`, and update it before doing any further work.

```shell
git switch main
git pull --ff-only origin main
git branch -d issue/<number>-<short-description>
git status --short --branch
```

The next issue must begin only after local `main` contains the merged pull request and the working tree is clean.

## Operational Recovery Notes

- GitHub API requests may occasionally fail because of HTTP/2 framing or TLS timeouts. Retry with bounded timeouts and HTTP/1.1 rather than changing repository state speculatively.
- If CI fails, inspect the failing check, fix it on the same issue branch, push the correction, and wait for the complete check set again.
- If a test creates tracked database changes or other generated artifacts, restore only those unintended artifacts before committing. Do not discard unrelated user changes.
- If an issue is already resolved on `main`, record evidence such as the resolving commit and relevant verification, then close the issue without an empty branch or pull request.

## Current Progress

| Issue | Scope | Status | Resolution |
| --- | --- | --- | --- |
| [#18](https://github.com/teaql/teaql-java/issues/18) | Repair `MemoryDatabaseTest` overrides so the runtime test suite compiles | Closed | Already resolved on `main` by commit `17fce966`; documented and closed without a redundant PR |
| [#19](https://github.com/teaql/teaql-java/issues/19) | Add `EntityKey` value semantics tests | Closed | Merged in [PR #29](https://github.com/teaql/teaql-java/pull/29) |
| [#20](https://github.com/teaql/teaql-java/issues/20) | Add `EntityChangeSet` behavior tests | Closed | Merged in [PR #30](https://github.com/teaql/teaql-java/pull/30) |
| [#21](https://github.com/teaql/teaql-java/issues/21) | Add `ChangeSetStack` scope behavior tests | Closed | Merged in [PR #31](https://github.com/teaql/teaql-java/pull/31) |
| [#22](https://github.com/teaql/teaql-java/issues/22) | Cover explicit-null shadowing across change scopes | Closed | Regression reproduced and fixed in [PR #32](https://github.com/teaql/teaql-java/pull/32) |
| [#23](https://github.com/teaql/teaql-java/issues/23) | Add `EntityRoot` lifecycle tests | Closed | Merged in [PR #33](https://github.com/teaql/teaql-java/pull/33) |
| [#24](https://github.com/teaql/teaql-java/issues/24) | Add `EntityRoot` merge behavior tests | Closed | Merged in [PR #34](https://github.com/teaql/teaql-java/pull/34) |
| [#25](https://github.com/teaql/teaql-java/issues/25) | Cover `BaseEntity` root-backed change tracking | Open | Next issue; not started |
| [#26](https://github.com/teaql/teaql-java/issues/26) | Cover `BaseEntity` equality and hash code behavior | Open | Pending |
| [#27](https://github.com/teaql/teaql-java/issues/27) | Cover `saveGraph` ledger classification and ordering | Open | Pending |
| [#28](https://github.com/teaql/teaql-java/issues/28) | Cover `saveGraph` relation-root merging | Open | Pending |

## Progress Summary

- 7 of 11 identified issues are closed.
- 6 focused pull requests have been merged; Issue #18 required no pull request because its fix was already present on `main`.
- The completed test work added 31 focused test methods across `EntityKeyTest`, `EntityChangeSetTest`, `ChangeSetStackTest`, and `EntityRootTest`.
- Pull requests #29 through #34 were squash-merged only after the Maven build, SpotBugs, and OWASP dependency checks passed.
- Local `main` was synchronized with `origin/main` at commit `5680545` after PR #34 was merged.
- Work is intentionally paused after Issue #24. No branch or implementation work has started for Issue #25.

## Planned Resume Order

When work resumes, process the remaining issues one at a time in this order:

1. Issue #25: `BaseEntity` root-backed change tracking
2. Issue #26: `BaseEntity` equality and hash code behavior
3. Issue #27: `saveGraph` ledger classification and ordering
4. Issue #28: `saveGraph` relation-root merging

Each issue must complete the entire issue-to-merge-to-cleanup cycle before the next branch is created.

## Definition of Done for Each Issue

An issue is complete only when:

- its focused tests are implemented and pass locally;
- any necessary production fix is minimal and covered by a regression test;
- the diff contains no unrelated or generated changes;
- the branch is pushed and the pull request references the issue;
- Maven build, SpotBugs, and OWASP dependency checks are green;
- the pull request is squash-merged into `main`;
- the GitHub issue is closed;
- the remote and local issue branches are deleted;
- local `main` is updated and the working tree is clean.
