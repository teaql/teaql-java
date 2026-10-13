# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.555-RELEASE] - 2026-10-13

Tracking: [Java #209](https://github.com/teaql/teaql-java/issues/209) and
[conformance #109](https://github.com/teaql/teaql-conformance/issues/109).

- Add the optional `teaql-opentelemetry` standard module with trusted environment
  activation, startup guidance and full safe traces, metrics and logs.
- Preserve expanded masked SQL, carried physical execution identity, request-tree
  comments and independent graph mutation audit. No per-method SDK setup is needed.
- Bound queued/exporting work, acknowledge receiver rejection separately from
  failed batches, and preserve business outcomes during finite shutdown/export failure.
- Retire the separate page terminal; regenerate libraries and use ordinary list
  execution with offset/limit and explicit count where needed.

Source and internal-candidate evidence is retained in the conformance
`2026-10-13-standard-telemetry-onboarding` report. Signed-tag and final public
artifact verification remain separate release gates.

## [1.526-RELEASE] - 2025-07-25

### Added
- Core TeaQL Java implementation
- Providers and runtime services
- Dialect mapColumnType for DDL type mapping
- LARGE_TEXT semantic type support with correct max attribute interpolation

### Fixed
- Revert reflection fallback for EntityDescriptor
- Entity reflection instantiation and postgres max token issues

### Tests
- saveGraph merge verification for to-one and collection relations
- BaseEntity equals/hashCode contract enforcement
- BaseEntity root-backed property change tracking coverage
