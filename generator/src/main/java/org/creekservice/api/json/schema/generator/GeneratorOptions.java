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

package org.creekservice.api.json.schema.generator;

import java.nio.file.Path;
import java.util.Set;
import org.creekservice.internal.json.schema.generator.output.DirectoryTreeOutputLocationStrategy;

/** Options to control the {@link JsonSchemaGenerator}. */
public interface GeneratorOptions {

    /** Specification of modules and packages to include when scanning for subtypes. */
    interface TypeScanningSpec {
        /**
         * The set of module names used to limit type scanning to only the specified modules.
         *
         * <p>Allowed module names can include the glob wildcard {@code *} character.
         *
         * <p>Default: empty, meaning all modules will be scanned.
         *
         * @return the set of module names in the white list.
         */
        default Set<String> moduleWhiteList() {
            return Set.of();
        }

        /**
         * The set of package name used to limit type scanning to only the specified packages.
         *
         * <p>Allowed package names can include the glob wildcard {@code *} character.
         *
         * <p>Default: empty, meaning all packages will be scanned.
         *
         * @return the set of package names in the white list.
         */
        default Set<String> packageWhiteList() {
            return Set.of();
        }
    }

    /**
     * Configure type scanning for finding types to generate schema for, i.e. types annotated with
     * {@link org.creekservice.api.base.annotation.schema.GeneratesSchema}.
     *
     * <p>By default, the full class and model path are scanned for types to generate schemas for
     * i.e. types annotated with {@link
     * org.creekservice.api.base.annotation.schema.GeneratesSchema}. Scan time can be reduced and
     * unwanted types excluded, e.g. types in dependencies, by configuring the subtype scanning.
     *
     * @return the type scanning config
     */
    default TypeScanningSpec typeScanning() {
        return new TypeScanningSpec() {};
    }

    /**
     * Configure type scanning for finding subtypes, i.e. subtypes of polymorphic types that are
     * part of base types.
     *
     * <p>By default, the full class and model path are scanned for subtypes when generating the
     * schema for polymorphic types that do not define an explicit set of subtypes, i.e. types
     * annotated with {@code @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)}, but not with
     * {@code @JsonSubTypes}. Scan time can be reduced and unwanted subtypes excluded by configuring
     * the subtype scanning.
     *
     * @return the type scanning config
     */
    default TypeScanningSpec subTypeScanning() {
        return new TypeScanningSpec() {};
    }

    /**
     * @return If set, the generator will parse and echo its arguments and exit. Useful for testing.
     */
    default boolean echoOnly() {
        return false;
    }

    /**
     * The base directory under which schema files will be written.
     *
     * <p>The full path to a generated schema file for a specific {@code type} is {@code
     * outputDirectory()}{@code .resolve(}{@link #outputLocationStrategy()}{@code
     * .outputPath(type))}
     *
     * @return The base directory to output schema files to.
     */
    Path outputDirectory();

    /**
     * Strategy to use for naming generated schemas
     *
     * <p>The full path to a generated schema file for a specific {@code type} is {@link
     * #outputDirectory()}{@code .resolve(}{@code outputLocationStrategy()}{@code
     * .outputPath(type))}
     *
     * @return strategy that controls where under the {@link #outputDirectory()} schema files will
     *     be written.
     */
    default OutputLocationStrategy outputLocationStrategy() {
        return new DirectoryTreeOutputLocationStrategy();
    }

    /** Control where generated schemas are output. */
    interface OutputLocationStrategy {
        /**
         * Get the path that the schema file for a specific {@code type} should be written too.
         *
         * @param type the type having its schema generated
         * @return the path the schema should be written too.
         */
        Path outputPath(Class<?> type);
    }
}
