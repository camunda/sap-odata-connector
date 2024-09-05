package io.camunda.sap_integration.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.camunda.connector.generator.dsl.Property.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Delete;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V2;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V4;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Patch;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Post;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Put;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Map;

public record ODataConnectorRequest(
    @TemplateProperty(
            group = "sap",
            label = "BTP destination name",
            description = "BTP destination pointing to the SAP System to connect to (e.g. a4h)",
            feel = FeelMode.optional)
        @NotEmpty
        String destination,
    @TemplateProperty(
            group = "sap",
            label = "OData base service path",
            description = "absolute base path, e.g. /sap/opu/odata/dmo/case",
            feel = FeelMode.optional)
        @Pattern(regexp = "^([/=]).*", message = "oDataService must start with a '/'")
        @NotEmpty
        String oDataService,
    @TemplateProperty(
            group = "sap",
            label = "OData Entity/-Set",
            description =
                "query target (e.g. bike(12) ), can also contain navigation properties\n( e.g. bike('12')/toWheels/toBolts )",
            feel = FeelMode.optional)
        @Pattern(regexp = "^[^/].*$", message = "entityOrEntitySet must not start with a '/'")
        @NotEmpty
        String entityOrEntitySet,
    @Valid HttpMethod httpMethod) {
  @TemplateDiscriminatorProperty(
      group = "sap",
      label = "OData version",
      description = "what version of the OData protocol the above service uses",
      name = "oDataVersion")
  public enum ODataVersion {
    V2,
    V4
  }

  @JsonTypeInfo(use = Id.NAME, property = "httpMethod")
  @JsonSubTypes({
    @Type(value = Get.class, name = "get"),
    @Type(value = Post.class, name = "post"),
    @Type(value = Put.class, name = "put"),
    @Type(value = Patch.class, name = "patch"),
    @Type(value = Delete.class, name = "delete")
  })
  @TemplateDiscriminatorProperty(
      name = "httpMethod",
      label = "Http method",
      description = "read, write, update or delete operation",
      group = "sap")
  public sealed interface HttpMethod {

    @TemplateSubType(id = "get", label = "Get")
    record Get(
        @TemplateProperty(
                group = "advanced",
                label = "$filter",
                description = "$filter on EntitySet",
                optional = true)
            String filter,
        @TemplateProperty(
                group = "advanced",
                label = "$top",
                description = "only the first $top results of an EntitySet",
                optional = true)
            Long top,
        @TemplateProperty(
                group = "advanced",
                label = "$skip",
                description = "skip the first $skip results of an EntitySet",
                optional = true)
            Long skip,
        @TemplateProperty(
                group = "advanced",
                label = "$orderBy",
                description = "order the EntitySet by one or more properties",
                optional = true)
            String orderBy,
        @TemplateProperty(
                group = "advanced",
                label = "$expand",
                description = "expand an Entity/-Set by another Entity/-Set",
                optional = true)
            String expand,
        @TemplateProperty(
                group = "select",
                label = "$select",
                description = "only select $select properties of an Entity/-Set",
                optional = true)
            String select,
        @TemplateProperty(
                group = "advanced",
                label = "$count",
                description = "$count of EntitySet",
                optional = true)
            Boolean count,
        @Valid ODataVersionGet oDataVersionGet)
        implements HttpMethod {

      @JsonTypeInfo(use = Id.NAME, property = "oDataVersionGet")
      @JsonSubTypes({@Type(value = V2.class, name = "V2"), @Type(value = V4.class, name = "V4")})
      @TemplateDiscriminatorProperty(
          group = "sap",
          label = "OData version",
          description = "what version of the OData protocol the above service uses",
          name = "oDataVersionGet")
      public sealed interface ODataVersionGet {

        @TemplateSubType(id = "V2", label = "V2")
        record V2() implements ODataVersionGet {}

        @TemplateSubType(id = "V4", label = "V4")
        record V4(
            @TemplateProperty(
                    group = "advanced",
                    label = "$inlinecount",
                    description = "$inlinecount result in EntitySet",
                    optional = true)
                Boolean inlinecount,
            @TemplateProperty(
                    group = "advanced",
                    label = "$search",
                    description = "search for $search in EntitySet",
                    optional = true)
                String search)
            implements ODataVersionGet {}
      }
    }

    @TemplateSubType(id = "post", label = "Post")
    record Post(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                description = "what version of the OData protocol the above service uses")
            ODataVersion oDataVersionPost,
        @TemplateProperty(group = "sap", label = "Request body", defaultValue = "={}") @NotEmpty
            Map<String, Object> payloadPost)
        implements HttpMethod {}

    @TemplateSubType(id = "put", label = "Put")
    record Put(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                description = "what version of the OData protocol the above service uses")
            ODataVersion oDataVersionPut,
        @TemplateProperty(group = "sap", label = "Request body", defaultValue = "={}") @NotEmpty
            Map<String, Object> payloadPut)
        implements HttpMethod {}

    @TemplateSubType(id = "patch", label = "Patch")
    record Patch(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                description = "what version of the OData protocol the above service uses")
            ODataVersion oDataVersionPatch,
        @TemplateProperty(group = "sap", label = "Request body", defaultValue = "={}") @NotEmpty
            Map<String, Object> payloadPatch)
        implements HttpMethod {}

    @TemplateSubType(id = "delete", label = "Delete")
    record Delete(
        @TemplateProperty(
                group = "sap",
                label = "OData version",
                description = "what version of the OData protocol the above service uses")
            ODataVersion oDataVersionDelete)
        implements HttpMethod {}
  }
}
