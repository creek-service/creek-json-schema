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

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
    private SchemaGenerator generator;
    @Mock private TypeScanningSpec subtypeScanning;

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
    void shouldIncludeImplicitSubTypesIfIncludedAndUsingIdTypeName() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        class BaseType {}

        class SubType1 extends BaseType {}

        class SubType2 extends BaseType {}

        generator.registerSubTypes(List.of(BaseType.class));

        // When:
        final JsonSchema<BaseType> result = generator.generateSchema(BaseType.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        ""
                                + "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/SubType1'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/SubType2'"));
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

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
        @JsonSubTypes({
            @Type(value = Model.SubType2.class, name = "sub1"),
            @Type(value = Model.SubType1.class, name = "sub2")
        })
        class Model {
            class SubType1 extends Model {}

            class SubType2 extends Model {}

            class IgnoredAsNotInTheList extends Model {}
        }

        // When:
        final JsonSchema<Model> result = generator.generateSchema(Model.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        ""
                                + "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/SubType2'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/SubType1'"));

        assertThat(result.text(), containsString("default: sub1"));
        assertThat(result.text(), containsString("default: sub2"));
    }

    @Test
    void shouldIncludeImplicitSubTypesIfIncludedAndNotUsingIdTypeName() {
        // Given:
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
        class BaseType {}
        class SubType1 extends BaseType {}
        class SubType2 extends BaseType {}

        // When:
        final JsonSchema<BaseType> result = generator.generateSchema(BaseType.class);

        // Then:
        assertThat(
                result.text(),
                containsString(
                        "oneOf:"
                                + lineSeparator()
                                + "- $ref: '#/definitions/SubType2'"
                                + lineSeparator()
                                + "- $ref: '#/definitions/SubType1'"));
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
                                + "        - sub_type1"
                                + lineSeparator()
                                + "        default: sub_type1"
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
                                + "        - sub_type2"
                                + lineSeparator()
                                + "        default: sub_type2"
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
}
