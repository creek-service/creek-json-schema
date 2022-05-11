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


import java.lang.management.ManagementFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.creekservice.api.base.schema.GeneratesSchemas;
import org.creekservice.api.base.type.JarVersion;
import org.creekservice.internal.json.schema.generator.SchemaGenerator;
import org.creekservice.internal.json.schema.generator.SchemaWriter;
import org.creekservice.internal.json.schema.generator.cli.PicoCliParser;

/**
 * Entry point for generating JSON schemas from types annotated with {@link
 * org.creekservice.api.base.annotation.schema.GeneratesSchema}.
 */
public final class JsonSchemaGenerator {

    private static final Logger LOGGER = LogManager.getLogger(JsonSchemaGenerator.class);

    private JsonSchemaGenerator() {}

    /**
     * Generates JSON schemas for types annotated with the {@code
     * org.creekservice.api.base.annotation.schema.GeneratesSchema} annotation.
     *
     * <p>See {@link org.creekservice.internal.json.schema.generator.cli.PicoCliParser} for details
     * of supported command line parameters.
     *
     * @param args the command line parameters.
     */
    public static void main(final String... args) {
        try {
            PicoCliParser.parse(args).ifPresent(JsonSchemaGenerator::generate);
        } catch (final Exception e) {
            LOGGER.fatal(e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Generates JSON schemas for types annotated with the {@code
     * org.creekservice.api.base.annotation.schema.GeneratesSchema} annotation.
     *
     * @param options the options used to customise the what, how and where schemas are generated.
     */
    public static void generate(final GeneratorOptions options) {
        if (options.echoOnly()) {
            echo(options);
            return;
        }

        final Set<Class<?>> types =
                GeneratesSchemas.scanner()
                        .withAllowedPackages(options.allowedBaseTypePackages())
                        .withAllowedModules(options.allowedModules())
                        .scan();

        final SchemaGenerator generator = new SchemaGenerator(options.allowedSubTypePackages());
        final SchemaWriter writer = new SchemaWriter(options.outputDirectory());
        generator.registerSubTypes(types);
        types.stream().map(generator::generateSchema).forEach(writer::write);
        LOGGER.info("Wrote {} schemas", types.size());
    }

    private static void echo(final GeneratorOptions options) {
        LOGGER.info(
                "JsonSchemaGenerator: "
                        + JarVersion.jarVersion(JsonSchemaGenerator.class).orElse("unknown"));
        LOGGER.info(classPath());
        LOGGER.info(modulePath());
        LOGGER.info(options);
    }

    private static String classPath() {
        final String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        return classPath.isEmpty() ? "" : "--class-path=" + classPath;
    }

    private static String modulePath() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(arg -> arg.startsWith("--module-path") || arg.startsWith("-p"))
                .collect(Collectors.joining(" "));
    }
}
