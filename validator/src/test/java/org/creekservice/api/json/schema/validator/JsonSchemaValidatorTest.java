/*
 * Copyright 2026 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.api.json.schema.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonSchemaValidatorTest {

    private static final String SIMPLE_SCHEMA_YAML =
            """
            $schema: https://json-schema.org/draft/2020-12/schema
            type: object
            properties:
              name:
                type: string
              age:
                type: integer
            required:
            - name
            """;

    private static final String DURATION_SCHEMA_YAML =
            """
            $schema: https://json-schema.org/draft/2020-12/schema
            type: object
            properties:
              duration:
                type: string
                format: duration
            required:
            - duration
            """;

    @Test
    void shouldParseValidYamlSchema() {
        JsonSchemaValidator.fromSchema(SIMPLE_SCHEMA_YAML);
    }

    @Test
    void shouldParseValidJsonSchema() {
        // Given:
        final String jsonSchema =
                """
                {"$schema":"https://json-schema.org/draft/2020-12/schema",\
                "type":"object","properties":{"name":{"type":"string"}}}\
                """;

        // Then: should not throw:
        JsonSchemaValidator.fromSchema(jsonSchema);
    }

    @Test
    void shouldThrowOnInvalidSchema() {
        assertThrows(
                SchemaValidationException.class,
                () -> JsonSchemaValidator.fromSchema("not: [valid: schema: content: [[["));
    }

    @Test
    void shouldPassValidationForConformingData() {
        // Given:
        final JsonSchemaValidator validator = JsonSchemaValidator.fromSchema(SIMPLE_SCHEMA_YAML);

        // Then: should not throw:
        validator.validate(Map.of("name", "Alice", "age", 30));
    }

    @Test
    void shouldFailValidationForNonConformingData() {
        // Given:
        final JsonSchemaValidator validator = JsonSchemaValidator.fromSchema(SIMPLE_SCHEMA_YAML);

        // When:
        final SchemaValidationException ex =
                assertThrows(
                        SchemaValidationException.class,
                        () -> validator.validate(Map.of("age", "not-an-integer")));

        // Then:
        assertThat(ex.getMessage(), containsString("Validation failed"));
    }

    @Test
    void shouldFailValidationForMissingRequiredField() {
        // Given:
        final JsonSchemaValidator validator = JsonSchemaValidator.fromSchema(SIMPLE_SCHEMA_YAML);

        // When:
        final SchemaValidationException ex =
                assertThrows(
                        SchemaValidationException.class,
                        () -> validator.validate(Map.of("age", 42)));

        // Then:
        assertThat(ex.getMessage(), containsString("Validation failed"));
    }

    @Test
    void shouldAcceptSubSecondDuration() {
        // Schema draft strictly doesn't allow sub-second duration, but this is very limiting.
        // Ongoing discussion will hopefully resolve this in the next draft:
        // See https://github.com/json-schema-org/json-schema-spec/issues/1603

        // Given:
        final JsonSchemaValidator validator = JsonSchemaValidator.fromSchema(DURATION_SCHEMA_YAML);

        // Then: does not throw:
        validator.validate(Map.of("duration", "PT0.5S"));
    }

    @Test
    void shouldEnforceFormatAssertions() {
        // Given:
        final JsonSchemaValidator validator = JsonSchemaValidator.fromSchema(DURATION_SCHEMA_YAML);

        // When:
        final SchemaValidationException ex =
                assertThrows(
                        SchemaValidationException.class,
                        () -> validator.validate(Map.of("duration", "not-a-duration")));

        // Then:
        assertThat(ex.getMessage(), containsString("Validation failed"));
    }
}
