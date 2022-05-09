module creek.json.schema.generator {
    requires creek.base.annotation;
    requires creek.base.type;
    requires creek.base.schema;
    requires info.picocli;
    requires org.apache.logging.log4j;
    requires java.management;

    exports org.creekservice.api.json.schema.generator;

    opens org.creekservice.internal.json.schema.generator to
            info.picocli;
}
