module creek.json.schema.generator {
    requires creek.base.annotation;
    requires creek.base.type;
    requires creek.base.schema;
    requires info.picocli;

    exports org.creek.api.json.schema.generator;

    opens org.creek.internal.json.schema.generator to
            info.picocli;
}
