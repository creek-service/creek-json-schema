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

package org.creekservice.test.types;

import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaBool;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInt;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import java.util.Set;
import org.creekservice.api.base.annotation.schema.GeneratesSchema;

@SuppressWarnings("unused") // Invoked via reflection
@GeneratesSchema
// Inject requirement for `thing` OR `listProp`:
@JsonSchemaInject(
        json =
                "{\"anyOf\":["
                        + "{\"required\":[\"uuid\"]},"
                        + "{\"required\":[\"with_description\"]}"
                        + "]}")
@JsonSchemaTitle("Custom Title")
public final class JsonSchemaModel {

    // Add a description:
    @JsonSchemaDescription("This property has a text description.")
    public String getWithDescription() {
        return null;
    }

    // Inject minLength requirement,  i.e. if supplied, it can't be empty:
    @JsonSchemaInject(ints = @JsonSchemaInt(path = "minLength", value = 1))
    public String getNonEmpty() {
        return null;
    }

    // Specify a format:
    @JsonSchemaFormat("uuid")
    public String getUuid() {
        return null;
    }

    // Specify in schema that items are unique and collection is not empty:
    @JsonSchemaInject(
            ints = {@JsonSchemaInt(path = "minItems", value = 1)},
            bools = {@JsonSchemaBool(path = "uniqueItems", value = true)})
    public Set<Integer> getSet() {
        return null;
    }
}
