package io.camunda.connector.sap.helper;

import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestCreate;
import com.sap.cloud.sdk.datamodel.odata.client.request.UriEncodingStrategy;
import java.net.URI;
import javax.annotation.Nonnull;

public class CustomODataRequestCreate extends ODataRequestCreate {

  public CustomODataRequestCreate(
      @Nonnull final String servicePath,
      @Nonnull final ODataResourcePath entityPath,
      @Nonnull final String serializedEntity,
      @Nonnull final ODataProtocol protocol) {
    super(servicePath, entityPath, serializedEntity, protocol);
  }

  @Nonnull
  @Override
  public URI getRelativeUri(@Nonnull final UriEncodingStrategy strategy) {
    if (this.getResourcePath().toString().contains("/") && getProtocol() == ODataProtocol.V4) {
      return super.getRelativeUri(UriEncodingStrategy.NONE);
    } else {
      return super.getRelativeUri(strategy);
    }
  }
}
