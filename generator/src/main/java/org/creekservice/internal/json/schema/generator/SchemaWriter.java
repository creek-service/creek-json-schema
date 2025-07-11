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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.creekservice.api.json.schema.generator.GeneratorOptions.OutputLocationStrategy;

/** Writes schema to a YAML file */
public final class SchemaWriter {

    private static final Logger LOGGER = LogManager.getLogger(SchemaWriter.class);

    private final Path rootDirectory;
    private final OutputLocationStrategy outputLocation;

    /**
     * @param rootDirectory the root directory under which schemas are written
     * @param outputLocation strategy used to determine where under {@code rootDirectory} generated
     *     schema should be written.
     */
    public SchemaWriter(final Path rootDirectory, final OutputLocationStrategy outputLocation) {
        this.rootDirectory = requireNonNull(rootDirectory, "rootDirectory");
        this.outputLocation = requireNonNull(outputLocation, "outputLocation");
    }

    /**
     * Persist the supplied schema to disk.
     *
     * @param schema the schema to persist.
     */
    public void write(final JsonSchema<?> schema) {
        final Class<?> type = schema.type();
        try {
            final Path path = rootDirectory.resolve(outputLocation.outputPath(type));

            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(path, schema.text().getBytes(StandardCharsets.UTF_8));

            final String name =
                    type.getCanonicalName() == null
                            ? type.getSimpleName()
                            : type.getCanonicalName();
            LOGGER.info("Wrote {}'s schema to {}", name, path.toUri());
        } catch (final Exception e) {
            throw new GenerateSchemaException("Failed to write schema for " + type, e);
        }
    }

    private static class GenerateSchemaException extends RuntimeException {
        GenerateSchemaException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
    }
}
