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

package org.creek.internal.json.schema.generator;

import static org.creek.internal.json.schema.generator.PicoCommandLineParser.parse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.creek.api.json.schema.generator.GeneratorOptions;
import org.junit.jupiter.api.Test;

class PicoCommandLineParserTest {

    @Test
    void shouldReturnEmptyOnHelp() {
        // Given:
        final String[] args = {"--help"};

        // When:
        final Optional<GeneratorOptions> result = parse(args);

        // Then:
        assertThat(result, is(Optional.empty()));
    }

    @Test
    void shouldThrowOnInvalidArgs() {
        // Given:
        final String[] args = {"--unknown"};

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> parse(args));

        // Then:
        assertThat(e.getMessage(), startsWith("Unknown option: '--unknown'"));
    }

    @Test
    void shouldParseMinimalSetWithDefaults() {
        // Given:
        final String[] args = {};

        // When:
        final Optional<GeneratorOptions> result = parse(args);

        // Then:
        assertThat(result.map(GeneratorOptions::echoOnly), is(Optional.of(false)));
    }

    @Test
    void shouldParseEchoOnly() {
        // Given:
        final String[] args = {"--echo-only"};

        // When:
        final Optional<GeneratorOptions> result = parse(args);

        // Then:
        assertThat(result.map(GeneratorOptions::echoOnly), is(Optional.of(true)));
    }
}
