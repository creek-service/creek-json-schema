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

package org.creekservice.internal.json.schema.generator.cli;

import static java.lang.System.lineSeparator;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.creekservice.api.base.type.JarVersion;
import org.creekservice.api.json.schema.generator.GeneratorOptions;
import org.creekservice.api.json.schema.generator.JsonSchemaGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public final class PicoCliParser {

    private static final Logger LOGGER = LogManager.getLogger(PicoCliParser.class);

    private PicoCliParser() {}

    public static Optional<GeneratorOptions> parse(final String... args) {
        final Options options = new Options();
        final CommandLine parser = new CommandLine(options);

        try {
            parser.parseArgs(args);

            if (parser.isUsageHelpRequested()) {
                LOGGER.info(parser.getUsageMessage());
                return Optional.empty();
            }

            if (parser.isVersionHelpRequested()) {
                LOGGER.info(
                        "JsonSchemaGenerator: "
                                + JarVersion.jarVersion(JsonSchemaGenerator.class)
                                        .orElse("unknown"));
                return Optional.empty();
            }

            return Optional.of(options);
        } catch (final Exception e) {
            throw new InvalidArgumentsException(parser.getUsageMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    @Command(name = "JsonSchemaGenerator", mixinStandardHelpOptions = true)
    private static class Options implements GeneratorOptions {
        @Option(
                names = {"-e", "--echo-only"},
                hidden = true)
        private boolean echoOnly;

        @Option(
                names = {"-o", "--output-directory"},
                required = true,
                description = "The directory the schemas will be written to.")
        private Path outputDirectory;

        @Option(
                names = {"-m", "--allowed-module"},
                description = {
                    "an optional module name to limit the base types to generate schemas for.",
                    "Only types under the supplied modules will be processed.",
                    "Specify multiple modules with multiple --allowed-modules args."
                })
        private final Set<String> allowedModules = Set.of();

        @Option(
                names = {"-btp", "--allowed-base-type-package"},
                description = {
                    "an optional package name to limit the base types to generate schemas for.",
                    "Only types under the supplied packages will be processed.",
                    "Package names can include the '*' wildcard.",
                    "Specify multiple packages with multiple --allowed-base-package args."
                })
        private final Set<String> allowedBaseTypePackages = Set.of();

        @Option(
                names = {"-stp", "--allowed-sub-type-package"},
                description = {
                    "an optional package name to limit the subtypes included in generate schemas.",
                    "Only subtypes under the supplied packages will be included.",
                    "Package names can include the '*' wildcard.",
                    "Specify multiple packages with multiple --allowed-sub-package args."
                })
        private final Set<String> allowedSubTypePackages = Set.of();

        @Override
        public boolean echoOnly() {
            return echoOnly;
        }

        @Override
        public Path outputDirectory() {
            return outputDirectory;
        }

        @Override
        public Set<String> allowedModules() {
            return Set.copyOf(allowedModules);
        }

        @Override
        public Set<String> allowedBaseTypePackages() {
            return Set.copyOf(allowedBaseTypePackages);
        }

        @Override
        public Set<String> allowedSubTypePackages() {
            return Set.copyOf(allowedSubTypePackages);
        }

        @Override
        public String toString() {
            return "--output-directory="
                    + outputDirectory
                    + lineSeparator()
                    + "--allowed-modules="
                    + formatAllowed(allowedModules)
                    + lineSeparator()
                    + "--allowed-base-type-packages="
                    + formatAllowed(allowedBaseTypePackages)
                    + lineSeparator()
                    + "--allowed-sub-type-packages="
                    + formatAllowed(allowedSubTypePackages);
        }

        private static String formatAllowed(final Set<String> allowed) {
            return allowed.isEmpty() ? "<ANY>" : allowed.toString();
        }
    }

    private static class InvalidArgumentsException extends RuntimeException {
        InvalidArgumentsException(final String usageMessage, final Throwable cause) {
            super(cause.getMessage() + lineSeparator() + usageMessage, cause);
        }
    }
}
