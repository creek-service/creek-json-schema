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

package org.creekservice.test.types

import com.fasterxml.jackson.annotation.JsonProperty
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDefault
import org.creekservice.api.base.annotation.schema.GeneratesSchema

@Suppress("unused")
@GeneratesSchema
class KotlinModel(
    @get:JsonProperty(defaultValue = PROP3_DEFAULT_VAL) val prop3: String = PROP3_DEFAULT_VAL
) {

    companion object {
        const val PROP3_DEFAULT_VAL = "another default"
    }

    @JsonProperty(required = true)
    fun getProp1(): String {return "";}

    @JsonSchemaDefault("a default value")
    var prop2: String? = null
}