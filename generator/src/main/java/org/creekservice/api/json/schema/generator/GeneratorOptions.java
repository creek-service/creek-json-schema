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

package org.creekservice.api.json.schema.generator;


import java.nio.file.Path;
import java.util.Set;

/** Options to control the {@link JsonSchemaGenerator}. */
public interface GeneratorOptions {

    /**
     * @return If set, the generator will parse and echo its arguments and exit. Useful for testing.
     */
    default boolean echoOnly() {
        return false;
    }

    /** @return The directory to output schema files to. */
    Path outputDirectory();

    /**
     * Allowed modules.
     *
     * <p>By default, schemas are generated for all types annotated with {@link
     * org.creekservice.api.base.annotation.schema.GeneratesSchema}. Specifying one or more allowed
     * modules restricts the returned types that belong to one of the supplied modules.
     *
     * <p>To use this feature the generator must be run from the module path, i.e. under JPMS.
     *
     * <p>Allowed module names can include the glob wildcard {@code *} character.
     *
     * @return allowed modules. If empty, all modules are allowed.
     */
    default Set<String> allowedModules() {
        return Set.of();
    }

    /**
     * Allowed packages for base types.
     *
     * <p>By default, schemas are generated for all types annotated with {@link
     * org.creekservice.api.base.annotation.schema.GeneratesSchema}. Specifying one or more allowed
     * base type packages restricts the returned types to only those under the supplied packages.
     *
     * <p>Allowed package names can include the glob wildcard {@code *} character.
     *
     * @return allowed base type packages. If empty, all packages are allowed.
     */
    default Set<String> allowedBaseTypePackages() {
        return Set.of();
    }

    /**
     * Allowed packages for subtypes.
     *
     * <p>By default, all subtypes are used when generating the schema for a type annotated with
     * {@code @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)}. Specifying one or more allowed subtype
     * packages restricts the subtypes included in the schema to only those under the supplied
     * packages.
     *
     * <p>Allowed package names can include the glob wildcard {@code *} character.
     *
     * @return allowed subtype packages. If empty, all packages are allowed.
     */
    default Set<String> allowedSubTypePackages() {
        return Set.of();
    }
}
