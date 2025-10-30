/*
 * Copyright 2022-2025 Creek Contributors (https://github.com/creek-service)
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

import static org.creekservice.api.test.util.coverage.CodeCoverage.codeCoverageCmdLineArg;
import static org.creekservice.api.test.util.debug.RemoteDebug.remoteDebugArguments;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.creekservice.api.base.type.Suppliers;
import org.creekservice.api.test.util.TestPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonSchemaGeneratorTest {

    // Change this to true locally to debug using attach me plugin:
    private static final boolean DEBUG = false;

    private static final Path LIB_DIR =
            TestPaths.moduleRoot("generator")
                    .resolve("build/install/generator/lib")
                    .toAbsolutePath();

    private static final Pattern VERSION_PATTERN =
            Pattern.compile(".*JsonSchemaGenerator: \\d+\\.\\d+\\.\\d+.*", Pattern.DOTALL);

    @TempDir private Path output;
    private Path stdErrFile;
    private Path stdOutFile;
    private Supplier<String> stdErr;
    private Supplier<String> stdOut;

    @BeforeEach
    void setUp() {
        stdOutFile = output.resolve("stdOut.txt");
        stdErrFile = output.resolve("stdErr.txt");

        stdOut = Suppliers.memoize(() -> readAll(stdOutFile));
        stdErr = Suppliers.memoize(() -> readAll(stdErrFile));
    }

    @Test
    void shouldOutputHelp() {
        // Given:
        final String[] args = {"-h"};

        // When:
        final int exitCode = runExecutor(args);

        // Then:
        assertThat(stdErr.get(), is(""));
        assertThat(stdOut.get(), startsWith("Usage: JsonSchemaGenerator"));
        assertThat(
                stdOut.get(), containsString("-h, --help      Show this help message and exit."));
        assertThat(stdOut.get(), containsString("-o, --output-directory=<outputDirectory>"));
        assertThat(exitCode, is(0));
    }

    @Test
    void shouldOutputVersion() {
        // Given:
        final String[] args = {"-V"};

        // When:
        final int exitCode = runExecutor(args);

        // Then:
        assertThat(stdErr.get(), is(""));
        assertThat(stdOut.get(), matchesPattern(VERSION_PATTERN));
        assertThat(exitCode, is(0));
    }

    @Test
    void shouldEchoArguments() {
        // Given:
        final String[] args = minimalArgs("--echo-only", "-p=some.*.package");

        // When:
        final int exitCode = runExecutor(args);

        // Then:
        assertThat(stdErr.get(), is(""));
        assertThat(stdOut.get(), matchesPattern(VERSION_PATTERN));
        assertThat(stdOut.get(), containsString("--output-directory=" + Path.of("some/path")));
        assertThat(stdOut.get(), containsString("--type-scanning-allowed-modules=<ANY>"));
        assertThat(
                stdOut.get(), containsString("--type-scanning-allowed-packages=[some.*.package]"));
        assertThat(stdOut.get(), containsString("--subtype-scanning-allowed-modules=<ANY>"));
        assertThat(stdOut.get(), containsString("--subtype-scanning-allowed-packages=<ANY>"));
        assertThat(exitCode, is(0));
    }

    @Test
    void shouldRunFromClassPath() {
        // Given:
        final String[] javaArgs = {"-cp", LIB_DIR + "/*", JsonSchemaGenerator.class.getName()};

        // When:
        final int exitCode = runExecutor(javaArgs, minimalArgs("--echo-only"));

        // Then:
        assertThat(stdErr.get(), is(""));
        assertThat(stdOut.get(), matchesPattern(VERSION_PATTERN));
        assertThat(stdOut.get(), containsString("--class-path=" + LIB_DIR));
        assertThat(stdOut.get(), not(containsString("--module-path")));
        assertThat(exitCode, is(0));
    }

    @Test
    void shouldEchoModulePath() {
        // Given:
        final String[] javaArgs = {
            "-p",
            "/another/path",
            "--module-path",
            LIB_DIR.toString(),
            "--module",
            "creek.json.schema.generator/org.creekservice.api.json.schema.generator.JsonSchemaGenerator"
        };

        // When:
        final int exitCode = runExecutor(javaArgs, minimalArgs("--echo-only"));

        // Then:
        assertThat(stdErr.get(), is(""));
        assertThat(stdOut.get(), containsString("--module-path=" + LIB_DIR));
        assertThat(stdOut.get(), containsString("--module-path=/another/path"));
        assertThat(exitCode, is(0));
    }

    @Test
    void shouldReportIssuesWithArguments() {
        // Given:
        final String[] args = minimalArgs("--unknown");

        // When:
        final int exitCode = runExecutor(args);

        // Then:
        assertThat(stdErr.get(), startsWith("Unknown option: '--unknown'"));
        assertThat(stdErr.get(), containsString("Usage: JsonSchemaGenerator"));
        assertThat(stdOut.get(), is(""));
        assertThat(exitCode, is(1));
    }

    @Test
    void shouldNotCheckInWithDebuggingEnabled() {
        assertThat("Do not check in with debugging enabled", !DEBUG);
    }

    private int runExecutor(final String[] cmdArgs) {
        final String[] javaArgs = {
            "--module-path",
            LIB_DIR.toString(),
            "--module",
            "creek.json.schema.generator/org.creekservice.api.json.schema.generator.JsonSchemaGenerator"
        };
        return runExecutor(javaArgs, cmdArgs);
    }

    @SuppressFBWarnings(value = "COMMAND_INJECTION", justification = "Test code")
    private int runExecutor(final String[] javaArgs, final String[] cmdArgs) {
        final List<String> cmd = buildCommand(javaArgs, cmdArgs);

        try {
            final Process executor =
                    new ProcessBuilder()
                            .command(cmd)
                            .redirectError(ProcessBuilder.Redirect.to(stdErrFile.toFile()))
                            .redirectOutput(ProcessBuilder.Redirect.to(stdOutFile.toFile()))
                            .start();

            executor.waitFor(30, TimeUnit.SECONDS);
            return executor.exitValue();
        } catch (final Exception e) {
            throw new AssertionError(
                    "Error executing: "
                            + cmd
                            + ", stdErr: "
                            + stdErr.get()
                            + ", stdOut: "
                            + stdOut.get(),
                    e);
        }
    }

    private static List<String> buildCommand(final String[] javaArgs, final String[] cmdArgs) {
        final List<String> cmd = new ArrayList<>(List.of("java"));
        if (DEBUG) {
            cmd.addAll(remoteDebugArguments());
        }
        codeCoverageCmdLineArg().ifPresent(cmd::add);
        cmd.addAll(List.of(javaArgs));
        cmd.addAll(List.of(cmdArgs));
        return cmd;
    }

    private static String readAll(final Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new AssertionError("Failed to read " + path, e);
        }
    }

    private static String[] minimalArgs(final String... additional) {
        final List<String> args = new ArrayList<>(List.of("--output-directory=some/path"));
        args.addAll(List.of(additional));
        return args.toArray(String[]::new);
    }
}
