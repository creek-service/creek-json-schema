/*
 * Copyright 2023-2025 Creek Contributors (https://github.com/creek-service)
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import org.creekservice.api.base.annotation.schema.GeneratesSchema;

@SuppressWarnings("unused") // Invoked via reflection
@GeneratesSchema
public final class OptionalModel {

    @JsonProperty(required = true)
    public String getNonOptional() {
        return "";
    }

    public Optional<String> getOptional() {
        return Optional.empty();
    }

    // Optional and required conflict, but which wins?
    @JsonProperty(required = true)
    public Optional<String> getRequiredOptional() {
        return Optional.empty();
    }
}
