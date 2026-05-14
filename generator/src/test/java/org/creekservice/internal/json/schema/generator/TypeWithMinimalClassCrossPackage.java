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

package org.creekservice.internal.json.schema.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base type in package {@code org.creekservice.internal.json.schema.generator} (47 chars).
 *
 * <p>Paired with {@code MinimalClassSub} in package {@code org.creek.test} (FQN = 30 chars {@code <
 * 47}) to expose Bug 1: {@code computeTypeIdentifier} uses {@code
 * subtype.getName().substring(baseType.getPackageName().length())} without a bounds check, causing
 * {@link StringIndexOutOfBoundsException} when the subtype FQN is shorter than the base package
 * name length.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonSubTypes(@JsonSubTypes.Type(value = org.creek.test.MinimalClassSub.class))
public class TypeWithMinimalClassCrossPackage {}
