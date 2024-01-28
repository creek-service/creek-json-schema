module creek.json.schema.test.types {
    requires creek.base.annotation;
    requires com.fasterxml.jackson.annotation;
    requires mbknor.jackson.jsonschema;
    requires kotlin.stdlib;

    exports org.creekservice.test.types to
            com.fasterxml.jackson.databind;
    exports org.creekservice.test.types.more to
            com.fasterxml.jackson.databind;
}
