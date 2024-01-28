/*
 * Copyright 2022-2024 Creek Contributors (https://github.com/creek-service)
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.creekservice.api.json.schema.generator.GeneratorOptions.OutputLocationStrategy;
import org.creekservice.api.test.util.TestPaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchemaWriterTest {

    @TempDir private Path outputDir;
    @Mock private OutputLocationStrategy outputLocation;
    private SchemaWriter writer;
    private Path expectedOutput;

    @BeforeEach
    void setUp() {
        writer = new SchemaWriter(outputDir, outputLocation);

        final Path outputFile = Paths.get("some.file");
        expectedOutput = outputDir.resolve(outputFile);
        when(outputLocation.outputPath(any())).thenReturn(outputFile);
    }

    @Test
    void shouldPassTypeToStrategy() {
        // Given:
        final JsonSchema<?> schema = new JsonSchema<>(SchemaWriterTest.class, "the schema");

        // When:
        writer.write(schema);

        // Then:
        verify(outputLocation).outputPath(SchemaWriterTest.class);
    }

    @Test
    void shouldWriteSchema() {
        // Given:
        final JsonSchema<?> schema = new JsonSchema<>(SchemaWriterTest.class, "the schema");

        // When:
        writer.write(schema);

        // Then:
        assertThat(
                TestPaths.listDirectory(outputDir).collect(Collectors.toList()),
                hasItem(expectedOutput));
        assertThat(TestPaths.readString(expectedOutput), is("the schema"));
    }

    @Test
    void shouldWriteSchemaForNestedType() {
        // Given:
        final JsonSchema<?> schema = new JsonSchema<>(Nested.class, "the schema");

        // When:
        writer.write(schema);

        // Then:
        assertThat(
                TestPaths.listDirectory(outputDir).collect(Collectors.toList()),
                hasItem(expectedOutput));
        assertThat(TestPaths.readString(expectedOutput), is("the schema"));
    }

    @Test
    void shouldWriteSchemaForLocalType() {
        // Given:
        final class Model {}
        final JsonSchema<?> schema = new JsonSchema<>(Model.class, "the schema");

        // When:
        writer.write(schema);

        // Then:
        assertThat(
                TestPaths.listDirectory(outputDir).collect(Collectors.toList()),
                hasItem(expectedOutput));
        assertThat(TestPaths.readString(expectedOutput), is("the schema"));
    }

    @Test
    void shouldThrowOnFailureToWrite() {
        // Given:
        final JsonSchema<?> schema = new JsonSchema<>(SchemaWriterTest.class, "the schema");
        TestPaths.ensureDirectories(expectedOutput);

        // When:
        final Exception e = assertThrows(RuntimeException.class, () -> writer.write(schema));

        // Then:
        assertThat(e.getMessage(), is("Failed to write schema for " + SchemaWriterTest.class));
        assertThat(e.getCause().getMessage(), containsString("Is a directory"));
    }

    private static final class Nested {}
}
