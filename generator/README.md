# Creek JSON schema generator

A command line tool for generating JSON schemas from code.

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

[1]: https://github.com/creek-service/creek-json-schema-gradle-plugin
[2]: src/main/java/org/creekservice/api/json/schema/generator/JsonSchemaGenerator.java
