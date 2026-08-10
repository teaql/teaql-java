# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
