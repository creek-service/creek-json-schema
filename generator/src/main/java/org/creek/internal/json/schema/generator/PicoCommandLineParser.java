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

import static org.creek.api.json.schema.generator.GeneratorOptions.generatorOptions;

import java.util.Optional;
import org.creek.api.json.schema.generator.GeneratorOptions;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public final class PicoCommandLineParser {

    private PicoCommandLineParser() {}

    public static Optional<GeneratorOptions> parse(final String... args) {
        final Options options = new Options();
        final CommandLine parser = new CommandLine(options);

        try {
            parser.parseArgs(args);

            if (options.usageHelpRequested) {
                System.out.println(parser.getUsageMessage());
                return Optional.empty();
            }

            final GeneratorOptions.Builder builder = generatorOptions();

            if (options.echoOnly) {
                builder.echoOnly();
            }

            return Optional.of(builder.build());
        } catch (final Exception e) {
            throw new InvalidArgumentsException(parser.getUsageMessage(), e);
        }
    }

    private static class Options {
        @Option(
                names = {"-h", "--help"},
                usageHelp = true,
                description = "display this help message")
        boolean usageHelpRequested;

        @Option(
                names = {"-e", "--echo-only"},
                hidden = true)
        boolean echoOnly = false;
    }

    private static class InvalidArgumentsException extends RuntimeException {
        InvalidArgumentsException(final String usageMessage, final Throwable cause) {
            super(cause.getMessage() + System.lineSeparator() + usageMessage, cause);
        }
    }
}
