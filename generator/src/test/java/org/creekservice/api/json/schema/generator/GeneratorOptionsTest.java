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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneratorOptionsTest {

    private GeneratorOptions options;

    @BeforeEach
    void setUp() {
        options = () -> null;
    }

    @Test
    void shouldDefaultToNotEchoing() {
        assertThat(options.echoOnly(), is(false));
    }

    @Test
    void shouldDefaultToNotFilteringTypeScanningModules() {
        assertThat(options.typeScanning().moduleWhiteList(), is(empty()));
    }

    @Test
    void shouldDefaultToNotFilteringTypeScanningPackages() {
        assertThat(options.typeScanning().packageWhiteList(), is(empty()));
    }

    @Test
    void shouldDefaultToNotFilteringSubTypeScanningModules() {
        assertThat(options.subTypeScanning().moduleWhiteList(), is(empty()));
    }

    @Test
    void shouldDefaultToNotFilteringSubTypeScanningPackages() {
        assertThat(options.subTypeScanning().packageWhiteList(), is(empty()));
    }
}
