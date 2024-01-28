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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DirectoryTreeOutputLocationStrategyTest {

    private DirectoryTreeOutputLocationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DirectoryTreeOutputLocationStrategy();
    }

    @Test
    void shouldReturnPath() {
        assertThat(
                strategy.outputPath(DirectoryTreeOutputLocationStrategyTest.class),
                is(
                        Paths.get(
                                "org/creekservice/internal/json/schema/generator/output/DirectoryTreeOutputLocationStrategyTest.yml")));
    }
}
