# Creek JSON schema generator

A command line tool for generating JSON schemas from code.

## Executing the schema generator

> ## NOTE
> There is a [Gradle plugin][1] for generating JSON schemas as part of your build process.

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
JSON schema for each. Internally it is using the [Jackson JSON Schema Generator][3]. It honours
both [Jackson's][4] annotations and the [generator's][3] annotations.

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
@GeneatesSchema
public class SimpleModel {
    public int getIntProp() {
        // ...
    }
    
    public String getStringProp() {
        // ...
    }
}
```

...will produce the schema:

##### [SimpleModel Schema](src/test/resources/test/types/org.creekservice.test.types.simple_model.yml)
```yaml
---
$schema: http://json-schema.org/draft-07/schema#
title: Simple Model
type: object
additionalProperties: false
properties:
  intProp:
    type: integer
  stringProp:
    type: string
required:
- intProp
```

Properties for both the getters have been included in the above schema, and the integer property is marked as required
as the return value from the getter can not be null.

### Type mapping

As you can see from the simple model example above, the generator automatically converts some standard Java types 
to their JSON schema counterparts. The generator will also leverage the [JSON Schema's `format` specifier][5] for
the some types and formats some others as multi-property objects:

| Java type       | Schema Type                                                                                                                 | 
|-----------------|-----------------------------------------------------------------------------------------------------------------------------|
| `URI`           | type: string <br> format: uri                                                                                               |
| `UUID`          | type: string <br> format: uuid                                                                                              |
| `LocalDate`     | type: string <br> format: date                                                                                              |
| `LocalTime`     | type: string <br> format: time                                                                                              |
| `LocalDateTime` | type: string <br> format: date-time                                                                                         |
| `ZonedDateTime` | type: string <br> format: date-time                                                                                         |
| `Instant`       | type: object <br> properties: <br>&nbsp;seconds <br>&nbsp;&nbsp; type: integer<br>&nbsp;nanos <br>&nbsp;&nbsp;type: integer |

For example:

##### [FormatModel Java Class](../test-types/src/main/java/org/creekservice/test/types/FormatModel.java)
```java
@GeneratesSchema
public class FormatModel {
    public URI getURI() {
        // ...
    }
    
    public Instant getInstant() {
        // ...
    }

    public ZonedDateTime getDateTime() { 
        // ...
    }
}
```

...will produce the schema:

##### [FormatModel Schema](src/test/resources/test/types/org.creekservice.test.types.format_model.yml)
```yaml
---
$schema: http://json-schema.org/draft-07/schema#
title: Format Model
type: object
additionalProperties: false
properties:
  dateTime:
    type: string
    format: date-time
  instant:
    $ref: '#/definitions/Instant'
  uri:
    type: string
    format: uri
definitions:
  Instant:
    type: object
    additionalProperties: false
    properties:
      nanos:
        type: integer
      seconds:
        type: integer
    required:
      - nanos
      - seconds
```

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

##### [JacksonModel Schema](src/test/resources/test/types/org.creekservice.test.types.jackson_model.yml)
```yaml
---
$schema: http://json-schema.org/draft-07/schema#
title: Jackson Model
type: object
additionalProperties: false
properties:
  optional_prop:
    type: string
  required_prop:
    type: string
required:
  - required_prop
```

#### Polymorphic types

Polymorphism is supported via the standard Jackson [`@JsonTypeInfo`][7] annotation.

For example:

##### [Polymorphic Java Class](../test-types/src/main/java/org/creekservice/test/types/PolymorphicModel.java)
```java
@GeneratesSchema
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SubType1.class, name = "type_1"),
        @JsonSubTypes.Type(value = SubType2.class, name = "type_2")
})
public interface PolymorphicModel {}

public class SubType1 implements PolymorphicModel {
    public String getProp1() {
        // ...
    }
}

private class SubType2 implements PolymorphicModel {
    public String getProp2() {
        // ...
    }
}
```

...will produce the schema:

##### [Polymorphic Schema](src/test/resources/test/types/org.creekservice.test.types.polymorphic_model.yml)
```yaml
---
$schema: http://json-schema.org/draft-07/schema#
title: Polymorphic Model
oneOf:
  - $ref: '#/definitions/SubType1'
  - $ref: '#/definitions/SubType2'
definitions:
  SubType1:
    type: object
    additionalProperties: false
    properties:
      '@type':
        type: string
        enum:
          - type_1
        default: type_1
      prop1:
        type: string
    title: type_1
    required:
      - '@type'
  SubType2:
    type: object
    additionalProperties: false
    properties:
      '@type':
        type: string
        enum:
          - type_2
        default: type_2
      prop2:
        type: string
    title: type_2
    required:
      - '@type'
