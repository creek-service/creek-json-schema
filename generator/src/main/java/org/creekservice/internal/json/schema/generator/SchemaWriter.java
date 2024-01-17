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

package org.creekservice.internal.json.schema.generator;

import static java.util.Objects.requireNonNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.creekservice.api.base.type.schema.GeneratedSchemas;

/** Writes schema to a YAML file */
public final class SchemaWriter {

    private static final Logger LOGGER = LogManager.getLogger(SchemaWriter.class);

    private final Path outputDir;

    /**
     * @param outputDir the directory into which to write the schema files
     */
    public SchemaWriter(final Path outputDir) {
        this.outputDir = requireNonNull(outputDir, "outputDir");
    }

    /**
     * Persist the supplied schema to disk.
     *
     * @param schema the schema to persist.
     */
    public void write(final JsonSchema<?> schema) {
        final Class<?> type = schema.type();
        try {
            final Path fileName =
                    GeneratedSchemas.schemaFileName(type, GeneratedSchemas.yamlExtension());
            final Path path = outputDir.resolve(fileName);

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
