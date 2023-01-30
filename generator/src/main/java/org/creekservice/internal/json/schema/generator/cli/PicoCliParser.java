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

package org.creekservice.internal.json.schema.generator.cli;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.creekservice.api.base.type.JarVersion;
import org.creekservice.api.json.schema.generator.GeneratorOptions;
import org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;
import org.creekservice.api.json.schema.generator.JsonSchemaGenerator;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Cli parser that leverages the PicoCli library. */
public final class PicoCliParser {

    private static final Logger LOGGER = LogManager.getLogger(PicoCliParser.class);

    private PicoCliParser() {}

    /**
     * Parse the supplied {@code args}.
     *
     * @param args the args to parse
     * @return the parsed args, or {@code empty} if the args have been handled and the app should
     *     exit.
     */
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

    private static final class TypeScanning implements TypeScanningSpec {

        private final Set<String> moduleWhiteList;
        private final Set<String> packageWhiteList;

        TypeScanning(
                final Collection<String> moduleWhiteList,
                final Collection<String> packageWhiteList) {
            this.moduleWhiteList = Set.copyOf(requireNonNull(moduleWhiteList, "moduleWhiteList"));
            this.packageWhiteList =
                    Set.copyOf(requireNonNull(packageWhiteList, "packageWhiteList"));
        }

        @Override
        public Set<String> moduleWhiteList() {
            return moduleWhiteList;
        }

        @Override
        public Set<String> packageWhiteList() {
            return packageWhiteList;
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
                names = {"-m", "--type-scanning-allowed-module"},
                description = {
                    "Optional module name(s) to limit scanning for @GeneratesSchema annotated types.",
                    "Only types under the supplied modules will be scanned.",
                    "Module names can include the '*' wildcard.",
                    "Specify multiple modules with multiple --type-scanning-allowed-module args."
                })
        private final Set<String> typeScanningModuleWhiteList = Set.of();

        @Option(
                names = {"-p", "--type-scanning-allowed-package"},
                description = {
                    "Optional package name(s) to limit scanning for @GeneratesSchema annotated types.",
                    "Only types under the supplied packages will be processed.",
                    "Package names can include the '*' wildcard.",
                    "Specify multiple packages with multiple --type-scanning-allowed-package args."
                })
        private final Set<String> typeScanningPackageWhiteList = Set.of();

        @Option(
                names = {"-sm", "--subtype-scanning-allowed-module"},
                description = {
                    "Optional module name(s) to limit scanning for subtypes of polymorphic types.",
                    "Only subtypes under the supplied modules will be scanned.",
                    "Module names can include the '*' wildcard.",
                    "Specify multiple modules with multiple --subtype-scanning-allowed-module args."
                })
        private final Set<String> subtypeScanningModuleWhiteList = Set.of();

        @Option(
                names = {"-sp", "--subtype-scanning-allowed-package"},
                description = {
                    "Optional package name(s) to limit scanning for subtypes of polymorphic types.",
                    "Only subtypes under the supplied packages will be included.",
                    "Package names can include the '*' wildcard.",
                    "Specify multiple packages with multiple --subtype-scanning-allowed-package args."
                })
        private final Set<String> subtypeScanningPackageWhiteList = Set.of();

        @Override
        public TypeScanningSpec typeScanning() {
            return new TypeScanning(typeScanningModuleWhiteList, typeScanningPackageWhiteList);
        }

        @Override
        public TypeScanningSpec subTypeScanning() {
            return new TypeScanning(
                    subtypeScanningModuleWhiteList, subtypeScanningPackageWhiteList);
        }

        @Override
        public boolean echoOnly() {
            return echoOnly;
        }

        @Override
        public Path outputDirectory() {
            return outputDirectory;
        }

        @Override
        public String toString() {
            return "--output-directory="
                    + outputDirectory
                    + lineSeparator()
                    + "--type-scanning-allowed-modules="
                    + formatAllowed(typeScanningModuleWhiteList)
                    + lineSeparator()
                    + "--type-scanning-allowed-packages="
                    + formatAllowed(typeScanningPackageWhiteList)
                    + lineSeparator()
                    + "--subtype-scanning-allowed-modules="
                    + formatAllowed(subtypeScanningModuleWhiteList)
                    + lineSeparator()
                    + "--subtype-scanning-allowed-packages="
                    + formatAllowed(subtypeScanningPackageWhiteList);
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
