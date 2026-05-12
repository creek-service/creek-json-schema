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

import static org.creekservice.internal.json.schema.generator.JsonSchemaGeneratorFactory.toTitleCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationException;
import net.jimblackler.jsonschemafriend.Validator;
import org.creekservice.api.base.annotation.schema.JsonSchemaInject;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings()
@SuppressWarnings("unused")
class JsonSchemaGeneratorFactoryTest {

    private final ObjectMapper yamlMapper =
            JsonMapper.builder(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
                    .addModule(new Jdk8Module())
                    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .build();

    private final com.github.victools.jsonschema.generator.SchemaGenerator generator =
            JsonSchemaGeneratorFactory.createGenerator(yamlMapper);

    private final ObjectMapper jsonMapper =
            JsonMapper.builder()
                    .addModule(new Jdk8Module())
                    .addModule(new JavaTimeModule())
                    .defaultPropertyInclusion(
                            JsonInclude.Value.construct(
                                    JsonInclude.Include.NON_EMPTY,
                                    JsonInclude.Include.USE_DEFAULTS))
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                    .build();

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(toTitleCase(null), is(nullValue()));
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        assertThat(toTitleCase(""), is(""));
    }

    @Test
    void shouldHandleSingleChar() {
        assertThat(toTitleCase("A"), is("A"));
    }

    @Test
    void shouldHandleSingleWord() {
        assertThat(toTitleCase("Model"), is("Model"));
    }

    @Test
    void shouldCapitaliseLowercaseFirstChar() {
        assertThat(toTitleCase("myModel"), is("My Model"));
    }

    @Test
    void shouldSplitSimpleCamelCase() {
        assertThat(toTitleCase("MyModel"), is("My Model"));
    }

    @Test
    void shouldSplitMultipleWords() {
        assertThat(toTitleCase("MyBigModel"), is("My Big Model"));
    }

    @Test
    void shouldHandleUppercaseAcronymInMiddle() {
        assertThat(toTitleCase("MyURIPath"), is("My URIPath"));
    }

    @Test
    void shouldHandleUppercaseAcronymAtStart() {
        assertThat(toTitleCase("URIPath"), is("URIPath"));
    }

    @Test
    void shouldHandleUppercaseAcronymAtEnd() {
        assertThat(toTitleCase("PathURI"), is("Path URI"));
    }

    @Test
    void shouldHandleAllUppercase() {
        assertThat(toTitleCase("URI"), is("URI"));
    }

