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

package org.creekservice.test.types.more;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.creekservice.api.base.annotation.schema.GeneratesSchema;

@SuppressWarnings("unused") // Invoked via reflection
@GeneratesSchema
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PolymorphicModel.SubType1.class, name = "type_1"),
    @JsonSubTypes.Type(value = PolymorphicModel.SubType2.class, name = "type_2")
})
public interface PolymorphicModel {

    class SubType1 implements PolymorphicModel {
        public String getProp1() {
            return null;
        }
    }

    class SubType2 implements PolymorphicModel {
        public String getProp2() {
            return null;
        }
    }
}
