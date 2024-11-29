package io.camunda.connector.sap;

// import static io.camunda.process.test.api.CamundaAssert.assertThat;

import io.camunda.connector.e2e.BpmnFile;
import io.camunda.connector.e2e.ZeebeTest;
import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.process.test.assertions.BpmnAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = LocalConnectorRuntime.class)
@TestPropertySource(locations = "classpath:application.properties")
public class LocalConnectorRuntimeTest {

  @Autowired Environment env;

  @Test
  void contextLoads() {
    var zeebeClient =
        ZeebeClient.newCloudClientBuilder()
            .withClusterId(env.getProperty("camunda.client.cluster-id"))
            .withClientId(env.getProperty("camunda.client.auth.client-id"))
            .withClientSecret(env.getProperty("camunda.client.auth.client-secret"))
            .withRegion(env.getProperty("camunda.client.region"))
            .build();
    var model = BpmnFile.replace("minimal-mockserver-sample.bpmn");
    var bpmnTest =
        ZeebeTest.with(zeebeClient).deploy(model).createInstance().waitForProcessCompletion();
    var processInstanceEvent = bpmnTest.getProcessInstanceEvent();
    BpmnAssert.assertThat(processInstanceEvent).hasVariableWithValue("result_get_v4_expr", "42");
  }
}
