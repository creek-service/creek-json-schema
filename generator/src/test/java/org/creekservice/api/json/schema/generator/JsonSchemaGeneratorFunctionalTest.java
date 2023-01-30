/*
 * Copyright 2022 Creek Contributors (https://github.com/creek-service)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.creekservice.api.json.schema.generator;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.creekservice.api.test.util.coverage.CodeCoverage.codeCoverageCmdLineArg;
import static org.creekservice.api.test.util.debug.RemoteDebug.remoteDebugArguments;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.creekservice.api.test.util.TestPaths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchemaGeneratorFunctionalTest {

    // Change this to true locally to debug using attach-me plugin:
    private static final boolean DEBUG = false;

    private static final Path LIB_DIR =
            TestPaths.moduleRoot("generator")
                    .resolve("build/install/generator/lib")
                    .toAbsolutePath();

    private static final Path TEST_TYPES_LIB_DIR =
            TestPaths.moduleRoot("test-types").resolve("build//libs").toAbsolutePath();

    private static final Path EXPECTED_SCHEMA_DIR =
            TestPaths.moduleRoot("generator").resolve("src/test/resources/test/types");

    @TempDir private static Path outputDir;

    @BeforeAll
    static void setUp() {
        generateSchemas(outputDir);
    }

    @AfterAll
    static void afterAll() {
        final Set<Path> expected =
                TestPaths.listDirectory(EXPECTED_SCHEMA_DIR)
                        .map(Path::getFileName)
                        .collect(Collectors.toUnmodifiableSet());

        final Set<Path> actual =
                TestPaths.listDirectory(outputDir)
                        .map(Path::getFileName)
                        .collect(Collectors.toUnmodifiableSet());

        assertThat(actual, is(expected));
    }

    @ParameterizedTest
    @MethodSource("expectedSchema")
    void shouldGenerateSchemasForTestType(final Path expectedSchemaPath) {
        final Path actualSchemaPath = outputDir.resolve(expectedSchemaPath.getFileName());
        assertThat(
                "Schema does not exist: " + actualSchemaPath.toUri(),
                Files.isRegularFile(actualSchemaPath));

        final String expectedSchema = readSchema(expectedSchemaPath);
        final String actualSchema = readSchema(actualSchemaPath);

        assertThat(
                "Actual schema: "
                        + actualSchemaPath.toUri()
                        + lineSeparator()
                        + "Does not match expected schema: "
                        + expectedSchemaPath.toUri(),
                actualSchema,
                is(expectedSchema));
    }

    @Test
    void shouldNotCheckInWithDebuggingEnabled() {
        assertThat("Do not check in with debugging enabled", !DEBUG);
    }

    // Run me to dump out updated schemas:
    public static void main(final String... args) {
        generateSchemas(TestPaths.moduleRoot("generator").resolve(EXPECTED_SCHEMA_DIR));
    }

    @SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "Test code")
    private static void generateSchemas(final Path outputDir) {
        final List<String> cmd = buildCommand(outputDir);

        try {
            final Process executor = new ProcessBuilder().command(cmd).start();

            executor.waitFor(30, TimeUnit.SECONDS);

            if (executor.exitValue() != 0) {
                throw new AssertionError(
                        "Failed:"
                                + System.lineSeparator()
                                + "stderr:"
                                + readAll(executor.getErrorStream())
                                + System.lineSeparator()
                                + "stdout:"
                                + readAll(executor.getInputStream()));
            }

            assertThat(
                    readAll(executor.getInputStream()),
                    matchesPattern(Pattern.compile(".*Wrote [1-9]\\d* schema.*", Pattern.DOTALL)));
        } catch (final Exception e) {
            throw new AssertionError("Error executing: " + cmd, e);
        }
    }

    private static List<String> buildCommand(final Path outputDir) {
        final List<String> cmd = new ArrayList<>(List.of("java"));

        if (DEBUG) {
            cmd.addAll(remoteDebugArguments());
        }
        codeCoverageCmdLineArg().ifPresent(cmd::add);
        cmd.addAll(
                List.of(
                        "--module-path",
                        LIB_DIR + System.getProperty("path.separator") + TEST_TYPES_LIB_DIR,
                        "--add-modules",
                        "creek.json.schema.test.types",
                        "--module",
                        "creek.json.schema.generator/org.creekservice.api.json.schema.generator.JsonSchemaGenerator",
                        "--output-directory=" + outputDir.toAbsolutePath(),
                        "--type-scanning-allowed-module=creek.json.schema.test.types",
                        "--subtype-scanning-allowed-module=creek.json.schema.test.types"));
        return cmd;
    }

    public static Stream<Path> expectedSchema() {
        return TestPaths.listDirectory(EXPECTED_SCHEMA_DIR);
    }

    private static String readAll(final InputStream stdErr) {
        return new BufferedReader(new InputStreamReader(stdErr, UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    private static String readSchema(final Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(line -> !line.startsWith("#"))
                    .collect(Collectors.joining(lineSeparator()));
        } catch (IOException e) {
            throw new AssertionError("Failed to read " + path.toUri());
        }
    }
}
