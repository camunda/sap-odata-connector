package io.camunda.connector.sap.odata.spring;

import io.camunda.connector.sap.odata.ODataConnector;
import io.camunda.connector.sap.odata.ODataConnectorConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ODataConnectorProperties.class)
public class ODataConnectorAutoConfiguration {

  @Bean
  public ODataConnector oDataConnector(ODataConnectorConfiguration oDataConnectorConfiguration) {
    return new ODataConnector(oDataConnectorConfiguration);
  }

  @Bean
  public ODataConnectorConfiguration oDataConnectorConfiguration(ODataConnectorProperties properties) {
    return new ODataConnectorConfiguration(properties.trustAllCertificates());
  }
}
