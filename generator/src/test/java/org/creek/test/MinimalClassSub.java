/*
 * Copyright 2022-2026 Creek Contributors (https://github.com/creek-service)
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

package org.creek.test;

import java.util.Objects;
import org.creekservice.internal.json.schema.generator.TypeWithMinimalClassCrossPackage;

/**
 * Subtype of {@link TypeWithMinimalClassCrossPackage} intentionally placed in a short package
 * ({@code org.creek.test}, 14 chars) so that its FQN ({@code org.creek.test.MinimalClassSub}, 30
 * chars) is shorter than the base type's package name length (47 chars for {@code
 * org.creekservice.internal.json.schema.generator}).
 *
 * <p>This exposes Bug 1: {@code computeTypeIdentifier} uses {@code
 * subtype.getName().substring(baseType.getPackageName().length())} without a bounds check, causing
 * {@link StringIndexOutOfBoundsException} when the subtype FQN is shorter than the base package
 * name.
 */
public final class MinimalClassSub extends TypeWithMinimalClassCrossPackage {
    @Override
    public boolean equals(final Object o) {
        return o != null && getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass());
    }
}
