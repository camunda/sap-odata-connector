package io.camunda.connector.sap.odata.helper;

import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestRead;
import com.sap.cloud.sdk.datamodel.odata.client.request.UriEncodingStrategy;
import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomODataRequestRead extends ODataRequestRead {

  public CustomODataRequestRead(
      @Nonnull final String servicePath,
      @Nonnull final String entityName,
      @Nullable final String encodedQuery,
      @Nonnull final ODataProtocol protocol) {
    super(servicePath, entityName, encodedQuery, protocol);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Nonnull
  @Override
  public URI getRelativeUri(@Nonnull final UriEncodingStrategy strategy) {
    if (this.getResourcePath().toString().contains("/") && getProtocol() == ODataProtocol.V4) {
      URI intermed = super.getRelativeUri(strategy);
      return Mangler.revert(intermed.toString());
    } else {
      return super.getRelativeUri(strategy);
    }
  }
}
