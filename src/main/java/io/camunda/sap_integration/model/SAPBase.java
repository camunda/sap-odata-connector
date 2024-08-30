package io.camunda.sap_integration.model;

import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty.PropertyCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record SAPBase(
    // destination
    @NotEmpty @TemplateProperty(group = "sap",
        label = "BTP destination name (e.g. a4h)",
        description = "BTP destination pointing to the SAP System to connect to",
        feel = FeelMode.optional) String destination,
    // httpMethod
    @TemplateProperty(group = "sap",
    label = "HTTP Method",
    description = "read, write, update or delete operation?",
    type = TemplateProperty.PropertyType.Dropdown,
    choices = {
        @TemplateProperty.DropdownPropertyChoice(value = "GET", label = "GET"),
        @TemplateProperty.DropdownPropertyChoice(value = "POST", label = "POST"),
        @TemplateProperty.DropdownPropertyChoice(value = "POST", label = "POST"),
        @TemplateProperty.DropdownPropertyChoice(value = "PATCH", label = "PATCH"),
        @TemplateProperty.DropdownPropertyChoice(value = "DELETE", label = "DELETE"),
    },
    defaultValue = "GET") @NotBlank @NotEmpty String HttpMethod,
    //
    @NotEmpty @TemplateProperty(group = "sap",
    label = "OData Entity/-Set",
    description = "query target (e.g. bike(12) ), can also contain navigation properties\\n( e.g. bike('12')/toWheels/toBolts )",
    feel = FeelMode.optional) String entityOrEntitySet, @TemplateProperty(group = "sap",
    label = "HTTP Method",
    description = "OData version",
    type = TemplateProperty.PropertyType.Dropdown,
    choices = {
        @TemplateProperty.DropdownPropertyChoice(value = "v2", label = "v2"),
        @TemplateProperty.DropdownPropertyChoice(value = "v4", label = "v4")
    },
    defaultValue = "v4") @NotBlank @NotEmpty String odataVersion, @TemplateProperty(group = "sap",
    id = "payload",
    label = "Payload",
    description = "JSON payload to send to the SAP System",
    feel = FeelMode.optional,
    condition = @PropertyCondition(property = "HttpMethod", oneOf = {
        "PATCH", "POST"
    })) String payload) {}
