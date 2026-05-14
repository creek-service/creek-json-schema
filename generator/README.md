[![javadoc](https://javadoc.io/badge2/org.creekservice/creek-json-schema-generator/javadoc.svg)](https://javadoc.io/doc/org.creekservice/creek-json-schema-generator)
# Creek JSON schema generator

A command line tool for generating JSON schemas from code.

> ## NOTE
> There is a [Gradle plugin][1] for generating JSON schemas as part of your build process.

## Executing the schema generator

The schema generator is designed to be run from build plugins, like the [Creek Schema Gradle Plugin][1].
However, it can be run directly as a command line tool:

```bash
  java \ 
    --module-path <lib-path> \
    --module creek.json.schema.generator/org.creekservice.api.json.schema.generator.JsonSchemaGenerator \
    --output-directory=some/path
```

(Run with `--help` for an up-to-date list of arguments)

...or you can interact programmatically with the main [JsonSchemaGenerator][2] class.

## Schema generation

The generator searches the class and module path for types annotated with `@GeneratesSchema` and writes out a 
JSON schema for each. Internally it uses the [victools JSON Schema Generator][3]. It honours
both [Jackson's][4] annotations, [Swagger v2][8] annotations, and Creek's own [`@JsonSchemaInject`][9] annotation.

The generator should work with any JVM based language. See the [Kotlin example](#non-java-types) below for a non-Java example.

Schemas are currently written in YAML, as this is compatible with, but more succinct than, JSON. See 
[issue-17](https://github.com/creek-service/creek-json-schema/issues/17) if you would like to vote on having an
option to write schemas out in JSON.

See below for some examples and refer to both the Jackson and generators documentation for more information.

### Simple model

A simple type, without any annotations other than `@GeneratesSchema`, will generate a schema compatible with how 
Jackson would serialize the type, i.e. it will include any properties with standard getter names:

For example:

##### [SimpleModel Java Class](../test-types/src/main/java/org/creekservice/test/types/SimpleModel.java)
```java
@GeneratesSchema
public class SimpleModel {
   public int getIntProp() {
      // ...
   }

   public Optional<String> getStringProp() {
      // ...
   }
}
```

...will produce the schema:

##### [SimpleModel Schema](src/test/resources/schemas/flat/org.creekservice.test.types.SimpleModel.yml)
```yaml
---
$schema: https://json-schema.org/draft/2020-12/schema
type: object
properties:
  intProp:
    type: integer
    minimum: -2147483648
    maximum: 2147483647
  stringProp:
    type: string
required:
- intProp
title: Simple Model
additionalProperties: false
```

Properties for both the getters have been included in the above schema.

The integer property is marked as required, as primitive types can't be `null`.

The string property is not marked as required. `Optional<String>` properties are absent-or-present:
when the `Optional` is empty, the property is simply omitted from the document.
The schema does not allow `null` values for `Optional` properties by design.
See [allowing `null` values](#allowing-null-values) if you explicitly need to permit `null`.

> ## NOTE
> Ensure Jackson is configured to exclude empty `Optional` values when serialising to JSON
> (e.g. by setting `@JsonInclude(JsonInclude.Include.NON_EMPTY)` or configuring the `ObjectMapper`).
> Failure to do so may result in schema validation failure.
> Creek's own serializers do this by default.

It is recommended, but not required by the plugin, to use the `Optional` standard Java type for optional properties. 

Non-primitive properties can be marked required using [Jackson annotations](#jackson-annotations).

### Type mapping

As you can see from the simple model example above, the generator automatically converts some standard Java types 
to their JSON schema counterparts. 

The generator will also leverage the [JSON Schema's `format` specifier][5] to convert the types below:

| Java type         | Schema type | format      | pattern                                                           | minimum              | maximum             | Notes  |
|-------------------|-------------|-------------|-------------------------------------------------------------------|----------------------|---------------------|--------|
| `URI`             | `string`    | `uri`       |                                                                   |                      |                     |        |
| `UUID`            | `string`    | `uuid`      |                                                                   |                      |                     |        |
| `OffsetTime`      | `string`    | `time`      |                                                                   |                      |                     |        |
| `OffsetDateTime`  | `string`    | `date-time` |                                                                   |                      |                     |        |
| `LocalDate`       | `string`    | `date`      |                                                                   |                      |                     |        |
| `LocalTime`       | `string`    |             | `^(?:[01]\d\|2[0-3]):(?:[0-5]\d)(?::(?:[0-5]\d)(?:\.\d{1,9})?)?$` |                      |                     | note 1 |
| `LocalDateTime`   | `string`    | `date-time` |                                                                   |                      |                     |        |
| `ZonedDateTime`   | `string`    | `date-time` |                                                                   |                      |                     | note 2 |
| `MonthDay`        | `string`    |             | `^--(?:0[1-9]\|1[0-2])-(?:0[1-9]\|[12]\d\|3[01])$`                |                      |                     | note 3 |
| `YearMonth`       | `string`    |             | `^-?\d{4,}-(?:0[1-9]\|1[0-2])$`                                   |                      |                     | note 3 |
| `Year`            | `string`    |             | `^-?\d+$`                                                         |                      |                     | note 3 |
| `Instant`         | `string`    | `date-time` |                                                                   |                      |                     |        |
| `Duration`        | `number`    |             |                                                                   |                      |                     | note 4 |
| `Period`          | `string`    | `duration`  | `^P(?=\d)(?:\d+Y)?(?:\d+M)?(?:\d+W)?(?:\d+D)?$`                   |                      |                     |        |
| `byte` / `Byte`   | `integer`   |             |                                                                   | -128                 | 127                 |        |
| `short` / `Short` | `integer`   |             |                                                                   | -32768               | 32767               |        |
| `int` / `Integer` | `integer`   |             |                                                                   | -2147483648          | 2147483647          |        |
| `long` / `Long`   | `integer`   |             |                                                                   | -9223372036854775808 | 9223372036854775807 |        |

All defaults in the table above can be overridden on individual properties using Swagger's `@Schema` annotation.
For example, to restrict an `int` property to a custom range:

```java
@Schema(minimum = "0", maximum = "100")
public int getPercentage() {
    // ...
}
```

**Note 1:**
`LocalTime` properties will be of type `string` in the generated schema, but will not have a `time` format set.
This is because Jackson serialization does not include an offset when serializing `LocalTime` and the `time` format requires an offset.
For this reason, the use of `LocalTime` is discouraged: use `OffsetTime` instead.

**Note 2:**
`ZonedDateTime` properties will be of type `string` and a format of `date-time` in the generated schema.
However, serialization of the textual zone information is not platform / language independent.
Jackson does not serialize the zone information, only the offset information.
The serialized form is the same as `OffsetDateTime`.
For this reason, the use of `ZonedDateTime` is discouraged: use `OffsetDateTime` instead.

**Note 3:**
Properties of type `MonthDay`, `YearMonth` & `Year` will be of type `string` with a pattern constraint, but no format, in the generated schema.
Jackson will serialize these types as strings, assuming the `JavaTimeModule` module is installed.
There is no defined JSON format to match these types, so a regex pattern is used to validate the serialized form instead.

**Note 4:**
`Duration` properties are serialized by Jackson as decimal numbers representing the number of seconds, 
therefore the generated schema will define the property as type `number` with no format.
Do not enable `SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS` if using your own mapper.

For example:

##### [FormatModel Java Class](../test-types/src/main/java/org/creekservice/test/types/FormatModel.java)
```java
@GeneratesSchema
public class FormatModel {
    public URI getURI() {
        return null;
    }

    public Instant getInstant() {
        // ...
    }

    public OffsetDateTime getDateTime() {
        // ...
    }

    public LocalDate getDate() {
        // ...
    }

    public OffsetTime getTime() {
        // ...
    }

    public Period getPeriod() {
        // ...
    }
}
```

...will produce the schema:

##### [FormatModel Schema](src/test/resources/schemas/flat/org.creekservice.test.types.FormatModel.yml)
```yaml
---
$schema: https://json-schema.org/draft/2020-12/schema
type: object
properties:
  URI:
    type: string
    format: uri
  date:
    type: string
    format: date
  dateTime:
    type: string
    format: date-time
  instant:
    type: string
    format: date-time
  period:
    type: string
    format: duration
    pattern: ^P(?=\d)(?:\d+Y)?(?:\d+M)?(?:\d+W)?(?:\d+D)?$
  time:
    type: string
    format: time
title: Format Model
additionalProperties: false
```

### Compatible Jackson configuration

The above type mapping is not automatically compatible with Jackson's default JSON serialization.

The minimal configuration for Jackson is shown below:

```java
class Example {
 JsonMapper mapper = JsonMapper.builder()
        .addModule(new Jdk8Module()) // Note 1
        .addModule(new JavaTimeModule()) // Note 2
        .serializationInclusion(JsonInclude.Include.NON_EMPTY) // Note 3
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // Note 4
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) // Note 5
        .build();
}
```

**Note 1:**
The `Jdk8Module` is required if models include any optional JDK types, e.g. `Optional`, `OptionalLong` etc.

**Note 2:**
The `JavaTimeModule` is required if models include any temporal JDK types, e.g. `Instant`, `OffsetDateTime` etc.

**Note 3:**
The generated schema does not allow `null` values by default _by design_.
Nulls are a common source of bugs and are best avoid in data, just like in code.
As the generated schema doesn't allow `null` values, Jackson will need configuring to exclude properties
with a `null` value. Setting serialization inclusion to `NON_EMPTY` will exclude properties with:
 * a `null` value
 * a `Optional.empty()` value
 * or return a empty collection or array.

See [Allowing null values](#allowing-null-values) if you _really_ want to allow nulls in your data.

**Note 4:**
The generated schema will map JDK temporal times, e.g. `OffsetDateTime`, to type `string` with an appropriate `format`.
Jackson, by default, writes dates as timestamps. This feature must be disabled so dates are written as schema-compatible strings.

**Note 5:**
(Optional): If you require temporal types to maintain the serialized offset, disable `ADJUST_DATES_TO_CONTEXT_TIME_ZONE`.
By default, Jackson will adjust temporal types to their local time equivalent when deserializing.

### Jackson Annotations

The generator honours [Jackson's annotations][6]. For example:

##### [JacksonModel Java Class](../test-types/src/main/java/org/creekservice/test/types/JacksonModel.java)
```java
@GeneratesSchema
public class JacksonModel {

    public JacksonModel(
            @JsonProperty(value = "required_prop", required = true) final String requiredProp,
            @JsonProperty("optional_prop") final Optional<String> optionalProp) {
        //...
    }

    @JsonIgnore
    public String getIgnoredProp() {
        // ...
    }

    @JsonGetter("required_prop")
    public String required() {
        // ...
    }

    @JsonGetter("optional_prop")
    public Optional<String> optional() {
        // ...
    }
}
```

...will produce the schema:

##### [JacksonModel Schema](src/test/resources/schemas/flat/org.creekservice.test.types.JacksonModel.yml)
```yaml
---
$schema: https://json-schema.org/draft/2020-12/schema
type: object
properties:
  optional_prop:
    type: string
  required_prop:
    type: string
required:
- required_prop
title: Jackson Model
additionalProperties: false
```

#### Polymorphic types

Polymorphism is supported via the standard Jackson [`@JsonTypeInfo`][7] annotation.

For example:

##### [Polymorphic Java Class](../test-types/src/main/java/org/creekservice/test/types/more/PolymorphicModel.java)
```java
@GeneratesSchema
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubType1.class, name = "type_1"),
        @JsonSubTypes.Type(value = SubType2.class, name = "type_2")
})
public interface PolymorphicModel {

    class SubType1 implements PolymorphicModel {
        public String getProp1() {
            // ...
        }
    }

    class SubType2 implements PolymorphicModel {
        public String getProp2() {
            // ...
        }
    }
}
```

...will produce the schema:

##### [Polymorphic Schema](src/test/resources/schemas/flat/org.creekservice.test.types.more.PolymorphicModel.yml)
```yaml
---
$schema: https://json-schema.org/draft/2020-12/schema
$defs:
  SubType1:
    type: object
    properties:
      prop1:
        type: string
      '@type':
        const: type_1
    title: Sub Type1
    additionalProperties: false
    required:
    - '@type'
  SubType2:
    type: object
    properties:
      prop2:
        type: string
      '@type':
        const: type_2
    title: Sub Type2
    additionalProperties: false
    required:
    - '@type'
oneOf:
- $ref: "#/$defs/SubType1"
- $ref: "#/$defs/SubType2"
```

##### Subtype discovery

The generator will search the class and module paths for subtypes of any polymorphic types that are annotated without 
a  `@JsonSubTypes` annotation to define explicit subtypes, using [ClassGraph](https://github.com/classgraph/classgraph).

For example:

##### [Thing Java Class](../test-types/src/main/java/org/creekservice/test/types/Thing.java)
```java
@GeneratesSchema
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public interface Thing {}

@JsonTypeName("big")
public class BigThing implements Thing {
    public String getProp1() {
        // ...
    }
}

private class SmallThing implements Thing {
    public String getProp2() {
        // ...
    }
}
```

...will produce the schema: 

##### [Thing Schema](src/test/resources/schemas/flat/org.creekservice.test.types.Thing.yml)
```yaml
---
$schema: https://json-schema.org/draft/2020-12/schema
$defs:
  big:
    type: object
    properties:
      prop1:
        type: string
      '@type':
        const: big
    title: Big Thing
    additionalProperties: false
    required:
    - '@type'
  SmallThing:
    type: object
    properties:
      prop2:
        type: string
      '@type':
        const: Thing$SmallThing
    title: Small Thing
    additionalProperties: false
    required:
    - '@type'
oneOf:
- $ref: "#/$defs/big"
- $ref: "#/$defs/SmallThing"
```

This behavior can be customised. See the [type scanning](#type-scanning) section for more information.

### Schema Annotations

The generator supports [Swagger v2 `@Schema` / `@ArraySchema`][8] annotations for additional schema control,
and Creek's own [`@JsonSchemaInject`][9] annotation for injecting arbitrary JSON fragments.

For example:

##### [SwaggerModel Java Class](../test-types/src/main/java/org/creekservice/test/types/SwaggerModel.java)
```java
@GeneratesSchema
@JsonSchemaInject(
        "{\"anyOf\":["
        + "{\"required\":[\"uuid\"]},"
        + "{\"required\":[\"withDescription\"]}"
        + "]}")
@Schema(title = "Custom Title")
public record SwaggerModel(
        @Schema(description = "This property has a text description.") String withDescription,
        @Schema(minLength = 1) String nonEmpty,
        @Schema(format = "uuid") String uuid,
        @ArraySchema(minItems = 1, uniqueItems = true) Set<Integer> set) {}
```

...will produce the schema:

##### [SwaggerModel Schema](src/test/resources/schemas/flat/org.creekservice.test.types.SwaggerModel.yml)
```yaml
---
$schema: https://json-schema.org/draft/2020-12/schema
type: object
properties:
  nonEmpty:
    type: string
    minLength: 1
  set:
    minItems: 1
    uniqueItems: true
    type: array
    items:
      type: integer
      minimum: -2147483648
      maximum: 2147483647
  uuid:
    type: string
    format: uuid
  withDescription:
    type: string
    description: This property has a text description.
title: Custom Title
additionalProperties: false
anyOf:
- required:
  - uuid
- required:
  - withDescription
```

Creek's `@JsonSchemaInject` annotation can be used to merge arbitrary JSON into any schema node.
For example, to inject an `anyOf` constraint at the type level:

```java
@GeneratesSchema
@JsonSchemaInject("{\"anyOf\":[{\"required\":[\"uuid\"]},{\"required\":[\"withDescription\"]}]}")
public class MyModel {
    // ...
}
```

#### Non-Java types

The generator should work with any JVM based language, for example here's a Kotlin class:

##### [KotlinModel Java Class](../test-types/src/main/kotlin/org/creekservice/test/types/KotlinModel.kt)
```kotlin
@GeneratesSchema
class KotlinModel(
   @get:JsonProperty(defaultValue = PROP3_DEFAULT_VAL) val prop3: String = PROP3_DEFAULT_VAL
) {

   companion object {
      const val PROP3_DEFAULT_VAL = "another default"
   }

   @JsonProperty(required = true)
   fun getProp1(): String {return "";}

   @Schema(defaultValue = "a default value")
   var prop2: String? = null
}
```

...will produce the schema:

##### [KotlinModel Schema](src/test/resources/schemas/flat/org.creekservice.test.types.KotlinModel.yml)
```yaml
---
$schema: https://json-schema.org/draft/2020-12/schema
type: object
properties:
   prop1:
      type: string
   prop2:
      type: string
      default: a default value
   prop3:
      type: string
      default: another default
required:
- prop1
title: Kotlin Model
additionalProperties: false
```

### Type Scanning

The generator scans the class path to: 

1. find types that require a schema generated, i.e. those annotated with `@GeneratesSchema`.
2. find subtypes of any polymorphic types it encounters that do not define an explicit set of subtypes, i.e. types
   annotated with `@JsonTypeInfo`, but not `@JsonSubTypes`.

By default, scans include the full class and module paths.  Such scans can be relatively slow *and* can result in 
unwanted schema generation, e.g. generating schema files for types found in dependencies.

Type scanning can be restricted by JPMS module name and/or Java package name. Both module and package names can include
the glob wildcard {@code *} character.

Type scanning, i.e. scanning for `@GeneratesSchema`, can be restricted using the `--type-scanning-allowed-module`
and `--type-scanning-allowed-package` command line parameters. Subtype scanning can be restricted using 
the `--subtype-scanning-allowed-module` and `--subtype-scanning-allowed-package` command line parameters. All of these
parameters can be specified multiple times on the command line to add multiple allowed module or package names.

#### Running under JPMS

When running under JPMS, Java Platform Modular System, it is necessary to `export` all packages contained 
`@GeneratesSchema` annotated types to `com.fasterxml.jackson.databind`. This is required to allow [Jackson][4] to work 
its magic and walk the object model.

Additionally, the modular system encapsulates resources, such as the generated schema files. 
If the schema files need to be accessible from outside the module, then the module descriptor must `opens`
the package containing the generated schema.

By default, the generator outputs schema files under the same directory structure as the source types.
(See `--output-strategy`).
This means if the `--out-directory` is a resource root for the project, then schema files are generated in the same
package as their source type. To expose the schema within the module, add an `opens` statement for the package containing
the source types.

For example, given types that generate schemas in an `acme.finance.model` package, ensure:

```java
module acme.model {
    // Creek annotations, e.g. @GeneratesSchema, @JsonSchemaInject
    requires transitive creek.base.annotation;
    // Jackson annotations, e.g. @JsonProperty
    requires transitive com.fasterxml.jackson.annotation;
    // Optionally, Swagger annotations, e.g. @Schema(description = ...)
    requires transitive io.swagger.v3.oas.annotations;

    // Export the model to Jackson:
    exports acme.finance.model to com.fasterxml.jackson.databind;
    // Or more normally, export the models to everyone:
    exports acme.finance.model;

    // Allow other modules to access the schemas generated into the same package:
    opens acme.finance.model;
}
```

### Allowing `null` values

While Creek strongly recommends avoid `null`s in JSON documents, just as in code, it can support `null`s. 
These are sometimes required when integrating with a 3rd-party system, which does not itself define a schema.

The `@JsonSchemaInject` annotation can be used to inject arbitrary JSON into the schema, and can be used to allow 
`null` values.

For example, the following `Model` type has a `foo` property annotated to explicitly allow either `string` or `null`
types:

```java
@GeneratesSchema
class Model {

   @JsonSchemaInject("{\"type\": [\"null\", \"string\"]}")
   public Optional<String> getFoo() {
      return s;
   }

   //...
}
```
      
The above will generate a schema with the `foo` property defined as:

```json
{
   "foo": {
      "type": [
         "null",
         "string"
      ]
   }
}
```
Meaning, documents can have `foo` set to either a string or `null` value.

[1]: https://github.com/creek-service/creek-json-schema-gradle-plugin
[2]: src/main/java/org/creekservice/api/json/schema/generator/JsonSchemaGenerator.java
[3]: https://github.com/victools/jsonschema-generator
[4]: https://github.com/FasterXML/jackson
[5]: https://json-schema.org/understanding-json-schema/reference/string.html#built-in-formats
[6]: https://github.com/FasterXML/jackson-annotations
[7]: https://fasterxml.github.io/jackson-annotations/javadoc/2.13/com/fasterxml/jackson/annotation/JsonTypeInfo.html
[8]: https://github.com/swagger-api/swagger-core/tree/master/modules/swagger-annotations
[9]: https://github.com/creek-service/creek-base/blob/main/annotation/src/main/java/org/creekservice/api/base/annotation/schema/JsonSchemaInject.java
