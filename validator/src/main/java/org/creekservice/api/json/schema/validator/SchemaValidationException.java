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

/** Thrown when JSON data fails schema validation, or when a schema cannot be parsed. */
public final class SchemaValidationException extends RuntimeException {

    /**
     * Create an exception with a message.
     *
     * @param message description of the failure.
     * @return a new exception.
     */
    public static SchemaValidationException of(final String message) {
        return new SchemaValidationException(message, null);
    }

    /**
     * Create an exception with a message and a cause.
     *
     * @param message description of the failure.
     * @param cause the underlying cause.
     * @return a new exception.
     */
    public static SchemaValidationException of(final String message, final Throwable cause) {
        return new SchemaValidationException(message, cause);
    }

    private SchemaValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
