/*
 * Copyright 2022 Creek Contributors (https://github.com/creek-service)
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

package org.creek.api.json.schema.generator;


import java.nio.file.Path;
import java.util.Optional;

/** Options to control the {@link JsonSchemaGenerator}. */
public interface GeneratorOptions {

    /**
     * @return If set, the generator will parse and echo its arguments and exit. Useful for testing.
     */
    boolean echoOnly();

    /** @return The directory to output schema files to. */
    Path outputDirectory();

    /**
     * @return Optional package name to limit the types to generate a schema for. Only types under
     *     the supplied package will be processed.
     */
    Optional<String> packageName();
}
