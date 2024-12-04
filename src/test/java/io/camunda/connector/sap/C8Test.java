package io.camunda.connector.sap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import io.camunda.zeebe.client.ZeebeClient;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@SpringBootTest(classes = LocalConnectorRuntime.class)
@EnabledIf(
    value =
        "#{environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].startsWith('integration-c8')}",
    loadContext = true)
public class C8Test {

  Environment env;
  ZeebeClient zeebeClient;

  C8Test(@Autowired Environment environment) {
    this.env = environment;
    zeebeClient =
        ZeebeClient.newCloudClientBuilder()
            .withClusterId(env.getProperty("camunda.client.cluster-id"))
            .withClientId(env.getProperty("camunda.client.auth.client-id"))
            .withClientSecret(env.getProperty("camunda.client.auth.client-secret"))
            .withRegion(env.getProperty("camunda.client.region"))
            .build();
  }

  @BeforeAll
  static void mockDestination() {
    DestinationAccessor.setLoader(null);
    var destination =
        DefaultHttpDestination.builder("http://localhost:4004")
            .authenticationType(AuthenticationType.BASIC_AUTHENTICATION)
            .basicCredentials("alice", "password")
            .trustAllCertificates()
            .build();
    DestinationAccessor.prependDestinationLoader((name, options) -> Try.success(destination));
  }

  @SneakyThrows
  @Test
  // gets the books from the local bookshop mockserver
  // via odata v2 + v4
  void get_v2_v4() {
    zeebeClient
        .newDeployResourceCommand()
        .addResourceFromClasspath("minimal-mockserver-sample.bpmn")
        .send()
        .join();

    var processInstanceResult =
        zeebeClient
            .newCreateInstanceCommand()
            .bpmnProcessId("minimal-mockserver-sample")
            .latestVersion()
            .withResult()
            .send()
            .join();

    ArrayList result_v4 =
        (ArrayList) processInstanceResult.getVariablesAsMap().get("result_get_v4_expr");
    assertEquals("Wuthering Heights", ((Map) result_v4.get(0)).get("title"));
    assertEquals(12, ((Map) result_v4.get(0)).get("stock"));
    ArrayList result_v2 =
        (ArrayList) processInstanceResult.getVariablesAsMap().get("result_get_v2_expr");
    assertEquals("Wuthering Heights", ((Map) result_v2.get(0)).get("title"));
    assertEquals(12, ((Map) result_v2.get(0)).get("stock"));
  }
}
