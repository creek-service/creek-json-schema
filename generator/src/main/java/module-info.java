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
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires mbknor.jackson.jsonschema;
    requires classgraph;
    requires scala.library;
    requires com.github.spotbugs.annotations;

    exports org.creekservice.api.json.schema.generator;

    opens org.creekservice.internal.json.schema.generator.cli to
            info.picocli;
}
