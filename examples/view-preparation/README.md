# Base-runtime view CLI

Build from the repository root:

```sh
mvn -pl examples/view-preparation -am package
```

Run `io.teaql.examples.view.BaseViewExample` using the produced example JAR and its
runtime dependencies on the Java classpath. It builds local metadata, supplies an
explicit current view policy, freezes a loaded entity/envelope, rejects hidden
getter access and renders only the immutable prepared value. It starts no listener
and requires no Blob provider, document implementation or key configuration.

The source evidence runner records the exact JAR paths/hashes and standalone Java
command. It runs without JUnit/test classes or source `target/classes` directories.
This source-built example does not prove later registry/generated-consumer gates.
