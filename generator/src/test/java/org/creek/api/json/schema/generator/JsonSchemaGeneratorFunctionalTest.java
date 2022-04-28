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

package org.creek.api.json.schema.generator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.creek.api.base.type.Suppliers;
import org.creek.api.test.util.TestPaths;
import org.junit.jupiter.api.Test;

class JsonSchemaGeneratorFunctionalTest {

    private static final Path LIB_DIR =
            TestPaths.moduleRoot("generator")
                    .resolve("build/install/generator/lib")
                    .toAbsolutePath();

    private Supplier<String> stdErr;
    private Supplier<String> stdOut;

    @Test
    void shouldOutputHelp() {
        // Given:
        final String[] args = {"-h"};

        // When:
        final int exitCode = runExecutor(args);

        // Then:
        assertThat(stdErr.get(), is(""));
        assertThat(stdOut.get(), startsWith("Usage: JsonSchemaGenerator"));
        assertThat(stdOut.get(), containsString("-h, --help   display this help message"));
        assertThat(stdOut.get(), containsString("-o, --output=<outputDirectory>"));
        assertThat(exitCode, is(0));
    }

    @Test
    void shouldEchoArguments() {
        // Given:
        final String[] args = minimalArgs("--echo-only");

        // When:
        final int exitCode = runExecutor(args);

        // Then:
        assertThat(stdErr.get(), is(""));
        assertThat(stdOut.get(), containsString("--output=some/path"));
        assertThat(stdOut.get(), containsString("--package=<Not Set>"));
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

    private int runExecutor(final String[] args) {
        final List<String> cmd =
                new ArrayList<>(
                        List.of(
                                "java",
                                "--module-path",
                                LIB_DIR.toString(),
                                "--module",
                                "creek.json.schema.generator/org.creek.api.json.schema.generator.JsonSchemaGenerator"));

        // If running with Jacoco coverage, find the java agent argument and pass to child process:
        findConvergeAgentCmdLineArg().ifPresent(arg -> cmd.add(1, arg));

        cmd.addAll(List.of(args));

        try {
            final Process executor = new ProcessBuilder().command(cmd).start();

            stdErr = Suppliers.memoize(() -> readAll(executor.getErrorStream()));
            stdOut = Suppliers.memoize(() -> readAll(executor.getInputStream()));
            executor.waitFor(30, TimeUnit.SECONDS);
            return executor.exitValue();
        } catch (final Exception e) {
            throw new AssertionError("Error executing: " + cmd, e);
        }
    }

    private static String readAll(final InputStream stdErr) {
        return new BufferedReader(new InputStreamReader(stdErr, UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    private static String[] minimalArgs(final String... additional) {
        final List<String> args = new ArrayList<>(List.of("--output=some/path"));
        args.addAll(List.of(additional));
        return args.toArray(String[]::new);
    }

    private static Optional<String> findConvergeAgentCmdLineArg() {
        final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getInputArguments().stream()
                .filter(arg -> arg.startsWith("-javaagent"))
                .filter(arg -> arg.contains("org.jacoco.agent"))
                .reduce((first, second) -> first);
    }
}