    @Test
    void shouldIncludeDefaultTitle() {
        // Given:
        class Model {}

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("title: Model"));
    }

    @Test
    void shouldIncludeCustomTitle() {
        // Given:
        @io.swagger.v3.oas.annotations.media.Schema(title = "Custom Title")
        class Model {}

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("title: Custom Title"));
    }

    @Test
    void shouldIncludeType() {
        // Given:
        class Model {}

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("type: object"));
    }

    @Test
    void shouldNotAllowAdditionalProps() {
        // Given:
        class Model {}

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("additionalProperties: false"));
    }

    @Test
    void shouldIncludeDescription() {
        // Given:
        @io.swagger.v3.oas.annotations.media.Schema(description = "Some details")
        class Model {}

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("description: Some details"));
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
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """

                        properties:
                          someLongPropertyName:
                            type: string
                        """));
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
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """

                        properties:
                          a:
                            type: string
                          b:
                            type: string
                          c:
                            type: string
                        """));
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
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """

                        properties:
                          b:
                            type: string
                          a:
                            type: string
                        """));
    }

    @Test
    void shouldIncludeDiscriminatorForDeepSubtype() {
        // When:
        final String result = generateYaml(ThreeLevelPolymorphism.class);

        // Then:
        assertThat(result, containsString("const: ConcreteLeaf"));
        assertThat(result, containsString("const: MiddleType"));
        assertThat(
                result,
                containsString(
                        """
                        oneOf:
                        - $ref: "#/$defs/MiddleType"
                        - $ref: "#/$defs/ConcreteLeaf"
                        """));

        assertAlignsWithJackson(
                result,
                ThreeLevelPolymorphism.class,
                new ThreeLevelPolymorphism.MiddleType(),
                new ThreeLevelPolymorphism.ConcreteLeaf());
    }

    @Test
    void shouldIncludeDiscriminatorForSubtypesOfPolymorphicInterface() {
        // When:
        final String result = generateYaml(PolyInterface.class);

        // Then:
        assertThat(result, containsString("const: implA"));
        assertThat(result, containsString("const: implB"));
        assertThat(
                result,
                containsString(
                        """
                        oneOf:
                        - $ref: "#/$defs/ImplA"
                        - $ref: "#/$defs/ImplB"
                        """));

        assertAlignsWithJackson(
                result, PolyInterface.class, new PolyInterface.ImplA(), new PolyInterface.ImplB());
    }

    @Test
    void shouldIncludeDiscriminatorForSubtypesOfSimpleNamePolymorphicInterface() {
        // When:
        final String result = generateYaml(SimpleNamePolyInterface.class);

        // Then:
        assertThat(result, containsString("oneOf:"));
        assertThat(result, containsString("$ref: \"#/$defs/ImplX\""));
        assertThat(result, containsString("$ref: \"#/$defs/ImplY\""));
        assertThat(result, containsString("const: ImplX"));
        assertThat(result, containsString("const: ImplY"));

        assertAlignsWithJackson(
                result,
                SimpleNamePolyInterface.class,
                new SimpleNamePolyInterface.ImplX(),
                new SimpleNamePolyInterface.ImplY());
    }

    @Test
    void shouldIncludeDiscriminatorForSubtypesOfMinimalClassPolymorphicInterface() {
        // When:
        final String result = generateYaml(MinimalClassPolyInterface.class);

        // Then:
        assertThat(result, containsString("oneOf:"));
        assertThat(result, containsString("$ref: \"#/$defs/ImplP\""));
        assertThat(result, containsString("$ref: \"#/$defs/ImplQ\""));

        final String implP =
                MinimalClassPolyInterface.ImplP.class
                        .getName()
                        .substring(MinimalClassPolyInterface.class.getPackageName().length());
        final String implQ =
                MinimalClassPolyInterface.ImplQ.class
                        .getName()
                        .substring(MinimalClassPolyInterface.class.getPackageName().length());
        assertThat(result, containsString("const: " + implP));
        assertThat(result, containsString("const: " + implQ));

        assertAlignsWithJackson(
                result,
                MinimalClassPolyInterface.class,
                new MinimalClassPolyInterface.ImplP(),
                new MinimalClassPolyInterface.ImplQ());
    }

    @Test
    void shouldHandleMinimalClassSubtypeFromDifferentPackage() {
        // When:
        final String result = generateYaml(TypeWithMinimalClassCrossPackage.class);

        // Then:
        assertThat(result, containsString("const: org.creek.test.MinimalClassSub"));
    }

    @Test
    void shouldIncludeSubTypeProperties() {
        // When:
        final String result = generateYaml(PolyTypeWithProps.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                        properties:
                              a:
                                type: string
                              '@type':
                                const: type-1\
                        """));

        assertThat(
                result,
                containsString(
                        """
                        properties:
                              b:
                                type: integer
                                minimum: -2147483648
                                maximum: 2147483647
                              '@type':
                                const: type-2\
                        """));

        assertAlignsWithJackson(
                result,
                PolyTypeWithProps.class,
                new PolyTypeWithProps.SubType1("text"),
                new PolyTypeWithProps.SubType2(56));
    }

    @Test
    void shouldIncludeInjectedSchema() {
        // When:
        final String result = generateYaml(TypeWithInjectedSchema.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          list:
                            maxItems: 2
                            type: array
                            items:
                              type: integer\
                        """));

        assertAlignsWithJackson(
                result,
                TypeWithInjectedSchema.class,
                List.of(new TypeWithInjectedSchema(List.of(1L))),
                List.of(new TypeWithInjectedSchema(List.of(1L, 2L, 3L))));
    }

    @Test
    void shouldApplyTypeLevelJsonSchemaInject() {
        // When:
        final String result = generateYaml(TypeWithClassLevelInject.class);

        // Then:
        assertThat(result, containsString("deprecated: true"));
        assertThat(result, containsString("x-custom: value"));
    }

    @Test
    void shouldThrowDescriptiveErrorForInvalidJsonSchemaInjectJson() {
        // When:
        final Exception e =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> generateYaml(TypeWithInvalidInject.class));

        // Then:
        assertThat(e.getMessage(), is("Invalid @JsonSchemaInject JSON: {not valid json}"));
    }

    @Test
    void shouldIncludeFormat() {
        // When:
        final String result = generateYaml(TypeWithFormat.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          x:
                            type: string
                            format: uuid\
                        """));

        assertAlignsWithJackson(
                result,
                TypeWithFormat.class,
                List.of(new TypeWithFormat(UUID.randomUUID().toString())),
                List.of(new TypeWithFormat("not a uuid")));
    }

    @Test
    void shouldInsertDecimalAsNumber() {
        // When:
        final String result = generateYaml(TypeWithDecimal.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "decimal", "type"), is("number"));

        assertThat(result, not(containsString("BigDecimal")));

        assertAlignsWithJackson(
                result, TypeWithDecimal.class, new TypeWithDecimal(new BigDecimal("0.1")));
    }

    @Test
    void shouldInsertLocalDateAsStringWithDateFormat() {
        // When:
        final String result = generateYaml(TypeWithLocalDate.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          date:
                            type: string
                            format: date\
                        """));

        assertThat(result, not(containsString("LocalDate")));

        assertAlignsWithJackson(
                result, TypeWithLocalDate.class, new TypeWithLocalDate(LocalDate.now()));
    }

    @Test
    void shouldInsertLocalTimeAsStringWithPatternButNoFormat() {
        // When:
        final String result = generateYaml(TypeWithLocalTime.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          time:
                            type: string
                            pattern:\
                        """));
        assertThat(result, not(containsString("format:")));
        assertThat(result, not(containsString("LocalTime")));

        assertAlignsWithJackson(
                result,
                TypeWithLocalTime.class,
                new TypeWithLocalTime(LocalTime.of(0, 0)),
                new TypeWithLocalTime(LocalTime.of(12, 30, 45)),
                new TypeWithLocalTime(LocalTime.of(23, 59, 59)),
                new TypeWithLocalTime(LocalTime.of(14, 30, 0)));
    }

    @Test
    void shouldInsertLocalDateTimeAsStringWithDateTimeFormat() {
        // When:
        final String result = generateYaml(TypeWithLocalDateTime.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          date:
                            type: string
                            format: date-time\
                        """));

        assertThat(result, not(containsString("LocalDateTime")));

        assertAlignsWithJackson(
                result,
                TypeWithLocalDateTime.class,
                new TypeWithLocalDateTime(LocalDateTime.now()));
    }

    @Test
    void shouldInsertZonedDateTimeAsStringWithDateTimeFormat() {
        // When:
        final String result = generateYaml(TypeWithZonedDateTime.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          date:
                            type: string
                            format: date-time\
                        """));

        assertThat(result, not(containsString("ZonedDateTime")));

        assertAlignsWithJackson(
                result,
                TypeWithZonedDateTime.class,
                new TypeWithZonedDateTime(ZonedDateTime.now()));
    }

    @Test
    void shouldInsertOffsetTimeAsStringWithTimeFormat() {
        // When:
        final String result = generateYaml(TypeWithOffsetTime.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          time:
                            type: string
                            format: time\
                        """));

        assertThat(result, not(containsString("OffsetTime")));

        assertAlignsWithJackson(
                result, TypeWithOffsetTime.class, new TypeWithOffsetTime(OffsetTime.now()));
    }

    @Test
    void shouldInsertOffsetDateTimeAsStringWithDateTimeFormat() {
        // When:
        final String result = generateYaml(TypeWithOffsetDateTime.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          date:
                            type: string
                            format: date-time\
                        """));

        assertThat(result, not(containsString("OffsetDateTime")));

        assertAlignsWithJackson(
                result,
                TypeWithOffsetDateTime.class,
                new TypeWithOffsetDateTime(OffsetDateTime.now()));
    }

    @Test
    void shouldInsertMonthDayAsStringWithPattern() {
        // When:
        final String result = generateYaml(TypeWithMonthDay.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "date", "type"), is("string"));
        assertThat(result, containsString("pattern:"));
        assertThat(result, not(containsString("format:")));
        assertThat(result, not(containsString("MonthDay")));

        assertAlignsWithJackson(
                result,
                TypeWithMonthDay.class,
                new TypeWithMonthDay(MonthDay.of(1, 1)),
                new TypeWithMonthDay(MonthDay.of(2, 29)),
                new TypeWithMonthDay(MonthDay.of(6, 15)),
                new TypeWithMonthDay(MonthDay.of(12, 31)),
                new TypeWithMonthDay(MonthDay.now()));
    }

    @Test
    void shouldInsertYearMonthAsStringWithPattern() {
        // When:
        final String result = generateYaml(TypeWithYearMonth.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "date", "type"), is("string"));
        assertThat(result, containsString("pattern:"));
        assertThat(result, not(containsString("format:")));
        assertThat(result, not(containsString("YearMonth")));

        assertAlignsWithJackson(
                result,
                TypeWithYearMonth.class,
                new TypeWithYearMonth(YearMonth.of(2024, 1)),
                new TypeWithYearMonth(YearMonth.of(2024, 12)),
                new TypeWithYearMonth(YearMonth.of(1, 6)),
                new TypeWithYearMonth(YearMonth.of(10_000, 6)),
                new TypeWithYearMonth(YearMonth.now()));
    }

    @Test
    void shouldInsertYearAsStringWithPattern() {
        // When:
        final String result = generateYaml(TypeWithYear.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "date", "type"), is("string"));
        assertThat(result, containsString("pattern:"));
        assertThat(result, not(containsString("format:")));

        assertAlignsWithJackson(
                result,
                TypeWithYear.class,
                new TypeWithYear(Year.of(1)),
                new TypeWithYear(Year.of(2024)),
                new TypeWithYear(Year.of(9999)),
                new TypeWithYear(Year.of(10_000)),
                new TypeWithYear(Year.now()));
    }

    @Test
    void shouldInsertInstantAsDateTime() {
        // When:
        final String result = generateYaml(TypeWithInstant.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          instant:
                            type: string
                            format: date-time\
                        """));

        assertAlignsWithJackson(result, TypeWithInstant.class, new TypeWithInstant(Instant.now()));
    }

    @Test
    void shouldInsertDurationAsNumber() {
        // When:
        final String result = generateYaml(TypeWithDuration.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "duration", "type"), is("number"));

        assertThat(result, not(containsString("format:")));
        assertThat(result, not(containsString("pattern:")));

        assertAlignsWithJackson(
                result,
                TypeWithDuration.class,
                new TypeWithDuration(Duration.ZERO),
                new TypeWithDuration(Duration.ofSeconds(1)),
                new TypeWithDuration(Duration.ofHours(1)),
                new TypeWithDuration(Duration.ofMillis(500)),
                new TypeWithDuration(Duration.ofDays(1)),
                new TypeWithDuration(Duration.ofSeconds(5)));
    }

    @Test
    void shouldInsertPeriodAsDurationWithPattern() {
        // When:
        final String result = generateYaml(TypeWithPeriod.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          period:
                            type: string
                            format: duration
                            pattern:\
                        """));

        assertAlignsWithJackson(
                result,
                TypeWithPeriod.class,
                new TypeWithPeriod(Period.parse("P1D")),
                new TypeWithPeriod(Period.parse("P1Y")),
                new TypeWithPeriod(Period.parse("P1Y2M")),
                new TypeWithPeriod(Period.parse("P1Y2M3D")),
                new TypeWithPeriod(Period.ofWeeks(2)),
                new TypeWithPeriod(Period.parse("P1Y2M3D")));
    }

    @Test
    void shouldInsertUriAsStringWithUriFormat() {
        // When:
        final String result = generateYaml(ModelWithUri.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          uri:
                            type: string
                            format: uri\
                        """));

        assertThat(result, not(containsString("URI")));

        assertAlignsWithJackson(
                result, ModelWithUri.class, new ModelWithUri(URI.create("https://something")));
    }

    @Test
    void shouldInsertUuidAsStringWithUuidFormat() {
        // When:
        final String result = generateYaml(TypeWithUuid.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          uuid:
                            type: string
                            format: uuid\
                        """));

        assertThat(result, not(containsString("UUID")));

        assertAlignsWithJackson(result, TypeWithUuid.class, new TypeWithUuid(UUID.randomUUID()));
    }

    @Test
    void shouldInsertBigIntegerAsNumber() {
        // When:
        final String result = generateYaml(TypeWithBigInteger.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "value", "type"), is("number"));

        assertThat(result, not(containsString("BigInteger")));

        assertAlignsWithJackson(
                result, TypeWithBigInteger.class, new TypeWithBigInteger(BigInteger.TWO));
    }

    @Test
    void shouldInsertIntAsIntegerWithMinMax() {
        // When:
        final String result = generateYaml(TypeWithInt.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "value", "type"), is("integer"));
        assertThat(yamlGet(parsedSchema, "properties", "value", "minimum"), is(-2147483648));
        assertThat(yamlGet(parsedSchema, "properties", "value", "maximum"), is(2147483647));

        assertAlignsWithJackson(result, TypeWithInt.class, new TypeWithInt(42));
    }

    @Test
    void shouldInsertLongAsIntegerWithMinMax() {
        // When:
        final String result = generateYaml(TypeWithLong.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "value", "type"), is("integer"));
        assertThat(
                yamlGet(parsedSchema, "properties", "value", "minimum"), is(-9223372036854775808L));
        assertThat(
                yamlGet(parsedSchema, "properties", "value", "maximum"), is(9223372036854775807L));

        assertAlignsWithJackson(result, TypeWithLong.class, new TypeWithLong(42L));
    }

    @Test
    void shouldInsertShortAsIntegerWithMinMax() {
        // When:
        final String result = generateYaml(TypeWithShort.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "value", "type"), is("integer"));
        assertThat(yamlGet(parsedSchema, "properties", "value", "minimum"), is(-32768));
        assertThat(yamlGet(parsedSchema, "properties", "value", "maximum"), is(32767));

        assertAlignsWithJackson(result, TypeWithShort.class, new TypeWithShort((short) 42));
    }

    @Test
    void shouldInsertByteAsIntegerWithMinMax() {
        // When:
        final String result = generateYaml(TypeWithByte.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "value", "type"), is("integer"));
        assertThat(yamlGet(parsedSchema, "properties", "value", "minimum"), is(-128));
        assertThat(yamlGet(parsedSchema, "properties", "value", "maximum"), is(127));

        assertAlignsWithJackson(result, TypeWithByte.class, new TypeWithByte((byte) 42));
    }

    @Test
    void shouldInsertBoxedIntAsIntegerWithMinMax() {
        // When:
        final String result = generateYaml(TypeWithBoxedInt.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "value", "type"), is("integer"));
        assertThat(yamlGet(parsedSchema, "properties", "value", "minimum"), is(-2147483648));
        assertThat(yamlGet(parsedSchema, "properties", "value", "maximum"), is(2147483647));

        assertAlignsWithJackson(result, TypeWithBoxedInt.class, new TypeWithBoxedInt(42));
    }

    @Test
    void shouldAllowSchemaAnnotationToOverrideDefaultFormat() {
        // When:
        final String result = generateYaml(TypeWithOverriddenFormat.class);

        // Then:
        assertThat(result, containsString("format: date"));
        assertThat(result, not(containsString("format: time")));
    }

    @Test
    void shouldAllowSchemaAnnotationToOverrideDefaultPattern() {
        // When:
        final String result = generateYaml(TypeWithOverriddenPattern.class);

        // Then:
        assertThat(result, containsString("pattern: \"^\\\\d{2}:\\\\d{2}$\""));
        assertThat(result, not(containsString("pattern: \"^(?:[01]\\\\d|2[0-3]):")));
    }

    @Test
    void shouldAllowSchemaAnnotationToOverrideDefaultMinimum() {
        // When:
        final String result = generateYaml(TypeWithOverriddenMinMax.class);

        // Then:
        final Map<String, ?> parsedSchema = parseYaml(result);
        assertThat(yamlGet(parsedSchema, "properties", "value", "type"), is("integer"));
        assertThat(yamlGet(parsedSchema, "properties", "value", "minimum"), is(0));
        assertThat(yamlGet(parsedSchema, "properties", "value", "maximum"), is(100));

        assertThat(result, not(containsString("minimum: -2147483648")));
        assertThat(result, not(containsString("maximum: 2147483647")));
    }

    @Test
    void shouldExcludeNonGetterMethods() {
        // Given:
        class Model {
            public String getSomeProp() {
                return null;
            }

            public String nonGetterMethod() {
                return null;
            }

            public String alsoNotAGetter() {
                return null;
            }
        }

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("someProp"));
        assertThat(result, not(containsString("nonGetterMethod")));
        assertThat(result, not(containsString("alsoNotAGetter")));
    }

    @Test
    void shouldExcludeJsonIgnoredGetterFromSchema() {
        // Given:
        class Model {
            public String getIncluded() {
                return null;
            }

            @JsonIgnore
            public String getExcluded() {
                return null;
            }
        }

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("included"));
        assertThat(result, not(containsString("excluded")));
        assertThat(result, not(containsString("Excluded")));
    }

    @Test
    void shouldIncludeJsonGetterMethodWithAnnotatedPropertyName() {
        // Given:
        class Model {
            @JsonGetter("my_prop")
            public String nonGetterNamedMethod() {
                return null;
            }
        }

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("my_prop"));
        assertThat(result, not(containsString("nonGetterNamedMethod")));
    }

    @Test
    void shouldMarkJsonGetterRequiredWhenConstructorParamIsRequired() {
        // When:
        final String result = generateYaml(TypeWithJsonGetter.class);

        // Then:
        assertThat(result, containsString("required:\n- my_prop"));
        assertThat(result, not(containsString("- optional_prop")));
        assertThat(result, not(containsString("nonGetterMethod")));
    }

    @Test
    void shouldMarkPrimitiveGetterReturnTypeAsRequired() {
        // Given:
        class Model {
            public int getIntProp() {
                return 0;
            }

            public boolean isBoolProp() {
                return false;
            }

            public String getStringProp() {
                return null;
            }
        }

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("required:"));
        assertThat(result, containsString("- intProp"));
        assertThat(result, containsString("- boolProp"));
        assertThat(result, not(containsString("- stringProp")));
    }

    @Test
    void shouldPickUpDefaultValueFromJsonPropertyOnGetter() {
        // Given:
        class Model {
            @JsonProperty(defaultValue = "hello")
            public String getProp() {
                return null;
            }
        }

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("default: hello"));
    }

    @Test
    void shouldHandleSingleCharGetStyleGetterName() {
        // Given:
        class Model {
            public String getA() {
                return null;
            }
        }

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("\n  a:\n"));
        assertThat(result, not(containsString("getA")));
    }

    @Test
    void shouldHandleSingleCharIsStyleGetterName() {
        // Given:
        class Model {
            public boolean isB() {
                return false;
            }
        }

        // When:
        final String result = generateYaml(Model.class);

        // Then:
        assertThat(result, containsString("\n  b:\n"));
        assertThat(result, not(containsString("isB")));
    }

    @Test
    void shouldHandleBasicEnum() {
        // When:
        final String result = generateYaml(ModelWithBasicEnum.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          status:
                            type: string
                            enum:
                            - ALPHA
                            - BETA
                            - GAMMA\
                        """));
    }

    @Test
    void shouldHandleEnumWithJsonProperty() {
        // When:
        final String result = generateYaml(ModelWithJsonPropertyEnum.class);

        // Then:
        assertThat(
                result,
                containsString(
                        """
                          coord:
                            type: string
                            enum:
                            - x_ray
                            - y_axis\
                        """));
    }

    private String generateYaml(final Class<?> type) {
        final ObjectNode schema = generator.generateSchema(type);
        try {
            return yamlMapper.writeValueAsString(schema);
        } catch (final JsonProcessingException e) {
            throw new AssertionError("Failed to convert schema to YAML", e);
        }
    }

    private Map<String, ?> parseYaml(final String yaml) {
        try {
            return yamlMapper.readValue(yaml, new TypeReference<Map<String, ?>>() {});
        } catch (final JsonProcessingException e) {
            throw new AssertionError("Failed to parse schema", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object yamlGet(final Map<String, ?> parsed, final String... props) {
        final StringBuilder path = new StringBuilder();
        Map<String, ?> current = parsed;
        for (int i = 0; i < props.length - 1; i++) {
            final String prop = props[i];
            path.append(".").append(prop);
            final Object v = current.get(prop);
            if (!(v instanceof Map<?, ?>)) {
                throw new AssertionError("Expected map for " + path + ", got " + v);
            }
            current = (Map<String, ?>) v;
        }

        return current.get(props[props.length - 1]);
    }

    @SafeVarargs
    private <T> void assertAlignsWithJackson(
            final String yaml, final Class<T> type, final T... instances) {
        final Schema parsedSchema = assertCanParse(yaml);
        for (final T instance : instances) {
            final String json = assertCanSerialize(instance);
            assertValidJson(json, parsedSchema, yaml);
            assertCanDeserialize(type, json, instance);
        }
    }

    private <T> void assertAlignsWithJackson(
            final String yaml,
            final Class<T> type,
            final Collection<T> validInstances,
            final Collection<T> invalidInstances) {
        final Schema parsedSchema = assertCanParse(yaml);
        for (final T instance : validInstances) {
            final String json = assertCanSerialize(instance);
            assertValidJson(json, parsedSchema, yaml);
            assertCanDeserialize(type, json, instance);
        }

        for (final T instance : invalidInstances) {
            final String json = assertCanSerialize(instance);
            assertInvalidJson(json, parsedSchema, yaml);
        }
    }

    private Schema assertCanParse(final String yaml) {
        try {
            final Object obj = yamlMapper.readValue(yaml, Object.class);
            final SchemaStore schemaStore = new SchemaStore(true);
            return schemaStore.loadSchema(obj);
        } catch (final Exception e) {
            throw new AssertionError("Invalid schema: " + yaml, e);
        }
    }

    private <T> String assertCanSerialize(final T instance) {
        try {
            return jsonMapper.writeValueAsString(instance);
        } catch (final Exception e) {
            throw new AssertionError("Error serializing: " + instance, e);
        }
    }

    private void assertValidJson(final String json, final Schema parsedSchema, final String yaml) {
        try {
            final Object o = jsonMapper.readValue(json, Object.class);
            new Validator(true).validate(parsedSchema, o);
        } catch (final Exception e) {
            throw new AssertionError(
                    "Invalid JSON: " + json + System.lineSeparator() + "Schema: " + yaml, e);
        }
    }

    private void assertInvalidJson(
            final String json, final Schema parsedSchema, final String yaml) {
        final Object o;

        try {
            o = jsonMapper.readValue(json, Object.class);
        } catch (final Exception e) {
            throw new AssertionError("Invalid JSON: " + json, e);
        }

        try {
            new Validator(true).validate(parsedSchema, o);
            throw new AssertionError(
                    "Invalid JSON not detected: "
                            + json
                            + System.lineSeparator()
                            + "Schema: "
                            + yaml);
        } catch (final ValidationException e) {
            // Expected
        }
    }

    private <T> void assertCanDeserialize(
            final Class<T> type, final String json, final T expected) {
        final T actual;
        try {
            actual = jsonMapper.readValue(json, type);
        } catch (final Exception e) {
            throw new AssertionError("Error deserializing: " + json, e);
        }

        assertThat(actual, is(expected));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME)
    @JsonSubTypes({
        @Type(value = ThreeLevelPolymorphism.MiddleType.class),
        @Type(value = ThreeLevelPolymorphism.ConcreteLeaf.class)
    })
    public static class ThreeLevelPolymorphism {
        @Override
        public boolean equals(final Object o) {
            return o != null && getClass().equals(o.getClass());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getClass());
        }

        public static class MiddleType extends ThreeLevelPolymorphism {}

        public static final class ConcreteLeaf extends MiddleType {}
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
    @JsonSubTypes({
        @Type(value = PolyInterface.ImplA.class, name = "implA"),
        @Type(value = PolyInterface.ImplB.class, name = "implB")
    })
    public interface PolyInterface {
        final class ImplA implements PolyInterface {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        final class ImplB implements PolyInterface {
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
    @JsonSubTypes({
        @Type(value = SimpleNamePolyInterface.ImplX.class),
        @Type(value = SimpleNamePolyInterface.ImplY.class)
    })
    public interface SimpleNamePolyInterface {
        final class ImplX implements SimpleNamePolyInterface {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        final class ImplY implements SimpleNamePolyInterface {
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
    @JsonSubTypes({
        @Type(value = MinimalClassPolyInterface.ImplP.class),
        @Type(value = MinimalClassPolyInterface.ImplQ.class)
    })
    public interface MinimalClassPolyInterface {
        final class ImplP implements MinimalClassPolyInterface {
            @Override
            public boolean equals(final Object o) {
                return o != null && getClass().equals(o.getClass());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getClass());
            }
        }

        final class ImplQ implements MinimalClassPolyInterface {
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

    public record TypeWithInjectedSchema(
            @JsonSchemaInject("{\"maxItems\":2}") @JsonProperty("list") List<Long> list) {}

    @JsonSchemaInject("{\"deprecated\": true, \"x-custom\": \"value\"}")
    public static final class TypeWithClassLevelInject {

        public String getName() {
            return null;
        }
    }

    @JsonSchemaInject("{not valid json}")
    public static final class TypeWithInvalidInject {

        public String getX() {
            return null;
        }
    }

    public record TypeWithFormat(
            @JsonProperty("x") @io.swagger.v3.oas.annotations.media.Schema(format = "uuid")
                    String x) {}

    public record TypeWithDecimal(@JsonProperty("decimal") BigDecimal decimal) {}

    public record TypeWithLocalDate(@JsonProperty("date") LocalDate date) {}

    public record TypeWithLocalTime(@JsonProperty("time") LocalTime time) {}

    public record TypeWithLocalDateTime(@JsonProperty("date") LocalDateTime date) {}

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
            if (!(o instanceof TypeWithZonedDateTime that)) {
                return false;
            }
            // Textual zone information is NOT serialized, so exclude from check:
            return Objects.equals(date.getOffset(), that.date.getOffset())
                    && Objects.equals(date.toLocalDateTime(), that.date.toLocalDateTime());
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }

    public record TypeWithOffsetTime(@JsonProperty("time") OffsetTime time) {}

    public record TypeWithOffsetDateTime(@JsonProperty("date") OffsetDateTime date) {}

    public record TypeWithMonthDay(@JsonProperty("date") MonthDay date) {}

    public record TypeWithYearMonth(@JsonProperty("date") YearMonth date) {}

    @io.swagger.v3.oas.annotations.media.Schema(title = "Bob")
    public record TypeWithYear(@JsonProperty("date") Year date) {}

    public record TypeWithInstant(@JsonProperty("instant") Instant instant) {}

    public record TypeWithDuration(@JsonProperty("duration") Duration duration) {}

    public record TypeWithPeriod(@JsonProperty("period") Period period) {}

    public record ModelWithUri(@JsonProperty("uri") URI uri) {}

    public record TypeWithUuid(@JsonProperty("uuid") UUID uuid) {}

    public record TypeWithBigInteger(@JsonProperty("value") BigInteger value) {}

    public record TypeWithInt(@JsonProperty("value") int value) {}

    public record TypeWithLong(@JsonProperty("value") long value) {}

    public record TypeWithShort(@JsonProperty("value") short value) {}

    public record TypeWithByte(@JsonProperty("value") byte value) {}

    public record TypeWithBoxedInt(@JsonProperty("value") Integer value) {}

    public record TypeWithOverriddenFormat(
            @io.swagger.v3.oas.annotations.media.Schema(format = "date") @JsonProperty("time")
                    OffsetTime time) {}

    public record TypeWithOverriddenPattern(
            @io.swagger.v3.oas.annotations.media.Schema(pattern = "^\\d{2}:\\d{2}$")
                    @JsonProperty("time")
                    LocalTime time) {}

    public record TypeWithOverriddenMinMax(
            @io.swagger.v3.oas.annotations.media.Schema(minimum = "0", maximum = "100")
                    @JsonProperty("value")
                    int value) {}

    public record ModelWithBasicEnum(@JsonProperty("status") BasicEnum status) {
        public enum BasicEnum {
            ALPHA,
            BETA,
            GAMMA
        }
    }

    public record ModelWithJsonPropertyEnum(@JsonProperty("coord") JsonPropertyEnum coord) {
        public enum JsonPropertyEnum {
            @JsonProperty("x_ray")
            X_RAY,
            @JsonProperty("y_axis")
            Y_AXIS
        }
    }

    public static final class TypeWithJsonGetter {

        TypeWithJsonGetter(
                @JsonProperty(value = "my_prop", required = true) final String requiredProp,
                @JsonProperty("optional_prop") final String optionalProp) {}

        @JsonGetter("my_prop")
        public String nonGetterNamedRequired() {
            return null;
        }

        @JsonGetter("optional_prop")
        public String nonGetterNamedOptional() {
            return null;
        }
    }
}
