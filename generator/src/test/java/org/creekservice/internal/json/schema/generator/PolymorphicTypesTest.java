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

package org.creekservice.internal.json.schema.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.creekservice.internal.json.schema.generator.PolymorphicTypes.PolymorphicType;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings()
@SuppressWarnings("unused")
class PolymorphicTypesTest {

    private static final String PACKAGE = PolymorphicTypesTest.class.getPackageName();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldNotBlowUpOnNoPolymorphicTypes() {
        // Given:
        class NoPolymorphismHere {}

        // When:
        final Collection<?> result = findPolymorphicTypes(NoPolymorphismHere.class);

        // Then:
        assertThat(result, is(empty()));
    }

    @Test
    void shouldIgnoreTypesNotUsingNameId() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        class NotUsingTypeId {}

        // When:
        final Collection<?> result = findPolymorphicTypes(NotUsingTypeId.class);

        // Then:
        assertThat(result, is(empty()));
    }

    @Test
    void shouldIgnoreTypesWithExplicitListOfTypes() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        @JsonSubTypes(@JsonSubTypes.Type(value = String.class, name = "sub"))
        class ExplicitListOfTypes {}

        // When:
        final Collection<?> result = findPolymorphicTypes(ExplicitListOfTypes.class);

        // Then:
        assertThat(result, is(empty()));
    }

    @Test
    void shouldFindImplementationsOfInterface() {
        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(SomeInterface.class);

        // Then:
        assertThat(result, contains(SomeInterface.EXPECTED));
    }

    @Test
    void shouldFindSubClassesOfClass() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class BaseModel {}
        class BaseModelOne extends BaseModel {}
        class BaseModelTwo extends BaseModelOne {}

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(BaseModel.class);

        // Then:
        assertThat(
                result,
                contains(
                        PolymorphicType.of(
                                BaseModel.class, Set.of(BaseModelOne.class, BaseModelTwo.class))));
    }

    @Test
    void shouldFindImplementationsOfInterfaceUsedInProperty() {
        // Given:
        class WithNestedInterface {
            public SomeInterface getPoly() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result =
                findPolymorphicTypes(WithNestedInterface.class);

        // Then:
        assertThat(result, contains(SomeInterface.EXPECTED));
    }

    @Test
    void shouldFindSubClassesOfClassUsedInProperty() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class Nested {}
        class NestedOne extends Nested {}

        class WithNestedClass {
            public Nested getPoly() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(WithNestedClass.class);

        // Then:
        assertThat(result, contains(PolymorphicType.of(Nested.class, Set.of(NestedOne.class))));
    }

    @Test
    void shouldFindSubClassesOfDeeplyNestedPolyTypes() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class Deep {}
        class DeepOne extends Deep {}

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class Nested {}
        class NestedOne extends Nested {
            public Deep getDeep() {
                return null;
            }
        }

        class WithDeeplyNested {
            public Nested getPoly() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(WithDeeplyNested.class);

        // Then:
        assertThat(
                result,
                containsInAnyOrder(
                        PolymorphicType.of(Nested.class, Set.of(NestedOne.class)),
                        PolymorphicType.of(Deep.class, Set.of(DeepOne.class))));
    }

    @Test
    void shouldFindSubTypesByPackage() {
        assertThat(findPolymorphicTypes(SomeInterface.class, "com.not.here"), is(empty()));
        assertThat(
                findPolymorphicTypes(SomeInterface.class, "org.creekservice.internal"),
                is(not(empty())));
    }

    @Test
    void shouldHandleRecursiveType() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class Recursive {}
        class RecursiveOne extends Recursive {
            public Recursive getRecursive() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(Recursive.class);

        // Then:
        assertThat(
                result, contains(PolymorphicType.of(Recursive.class, Set.of(RecursiveOne.class))));
    }

    @Test
    void shouldFindPolyTypesInGenerics() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class ElementType {}
        class ElementTypeOne extends ElementType {}

        class WithGenerics {
            public List<ElementType> getArray() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(WithGenerics.class);

        // Then:
        assertThat(
                result,
                contains(PolymorphicType.of(ElementType.class, Set.of(ElementTypeOne.class))));
    }

    @Test
    void shouldFindPolyTypesInGenericBounds() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class ElementType {}
        class ElementTypeOne extends ElementType {}

        class WithBoundedWildcard {
            public List<? extends ElementType> getArray() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result =
                findPolymorphicTypes(WithBoundedWildcard.class);

        // Then:
        assertThat(
                result,
                contains(PolymorphicType.of(ElementType.class, Set.of(ElementTypeOne.class))));
    }

    @Test
    void shouldFindPolyTypesInTypeNameBounds() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class GenericType {}
        class GenericTypeOne extends GenericType {}

        class WithTypeName<T extends GenericType> {
            public T getT() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(WithTypeName.class);

        // Then:
        assertThat(
                result,
                contains(PolymorphicType.of(GenericType.class, Set.of(GenericTypeOne.class))));
    }

    @Test
    void shouldFindPolyTypesInNestedArray() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class ElementType {}
        class ElementTypeOne extends ElementType {}

        class WithArray {
            public List<List<ElementType>> getProp() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(WithArray.class);

        // Then:
        assertThat(
                result,
                containsInAnyOrder(
                        PolymorphicType.of(ElementType.class, Set.of(ElementTypeOne.class))));
    }

    @Test
    void shouldFindPolyTypesInNestedMap() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class KeyType {}
        class KeyTypeOne extends KeyType {}

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class ValueType {}
        class ValueTypeOne extends ValueType {}

        class WithMap {
            public List<Map<KeyType, ValueType>> getProp() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(WithMap.class);

        // Then:
        assertThat(
                result,
                containsInAnyOrder(
                        PolymorphicType.of(KeyType.class, Set.of(KeyTypeOne.class)),
                        PolymorphicType.of(ValueType.class, Set.of(ValueTypeOne.class))));
    }

    @Test
    void shouldFindPolyTypesInRequiredProperty() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class Required {}
        class RequiredOne extends Required {}
        class WithRequired {
            @JsonProperty(required = true)
            public Required getProp() {
                return null;
            }
        }

        // When:
        final Collection<PolymorphicType<?>> result = findPolymorphicTypes(WithRequired.class);

        // Then:
        assertThat(
                result,
                containsInAnyOrder(PolymorphicType.of(Required.class, Set.of(RequiredOne.class))));
    }

    private Collection<PolymorphicType<?>> findPolymorphicTypes(final Class<?> type) {
        return findPolymorphicTypes(type, PACKAGE);
    }

    private Collection<PolymorphicType<?>> findPolymorphicTypes(
            final Class<?> type, final String packageName) {
        return PolymorphicTypes.findPolymorphicTypes(List.of(type), Set.of(packageName), MAPPER);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    public interface SomeInterface {
        PolymorphicType<SomeInterface> EXPECTED =
                PolymorphicType.of(
                        SomeInterface.class,
                        Set.of(StaticNestedClass.class, AnotherInterface.class, NestedClass.class));
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class NestedClass extends StaticNestedClass {}

    public interface AnotherInterface extends SomeInterface {}

    public static class StaticNestedClass implements AnotherInterface {}
}
