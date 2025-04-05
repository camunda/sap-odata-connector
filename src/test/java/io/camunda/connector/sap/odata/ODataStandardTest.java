package io.camunda.connector.sap.odata;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import io.camunda.connector.sap.odata.model.HttpMethod;
import io.camunda.connector.sap.odata.model.ODataConnectorRequest;
import io.camunda.connector.sap.odata.model.ODataConnectorResponse;
import io.camunda.connector.sap.odata.model.ODataConnectorResponseWithCount;
import io.camunda.connector.sap.odata.model.ODataRequestDetails.SimpleRequest;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.vavr.control.Try;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
      "localMockServer"; //> just there for completness sake, destination resolution is wired in

  // statically (see above @link mockDestination)

  private static int randomId() {
    int id = ThreadLocalRandom.current().nextInt(200, Integer.MAX_VALUE);
    return id;
  }

  @NotNull
  private static String randomString() {
    StringBuilder name = new StringBuilder();
    for (int i = 1; i <= 10; i++) {
      name.append((char) ThreadLocalRandom.current().nextInt(65, 90));
    }
    return name.toString();
  }

  static HttpMethod.Get.ODataVersionGet oDataVersionGet(String protocol) {
    if (protocol.equals("V2")) {
      return new HttpMethod.Get.ODataVersionGet.V2(false);
    } else if (protocol.equals("V4")) {
      return new HttpMethod.Get.ODataVersionGet.V4(null, null);
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

    static List<Arguments> get_with_count =
        Arrays.asList(arguments("V2", "/odata/v2/admin"), arguments("V4", "/admin"));

    static List<Arguments> get_set =
        Arrays.asList(arguments("V2", "/odata/v2/admin"), arguments("V4", "/admin"));

    static List<Arguments> get_set_with_orderby =
        Arrays.asList(
            arguments("V2", "/odata/v2/admin", "Books"),
            arguments("V2", "/odata/v2/admin", "Books"),
            arguments("V4", "/admin", "Books"),
            arguments("V4", "/admin", "Books"));

    @DisplayName("a single entity")
    @ParameterizedTest(name = "{2} GET {1}")
    @FieldSource({"v2_get", "v4_get"})
    void entity(String path, String entity, String protocol, String expected) {
      var httpMethod =
          new HttpMethod.Get(null, null, null, null, null, null, oDataVersionGet(protocol));
      var requestDetails = new SimpleRequest(entity, httpMethod, null);

      var input = new ODataConnectorRequest(tpl_Destination, path, requestDetails);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = function.execute(context);

      //> REVISIT: no sig for 2nd param to "extracting" for a type cast
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(((ODataConnectorResponse) response).result().get("name").asText())
          .isEqualTo(expected);
    }

    @Test
    @DisplayName("validate URL-safe encoding of $filter")
    void validate_encoded_filter() {
      var httpMethod =
          new HttpMethod.Get(
              "name eq 'Edgar Allen Poe'",
              null,
              null,
              null,
              null,
              null,
              new HttpMethod.Get.ODataVersionGet.V2(false));
      var requestDetails = new SimpleRequest("Authors", httpMethod, null);

      var input = new ODataConnectorRequest(tpl_Destination, "/odata/v2/admin", requestDetails);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = function.execute(context);

      //> REVISIT: no sig for 2nd param to "extracting" for a type cast
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(((ODataConnectorResponse) response).result().get(0).get("name").asText())
          .isEqualTo("Edgar Allen Poe");
    }

    @DisplayName("test for presence of $count (v4) and $inlinecount=allpages (v2)")
    @ParameterizedTest(name = "{2} GET {1}")
    @FieldSource("get_with_count")
    void entity_count(String protocol, String path) {
      var httpMethodWithCount =
          protocol.equals("V2")
              ? new HttpMethod.Get(
                  null, null, null, null, null, null, new HttpMethod.Get.ODataVersionGet.V2(true))
              : new HttpMethod.Get(
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  new HttpMethod.Get.ODataVersionGet.V4(null, true));

      var requestDetailsWithCount = new SimpleRequest("Books", httpMethodWithCount, null);
      var inputWithCount =
          new ODataConnectorRequest(tpl_Destination, path, requestDetailsWithCount);
      var contextWithCount =
          OutboundConnectorContextBuilder.create().variables(inputWithCount).build();

      var httpMethod =
          protocol.equals("V2")
              ? new HttpMethod.Get(
                  null, null, null, null, null, null, new HttpMethod.Get.ODataVersionGet.V2(false))
              : new HttpMethod.Get(
                  null,
                  null,
                  null,
                  null,
                  null,
                  null,
                  new HttpMethod.Get.ODataVersionGet.V4(null, false));

      var requestDetails = new SimpleRequest("Books", httpMethod, null);
      var input = new ODataConnectorRequest(tpl_Destination, path, requestDetails);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();

      var responseWithCount = function.execute(contextWithCount);
      var response = function.execute(context);

      //> REVISIT: no sig for 2nd param to "extracting" for a type cast
      assertThat(responseWithCount).extracting("result").isNotEqualTo("NOK");
      assertThat(((ODataConnectorResponseWithCount) responseWithCount).result().size())
          .isEqualTo(5);
      assertThat(((ODataConnectorResponseWithCount) responseWithCount).countOrInlineCount())
          .isEqualTo(5);

      assertThat(
              Arrays.stream(response.getClass().getDeclaredMethods())
                  .noneMatch(method -> method.getName().contains("countOrInlineCount")))
          .isTrue();
    }

    @DisplayName("an entityset")
    @ParameterizedTest
    @FieldSource("get_set")
    void entity_set(String protocol, String path) {
      var httpMethod =
          new HttpMethod.Get(null, null, null, null, null, null, oDataVersionGet(protocol));
      var requestDetails = new SimpleRequest("Books", httpMethod, null);

      var input = new ODataConnectorRequest(tpl_Destination, path, requestDetails);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      // when
      var response = function.execute(context);
      // then
      //> REVISIT: no sig for 2nd param to "extracting" for a type cast
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(((ODataConnectorResponse) response).result().size()).isEqualTo(5);
      assertThat(((ODataConnectorResponse) response).result().get(0).get("title").asText())
          .isEqualTo("Wuthering Heights");
    }

    @DisplayName("an entityset order by $orderby")
    @ParameterizedTest(name = "{0}, GET {1}, entity {2}")
    @FieldSource("get_set_with_orderby")
    void entity_set_ordered(String protocol, String path, String entity) {
      var httpMethodSortAsc =
          new HttpMethod.Get(null, null, null, "title", null, "title", oDataVersionGet(protocol));
      var requestDetailsSortAsc = new SimpleRequest(entity, httpMethodSortAsc, null);
      var input_sort_asc = new ODataConnectorRequest(tpl_Destination, path, requestDetailsSortAsc);

      var httpMethodSortDesc =
          new HttpMethod.Get(
              null, null, null, "title desc", null, "title", oDataVersionGet(protocol));
      var requestDetailsSortDesc = new SimpleRequest("Books", httpMethodSortDesc, null);
      var input_sort_desc =
          new ODataConnectorRequest(tpl_Destination, path, requestDetailsSortDesc);

      var context_sort_asc =
          OutboundConnectorContextBuilder.create().variables(input_sort_asc).build();
      var context_sort_desc =
          OutboundConnectorContextBuilder.create().variables(input_sort_desc).build();

      var function = new ODataConnector();
      var response_sort_asc = function.execute(context_sort_asc);
      var response_sort_desc = function.execute(context_sort_desc);

      assertThat(((ODataConnectorResponse) response_sort_asc).result())
          .extracting(node -> node.get("title").asText())
          .containsExactly("Catweazle", "Eleonora", "Jane Eyre", "The Raven", "Wuthering Heights");

      assertThat(((ODataConnectorResponse) response_sort_desc).result())
          .extracting(node -> node.get("title").asText())
          .containsExactly("Wuthering Heights", "The Raven", "Jane Eyre", "Eleonora", "Catweazle");
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
      Map<String, Object> payload = ofEntries(entry("ID", id), entry("name", name));

      var httpMethod = new HttpMethod.Post(HttpMethod.ODataVersion.valueOf(protocol));
      var requestDetails = new SimpleRequest("Authors", httpMethod, payload);

      var input = new ODataConnectorRequest(tpl_Destination, path, requestDetails);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = function.execute(context);
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(((ODataConnectorResponse) response).result().get("name").asText()).isEqualTo(name);
      assertThat(response).extracting("statusCode").isEqualTo(201);
    }

    @Test
    void v4_bound_action() {
      var httpMethod = new HttpMethod.Post(HttpMethod.ODataVersion.valueOf("V4"));
      var requestDetails = new SimpleRequest("Books(201)/SetStatusReject", httpMethod, null);

      var input = new ODataConnectorRequest(tpl_Destination, "/admin", requestDetails);
      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = function.execute(context);
      assertThat(((ODataConnectorResponse) response).result().asText()).isEqualTo("works");
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
      Map<String, Object> payload = ofEntries(entry("name", name));

      var httpMethod = new HttpMethod.Put(HttpMethod.ODataVersion.valueOf(protocol));
      var requestDetails = new SimpleRequest(entity, httpMethod, payload);

      var input = new ODataConnectorRequest(tpl_Destination, path, requestDetails);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = function.execute(context);
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(((ODataConnectorResponse) response).result().get("name").asText()).isEqualTo(name);
      assertThat(response).extracting("statusCode").isEqualTo(200);
    }

    @DisplayName("update a single entity")
    @ParameterizedTest(name = "{1} PATCH {0}")
    @FieldSource({"v2_put", "v4_put"})
    void update(String entity, String protocol, String path) {
      String name = randomString();
      Map<String, Object> payload = ofEntries(entry("name", name));

      var httpMethod = new HttpMethod.Patch(HttpMethod.ODataVersion.valueOf(protocol));
      var requestDetails = new SimpleRequest(entity, httpMethod, payload);

      var input = new ODataConnectorRequest(tpl_Destination, path, requestDetails);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();

      var function = new ODataConnector();
      var response = function.execute(context);
      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(((ODataConnectorResponse) response).result().get("name").asText()).isEqualTo(name);
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
      Map<String, Object> payload = ofEntries(entry("ID", id), entry("name", name));

      var httpMethodPost = new HttpMethod.Post(HttpMethod.ODataVersion.valueOf(protocol));
      var requestDetailsPost = new SimpleRequest("Authors", httpMethodPost, payload);
      var input = new ODataConnectorRequest(tpl_Destination, path, requestDetailsPost);

      var context = OutboundConnectorContextBuilder.create().variables(input).build();
      new ODataConnector().execute(context);

      // delete newly created entity
      var httpMethodDelete = new HttpMethod.Delete(HttpMethod.ODataVersion.valueOf(protocol));
      var requestDetailsDelete = new SimpleRequest("Authors(" + id + ")", httpMethodDelete, null);
      input = new ODataConnectorRequest(tpl_Destination, path, requestDetailsDelete);

      context = OutboundConnectorContextBuilder.create().variables(input).build();
      var function = new ODataConnector();
      var response = (ODataConnectorResponse) function.execute(context);

      assertThat(response).extracting("result").isNotEqualTo("NOK");
      assertThat(response.result()).isEmpty();
      assertThat(response).extracting("statusCode").isEqualTo(204);

      // make sure it's not there anymore
      var httpMethodGet =
          new HttpMethod.Get(null, null, null, null, null, null, oDataVersionGet(protocol));
      var requestDetailsGet = new SimpleRequest("Authors(" + id + ")", httpMethodGet, null);
      input = new ODataConnectorRequest(tpl_Destination, path, requestDetailsGet);

      context = OutboundConnectorContextBuilder.create().variables(input).build();
      OutboundConnectorContextBuilder.TestConnectorContext finalContext = context;

      Exception exception =
          assertThrows(Exception.class, () -> new ODataConnector().execute(finalContext));
      var msg = exception.getMessage();

      assertThat(msg).contains("404");
    }
  }
}
