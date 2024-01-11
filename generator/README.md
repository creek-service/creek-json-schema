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

   public Optional<String> getStringProp() {
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

Properties for both the getters have been included in the above schema. 

The integer property is marked as required, as primitive types can't be `null`.

The string property is not marked as required, as non-primitive types can be null. 
However, the schema intentionally does not allow the string property to be explicitly set to `null`.
(Defaults in Creek encourage developers away from using `null` values).
Instead, any JSON that omits the string property is valid, 
i.e. rather than setting `stringProp` to `null` if it's not provided, the `stringProp` property should just not be set.

While Creek strongly recommends avoiding the use of `null` in JSON documents, just as it recommends avoiding `null` in 
Java code, it is still possible to build a schema that accepts `null` values. 
See [allowing `null` values](#allowing-null-values) for more info.

> ## NOTE
> It is important to ensure properties with `null` values are excluded when serialising data to JSON.
> Failure to do so may result in schema validation failure.
> Creek's own serializers do this by default.

It is recommended, but not required by the plugin, to use the `Optional` standard Java type for optional properties. 

Non-primitive properties can be marked required using [Jackson annotations](#jackson-annotations).

### Type mapping

As you can see from the simple model example above, the generator automatically converts some standard Java types 
to their JSON schema counterparts. 

The generator will also leverage the [JSON Schema's `format` specifier][5] to convert the types below:

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

This behavior can be customised. See the [type scanning](#type-scanning) section for more information.

### JsonSchema Annotations

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
class KotlinModel(
   @get:JsonProperty(defaultValue = PROP3_DEFAULT_VAL) val prop3: String = PROP3_DEFAULT_VAL
) {

   companion object {
      const val PROP3_DEFAULT_VAL = "another default"
   }

   @JsonProperty(required = true)
   fun getProp1(): String {return "";}

   @JsonSchemaDefault("a default value")
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
      default: a default value
   prop3:
      type: string
      default: another default
required:
   - prop1
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
its magic and walk the object model. For example:

```java
module acme.model {
    requires creek.base.annotation;
    requires com.fasterxml.jackson.annotation;
    requires mbknor.jackson.jsonschema;

    exports acme.finance.model to com.fasterxml.jackson.databind;
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
 
   @JsonSchemaInject(json = "{\"type\": [\"null\", \"string\"]}")
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
[3]: https://github.com/mbknor/mbknor-jackson-jsonSchema
[4]: https://github.com/FasterXML/jackson
[5]: https://json-schema.org/understanding-json-schema/reference/string.html#built-in-formats
[6]: https://github.com/FasterXML/jackson-annotations
[7]: https://fasterxml.github.io/jackson-annotations/javadoc/2.13/com/fasterxml/jackson/annotation/JsonTypeInfo.html

