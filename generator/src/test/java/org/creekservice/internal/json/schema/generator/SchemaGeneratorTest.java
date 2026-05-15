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

import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;
import org.creekservice.api.json.schema.validator.JsonSchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressFBWarnings()
@SuppressWarnings("unused")
class SchemaGeneratorTest {

    private Instant now = Instant.now();
    @Mock private TypeScanningSpec subtypeScanning;

    private final ObjectMapper jsonMapper =
            JsonMapper.builder()
                    .changeDefaultPropertyInclusion(
                            v ->
                                    JsonInclude.Value.construct(
                                            JsonInclude.Include.NON_EMPTY,
                                            JsonInclude.Include.USE_DEFAULTS))
                    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                    .build();

    private final ObjectMapper yamlMapper =
            new ObjectMapper(
                    YAMLFactory.builder().enable(YAMLWriteFeature.MINIMIZE_QUOTES).build());

    private SchemaGenerator generator;

    @BeforeEach
    void setUp() {
        when(subtypeScanning.moduleWhiteList()).thenReturn(Set.of("creek.json.schema.generator"));
        when(subtypeScanning.packageWhiteList())
                .thenReturn(Set.of(SchemaGeneratorTest.class.getPackageName()));

        generator = new SchemaGenerator(subtypeScanning, () -> now);
    }

    @Test
    void shouldReturnSchemaWithTypeSet() {
        // Given:
        class Model {}

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(result.type(), is(Model.class));
    }

