/** Module for validating JSON data against JSON Schemas. */
module creek.json.schema.validator {
    requires com.networknt.schema;
    requires com.fasterxml.jackson.databind;

    exports org.creekservice.api.json.schema.validator;
}
