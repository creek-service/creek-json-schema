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

package org.creekservice.internal.json.schema.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.creekservice.api.json.schema.validator.SchemaValidationException;

/** Internal networknt-based implementation of JSON schema validation. */
public final class NetworkntJsonSchemaValidator {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final SchemaRegistry REGISTRY =
            SchemaRegistry.withDefaultDialect(
                    SpecificationVersion.DRAFT_2020_12,
                    b ->
                            b.schemaRegistryConfig(
                                    SchemaRegistryConfig.builder()
                                            .formatAssertionsEnabled(true)
                                            .strict("duration", false)
                                            .build()));

    private final Schema schema;

    private NetworkntJsonSchemaValidator(final Schema schema) {
        this.schema = schema;
    }

    /**
     * Create a validator from a YAML or JSON schema string.
     *
     * @param schemaContent the schema content.
     * @return a new validator instance.
     * @throws SchemaValidationException if the schema cannot be parsed.
     */
    public static NetworkntJsonSchemaValidator fromSchema(final String schemaContent) {
        try {
            final InputStream inputStream =
                    new ByteArrayInputStream(schemaContent.getBytes(StandardCharsets.UTF_8));
            return new NetworkntJsonSchemaValidator(
                    REGISTRY.getSchema(inputStream, InputFormat.YAML));
        } catch (final SchemaValidationException e) {
            throw e;
        } catch (final Exception e) {
            throw SchemaValidationException.of("Failed to parse schema", e);
        }
    }

    /**
     * Validate the supplied object properties against the schema.
     *
     * @param objectProperties the object's properties to validate.
     * @throws SchemaValidationException if validation fails.
     */
    public void validate(final Map<String, ?> objectProperties) {
        try {
            final JsonNode node = JSON_MAPPER.valueToTree(objectProperties);
            final List<Error> errors = schema.validate(node);
            if (!errors.isEmpty()) {
                final String errorMsg =
                        errors.stream().map(Error::getMessage).collect(Collectors.joining(", "));
                throw SchemaValidationException.of("Validation failed: " + errorMsg);
            }
        } catch (final SchemaValidationException e) {
            throw e;
        } catch (final Exception e) {
            throw SchemaValidationException.of("Validation error", e);
        }
    }
}
