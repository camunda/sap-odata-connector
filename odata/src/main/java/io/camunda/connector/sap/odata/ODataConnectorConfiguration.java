package io.camunda.connector.sap.odata;

public record ODataConnectorConfiguration(boolean trustAllCertificates) {
  public static final boolean DEFAULT_TRUST_ALL_CERTIFICATES = false;
  private static ODataConnectorConfiguration instance = new ODataConnectorConfiguration(DEFAULT_TRUST_ALL_CERTIFICATES);

  public static ODataConnectorConfiguration getInstance() {
    return instance;
  }

  public static void setInstance(ODataConnectorConfiguration instance) {
    if (instance != null) {
      ODataConnectorConfiguration.instance = instance;
    }
  }
}
