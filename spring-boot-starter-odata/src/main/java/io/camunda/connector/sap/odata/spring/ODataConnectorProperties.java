package io.camunda.connector.sap.odata.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("camunda.connector.odata")
public record ODataConnectorProperties(boolean trustAllCertificates) {}
