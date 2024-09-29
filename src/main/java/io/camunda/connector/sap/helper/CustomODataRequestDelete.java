package io.camunda.connector.sap.helper;


import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestDelete;
import com.sap.cloud.sdk.datamodel.odata.client.request.UriEncodingStrategy;

public class CustomODataRequestDelete extends ODataRequestDelete {

  public CustomODataRequestDelete(
      @Nonnull final String servicePath,
      @Nonnull final ODataResourcePath entityPath,
      @Nullable final String versionIdentifier,
      @Nonnull final ODataProtocol protocol) {
    super(servicePath, entityPath, versionIdentifier, protocol);
  }

  @Nonnull
  @Override
  public URI getRelativeUri(@Nonnull final UriEncodingStrategy strategy) {
    if (getResourcePath().toString().contains("/") && getProtocol() == ODataProtocol.V4) {
      return super.getRelativeUri(UriEncodingStrategy.NONE);
    }
    return super.getRelativeUri(strategy);
  }
}