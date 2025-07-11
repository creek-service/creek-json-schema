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

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaDescription;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaFormat;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInject;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaInt;
import com.kjetland.jackson.jsonSchema.annotations.JsonSchemaTitle;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationException;
import net.jimblackler.jsonschemafriend.Validator;
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

    private final ObjectMapper jsonMapper =
            JsonMapper.builder()
                    .addModule(new Jdk8Module())
                    .addModule(new JavaTimeModule())
                    .serializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                    .build();
    private final ObjectMapper yamlMapper =
            new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));

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
                result.text().replace("\r\n", "${win-ln}").replace("\n", "${unix-ln}"),
                result.text(),
                startsWith(
                        "---\n"
                                + "# timestamp="
                                + now.toEpochMilli()
                                + "\n$schema: http://json-schema.org/draft-07/schema#"));
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
                        "\nproperties:\n" + "  someLongPropertyName:\n" + "    type: string\n"));
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
                        "\nproperties:\n"
                                + "  a:\n"
                                + "    type: string\n"
                                + "  b:\n"
                                + "    type: string\n"
                                + "  c:\n"
                                + "    type: string\n"));
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
                        "\nproperties:\n"
                                + "  b:\n"
                                + "    type: string\n"
                                + "  a:\n"
                                + "    type: string\n"));
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
                        "oneOf:\n"
                                + "- $ref: \"#/definitions/ExplicitlyNamed\"\n"
                                + "- $ref: \"#/definitions/ImplicitlyNamed\"\n"));

        assertThat(result.text(), containsString("default: " + explicitType));
        assertThat(result.text(), containsString("default: " + implicitType));

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

        jsonMapper.registerSubtypes(TypeWithImplicitPolymorphism.ExplicitlyNamed.class);
        jsonMapper.registerSubtypes(TypeWithImplicitPolymorphism.ImplicitlyNamed.class);

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
                                + "- $ref: \"#/definitions/ImplicitlyNamed\"\n"
                                + "- $ref: \"#/definitions/"
                                + explicitType
                                + "\""));

        assertThat(result.text(), containsString("default: " + explicitType));
        assertThat(result.text(), containsString("default: " + implicitType));

        assertAlignsWithJackson(
                result,
                new TypeWithImplicitPolymorphism.ExplicitlyNamed(),
                new TypeWithImplicitPolymorphism.ImplicitlyNamed());
    }

    @Test
    void shouldIncludeImplicitSubTypesUsingSimpleName() {
        // Given:
        generator.registerSubTypes(List.of(TypeWithImplicitSimplePolymorphism.class));

        jsonMapper.registerSubtypes(TypeWithImplicitSimplePolymorphism.ExplicitlyNamed.class);
        jsonMapper.registerSubtypes(TypeWithImplicitSimplePolymorphism.ImplicitlyNamed.class);

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
                                + "- $ref: \"#/definitions/"
                                + explicitType
                                + "\"\n"
                                + "- $ref: \"#/definitions/ImplicitlyNamed\""));

        assertThat(result.text(), containsString("default: " + explicitType));
        assertThat(result.text(), containsString("default: " + implicitType));

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
                        "oneOf:\n"
                                + "- $ref: \"#/definitions/the-explicit-name\"\n"
                                + "- $ref: \"#/definitions/ImplicitlyNamed\""));

        assertThat(result.text(), containsString("default: " + explicitClass));
        assertThat(result.text(), containsString("default: " + implicitClass));

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
                        "oneOf:\n"
                                + "- $ref: \"#/definitions/the-explicit-name\"\n"
                                + "- $ref: \"#/definitions/ImplicitlyNamed\""));

        assertThat(result.text(), containsString("default: " + explicitClass));
        assertThat(result.text(), containsString("default: " + implicitClass));

        assertAlignsWithJackson(
                result,
                new TypeWithMinimalClassPolymorphism.ExplicitlyNamed(),
                new TypeWithMinimalClassPolymorphism.ImplicitlyNamed());
    }

    @Test
    void shouldIncludeSubTypeProperties() {
        // When:
        final JsonSchema<PolyTypeWithProps> result =
                generator.generateSchema(PolyTypeWithProps.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "properties:\n"
                                + "      '@type':\n"
                                + "        type: string\n"
                                + "        enum:\n"
                                + "        - type-1\n"
                                + "        default: type-1\n"
                                + "      a:\n"
                                + "        type: string"));

        assertThat(
                result.text(),
                containsString(
                        "properties:\n"
                                + "      '@type':\n"
                                + "        type: string\n"
                                + "        enum:\n"
                                + "        - type-2\n"
                                + "        default: type-2\n"
                                + "      b:\n"
                                + "        type: integer"));

        assertAlignsWithJackson(
                result, new PolyTypeWithProps.SubType1("text"), new PolyTypeWithProps.SubType2(56));
    }

    @Test
    void shouldIncludeInjectedSchema() {
        // When:
        final JsonSchema<TypeWithInjectedSchema> result =
                generator.generateSchema(TypeWithInjectedSchema.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "  list:\n"
                                + "    type: array\n"
                                + "    items:\n"
                                + "      type: integer\n"
                                + "    maxItems: 2"));

        assertAlignsWithJackson(
                result,
                List.of(new TypeWithInjectedSchema(List.of(1L))),
                List.of(new TypeWithInjectedSchema(List.of(1L, 2L, 3L))));
    }

    @Test
    void shouldIncludeFormat() {
        // When:
        final JsonSchema<TypeWithFormat> result = generator.generateSchema(TypeWithFormat.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  x:\n" + "    type: string\n" + "    format: uuid"));

        assertAlignsWithJackson(
                result,
                List.of(new TypeWithFormat(UUID.randomUUID().toString())),
                List.of(new TypeWithFormat("not a uuid")));
    }

    @Test
    void shouldInsertDecimalAsNumber() {
        // When:
        final JsonSchema<TypeWithDecimal> result = generator.generateSchema(TypeWithDecimal.class);

        // Then:
        assertThat(result.text(), containsString("decimal:\n    type: number"));
        assertThat(result.text(), not(containsString("BigDecimal")));

        assertAlignsWithJackson(result, new TypeWithDecimal(new BigDecimal("0.1")));
    }

    @Test
    void shouldInsertLocalDateAsStringWithDateFormat() {
        // When:
        final JsonSchema<TypeWithLocalDate> result =
                generator.generateSchema(TypeWithLocalDate.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  date:\n" + "    type: string\n" + "    format: date"));

        assertThat(result.text(), not(containsString("LocalDate")));

        assertAlignsWithJackson(result, new TypeWithLocalDate(LocalDate.now()));
    }

    @Test
    void shouldInsertLocalTimeAsStringWithNoFormat() {
        // When:
        final JsonSchema<TypeWithLocalTime> result =
                generator.generateSchema(TypeWithLocalTime.class);

        // Then:
        assertThat(result.text(), containsString("  time:\n    type: string"));

        assertThat(result.text(), not(containsString("format:")));
        assertThat(result.text(), not(containsString("LocalTime")));

        assertAlignsWithJackson(result, new TypeWithLocalTime(LocalTime.now()));
    }

    @Test
    void shouldInsertLocalDateTimeAsStringWithDateTimeFormat() {
        // When:
        final JsonSchema<TypeWithLocalDateTime> result =
                generator.generateSchema(TypeWithLocalDateTime.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  date:\n" + "    type: string\n" + "    format: date-time"));

        assertThat(result.text(), not(containsString("LocalDateTime")));

        assertAlignsWithJackson(result, new TypeWithLocalDateTime(LocalDateTime.now()));
    }

    @Test
    void shouldInsertZonedDateTimeAsStringWithDateTimeFormat() {
        // When:
        final JsonSchema<TypeWithZonedDateTime> result =
                generator.generateSchema(TypeWithZonedDateTime.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  date:\n" + "    type: string\n" + "    format: date-time"));

        assertThat(result.text(), not(containsString("ZonedDateTime")));

        assertAlignsWithJackson(result, new TypeWithZonedDateTime(ZonedDateTime.now()));
    }

    @Test
    void shouldInsertOffsetTimeAsStringWithTimeFormat() {
        // When:
        final JsonSchema<TypeWithOffsetTime> result =
                generator.generateSchema(TypeWithOffsetTime.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  time:\n" + "    type: string\n" + "    format: time"));

        assertThat(result.text(), not(containsString("OffsetTime")));

        assertAlignsWithJackson(result, new TypeWithOffsetTime(OffsetTime.now()));
    }

    @Test
    void shouldInsertOffsetDateTimeAsStringWithDateTimeFormat() {
        // When:
        final JsonSchema<TypeWithOffsetDateTime> result =
                generator.generateSchema(TypeWithOffsetDateTime.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  date:\n" + "    type: string\n" + "    format: date-time"));

        assertThat(result.text(), not(containsString("OffsetDateTime")));

        assertAlignsWithJackson(result, new TypeWithOffsetDateTime(OffsetDateTime.now()));
    }

    @Test
    void shouldInsertMonthDayAsStringWithNoFormat() {
        // When:
        final JsonSchema<TypeWithMonthDay> result =
                generator.generateSchema(TypeWithMonthDay.class);

        // Then:
        assertThat(result.text(), containsString("  date:\n    type: string"));

        assertThat(result.text(), not(containsString("format:")));
        assertThat(result.text(), not(containsString("MonthDay")));

        assertAlignsWithJackson(result, new TypeWithMonthDay(MonthDay.now()));
    }

    @Test
    void shouldInsertYearMonthAsStringWithNoFormat() {
        // When:
        final JsonSchema<TypeWithYearMonth> result =
                generator.generateSchema(TypeWithYearMonth.class);

        // Then:
        assertThat(result.text(), containsString("  date:\n    type: string"));

        assertThat(result.text(), not(containsString("format:")));
        assertThat(result.text(), not(containsString("YearMonth")));

        assertAlignsWithJackson(result, new TypeWithYearMonth(YearMonth.now()));
    }

    @Test
    void shouldInsertYearAsStringWithNoFormat() {
        // When:
        final JsonSchema<TypeWithYear> result = generator.generateSchema(TypeWithYear.class);

        // Then:
        assertThat(result.text(), containsString("  date:\n    type: string"));

        assertThat(result.text(), not(containsString("format:")));
        assertThat(result.text(), not(containsString("Year")));

        assertAlignsWithJackson(result, new TypeWithYear(Year.now()));
    }

    @Test
    void shouldInsertInstantAsDateTime() {
        // When:
        final JsonSchema<TypeWithInstant> result = generator.generateSchema(TypeWithInstant.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  instant:\n" + "    type: string\n" + "    format: date-time"));

        assertAlignsWithJackson(result, new TypeWithInstant(Instant.now()));
    }

    @Test
    void shouldInsertDurationAsNumberWithNoFormat() {
        // When:
        final JsonSchema<TypeWithDuration> result =
                generator.generateSchema(TypeWithDuration.class);

        // Then:
        assertThat(result.text(), containsString("  duration:\n    type: number"));

        assertThat(result.text(), not(containsString("format:")));

        assertAlignsWithJackson(
                result, new TypeWithDuration(Duration.parse("P2DT3H4M0.345000025S")));
    }

    @Test
    void shouldInsertPeriodAsDuration() {
        // When:
        final JsonSchema<TypeWithPeriod> result = generator.generateSchema(TypeWithPeriod.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  period:\n" + "    type: string\n" + "    format: duration"));

        assertAlignsWithJackson(result, new TypeWithPeriod(Period.parse("P1Y2M3D")));
    }

    @Test
    void shouldInsertUriAsStringWithUriFormat() {
        // When:
        final JsonSchema<ModelWithUri> result = generator.generateSchema(ModelWithUri.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  uri:\n" + "    type: string\n" + "    format: uri"));

        assertThat(result.text(), not(containsString("URI")));

        assertAlignsWithJackson(result, new ModelWithUri(URI.create("https://something")));
    }

    @Test
    void shouldInsertUuidAsStringWithUuidFormat() {
        // When:
        final JsonSchema<TypeWithUuid> result = generator.generateSchema(TypeWithUuid.class);

        // Then:
        assertThat(
                result.text(),
                containsString("  uuid:\n" + "    type: string\n" + "    format: uuid"));

        assertThat(result.text(), not(containsString("UUID")));

        assertAlignsWithJackson(result, new TypeWithUuid(UUID.randomUUID()));
    }

    @SafeVarargs
    private <T> void assertAlignsWithJackson(final JsonSchema<T> schema, final T... instances) {
        final Schema parsedSchema = assertCanParse(schema);
        for (final T instance : instances) {
            final String json = assertCanSerialize(instance);
            assertValidJson(json, parsedSchema, schema);
            assertCanDeserialize(schema, json, instance);
        }
    }

    private <T> void assertAlignsWithJackson(
            final JsonSchema<T> schema,
            final Collection<T> validInstances,
            final Collection<T> invalidInstances) {
        final Schema parsedSchema = assertCanParse(schema);
        for (final T instance : validInstances) {
            final String json = assertCanSerialize(instance);
            assertValidJson(json, parsedSchema, schema);
            assertCanDeserialize(schema, json, instance);
        }

        for (final T instance : invalidInstances) {
            final String json = assertCanSerialize(instance);
            assertInvalidJson(json, parsedSchema, schema);
        }
    }

    private Schema assertCanParse(final JsonSchema<?> schema) {
        try {
            final Object obj = yamlMapper.readValue(schema.text(), Object.class);
            final SchemaStore schemaStore = new SchemaStore(true);
            return schemaStore.loadSchema(obj);
        } catch (Exception e) {
            throw new AssertionError("Invalid schema: " + schema.text(), e);
        }
    }

    private <T> String assertCanSerialize(final T instance) {
        try {
            return jsonMapper.writeValueAsString(instance);
        } catch (Exception e) {
            throw new AssertionError("Error serializing: " + instance, e);
        }
    }

    private void assertValidJson(
            final String json, final Schema parsedSchema, final JsonSchema<?> schema) {
        try {
            final Object o = jsonMapper.readValue(json, Object.class);
            new Validator(true).validate(parsedSchema, o);
        } catch (Exception e) {
            throw new AssertionError(
                    "Invalid JSON: " + json + System.lineSeparator() + "Schema: " + schema.text(),
                    e);
        }
    }

    private void assertInvalidJson(
            final String json, final Schema parsedSchema, final JsonSchema<?> schema) {
        final Object o;

        try {
            o = jsonMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new AssertionError("Invalid JSON: " + json, e);
        }

        try {
            new Validator(true).validate(parsedSchema, o);
            throw new AssertionError(
                    "Invalid JSON not detected: "
                            + json
                            + System.lineSeparator()
                            + "Schema: "
                            + schema.text());
        } catch (ValidationException e) {
            // Expected
        }
    }

    private <T> void assertCanDeserialize(
            final JsonSchema<T> schema, final String json, final T expected) {
        final T actual;
        try {
            actual = jsonMapper.readValue(json, schema.type());
        } catch (Exception e) {
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @Type(value = PolyTypeWithProps.SubType1.class, name = "type-1"),
        @Type(value = PolyTypeWithProps.SubType2.class, name = "type-2")
    })
    public static class PolyTypeWithProps {

        public static final class SubType1 extends PolyTypeWithProps {

            private final String a;

            SubType1(@JsonProperty("a") final String a) {
                this.a = a;
            }

            public String getA() {
                return a;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                final SubType1 subType1 = (SubType1) o;
                return Objects.equals(a, subType1.a);
            }

            @Override
            public int hashCode() {
                return Objects.hash(a);
            }
        }

        public static final class SubType2 extends PolyTypeWithProps {

            private final Integer b;

            SubType2(@JsonProperty("b") final Integer b) {
                this.b = b;
            }

            public Integer getB() {
                return b;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                final SubType2 subType2 = (SubType2) o;
                return Objects.equals(b, subType2.b);
            }

            @Override
            public int hashCode() {
                return Objects.hash(b);
            }
        }
    }

    public static final class TypeWithInjectedSchema {

        private final List<Long> list;

        TypeWithInjectedSchema(@JsonProperty("list") final List<Long> list) {
            this.list = list;
        }

        @JsonSchemaInject(ints = {@JsonSchemaInt(path = "maxItems", value = 2)})
        public List<Long> getList() {
            return list;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithInjectedSchema that = (TypeWithInjectedSchema) o;
            return Objects.equals(list, that.list);
        }

        @Override
        public int hashCode() {
            return Objects.hash(list);
        }
    }

    public static final class TypeWithFormat {

        private final String x;

        TypeWithFormat(@JsonProperty("x") final String x) {
            this.x = x;
        }

        @JsonSchemaFormat("uuid")
        public String getX() {
            return x;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithFormat that = (TypeWithFormat) o;
            return Objects.equals(x, that.x);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x);
        }
    }

    public static final class TypeWithDecimal {

        private final BigDecimal decimal;

        TypeWithDecimal(@JsonProperty("decimal") final BigDecimal decimal) {
            this.decimal = decimal;
        }

        public BigDecimal getDecimal() {
            return decimal;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithDecimal that = (TypeWithDecimal) o;
            return Objects.equals(decimal, that.decimal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(decimal);
        }
    }

    public static final class TypeWithLocalDate {
        private final LocalDate date;

        TypeWithLocalDate(@JsonProperty("date") final LocalDate date) {
            this.date = date;
        }

        public LocalDate getDate() {
            return date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithLocalDate that = (TypeWithLocalDate) o;
            return Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    public static final class TypeWithLocalTime {
        private final LocalTime time;

        TypeWithLocalTime(@JsonProperty("time") final LocalTime date) {
            this.time = date;
        }

        public LocalTime getTime() {
            return time;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithLocalTime that = (TypeWithLocalTime) o;
            return Objects.equals(time, that.time);
        }

        @Override
        public int hashCode() {
            return Objects.hash(time);
        }
    }

    public static final class TypeWithLocalDateTime {
        private final LocalDateTime date;

        TypeWithLocalDateTime(@JsonProperty("date") final LocalDateTime date) {
            this.date = date;
        }

        public LocalDateTime getDate() {
            return date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithLocalDateTime that = (TypeWithLocalDateTime) o;
            return Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    public static final class TypeWithZonedDateTime {

        private final ZonedDateTime date;

        TypeWithZonedDateTime(@JsonProperty("date") final ZonedDateTime date) {
            this.date = date;
        }

        public ZonedDateTime getDate() {
            return date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithZonedDateTime that = (TypeWithZonedDateTime) o;
            // Textual zone information is NOT serialized, so exclude from check:
            return Objects.equals(date.getOffset(), that.date.getOffset())
                    && Objects.equals(date.toLocalDateTime(), that.date.toLocalDateTime());
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    public static final class TypeWithOffsetTime {

        private final OffsetTime time;

        TypeWithOffsetTime(@JsonProperty("time") final OffsetTime time) {
            this.time = time;
        }

        public OffsetTime getTime() {
            return time;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithOffsetTime that = (TypeWithOffsetTime) o;
            return Objects.equals(time, that.time);
        }

        @Override
        public int hashCode() {
            return Objects.hash(time);
        }
    }

    public static final class TypeWithOffsetDateTime {

        private final OffsetDateTime date;

        TypeWithOffsetDateTime(@JsonProperty("date") final OffsetDateTime date) {
            this.date = date;
        }

        public OffsetDateTime getDate() {
            return date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithOffsetDateTime that = (TypeWithOffsetDateTime) o;
            return Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    public static final class TypeWithMonthDay {

        private final MonthDay date;

        TypeWithMonthDay(@JsonProperty("date") final MonthDay date) {
            this.date = date;
        }

        public MonthDay getDate() {
            return date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithMonthDay that = (TypeWithMonthDay) o;
            return Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    public static final class TypeWithYearMonth {

        private final YearMonth date;

        TypeWithYearMonth(@JsonProperty("date") final YearMonth date) {
            this.date = date;
        }

        public YearMonth getDate() {
            return date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithYearMonth that = (TypeWithYearMonth) o;
            return Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    @JsonSchemaTitle("Bob")
    public static final class TypeWithYear {

        private final Year date;

        TypeWithYear(@JsonProperty("date") final Year date) {
            this.date = date;
        }

        public Year getDate() {
            return date;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithYear that = (TypeWithYear) o;
            return Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    public static final class TypeWithInstant {

        private final Instant instant;

        TypeWithInstant(@JsonProperty("instant") final Instant instant) {
            this.instant = instant;
        }

        public Instant getInstant() {
            return instant;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithInstant that = (TypeWithInstant) o;
            return Objects.equals(instant, that.instant);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instant);
        }
    }

    public static final class TypeWithDuration {

        private final Duration duration;

        TypeWithDuration(@JsonProperty("duration") final Duration duration) {
            this.duration = duration;
        }

        public Duration getDuration() {
            return duration;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithDuration that = (TypeWithDuration) o;
            return Objects.equals(duration, that.duration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(duration);
        }
    }

    public static final class TypeWithPeriod {

        private final Period period;

        TypeWithPeriod(@JsonProperty("period") final Period period) {
            this.period = period;
        }

        public Period getPeriod() {
            return period;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithPeriod that = (TypeWithPeriod) o;
            return Objects.equals(period, that.period);
        }

        @Override
        public int hashCode() {
            return Objects.hash(period);
        }
    }

    public static final class ModelWithUri {

        private final URI uri;

        ModelWithUri(@JsonProperty("uri") final URI uri) {
            this.uri = uri;
        }

        public URI getUri() {
            return uri;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ModelWithUri that = (ModelWithUri) o;
            return Objects.equals(uri, that.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri);
        }
    }

    public static final class TypeWithUuid {

        private final UUID uuid;

        TypeWithUuid(@JsonProperty("uuid") final UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUuid() {
            return uuid;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TypeWithUuid that = (TypeWithUuid) o;
            return Objects.equals(uuid, that.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }
    }
}
