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

    @Test
    void engineDoesNotDependOnApiFacade() throws IOException {
        List<Path> violations =
                javaFiles(Path.of("src/main/java/com/time_engine/engine"))
                        .filter(ArchitectureBoundaryTest::importsApiReference)
                        .toList();

        assertTrue(
                violations.isEmpty(),
                () -> "engine must not import api facade types: " + violations);
    }

    @Test
    void engineCommonDoesNotDependOnClientCode() throws IOException {
        List<Path> violations =
                javaFiles(Path.of("src/main/java/com/time_engine/engine/common"))
                        .filter(ArchitectureBoundaryTest::containsClientReference)
                        .toList();

        assertTrue(
                violations.isEmpty(),
                () -> "engine/common must not reference client-only code: " + violations);
    }

    @Test
    void sandevistanDoesNotImportEngineInternalsDirectly() throws IOException {
        List<Path> violations =
                javaFiles(Path.of("src/main/java/com/time_engine/sandevistan"))
                        .filter(ArchitectureBoundaryTest::importsEngineInternalReference)
                        .toList();

        assertTrue(
                violations.isEmpty(),
                () ->
                        "sandevistan should use api facades instead of engine internals: "
                                + violations);
    }

    private static Stream<Path> javaFiles(Path root) throws IOException {
        if (!Files.exists(root)) {
            return Stream.empty();
        }
        return Files.walk(root).filter(path -> path.toString().endsWith(".java"));
    }

    private static boolean containsSandevistanReference(Path path) {
        return fileContains(path, "com.time_engine.sandevistan");
    }

    private static boolean containsClientReference(Path path) {
        String content = read(path);
        return content.contains("net.minecraft.client")
                || content.contains("com.time_engine.engine.client");
    }

    private static boolean importsEngineInternalReference(Path path) {
        return fileContains(path, "import com.time_engine.engine.");
    }

    private static boolean importsApiReference(Path path) {
        return fileContains(path, "import com.time_engine.api.");
    }

    private static boolean fileContains(Path path, String pattern) {
        return read(path).contains(pattern);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }
}
