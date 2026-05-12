module creek.json.schema.test.types {
    requires creek.base.annotation;
    requires com.fasterxml.jackson.annotation;
    requires com.github.spotbugs.annotations;
    requires io.swagger.v3.oas.annotations;
    requires kotlin.stdlib;

    exports org.creekservice.test.types to
            com.fasterxml.jackson.databind;
    exports org.creekservice.test.types.more to
            com.fasterxml.jackson.databind;
}