    @Test
    void shouldSetHeaderWithVersion() {
        // Given:
        class Model {}

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                startsWith(
                        "---"
                                + lineSeparator()
                                + "# timestamp="
                                + now.toEpochMilli()
                                + "\n$schema: https://json-schema.org/draft/2020-12/schema"));
    }

    @Test
    void shouldUpdateVersionOnEachCall() {
        // Given:
        class Model {}
        now = now.plus(1, ChronoUnit.SECONDS);

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(result.text(), containsString("# timestamp=" + now.toEpochMilli()));
    }

    @Test
    void shouldNotIncludeImplicitSubTypesIfInDifferentModule() {
        // Given:
        when(subtypeScanning.moduleWhiteList()).thenReturn(Set.of("different.module"));

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class BaseType {}

        class SubType1 extends BaseType {}

        generator.registerSubTypes(List.of(BaseType.class));

        // When:
        final JsonSchema<BaseType> result = generator.generateSchema(BaseType.class);

        // Then:
        assertThat(result.text(), not(containsString("oneOf")));
    }

    @Test
    void shouldNotIncludeImplicitSubTypesIfInDifferentPackage() {
        // Given:
        when(subtypeScanning.packageWhiteList()).thenReturn(Set.of("different.package"));

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class BaseType {}

        class SubType1 extends BaseType {}

        generator.registerSubTypes(List.of(BaseType.class));

        // When:
        final JsonSchema<BaseType> result = generator.generateSchema(BaseType.class);

        // Then:
        assertThat(result.text(), not(containsString("oneOf")));
    }

    @Test
    void shouldIncludeExplicitSubTypesRegardlessOfFilters() {
        // Given:
        when(subtypeScanning.moduleWhiteList()).thenReturn(Set.of("different.module"));
        when(subtypeScanning.packageWhiteList()).thenReturn(Set.of("different.package"));

        final String explicitType = "the-explicit-name";
        final String implicitType =
                "SchemaGeneratorTest$TypeWithExplicitPolymorphism$ImplicitlyNamed";

        // When:
        final JsonSchema<TypeWithExplicitPolymorphism> result =
                generator.generateSchema(TypeWithExplicitPolymorphism.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        """
                        oneOf:
                        - $ref: "#/$defs/ExplicitlyNamed"
                        - $ref: "#/$defs/ImplicitlyNamed"
                        """));

        assertThat(result.text(), containsString("const: " + explicitType));
        assertThat(result.text(), containsString("const: " + implicitType));

        assertThat(result.text().toLowerCase(), not(containsString("ignored")));

        assertAlignsWithJackson(
                result,
                new TypeWithExplicitPolymorphism.ExplicitlyNamed(),
                new TypeWithExplicitPolymorphism.ImplicitlyNamed());
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingTypeName() {
        // Given:
        generator.registerSubTypes(List.of(TypeWithImplicitPolymorphism.class));

        jsonMapper
                .serializationConfig()
                .getSubtypeResolver()
                .registerSubtypes(TypeWithImplicitPolymorphism.ExplicitlyNamed.class);
        jsonMapper
                .serializationConfig()
                .getSubtypeResolver()
                .registerSubtypes(TypeWithImplicitPolymorphism.ImplicitlyNamed.class);

        final String implicitType =
                "SchemaGeneratorTest$TypeWithImplicitPolymorphism$ImplicitlyNamed";
        final String explicitType = "the-explicit-name";

        // When:
        final JsonSchema<TypeWithImplicitPolymorphism> result =
                generator.generateSchema(TypeWithImplicitPolymorphism.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "oneOf:\n"
                                + "- $ref: \"#/$defs/"
                                + explicitType
                                + "\"\n"
                                + "- $ref: \"#/$defs/ImplicitlyNamed\""));

        assertThat(result.text(), containsString("const: " + explicitType));
        assertThat(result.text(), containsString("const: " + implicitType));

        assertAlignsWithJackson(
                result,
                new TypeWithImplicitPolymorphism.ExplicitlyNamed(),
                new TypeWithImplicitPolymorphism.ImplicitlyNamed());
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingSimpleName() {
        // Given:
        generator.registerSubTypes(List.of(TypeWithImplicitSimplePolymorphism.class));

        jsonMapper
                .serializationConfig()
                .getSubtypeResolver()
                .registerSubtypes(TypeWithImplicitSimplePolymorphism.ExplicitlyNamed.class);
        jsonMapper
                .serializationConfig()
                .getSubtypeResolver()
                .registerSubtypes(TypeWithImplicitSimplePolymorphism.ImplicitlyNamed.class);

        final String implicitType =
                TypeWithImplicitSimplePolymorphism.ImplicitlyNamed.class.getSimpleName();
        final String explicitType = "the-explicit-name";

        // When:
        final JsonSchema<TypeWithImplicitSimplePolymorphism> result =
                generator.generateSchema(TypeWithImplicitSimplePolymorphism.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "oneOf:\n"
                                + "- $ref: \"#/$defs/"
                                + explicitType
                                + "\"\n"
                                + "- $ref: \"#/$defs/ImplicitlyNamed\""));

        assertThat(result.text(), containsString("const: " + explicitType));
        assertThat(result.text(), containsString("const: " + implicitType));

        assertAlignsWithJackson(
                result,
                new TypeWithImplicitSimplePolymorphism.ExplicitlyNamed(),
                new TypeWithImplicitSimplePolymorphism.ImplicitlyNamed());
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingClass() {
        // Given:
        final String explicitClass = TypeWithClassPolymorphism.ExplicitlyNamed.class.getName();
        final String implicitClass = TypeWithClassPolymorphism.ImplicitlyNamed.class.getName();

        // When:
        final JsonSchema<TypeWithClassPolymorphism> result =
                generator.generateSchema(TypeWithClassPolymorphism.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        """
                        oneOf:
                        - $ref: "#/$defs/the-explicit-name"
                        - $ref: "#/$defs/ImplicitlyNamed"\
                        """));

        assertThat(result.text(), containsString("const: " + explicitClass));
        assertThat(result.text(), containsString("const: " + implicitClass));

        assertAlignsWithJackson(
                result,
                new TypeWithClassPolymorphism.ExplicitlyNamed(),
                new TypeWithClassPolymorphism.ImplicitlyNamed());
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingMinimalClass() {
        // When:
        final JsonSchema<TypeWithMinimalClassPolymorphism> result =
                generator.generateSchema(TypeWithMinimalClassPolymorphism.class);
        final String explicitClass =
                ".SchemaGeneratorTest$TypeWithMinimalClassPolymorphism$ExplicitlyNamed";
        final String implicitClass =
                ".SchemaGeneratorTest$TypeWithMinimalClassPolymorphism$ImplicitlyNamed";

        // Then:
        assertThat(
                result.text(),
                containsString(
                        """
                        oneOf:
                        - $ref: "#/$defs/the-explicit-name"
                        - $ref: "#/$defs/ImplicitlyNamed"\
                        """));

        assertThat(result.text(), containsString("const: " + explicitClass));
        assertThat(result.text(), containsString("const: " + implicitClass));

        assertAlignsWithJackson(
                result,
                new TypeWithMinimalClassPolymorphism.ExplicitlyNamed(),
                new TypeWithMinimalClassPolymorphism.ImplicitlyNamed());
    }

    @SafeVarargs
    private <T> void assertAlignsWithJackson(final JsonSchema<T> schema, final T... instances) {
        final JsonSchemaValidator validator = assertCanParse(schema);
        for (final T instance : instances) {
            final String json = assertCanSerialize(instance);
            assertValidJson(json, validator, schema);
            assertCanDeserialize(schema, json, instance);
        }
    }

    private <T> JsonSchemaValidator assertCanParse(final JsonSchema<T> schema) {
        try {
            return JsonSchemaValidator.fromSchema(schema.text());
        } catch (final Exception e) {
            throw new AssertionError("Invalid schema: " + schema.text(), e);
        }
    }

    private <T> String assertCanSerialize(final T instance) {
        try {
            return jsonMapper.writeValueAsString(instance);
        } catch (final Exception e) {
            throw new AssertionError("Error serializing: " + instance, e);
        }
    }

    private void assertValidJson(
            final String json, final JsonSchemaValidator validator, final JsonSchema<?> schema) {
        try {
            final Map<String, ?> props =
                    jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            validator.validate(props);
        } catch (final Exception e) {
            throw new AssertionError(
                    "Invalid JSON: " + json + System.lineSeparator() + "Schema: " + schema.text(),
                    e);
        }
    }

    private <T> void assertCanDeserialize(
            final JsonSchema<T> schema, final String json, final T expected) {
        final T actual;
        try {
            actual = jsonMapper.readValue(json, schema.type());
        } catch (final Exception e) {
            throw new AssertionError("Error deserializing: " + json, e);
        }

        assertThat(actual, is(expected));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @Type(
                value = TypeWithExplicitPolymorphism.ExplicitlyNamed.class,
                name = "the-explicit-name"),
        @Type(value = TypeWithExplicitPolymorphism.ImplicitlyNamed.class)
    })
    public static class TypeWithExplicitPolymorphism {
        public static final class ExplicitlyNamed extends TypeWithExplicitPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        public static final class ImplicitlyNamed extends TypeWithExplicitPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        public static final class IgnoredAsNotInTheList extends TypeWithExplicitPolymorphism {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    public static class TypeWithImplicitPolymorphism {
        @JsonTypeName("the-explicit-name")
        public static final class ExplicitlyNamed extends TypeWithImplicitPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        public static final class ImplicitlyNamed extends TypeWithImplicitPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
    public static class TypeWithImplicitSimplePolymorphism {
        @JsonTypeName("the-explicit-name")
        public static final class ExplicitlyNamed extends TypeWithImplicitSimplePolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        public static final class ImplicitlyNamed extends TypeWithImplicitSimplePolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public static class TypeWithClassPolymorphism {
        @JsonTypeName("the-explicit-name")
        public static final class ExplicitlyNamed extends TypeWithClassPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        public static final class ImplicitlyNamed extends TypeWithClassPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
    public static class TypeWithMinimalClassPolymorphism {
        @JsonTypeName("the-explicit-name")
        public static final class ExplicitlyNamed extends TypeWithMinimalClassPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        public static final class ImplicitlyNamed extends TypeWithMinimalClassPolymorphism {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }
    }
}
