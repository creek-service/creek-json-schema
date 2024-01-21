/*
 * Copyright 2022-2023 Creek Contributors (https://github.com/creek-service)
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

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInt;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.creekservice.api.json.schema.generator.GeneratorOptions.TypeScanningSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressFBWarnings()
@SuppressWarnings("unused")
class SchemaGeneratorTest {

    private Instant now = Instant.now();
    @Mock private TypeScanningSpec subtypeScanning;

    private ObjectMapper mapper;
    private SchemaGenerator generator;

    @BeforeEach
    void setUp() {
        when(subtypeScanning.moduleWhiteList()).thenReturn(Set.of("creek.json.schema.generator"));
        when(subtypeScanning.packageWhiteList())
                .thenReturn(Set.of(SchemaGeneratorTest.class.getPackageName()));

        generator = new SchemaGenerator(subtypeScanning, () -> now);
        mapper = JsonMapper.builder().build();
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
                                + lineSeparator()
                                + "$schema: http://json-schema.org/draft-07/schema#"));
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
    void shouldIncludeDefaultTitle() {
        // Given:
        class Model {}

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(result.text(), containsString("title: Model"));
    }

    @Test
    void shouldIncludeCustomTitle() {
        // Given:
        @JsonSchemaTitle("Custom Title")
        class Model {}

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(result.text(), containsString("title: Custom Title"));
    }

    @Test
    void shouldIncludeType() {
        // Given:
        class Model {}

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(result.text(), containsString("type: object"));
    }

    @Test
    void shouldNotAllowAdditionalProps() {
        // Given:
        class Model {}

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(result.text(), containsString("additionalProperties: false"));
    }

    @Test
    void shouldIncludeDescription() {
        // Given:
        @JsonSchemaDescription("Some details")
        class Model {}

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(result.text(), containsString("description: Some details"));
    }

    @Test
    void shouldFormatPropertyNames() {
        // Given:
        class Model {
            public String getSomeLongPropertyName() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        lineSeparator()
                                + "properties:"
                                + lineSeparator()
                                + "  someLongPropertyName:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()));
    }

    @Test
    void shouldOrderPropertiesAlphabeticallyByDefault() {
        // Given:
        class Model {
            public String getC() {
                return null;
            }

            public String getA() {
                return null;
            }

            public String getB() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        lineSeparator()
                                + "properties:"
                                + lineSeparator()
                                + "  a:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "  b:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "  c:"
                                + lineSeparator()
                                + "    type: string"));
    }

    @Test
    void shouldOrderPropertiesAsInstructed() {
        // Given:
        @JsonPropertyOrder(value = {"b", "a"})
        class Model {
            public String getA() {
                return null;
            }

            public String getB() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        lineSeparator()
                                + "properties:"
                                + lineSeparator()
                                + "  b:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "  a:"
                                + lineSeparator()
                                + "    type: string"));
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
    void shouldIncludeExplicitSubTypesRegardlessOfFilters() throws Exception {
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
                        ""
                                + "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/ExplicitlyNamed'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/ImplicitlyNamed'"));

        assertThat(result.text(), containsString("default: " + explicitType));
        assertThat(result.text(), containsString("default: " + implicitType));

        assertThat(result.text().toLowerCase(), not(containsString("ignored")));

        // Should align with Jackson:
        final String explicitJson =
                mapper.writeValueAsString(new TypeWithExplicitPolymorphism.ExplicitlyNamed());
        final String implicitJson =
                mapper.writeValueAsString(new TypeWithExplicitPolymorphism.ImplicitlyNamed());
        assertThat(explicitJson, is("{\"@type\":\"" + explicitType + "\"}"));
        assertThat(implicitJson, is("{\"@type\":\"" + implicitType + "\"}"));
        assertThat(
                mapper.readValue(explicitJson, TypeWithExplicitPolymorphism.class),
                is(instanceOf(TypeWithExplicitPolymorphism.ExplicitlyNamed.class)));
        assertThat(
                mapper.readValue(implicitJson, TypeWithExplicitPolymorphism.class),
                is(instanceOf(TypeWithExplicitPolymorphism.ImplicitlyNamed.class)));
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingTypeName() throws Exception {
        // Given:
        generator.registerSubTypes(List.of(TypeWithImplicitPolymorphism.class));

        mapper.registerSubtypes(TypeWithImplicitPolymorphism.ExplicitlyNamed.class);
        mapper.registerSubtypes(TypeWithImplicitPolymorphism.ImplicitlyNamed.class);

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
                        ""
                                + "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/ImplicitlyNamed'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/"
                                + explicitType
                                + "'"));

        assertThat(result.text(), containsString("default: " + explicitType));
        assertThat(result.text(), containsString("default: " + implicitType));

        // Should align with Jackson:
        final String explicitJson =
                mapper.writeValueAsString(new TypeWithImplicitPolymorphism.ExplicitlyNamed());
        final String implicitJson =
                mapper.writeValueAsString(new TypeWithImplicitPolymorphism.ImplicitlyNamed());
        assertThat(explicitJson, is("{\"@type\":\"" + explicitType + "\"}"));
        assertThat(implicitJson, is("{\"@type\":\"" + implicitType + "\"}"));
        assertThat(
                mapper.readValue(explicitJson, TypeWithImplicitPolymorphism.class),
                is(instanceOf(TypeWithImplicitPolymorphism.ExplicitlyNamed.class)));
        assertThat(
                mapper.readValue(implicitJson, TypeWithImplicitPolymorphism.class),
                is(instanceOf(TypeWithImplicitPolymorphism.ImplicitlyNamed.class)));
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingSimpleName() throws Exception {
        // Given:
        generator.registerSubTypes(List.of(TypeWithImplicitSimplePolymorphism.class));

        mapper.registerSubtypes(TypeWithImplicitSimplePolymorphism.ExplicitlyNamed.class);
        mapper.registerSubtypes(TypeWithImplicitSimplePolymorphism.ImplicitlyNamed.class);

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
                        ""
                                + "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/"
                                + explicitType
                                + "'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/ImplicitlyNamed'"));

        assertThat(result.text(), containsString("default: " + explicitType));
        assertThat(result.text(), containsString("default: " + implicitType));

        // Should align with Jackson:
        final String explicitJson =
                mapper.writeValueAsString(new TypeWithImplicitSimplePolymorphism.ExplicitlyNamed());
        final String implicitJson =
                mapper.writeValueAsString(new TypeWithImplicitSimplePolymorphism.ImplicitlyNamed());
        assertThat(explicitJson, is("{\"@type\":\"" + explicitType + "\"}"));
        assertThat(implicitJson, is("{\"@type\":\"" + implicitType + "\"}"));
        assertThat(
                mapper.readValue(explicitJson, TypeWithImplicitSimplePolymorphism.class),
                is(instanceOf(TypeWithImplicitSimplePolymorphism.ExplicitlyNamed.class)));
        assertThat(
                mapper.readValue(implicitJson, TypeWithImplicitSimplePolymorphism.class),
                is(instanceOf(TypeWithImplicitSimplePolymorphism.ImplicitlyNamed.class)));
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingClass() throws Exception {
        // When:
        final JsonSchema<TypeWithClassPolymorphism> result =
                generator.generateSchema(TypeWithClassPolymorphism.class);
        final String explicitClass = TypeWithClassPolymorphism.ExplicitlyNamed.class.getName();
        final String implicitClass = TypeWithClassPolymorphism.ImplicitlyNamed.class.getName();

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/the-explicit-name'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/ImplicitlyNamed'"));

        assertThat(result.text(), containsString("default: " + explicitClass));
        assertThat(result.text(), containsString("default: " + implicitClass));

        // Should align with Jackson:
        final String explicitJson =
                mapper.writeValueAsString(new TypeWithClassPolymorphism.ExplicitlyNamed());
        final String implicitJson =
                mapper.writeValueAsString(new TypeWithClassPolymorphism.ImplicitlyNamed());
        assertThat(explicitJson, is("{\"@class\":\"" + explicitClass + "\"}"));
        assertThat(implicitJson, is("{\"@class\":\"" + implicitClass + "\"}"));
        assertThat(
                mapper.readValue(explicitJson, TypeWithClassPolymorphism.class),
                is(instanceOf(TypeWithClassPolymorphism.ExplicitlyNamed.class)));
        assertThat(
                mapper.readValue(implicitJson, TypeWithClassPolymorphism.class),
                is(instanceOf(TypeWithClassPolymorphism.ImplicitlyNamed.class)));
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingMinimalClass() throws Exception {
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
                        "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/the-explicit-name'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/ImplicitlyNamed'"));

        assertThat(result.text(), containsString("default: " + explicitClass));
        assertThat(result.text(), containsString("default: " + implicitClass));

        // Should align with Jackson:
        final String explicitJson =
                mapper.writeValueAsString(new TypeWithMinimalClassPolymorphism.ExplicitlyNamed());
        final String implicitJson =
                mapper.writeValueAsString(new TypeWithMinimalClassPolymorphism.ImplicitlyNamed());
        assertThat(explicitJson, is("{\"@c\":\"" + explicitClass + "\"}"));
        assertThat(implicitJson, is("{\"@c\":\"" + implicitClass + "\"}"));
        assertThat(
                mapper.readValue(explicitJson, TypeWithMinimalClassPolymorphism.class),
                is(instanceOf(TypeWithMinimalClassPolymorphism.ExplicitlyNamed.class)));
        assertThat(
                mapper.readValue(implicitJson, TypeWithMinimalClassPolymorphism.class),
                is(instanceOf(TypeWithMinimalClassPolymorphism.ImplicitlyNamed.class)));
    }

    @Test
    void shouldIncludeSubTypeProperties() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class BaseType {}

        class SubType1 extends BaseType {
            public String getA() {
                return null;
            }
        }

        class SubType2 extends BaseType {
            public Integer getB() {
                return null;
            }
        }

        generator.registerSubTypes(List.of(BaseType.class));

        // When:
        final JsonSchema<BaseType> result = generator.generateSchema(BaseType.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "properties:"
                                + lineSeparator()
                                + "      '@type':"
                                + lineSeparator()
                                + "        type: string"
                                + lineSeparator()
                                + "        enum:"
                                + lineSeparator()
                                + "        - SchemaGeneratorTest$3SubType1"
                                + lineSeparator()
                                + "        default: SchemaGeneratorTest$3SubType1"
                                + lineSeparator()
                                + "      a:"
                                + lineSeparator()
                                + "        type: string"));

        assertThat(
                result.text(),
                containsString(
                        "properties:"
                                + lineSeparator()
                                + "      '@type':"
                                + lineSeparator()
                                + "        type: string"
                                + lineSeparator()
                                + "        enum:"
                                + lineSeparator()
                                + "        - SchemaGeneratorTest$1SubType2"
                                + lineSeparator()
                                + "        default: SchemaGeneratorTest$1SubType2"
                                + lineSeparator()
                                + "      b:"
                                + lineSeparator()
                                + "        type: integer"));
    }

    @Test
    void shouldIncludeInjectedSchema() {
        // Given:
        class Model {
            @JsonSchemaInject(ints = {@JsonSchemaInt(path = "maxItems", value = 10)})
            public List<Long> getList() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  list:"
                                + lineSeparator()
                                + "    type: array"
                                + lineSeparator()
                                + "    items:"
                                + lineSeparator()
                                + "      type: integer"
                                + lineSeparator()
                                + "    maxItems: 10"));
    }

    @Test
    void shouldIncludeFormat() {
        // Given:
        class Model {
            @JsonSchemaFormat("number")
            public String getX() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        lineSeparator()
                                + "  x:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "    format: number"));
    }

    @Test
    void shouldInsertDecimalAsNumber() {
        // Given:
        class Model {
            public BigDecimal getDecimal() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(), containsString("decimal:" + lineSeparator() + "    type: number"));
        assertThat(result.text(), not(containsString("BigDecimal")));
    }

    @Test
    void shouldInsertLocalDateAsStringWithDateFormat() {
        // Given:
        class Model {
            public LocalDate getDate() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  date:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "    format: date"));

        assertThat(result.text(), not(containsString("LocalDate")));
    }

    @Test
    void shouldInsertLocalTimeAsStringWithTimeFormat() {
        // Given:
        class Model {
            public LocalTime getTime() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  time:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "    format: time"));

        assertThat(result.text(), not(containsString("LocalTime")));
    }

    @Test
    void shouldInsertLocalDateTimeAsStringWithDateTimeFormat() {
        // Given:
        class Model {

            public LocalDateTime getDateTime() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  dateTime:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "    format: date-time"));

        assertThat(result.text(), not(containsString("LocalDateTime")));
    }

    @Test
    void shouldInsertZonedDateTimeAsStringWithDateTimeFormat() {
        // Given:
        class Model {

            public ZonedDateTime getDateTime() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  dateTime:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "    format: date-time"));

        assertThat(result.text(), not(containsString("ZonedDateTime")));
    }

    @Test
    void shouldInsertUriAsStringWithUriFormat() {
        // Given:
        class Model {
            public URI getUri() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  uri:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "    format: uri"));

        assertThat(result.text(), not(containsString("URI")));
    }

    @Test
    void shouldInsertUuidAsStringWithUuidFormat() {
        // Given:
        class Model {
            public UUID getUuid() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  uuid:"
                                + lineSeparator()
                                + "    type: string"
                                + lineSeparator()
                                + "    format: uuid"));

        assertThat(result.text(), not(containsString("UUID")));
    }

    @Test
    void shouldInsertInstantAsObject() {
        // Given:
        class Model {
            public Instant getInstant() {
                return null;
            }
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  instant:" + lineSeparator() + "    $ref: '#/definitions/Instant'"));

        assertThat(
                result.text(),
                containsString(
                        "  Instant:"
                                + lineSeparator()
                                + "    type: object"
                                + lineSeparator()
                                + "    additionalProperties: false"
                                + lineSeparator()
                                + "    properties:"
                                + lineSeparator()
                                + "      nanos:"
                                + lineSeparator()
                                + "        type: integer"
                                + lineSeparator()
                                + "      seconds:"
                                + lineSeparator()
                                + "        type: integer"
                                + lineSeparator()
                                + "    required:"
                                + lineSeparator()
                                + "    - nanos"
                                + lineSeparator()
                                + "    - seconds"));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        // With explicit logical name:
        @Type(
                value = TypeWithExplicitPolymorphism.ExplicitlyNamed.class,
                name = "the-explicit-name"),
        // With  implicit logical name:
        @Type(value = TypeWithExplicitPolymorphism.ImplicitlyNamed.class)
    })
    public static class TypeWithExplicitPolymorphism {
        public static class ExplicitlyNamed extends TypeWithExplicitPolymorphism {}

        public static class ImplicitlyNamed extends TypeWithExplicitPolymorphism {}

        public static class IgnoredAsNotInTheList extends TypeWithExplicitPolymorphism {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    public static class TypeWithImplicitPolymorphism {
        @JsonTypeName("the-explicit-name")
        public static class ExplicitlyNamed extends TypeWithImplicitPolymorphism {}

        public static class ImplicitlyNamed extends TypeWithImplicitPolymorphism {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
    public static class TypeWithImplicitSimplePolymorphism {
        @JsonTypeName("the-explicit-name")
        public static class ExplicitlyNamed extends TypeWithImplicitSimplePolymorphism {}

        public static class ImplicitlyNamed extends TypeWithImplicitSimplePolymorphism {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    public static class TypeWithClassPolymorphism {
        @JsonTypeName("the-explicit-name")
        public static class ExplicitlyNamed extends TypeWithClassPolymorphism {}

        public static class ImplicitlyNamed extends TypeWithClassPolymorphism {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
    public static class TypeWithMinimalClassPolymorphism {
        @JsonTypeName("the-explicit-name")
        public static class ExplicitlyNamed extends TypeWithMinimalClassPolymorphism {}

        public static class ImplicitlyNamed extends TypeWithMinimalClassPolymorphism {}
    }
}
