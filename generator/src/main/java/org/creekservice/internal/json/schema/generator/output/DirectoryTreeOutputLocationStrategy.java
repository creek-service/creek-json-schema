/*
 * Copyright 2024 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.internal.json.schema.generator.output;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.creekservice.api.base.type.schema.GeneratedSchemas;
import org.creekservice.api.json.schema.generator.GeneratorOptions;

/**
 * Creek output location strategy.
 *
 * <p>Compatible with other Creek components.
 *
 * <p>Schema files will be output at a path matching the package and type names of the types having
 * schemas generated.
 *
 * <p>For example, given a type {@code org.acme.some.package.TheType}, the schema will be output
 * under {@code org/acme/some/package/TheType.yaml}
 */
public final class DirectoryTreeOutputLocationStrategy
        implements GeneratorOptions.OutputLocationStrategy {

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    @Override
    public Path outputPath(final Class<?> type) {
        return Paths.get(GeneratedSchemas.schemaFileName(type, GeneratedSchemas.yamlExtension()));
    }
}
