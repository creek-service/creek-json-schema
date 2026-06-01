/** Module for validating JSON data against JSON Schemas. */
module creek.json.schema.validator {
    requires com.networknt.schema;
    requires tools.jackson.databind;

    // networknt only declares jackson-dataformat-yaml as `requires static` (optional), but
    // required:
    requires tools.jackson.dataformat.yaml;

    exports org.creekservice.api.json.schema.validator;
}
