package io.camunda.sap_integration;

import static java.util.Map.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.sap_integration.model.ODataConnectorRequest;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Delete;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get.ODataVersionGet;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V2;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Get.ODataVersionGet.V4;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Patch;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Post;
import io.camunda.sap_integration.model.ODataConnectorRequest.HttpMethod.Put;
import io.camunda.sap_integration.model.ODataConnectorRequest.ODataVersion;
import io.camunda.sap_integration.model.ODataConnectorResponse;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ODataStandardTest {

  String tpl_Destination =
      "localMockServer"; //> just there for completeness sake, destination resolution is wired in

  // statically (see above @link mockDestination)

  private static int randomId() {
    int id = ThreadLocalRandom.current().nextInt(200, Integer.MAX_VALUE);
    return id;
  }

  @NotNull
  private static String randomString() {
    String name = "";
    for (int i = 1; i <= 10; i++) {
      name += (char) ThreadLocalRandom.current().nextInt(65, 90);
    }
    return name;
  }

  static ODataVersionGet oDataVersionGet(String protocol) {
    if (protocol.equals("V2")) {
      return new V2();
    } else if (protocol.equals("V4")) {
      return new V4(null, null);
    } else {
      throw new IllegalArgumentException("Unsupported protocol: " + protocol);
    }
  }

  @BeforeEach
  // enable static destination resolution independent of the env var
  void mockDestination() {
    DestinationAccessor.setLoader(null);
    var destination =
        DefaultHttpDestination.builder("http://localhost:4004")
            .authenticationType(AuthenticationType.BASIC_AUTHENTICATION)
            .basicCredentials("alice", "password")
            .trustAllCertificates()
            .build();
    DestinationAccessor.prependDestinationLoader((name, options) -> Try.success(destination));
  }

  @AfterEach
  void resetDestination() {
    DestinationAccessor.setLoader(null);
  }

  @ResourceLock("DestinationAccessor")
  @Nested
  class get {
    static List<Arguments> v2_get =
        Arrays.asList(
            arguments("/odata/v2/admin", "Authors(150)", "V2", "Edgar Allen Poe"),
            arguments("/odata/v2/admin", "Authors(ID=150)", "V2", "Edgar Allen Poe"),
            arguments(
                "/odata/v2/admin",
                "AuthorsByDateTimeKey(2012-04-07T23:00:00Z)",
                "V2",
                "James Lee Burke"), // 2012-04-07T23:00:00.000Z doesn't work!
            arguments(
                "/odata/v2/admin",
                "AuthorsByMultKeyDateTime(ID=4919528,dateOfBirth=2014-08-11T23:00:00Z)",
                "V2",
                "James Lee Burke") // 2014-08-11T23:00:00.000Z doesn't work!
            );
    static List<Arguments> v4_get =
        Arrays.asList(
            arguments("/admin", "Authors(150)", "V4", "Edgar Allen Poe"),
            arguments("/admin", "Authors(ID=150)", "V4", "Edgar Allen Poe"),
            arguments(
                "/admin",
                "AuthorsByDateTimeKey(2012-04-07T23:00:00Z)",
                "V4",
                "James Lee Burke"), // 2012-04-07T23:00:00.000Z doesn't work!
            arguments(
                "/admin",
                "AuthorsByMultKeyDateTime(ID=4919528,dateOfBirth=2014-08-11T23:00:00Z)",
                "V4",
                "James Lee Burke") // 2014-08-11T23:00:00.000Z doesn't work!
            );
    static List<Arguments> get_set =
        Arrays.asList(arguments("V2", "/odata/v2/admin"), arguments("V4", "/admin"));

    @DisplayName("a single entity")
    @ParameterizedTest(name = "{2} GET {1}")
    @FieldSource({"v2_get", "v4_get"})
    void entity(String path, String entity, String protocol, String expected) {

      var input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              entity,
              new Get(null, null, null, null, null, null, null, oDataVersionGet(protocol)));

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      // when
      var response = (ODataConnectorResponse) function.execute(context);
      // then
      //> REVISIT: no sig for 2nd param to "extracting" for a type cast
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(response.result().get("name").asText()).isEqualTo(expected);
    }

    @DisplayName("an entityset")
    @ParameterizedTest
    @FieldSource("get_set")
    void entity_set(String protocol, String path) {
      var input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              "Books",
              new Get(null, null, null, null, null, null, null, oDataVersionGet(protocol)));

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      // when
      var response = (ODataConnectorResponse) function.execute(context);
      // then
      //> REVISIT: no sig for 2nd param to "extracting" for a type cast
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(response.result().size()).isEqualTo(5);
      assertThat(response.result().get(0).get("title").asText()).isEqualTo("Wuthering Heights");
    }
  }

  @Nested
  class post {

    static List<Arguments> createEntity =
        Arrays.asList(arguments("V2", "/odata/v2/admin"), arguments("V4", "/admin"));

    @DisplayName("create a single entity")
    @ParameterizedTest
    @FieldSource("createEntity")
    void create(String protocol, String path) {
      String name = randomString();
      int id = randomId();
      var input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              "Authors",
              new Post(
                  ODataVersion.valueOf(protocol), ofEntries(entry("ID", id), entry("name", name))));

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = (ODataConnectorResponse) function.execute(context);
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(response.result().get("name").asText()).isEqualTo(name);
      assertThat(response).extracting("statusCode").isEqualTo(201);
    }

    @Disabled
    void deepCreate() {}
  }

  @Nested
  class put_or_patch {
    static List<Arguments> v2_put =
        Arrays.asList(
            arguments("Authors(101)", "V2", "/odata/v2/admin"),
            arguments("Authors(ID=101)", "V2", "/odata/v2/admin"),
            arguments("AuthorsByDateTimeKey(2001-01-24T23:00:00Z)", "V2", "/odata/v2/admin"),
            arguments(
                "AuthorsByMultKeyDateTime(ID=4919527,dateOfBirth=2015-10-09T23:00:00Z)",
                "V2",
                "/odata/v2/admin"));
    static List<Arguments> v4_put =
        Arrays.asList(
            arguments("Authors(101)", "V4", "/admin"),
            arguments("Authors(ID=101)", "V4", "/admin"),
            arguments("AuthorsByDateTimeKey(2001-01-24T23:00:00Z)", "V4", "/admin"),
            arguments(
                "AuthorsByMultKeyDateTime(ID=4919527,dateOfBirth=2015-10-09T23:00:00Z)",
                "V4",
                "/admin"));

    @DisplayName("replace a single entity")
    @ParameterizedTest(name = "{1} PUT {0}")
    @FieldSource({"v2_put", "v4_put"})
    void replace(String entity, String protocol, String path) {
      String name = randomString();
      var input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              entity,
              new Put(ODataVersion.valueOf(protocol), ofEntries(entry("name", name))));

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = (ODataConnectorResponse) function.execute(context);
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(response.result().get("name").asText()).isEqualTo(name);
      assertThat(response).extracting("statusCode").isEqualTo(200);
    }

    @DisplayName("update a single entity")
    @ParameterizedTest(name = "{1} PATCH {0}")
    @FieldSource({"v2_put", "v4_put"})
    void update(String entity, String protocol, String path) {
      String name = randomString();
      var input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              entity,
              new Patch(ODataVersion.valueOf(protocol), ofEntries(entry("name", name))));

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = (ODataConnectorResponse) function.execute(context);
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(response.result().get("name").asText()).isEqualTo(name);
      assertThat(response).extracting("statusCode").isEqualTo(200);
    }
  }

  @Nested
  class delete {
    static List<Arguments> deleteEntity =
        Arrays.asList(arguments("V2", "/odata/v2/admin"), arguments("V4", "/admin"));

    @DisplayName("a single entity")
    @ParameterizedTest
    @FieldSource("deleteEntity")
    void remove_an_entity(String protocol, String path) {

      // first, create a new entity
      String name = randomString();
      int id = randomId();
      var input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              "Authors",
              new Post(
                  ODataVersion.valueOf(protocol), ofEntries(entry("ID", id), entry("name", name))));

      var context = OutboundConnectorContextBuilder.create().variables(input).build();
      new ODataConnector().execute(context);

      // delete newly created entity
      input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              "Authors(" + id + ")",
              new Delete(ODataVersion.valueOf(protocol)));
      context = OutboundConnectorContextBuilder.create().variables(input).build();
      var function = new ODataConnector();
      var response = (ODataConnectorResponse) function.execute(context);

      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(response.result()).isEmpty();
      assertThat(response).extracting("statusCode").isEqualTo(204);

      // make sure it's not there anymore
      input =
          new ODataConnectorRequest(
              tpl_Destination,
              path,
              "Authors(" + id + ")",
              new Get(null, null, null, null, null, null, null, oDataVersionGet(protocol)));

      context = OutboundConnectorContextBuilder.create().variables(input).build();
      OutboundConnectorContextBuilder.TestConnectorContext finalContext = context;

      Exception exception =
          assertThrows(
              Exception.class,
              () -> {
                new ODataConnector().execute(finalContext);
              });
      var msg = exception.getMessage();

      assertThat(msg).contains("404");
    }
  }
}
