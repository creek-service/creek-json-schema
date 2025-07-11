/*
 * Copyright 2022-2025 Creek Contributors (https://github.com/creek-service)
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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import org.creekservice.api.base.annotation.schema.GeneratesSchema;

@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"}) // Invoked via reflection
@GeneratesSchema
public class JacksonModel {

    public JacksonModel(
            @JsonProperty(value = "required_prop", required = true) final String requiredProp,
            @JsonProperty("optional_prop") final Optional<String> optionalProp) {}

    @JsonIgnore
    public String getIgnoredProp() {
        return null;
    }

    @JsonGetter("required_prop")
    public String required() {
        return null;
    }

    @JsonGetter("optional_prop")
    public Optional<String> optional() {
        return Optional.empty();
    }
}
