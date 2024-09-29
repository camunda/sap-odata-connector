package io.camunda.connector.sap.helper;


import java.net.URI;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sap.cloud.sdk.datamodel.odata.client.ODataProtocol;
import com.sap.cloud.sdk.datamodel.odata.client.expression.ODataResourcePath;
import com.sap.cloud.sdk.datamodel.odata.client.query.StructuredQuery;
import com.sap.cloud.sdk.datamodel.odata.client.request.ODataRequestRead;
import com.sap.cloud.sdk.datamodel.odata.client.request.UriEncodingStrategy;

public class CustomODataRequestRead extends ODataRequestRead {

  public CustomODataRequestRead(
      @Nonnull final String servicePath,
      @Nonnull final String entityName,
      @Nullable final String encodedQuery,
      @Nonnull final ODataProtocol protocol) {
    super(servicePath, entityName, encodedQuery, protocol);
  }

  public CustomODataRequestRead(
      @Nonnull final String servicePath,
      @Nonnull final ODataResourcePath entityPath,
      @Nullable final String encodedQuery,
      @Nonnull final ODataProtocol protocol) {
    super(servicePath, entityPath, encodedQuery, protocol);
  }

  public CustomODataRequestRead(
      @Nonnull final String servicePath,
      @Nonnull final ODataResourcePath entityPath,
      @Nonnull final StructuredQuery query) {
    super(servicePath, entityPath, query);
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