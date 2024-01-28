/*
 * Copyright 2024 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.internal.json.schema.generator.output;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlatDirectoryOutputLocationStrategyTest {

    private FlatDirectoryOutputLocationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new FlatDirectoryOutputLocationStrategy();
    }

    @Test
    void shouldReturnPath() {
        assertThat(
                strategy.outputPath(FlatDirectoryOutputLocationStrategyTest.class).toString(),
                is(
                        "org.creekservice.internal.json.schema.generator.output.FlatDirectoryOutputLocationStrategyTest.yml"));
    }

    @Test
    void shouldWorkSchemaForNestedType() {
        assertThat(
                strategy.outputPath(Nested.class).toString(),
                is(
                        "org.creekservice.internal.json.schema.generator.output.FlatDirectoryOutputLocationStrategyTest$Nested.yml"));
    }

    @Test
    void shouldWorkSchemaForLocalType() {
        // Given:
        final class Model {}

        // Then:
        assertThat(
                strategy.outputPath(Model.class).toString(),
                is(
                        "org.creekservice.internal.json.schema.generator.output.FlatDirectoryOutputLocationStrategyTest$1Model.yml"));
    }

    private static final class Nested {}
}
