/*
 * Copyright 2022-2026 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.test.types;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import org.creekservice.api.base.annotation.schema.GeneratesSchema;
import org.creekservice.api.base.annotation.schema.JsonSchemaInject;

@SuppressWarnings("unused") // Invoked via reflection
@SuppressFBWarnings
@GeneratesSchema
@JsonSchemaInject(
        "{\"anyOf\":["
                + "{\"required\":[\"uuid\"]},"
                + "{\"required\":[\"withDescription\"]}"
                + "]}")
@Schema(title = "Custom Title")
public record SwaggerModel(
        @Schema(description = "This property has a text description.") String withDescription,
        @Schema(minLength = 1) String nonEmpty,
        @Schema(format = "uuid") String uuid,
        @ArraySchema(minItems = 1, uniqueItems = true) Set<Integer> set) {}
