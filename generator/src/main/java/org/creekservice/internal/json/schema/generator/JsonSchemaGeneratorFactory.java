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

import static org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaDraft;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import com.kjetland.jackson.jsonSchema.SubclassesResolver;
import com.kjetland.jackson.jsonSchema.SubclassesResolverImpl;
import io.github.classgraph.ClassGraph;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.creekservice.internal.json.schema.generator.mixin.Instant;

final class JsonSchemaGeneratorFactory {

    private JsonSchemaGeneratorFactory() {}

    static JsonSchemaGenerator createGenerator(
            final ObjectMapper mapper, final TypeScanningSpec subtypeScanning) {
        return new JsonSchemaGenerator(mapper, createConfig(subtypeScanning));
    }

    private static JsonSchemaConfig createConfig(final TypeScanningSpec subtypeScanning) {
        final JsonSchemaConfig vanilla =
                JsonSchemaConfig.vanillaJsonSchemaDraft4()
                        .withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07);

        return JsonSchemaConfig.create(
                        vanilla.autoGenerateTitleForProperties(),
                        Optional.empty(),
                        vanilla.useOneOfForOption(),
                        vanilla.useOneOfForNullables(),
                        vanilla.usePropertyOrdering(),
                        vanilla.hidePolymorphismTypeProperty(),
                        // Warnings generate too much noise:
                        true,
                        vanilla.useMinLengthForNotNull(),
                        vanilla.useTypeIdForDefinitionName(),
                        formatMapping(),
                        vanilla.useMultipleEditorSelectViaProperty(),
                        Set.of(),
                        classTypeReMapping(),
                        Map.of(),
                        subTypeResolver(subtypeScanning),
                        vanilla.failOnUnknownProperties(),
                        null)
                .withJsonSchemaDraft(JsonSchemaDraft.DRAFT_07);
    }

    private static SubclassesResolver subTypeResolver(final TypeScanningSpec subtypeScanning) {
        final ClassGraph classGraph =
                new ClassGraph()
                        .ignoreClassVisibility()
                        .enableClassInfo()
                        .acceptModules(subtypeScanning.moduleWhiteList().toArray(String[]::new))
                        .acceptPackages(subtypeScanning.packageWhiteList().toArray(String[]::new));

        return new SubclassesResolverImpl().withClassGraph(classGraph);
    }

    /** Format mappings add a "format" node to the schema for specific types. */
    private static Map<String, String> formatMapping() {
        return Map.of(
                URI.class.getName(), "uri",
                LocalDate.class.getName(), "date",
                LocalTime.class.getName(), "time",
                LocalDateTime.class.getName(), "date-time",
                ZonedDateTime.class.getName(), "date-time");
    }

    /** Overrides the type used in the schema for specific types. */
    private static Map<Class<?>, Class<?>> classTypeReMapping() {
        return Map.of(
                URI.class, String.class,
                LocalDate.class, String.class,
                LocalTime.class, String.class,
                LocalDateTime.class, String.class,
                ZonedDateTime.class, String.class,
                java.time.Instant.class, Instant.class);
    }
}
