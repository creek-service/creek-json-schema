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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Writes schema to a file */
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
            final Path fileName = schemaFileName(type);
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
            LOGGER.info("Wrote {}'s schema to {}", name, path);
        } catch (final Exception e) {
            throw new GenerateSchemaException("Failed to write schema for " + type, e);
        }
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "False positive")
    private static Path schemaFileName(final Class<?> type) {
        final String name =
                type.getName()
                        .replaceAll("([A-Z])", "_$1")
                        .replaceFirst("_", "")
                        .replaceAll("\\$_", "\\$")
                        .toLowerCase();

        return Paths.get(name + ".yml");
    }

    private static class GenerateSchemaException extends RuntimeException {
        GenerateSchemaException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
    }
}
