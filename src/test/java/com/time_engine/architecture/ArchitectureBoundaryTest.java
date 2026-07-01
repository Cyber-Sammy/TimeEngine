package com.time_engine.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {
    @Test
    void engineAndApiDoNotDependOnSandevistan() throws IOException {
        List<Path> violations =
                Stream.concat(
                                javaFiles(Path.of("src/main/java/com/time_engine/engine")),
                                javaFiles(Path.of("src/main/java/com/time_engine/api")))
                        .filter(ArchitectureBoundaryTest::containsSandevistanReference)
                        .toList();

        assertTrue(
                violations.isEmpty(),
                () -> "engine/api must not reference sandevistan: " + violations);
    }

    private static Stream<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
    }

    private static boolean containsSandevistanReference(Path path) {
        try {
            return Files.readString(path).contains("com.time_engine.sandevistan");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }
}
