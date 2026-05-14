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

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.creekservice.internal.json.schema.validator.NetworkntJsonSchemaValidator;

/**
 * Validates JSON data against a JSON Schema.
 *
 * <p>Wraps the networknt json-schema-validator library, keeping its types out of the public API.
 * Format assertions are enabled, matching real-world ISO 8601 behaviour (e.g. {@code "PT0.5S"} is
 * accepted for sub-second durations).
 */
public final class JsonSchemaValidator {

    private final NetworkntJsonSchemaValidator internal;

    private JsonSchemaValidator(final NetworkntJsonSchemaValidator internal) {
        this.internal = requireNonNull(internal, "internal");
    }

    /**
     * Create a validator from a YAML or JSON schema string.
     *
     * <p>Because YAML is a superset of JSON, this method accepts either format.
     *
     * @param schema the schema content, in YAML or JSON format.
     * @return a new validator instance.
     * @throws SchemaValidationException if the schema cannot be parsed.
     */
    public static JsonSchemaValidator fromSchema(final String schema) {
        return new JsonSchemaValidator(NetworkntJsonSchemaValidator.fromSchema(schema));
    }

    /**
     * Validate the supplied object properties against the schema.
     *
     * @param objectProperties the object's properties, as returned by Jackson deserialisation.
     * @throws SchemaValidationException if validation fails.
     */
    public void validate(final Map<String, ?> objectProperties) {
        internal.validate(objectProperties);
    }
}
