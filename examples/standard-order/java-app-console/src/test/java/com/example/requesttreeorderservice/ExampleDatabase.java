package com.example.requesttreeorderservice;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.rules.TemporaryFolder;

public final class ExampleDatabase extends TemporaryFolder {
    String url(String scenario) throws IOException {
        String retained = System.getenv("TEAQL_JAVA_STANDARD_ORDER_DATABASE_DIR");
        Path directory = retained == null ? getRoot().toPath() : Path.of(retained);
        if (!Files.isDirectory(directory)) {
            throw new IOException("Order example requires a fresh existing evidence directory");
        }
        Path database = directory.resolve(scenario + ".sqlite");
        Files.createFile(database);
        return "jdbc:sqlite:" + database.toAbsolutePath();
    }
}
