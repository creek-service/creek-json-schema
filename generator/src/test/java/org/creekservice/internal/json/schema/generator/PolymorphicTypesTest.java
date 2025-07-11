/*
 * Copyright 2022-2025 Creek Contributors (https://github.com/creek-service)
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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.testing.EqualsTester;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;
import org.creekservice.internal.json.schema.generator.PolymorphicTypes.PolymorphicType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings()
@SuppressWarnings("unused")
class PolymorphicTypesTest {

    private static final String PACKAGE = PolymorphicTypesTest.class.getPackageName();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Mock private TypeScanningSpec subtypeScanning;

    @Test
    void shouldImplementHashCodeAndEquals() {
        new EqualsTester()
                .addEqualityGroup(
                        new PolymorphicType<>(
                                AnotherInterface.class,
                                List.of(AnotherInterface.class, StaticNestedClass.class)),
                        new PolymorphicType<>(
                                AnotherInterface.class,
                                List.of(AnotherInterface.class, StaticNestedClass.class)))
                .addEqualityGroup(
                        new PolymorphicType<>(
                                SomeInterface.class,
                                List.of(AnotherInterface.class, StaticNestedClass.class)))
                .addEqualityGroup(
                        new PolymorphicType<>(
                                AnotherInterface.class, List.of(StaticNestedClass.class)))
                .testEquals();
    }

    @Test
    void shouldImplementToStringForEasierDebugging() {
        // Given:
        final PolymorphicType<AnotherInterface> polyType =
                new PolymorphicType<>(
                        AnotherInterface.class,
                        List.of(AnotherInterface.class, StaticNestedClass.class));

        // Then:
        assertThat(
                polyType.toString(),
                is(
                        "PolymorphicType{type=interface"
                            + " org.creekservice.internal.json.schema.generator.PolymorphicTypesTest$AnotherInterface,"
                            + " subTypes=[interface"
                            + " org.creekservice.internal.json.schema.generator.PolymorphicTypesTest$AnotherInterface,"
                            + " class"
                            + " org.creekservice.internal.json.schema.generator.PolymorphicTypesTest$StaticNestedClass"
                            + "]}"));
    }

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
                        new PolymorphicType<>(
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
        assertThat(result, contains(new PolymorphicType<>(Nested.class, Set.of(NestedOne.class))));
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
                        new PolymorphicType<>(Nested.class, Set.of(NestedOne.class)),
                        new PolymorphicType<>(Deep.class, Set.of(DeepOne.class))));
    }

    @Test
    void shouldExcludeSubtypesNotInModuleWhiteList() {
        // Given:
        when(subtypeScanning.moduleWhiteList()).thenReturn(Set.of("not.this.module"));

        // Then:
        assertThat(findPolymorphicTypes(SomeInterface.class), is(empty()));
    }

    @Test
    void shouldIncludeSubtypesInModuleWhiteList() {
        // Given:
        when(subtypeScanning.moduleWhiteList()).thenReturn(Set.of("creek.json.schema.generator"));

        // Then:
        assertThat(findPolymorphicTypes(SomeInterface.class), is(not(empty())));
    }

    @Test
    void shouldExcludeSubtypesNotInPackageWhiteList() {
        // Given:
        when(subtypeScanning.packageWhiteList()).thenReturn(Set.of("com.not.here"));

        // Then:
        assertThat(findPolymorphicTypes(SomeInterface.class), is(empty()));
    }

    @Test
    void shouldIncludeSubtypesInPackageWhiteList() {
        // Given:
        when(subtypeScanning.packageWhiteList())
                .thenReturn(Set.of(SomeInterface.class.getPackageName()));

        // Then:
        assertThat(findPolymorphicTypes(SomeInterface.class), is(not(empty())));
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
                result,
                contains(new PolymorphicType<>(Recursive.class, Set.of(RecursiveOne.class))));
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
                contains(new PolymorphicType<>(ElementType.class, Set.of(ElementTypeOne.class))));
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
                contains(new PolymorphicType<>(ElementType.class, Set.of(ElementTypeOne.class))));
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
                contains(new PolymorphicType<>(GenericType.class, Set.of(GenericTypeOne.class))));
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
                        new PolymorphicType<>(ElementType.class, Set.of(ElementTypeOne.class))));
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
                        new PolymorphicType<>(KeyType.class, Set.of(KeyTypeOne.class)),
                        new PolymorphicType<>(ValueType.class, Set.of(ValueTypeOne.class))));
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
                containsInAnyOrder(
                        new PolymorphicType<>(Required.class, Set.of(RequiredOne.class))));
    }

    private Collection<PolymorphicType<?>> findPolymorphicTypes(final Class<?> type) {
        return PolymorphicTypes.findPolymorphicTypes(List.of(type), subtypeScanning, MAPPER);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    public interface SomeInterface {
        PolymorphicType<SomeInterface> EXPECTED =
                new PolymorphicType<>(
                        SomeInterface.class,
                        Set.of(StaticNestedClass.class, AnotherInterface.class, NestedClass.class));
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class NestedClass extends StaticNestedClass {}

    public interface AnotherInterface extends SomeInterface {}

    public static class StaticNestedClass implements AnotherInterface {}
}
