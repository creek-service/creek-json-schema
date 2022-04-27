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

package org.creek.api.json.schema.generator;


import org.creek.internal.json.schema.generator.PicoCommandLineParser;

/** Entry point for generating JSON schemas. */
public final class JsonSchemaGenerator {

    private JsonSchemaGenerator() {}

    public static void main(final String... args) {
        try {
            PicoCommandLineParser.parse(args).ifPresent(JsonSchemaGenerator::generate);
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    public static void generate(final GeneratorOptions options) {
        if (options.echoOnly()) {
            System.out.println(options);
        }
    }
}
