/*
 * Copyright 2022-2023 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.internal.json.schema.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.stream.Collectors;
import org.creekservice.api.test.util.TestPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SchemaWriterTest {

    @TempDir private Path outputDir;
    private SchemaWriter writer;

    @BeforeEach
    void setUp() {
        writer = new SchemaWriter(outputDir);
    }

    @Test
    void shouldWriteSchema() {
        // Given:
        final JsonSchema<?> schema = new JsonSchema<>(SchemaWriterTest.class, "the schema");

        // When:
        writer.write(schema);

        // Then:
        final Path expected =
                outputDir.resolve(
                        "org.creekservice.internal.json.schema.generator.schema_writer_test.yml");
        assertThat(
                TestPaths.listDirectory(outputDir).collect(Collectors.toList()), hasItem(expected));
        assertThat(TestPaths.readString(expected), is("the schema"));
    }

    @Test
    void shouldWriteSchemaForNestedType() {
        // Given:
        final JsonSchema<?> schema = new JsonSchema<>(Nested.class, "the schema");

        // When:
        writer.write(schema);

        // Then:
        final Path expected =
                outputDir.resolve(
                        "org.creekservice.internal.json.schema.generator.schema_writer_test$nested.yml");
        assertThat(
                TestPaths.listDirectory(outputDir).collect(Collectors.toList()), hasItem(expected));
        assertThat(TestPaths.readString(expected), is("the schema"));
    }

    @Test
    void shouldWriteSchemaForLocalType() {
        // Given:
        final class Model {}
        final JsonSchema<?> schema = new JsonSchema<>(Model.class, "the schema");

        // When:
        writer.write(schema);

        // Then:
        final Path expected =
                outputDir.resolve(
                        "org.creekservice.internal.json.schema.generator.schema_writer_test$1_model.yml");
        assertThat(
                TestPaths.listDirectory(outputDir).collect(Collectors.toList()), hasItem(expected));
        assertThat(TestPaths.readString(expected), is("the schema"));
    }

    @Test
    void shouldThrowOnFailureToWrite() {
        // Given:
        final JsonSchema<?> schema = new JsonSchema<>(SchemaWriterTest.class, "the schema");
        TestPaths.ensureDirectories(
                outputDir.resolve(
                        "org.creekservice.internal.json.schema.generator.schema_writer_test.yml"));

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> writer.write(schema));

        // Then:
        assertThat(e.getMessage(), is("Failed to write schema for " + SchemaWriterTest.class));
        assertThat(e.getCause().getMessage(), containsString("Is a directory"));
    }

    private static final class Nested {}
}
