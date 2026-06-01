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

package org.creekservice.test.validator.modulepath;

import java.util.Map;
import org.creekservice.api.json.schema.validator.JsonSchemaValidator;

/**
 * Downstream-consumer simulation that validates YAML schema parsing works on the module path.
 *
 * <p>Run via {@code JavaExec} with only the validator's {@code runtimeClasspath}. If the validator
 * module-info is missing a required dependency (e.g. jackson-dataformat-yaml), this program will
 * fail, catching the bug that unit tests miss.
 */
public final class ValidatorYamlCheck {

    private ValidatorYamlCheck() {}

    public static void main(final String[] args) {
        final String yamlSchema =
                """
                $schema: https://json-schema.org/draft/2020-12/schema
                type: object
                properties:
                  name:
                    type: string
                required:
                - name
                """;

        final JsonSchemaValidator validator = JsonSchemaValidator.fromSchema(yamlSchema);
        validator.validate(Map.of("name", "test"));
    }
}
