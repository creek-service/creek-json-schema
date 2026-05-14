/** Module for generating Json/YAML schemas from types. */
module creek.json.schema.generator {
    requires creek.base.annotation;
    requires creek.base.type;
    requires creek.base.schema;
    requires info.picocli;
    requires org.apache.logging.log4j;
    requires org.slf4j;
    requires java.management;
    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.databind;
    requires tools.jackson.dataformat.yaml;
    requires com.fasterxml.classmate;
    requires com.github.victools.jsonschema.generator;
    requires com.github.victools.jsonschema.module.jackson;
    requires com.github.victools.jsonschema.module.swagger.two;
    requires io.swagger.v3.oas.annotations;
    requires io.github.classgraph;
    requires com.github.spotbugs.annotations;

    exports org.creekservice.api.json.schema.generator;

    opens org.creekservice.internal.json.schema.generator.cli to
            info.picocli;
}