```

##### Subtype discovery

The generator will search the class and module paths for subtypes of any polymorphic types that are annotated without 
a  `@JsonSubTypes` annotation to define explicit subtypes.

For example:

##### [Thing Java Class](../test-types/src/main/java/org/creekservice/test/types/Thing.java)
```java
@GeneratesSchema
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public interface Thing {}

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

##### [Thing Schema](src/test/resources/test/types/org.creekservice.test.types.thing.yml)
```yaml
---
$schema: http://json-schema.org/draft-07/schema#
title: Thing
oneOf:
  - $ref: '#/definitions/SmallThing'
  - $ref: '#/definitions/BigThing'
definitions:
  SmallThing:
    type: object
    additionalProperties: false
    properties:
      '@type':
        type: string
        enum:
          - small
        default: small
      prop2:
        type: string
    title: small
    required:
      - '@type'
  BigThing:
    type: object
    additionalProperties: false
    properties:
      '@type':
        type: string
        enum:
          - big
        default: big
      prop1:
        type: string
    title: big
    required:
      - '@type'
```

## JsonSchema Annotations

The generator also supports the [JsonSchema annotations][3]. These allow much more control of the generated schema, 
including allowing for arbitrary schema elements to be injected.

For example:

##### [JsonSchemaModel Java Class](../test-types/src/main/java/org/creekservice/test/types/JsonSchemaModel.java)
```java
@GeneratesSchema
// Inject requirement for `thing` OR `listProp`:
@JsonSchemaInject(json =  "{\"anyOf\":[" +
        "{\"required\":[\"uuid\"]}," +
        "{\"required\":[\"with_description\"]}" +
        "]}")
@JsonSchemaTitle("Custom Title")
public final class JsonSchemaModel {

    // Add a description:
    @JsonSchemaDescription("This property has a text description.")
    public String getWithDescription() {
        return null;
    }

    // Inject minLength requirement,  i.e. if supplied, it can't be empty:
    @JsonSchemaInject(ints = @JsonSchemaInt(path = "minLength", value = 1))
    public String getNonEmpty() {
        // ...
    }

    // Specify a format:
    @JsonSchemaFormat("uuid")
    public String getUuid() {
        // ...
    }
    
    // Specify in schema that items are unique and collection is not empty:
    @JsonSchemaInject(
            ints = {@JsonSchemaInt(path = "minItems", value = 1)},
            bools = {@JsonSchemaBool(path = "uniqueItems", value = true)})
    public Set<Integer> getSet() {
        // ...
    }
}
```

...will produce the schema:

##### [JsonSchemaModel Schema](src/test/resources/test/types/org.creekservice.test.types.json_schema_model.yml)
```yaml
---
$schema: http://json-schema.org/draft-07/schema#
title: Custom Title
type: object
additionalProperties: false
anyOf:
  - required:
      - uuid
  - required:
      - with_description
properties:
  nonEmpty:
    type: string
    minLength: 1
  set:
    type: array
    items:
      type: integer
    minItems: 1
    uniqueItems: true
  uuid:
    type: string
    format: uuid
  withDescription:
    type: string
    description: This property has a text description.
```

#### Non-Java types

The generator should work with any JVM based language, for example here's a Kotlin class:

##### [KotlinModel Java Class](../test-types/src/main/kotlin/org/creekservice/test/types/KotlinModel.kt)
```kotlin
@GeneratesSchema
class KotlinModel {
    fun getProp1(): String? {return null;}

    var prop2: String? = null
}
```

...will produce the schema:

##### [JsonSchemaModel Schema](src/test/resources/test/types/org.creekservice.test.types.kotlin_model.yml)
```yaml
---
$schema: http://json-schema.org/draft-07/schema#
title: Kotlin Model
type: object
additionalProperties: false
properties:
  prop1:
    type: string
  prop2:
    type: string
```

## Type Scanning

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

### Running under JPMS

When running under JPMS, Java Platform Modular System, it is necessary to `export` all packages contained 
`@GeneratesSchema` annotated types to `com.fasterxml.jackson.databind`. This is required to allow [Jackson][4] to work 
its magic and walk the object model. For example:

```java
module acme.model {
    requires creek.base.annotation;
    requires com.fasterxml.jackson.annotation;
    requires mbknor.jackson.jsonschema;

    exports acme.finance.model to com.fasterxml.jackson.databind;
}
```

[1]: https://github.com/creek-service/creek-json-schema-gradle-plugin
[2]: src/main/java/org/creekservice/api/json/schema/generator/JsonSchemaGenerator.java
[3]: https://github.com/mbknor/mbknor-jackson-jsonSchema
[4]: https://github.com/FasterXML/jackson
[5]: https://json-schema.org/understanding-json-schema/reference/string.html#built-in-formats
[6]: https://github.com/FasterXML/jackson-annotations
[7]: https://fasterxml.github.io/jackson-annotations/javadoc/2.13/com/fasterxml/jackson/annotation/JsonTypeInfo.html

