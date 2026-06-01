/*
 * Copyright 2026 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.test.generator.modulepath;

import org.creekservice.api.json.schema.generator.JsonSchemaGenerator;

/**
 * Downstream-consumer simulation that verifies the generator module resolves all dependencies on
 * the module path.
 *
 * <p>Run via {@code JavaExec} with only the generator's {@code runtimeClasspath}. If the generator
 * module-info is missing a required dependency, this program will fail with a {@code
 * NoClassDefFoundError}, catching the bug that unit tests miss.
 */
public final class GeneratorModuleCheck {

    private GeneratorModuleCheck() {}

    public static void main(final String[] args) {
        // --version prints version info and exits, forcing all requires to resolve:
        JsonSchemaGenerator.main("--version");
    }
}
