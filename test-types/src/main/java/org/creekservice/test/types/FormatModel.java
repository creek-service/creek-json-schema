/*
 * Copyright 2022-2024 Creek Contributors (https://github.com/creek-service)
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

package org.creekservice.test.types;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import org.creekservice.api.base.annotation.schema.GeneratesSchema;

@SuppressWarnings("unused") // Invoked via reflection
@GeneratesSchema
public class FormatModel {
    public URI getURI() {
        return null;
    }

    public Instant getInstant() {
        return null;
    }

    public OffsetDateTime getDateTime() {
        return null;
    }

    public LocalDate getDate() {
        return null;
    }

    public OffsetTime getTime() {
        return null;
    }

    public Period getPeriod() {
        return null;
    }
}
