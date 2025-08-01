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

package org.creekservice.internal.json.schema.generator;

import static java.util.Objects.requireNonNull;

/**
 * Tuple of a type and its schema.
 *
 * @param <T> the Java type of the schema
 */
public final class JsonSchema<T> {

    private final Class<T> type;
    private final String schema;

    /**
     * @param type the type
     * @param schema it's schema
     */
    public JsonSchema(final Class<T> type, final String schema) {
        this.type = requireNonNull(type, "type");
        this.schema = requireNonNull(schema, "schema");
    }

    /**
     * @return the type
     */
    public Class<T> type() {
        return type;
    }

    /**
     * @return it's schema
     */
    public String text() {
        return schema;
    }
}
