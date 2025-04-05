package io.camunda.connector.sap.odata.helper;

import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestDelete;
import com.sap.cloud.sdk.datamodel.odata.client.request.UriEncodingStrategy;
import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomODataRequestDelete extends ODataRequestDelete {

  public CustomODataRequestDelete(
      @Nonnull final String servicePath,
      @Nonnull final ODataResourcePath entityPath,
      @Nullable final String versionIdentifier,
      @Nonnull final ODataProtocol protocol) {
    super(servicePath, entityPath, versionIdentifier, protocol);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Nonnull
  @Override
  public URI getRelativeUri(@Nonnull final UriEncodingStrategy strategy) {
    if (getResourcePath().toString().contains("/") && getProtocol() == ODataProtocol.V4) {
      URI intermed = super.getRelativeUri(strategy);
      return Mangler.revert(intermed.getPath());
    }
    return super.getRelativeUri(strategy);
  }
}
