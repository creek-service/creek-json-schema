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

package org.creekservice.internal.json.schema.generator.mixin;

/**
 * A mixin type used to set what fields of {@link java.time.Instant} are part of the schema.
 *
 * <p>Instances of {@link java.time.Instant} will have {@code seconds} and {@code nanos} in the
 * schema.
 */
@SuppressWarnings("unused") // Invoked via reflection.
public interface Instant {

    /**
     * @return the number of seconds since epoc
     */
    long getSeconds();

    /**
     * @return the additional nanosecond past epoc
     */
    long getNanos();
}
