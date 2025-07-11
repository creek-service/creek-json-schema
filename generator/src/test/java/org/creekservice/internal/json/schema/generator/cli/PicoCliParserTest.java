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

package org.creekservice.internal.json.schema.generator.cli;

import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.creekservice.api.json.schema.generator.GeneratorOptions;
import org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;
import org.creekservice.internal.json.schema.generator.output.DirectoryTreeOutputLocationStrategy;
import org.creekservice.internal.json.schema.generator.output.FlatDirectoryOutputLocationStrategy;
import org.junit.jupiter.api.Test;

class PicoCliParserTest {

    @Test
    void shouldReturnEmptyOnHelp() {
        // Given:
        final String[] args = {"--help"};

        // When:
        final Optional<?> result = PicoCliParser.parse(args);

        // Then:
        assertThat(result, is(Optional.empty()));
    }

    @Test
    void shouldReturnEmptyOnVersion() {
        // Given:
        final String[] args = {"--version"};

        // When:
        final Optional<?> result = PicoCliParser.parse(args);

        // Then:
        assertThat(result, is(Optional.empty()));
    }

    @Test
    void shouldThrowOnInvalidArgs() {
        // Given:
        final String[] args = minimalArgs("--unknown");

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> PicoCliParser.parse(args));

        // Then:
        assertThat(e.getMessage(), startsWith("Unknown option: '--unknown'"));
    }

    @Test
    void shouldParseMinimalSetWithDefaults() {
        // Given:
        final String[] args = minimalArgs();

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(
                result.map(GeneratorOptions::outputDirectory),
                is(Optional.of(Paths.get("some/path"))));
        assertThat(result.map(GeneratorOptions::echoOnly), is(Optional.of(false)));
        assertThat(
                result.orElseThrow().outputLocationStrategy(),
                is(instanceOf(DirectoryTreeOutputLocationStrategy.class)));
    }

    @Test
    void shouldParseOutputStrategy() {
        // Given:
        final String[] args = minimalArgs("--output-strategy=flatDirectory");

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(
                result.orElseThrow().outputLocationStrategy(),
                is(instanceOf(FlatDirectoryOutputLocationStrategy.class)));
    }

    @Test
    void shouldParseEchoOnly() {
        // Given:
        final String[] args = minimalArgs("--echo-only");

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(result.map(GeneratorOptions::echoOnly), is(Optional.of(true)));
    }

    @Test
    void shouldParseTypeScanningAllowedModule() {
        // Given:
        final String[] args =
                minimalArgs("--type-scanning-allowed-module=some.module", "-m=another.module");

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(
                result.map(GeneratorOptions::typeScanning).map(TypeScanningSpec::moduleWhiteList),
                is(Optional.of(Set.of("some.module", "another.module"))));
    }

    @Test
    void shouldParseTypeScanningAllowedPackage() {
        // Given:
        final String[] args =
                minimalArgs(
                        "--type-scanning-allowed-package=some.package.name", "-p=another.package");

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(
                result.map(GeneratorOptions::typeScanning).map(TypeScanningSpec::packageWhiteList),
                is(Optional.of(Set.of("some.package.name", "another.package"))));
    }

    @Test
    void shouldParseSubtypeScanningAllowedModule() {
        // Given:
        final String[] args =
                minimalArgs("--subtype-scanning-allowed-module=some.module", "-sm=another.module");

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(
                result.map(GeneratorOptions::subTypeScanning)
                        .map(TypeScanningSpec::moduleWhiteList),
                is(Optional.of(Set.of("some.module", "another.module"))));
    }

    @Test
    void shouldParseSubtypeScanningAllowedPackage() {
        // Given:
        final String[] args =
                minimalArgs(
                        "--subtype-scanning-allowed-package=some.package.name",
                        "-sp=another.package");

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(
                result.map(GeneratorOptions::subTypeScanning)
                        .map(TypeScanningSpec::packageWhiteList),
                is(Optional.of(Set.of("some.package.name", "another.package"))));
    }

    @Test
    void shouldImplementToStringOnReturnedOptions() {
        // Given:
        final String[] args = minimalArgs("-m=some.module");

        // When:
        final Optional<GeneratorOptions> result = PicoCliParser.parse(args);

        // Then:
        assertThat(
                result.map(Object::toString),
                is(
                        Optional.of(
                                "--output-directory=some/path"
                                        + lineSeparator()
                                        + "--output-strategy=directoryTree"
                                        + lineSeparator()
                                        + "--type-scanning-allowed-modules=[some.module]"
                                        + lineSeparator()
                                        + "--type-scanning-allowed-packages=<ANY>"
                                        + lineSeparator()
                                        + "--subtype-scanning-allowed-modules=<ANY>"
                                        + lineSeparator()
                                        + "--subtype-scanning-allowed-packages=<ANY>")));
    }

    private static String[] minimalArgs(final String... additional) {
        final List<String> args = new ArrayList<>(List.of("--output-directory=some/path"));
        args.addAll(List.of(additional));
        return args.toArray(String[]::new);
    }
}
