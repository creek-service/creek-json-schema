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

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import org.creekservice.api.base.annotation.VisibleForTesting;
import org.creekservice.api.base.type.temporal.Clock;
import org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;

/** Generates a YAML schema file. */
public final class SchemaGenerator {

    private final ObjectMapper mapper =
            JsonMapper.builder(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
                    .addModule(new Jdk8Module())
                    // Ensure consistent ordering of properties in the schema:
                    // Otherwise, ordering can change from one run of the application to the next.
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .build();

    private final JsonSchemaGenerator generator;
    private final TypeScanningSpec subtypeScanning;
    private final Clock clock;

    /**
     * @param subtypeScanning config for subtype scanning.
     */
    public SchemaGenerator(final TypeScanningSpec subtypeScanning) {
        this(subtypeScanning, Instant::now);
    }

    @VisibleForTesting
    SchemaGenerator(final TypeScanningSpec subtypeScanning, final Clock clock) {
        this.subtypeScanning = requireNonNull(subtypeScanning, "subtypeScanning");
        this.generator = JsonSchemaGeneratorFactory.createGenerator(mapper, subtypeScanning);
        this.clock = requireNonNull(clock, "clock");
    }

    /**
     * Find and register any polymorphic subtypes required by the supplied {@code types} that need
     * to be registered.
     *
     * @param types the types to inspect, i.e. the types you intend to pass to {@link
     *     #generateSchema}.
     */
    public void registerSubTypes(final Collection<Class<?>> types) {
        PolymorphicTypes.findPolymorphicTypes(types, subtypeScanning, mapper).stream()
                .map(PolymorphicTypes.PolymorphicType::subTypes)
                .flatMap(Set::stream)
                .forEach(mapper::registerSubtypes);
    }

    /**
     * Generate the Yaml schema for the supplied {@code type}.
     *
     * @param type the type to generate a schema for.
     * @param <T> the type to generate a schema for.
     * @return the schema
     */
    public <T> JsonSchema<T> generateSchema(final Class<T> type) {
        try {
            final JsonNode jsonSchema = generator.generateJsonSchema(type);
            final String yaml =
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
            return new JsonSchema<>(type, injectVersionTimestamp(yaml));
        } catch (final Exception e) {
            throw new SchemaGeneratorException(
                    "Failed to generate schema for " + type.getCanonicalName(), e);
        }
    }

    private String injectVersionTimestamp(final String yaml) {
        return yaml.replaceFirst(
                "---", "---" + lineSeparator() + "# timestamp=" + clock.get().toEpochMilli());
    }

    private static final class SchemaGeneratorException extends RuntimeException {

        SchemaGeneratorException(final String msg, final Throwable cause) {
            super(msg, cause);
        }
    }
}
