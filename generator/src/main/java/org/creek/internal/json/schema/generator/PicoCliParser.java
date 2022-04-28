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

package org.creek.internal.json.schema.generator;

import static java.lang.System.lineSeparator;
import static org.creek.api.base.type.JarVersion.jarVersion;

import java.nio.file.Path;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.creek.api.json.schema.generator.GeneratorOptions;
import org.creek.api.json.schema.generator.JsonSchemaGenerator;
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
                                + jarVersion(JsonSchemaGenerator.class).orElse("unknown"));
                return Optional.empty();
            }

            return Optional.of(options);
        } catch (final Exception e) {
            throw new InvalidArgumentsException(parser.getUsageMessage(), e);
        }
    }

    @SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
    @Command(name = "JsonSchemaGenerator", mixinStandardHelpOptions = true)
    private static class Options implements GeneratorOptions {
        @Option(
                names = {"-e", "--echo-only"},
                hidden = true)
        private boolean echoOnly;

        @Option(
                names = {"-o", "--output"},
                required = true,
                description = "The directory the schemas will be written to.")
        private Path outputDirectory;

        @Option(
                names = {"-p", "--package"},
                description =
                        "an optional package name to limit the types to generate a schema for. "
                                + "Only types under the supplied package will be processed.")
        private Optional<String> packageName;

        @Override
        public boolean echoOnly() {
            return echoOnly;
        }

        @Override
        public Path outputDirectory() {
            return outputDirectory;
        }

        @Override
        public Optional<String> packageName() {
            return packageName;
        }

        @Override
        public String toString() {
            return "--output="
                    + outputDirectory
                    + lineSeparator()
                    + "--package="
                    + packageName.orElse("<Not Set>");
        }
    }

    private static class InvalidArgumentsException extends RuntimeException {
        InvalidArgumentsException(final String usageMessage, final Throwable cause) {
            super(cause.getMessage() + lineSeparator() + usageMessage, cause);
        }
    }
}
