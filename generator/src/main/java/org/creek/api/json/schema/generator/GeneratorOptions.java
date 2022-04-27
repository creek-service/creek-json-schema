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

public final class GeneratorOptions {

    private final boolean echoOnly;

    public static Builder generatorOptions() {
        return new Builder();
    }

    private GeneratorOptions(final boolean echoOnly) {
        this.echoOnly = echoOnly;
    }

    public boolean echoOnly() {
        return echoOnly;
    }

    @Override
    public String toString() {
        return "--echoOnly=" + echoOnly;
    }

    public static final class Builder {

        private boolean echoOnly = false;

        private Builder() {}

        /**
         * If set, the generator will echo its arguments and exit. Useful for debugging.
         *
         * @return self.
         */
        public Builder echoOnly() {
            echoOnly = true;
            return this;
        }

        /** @return the built options */
        public GeneratorOptions build() {
            return new GeneratorOptions(echoOnly);
        }
    }
}
