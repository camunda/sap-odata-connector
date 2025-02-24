package io.camunda.connector.sap.odata;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sap.cloud.sdk.cloudplatform.connectivity.*;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.sap.odata.model.ErrorCodes;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
public class DestinationProviderTest {

  @Mock private Destination mockDestination;

  @BeforeEach
  public void setUp() {
    reset(mockDestination);
  }

  @Test
  public void success() {
    DestinationAccessor.setLoader(null);
    var destination =
        DefaultHttpDestination.builder("http://localhost:4004")
            .property("name", "httpDestination")
            .build();
    DestinationAccessor.prependDestinationLoader((name, options) -> Try.success(destination));

    var resolvedDestination =
        DestinationProvider.getDestination("httpDestination", DestinationType.HTTP);

    assertEquals("http://localhost:4004", resolvedDestination.asHttp().getUri().toString());
    DestinationAccessor.setLoader(null);
  }

  @Test
  public void retry_then_success() {
    String destinationName = "notImportant";
    try (MockedStatic<DestinationAccessor> mockedDestinationAccessor =
        mockStatic(DestinationAccessor.class)) {
      mockedDestinationAccessor
          .when(() -> DestinationAccessor.getDestination(destinationName))
          .thenThrow(new RuntimeException("Destination not found"))
          .thenThrow(new RuntimeException("Destination not found"))
          .thenThrow(new RuntimeException("Destination not found"))
          .thenReturn(mockDestination);

      DestinationProvider.getDestination(destinationName, DestinationType.HTTP);

      mockedDestinationAccessor.verify(
          () -> DestinationAccessor.getDestination(destinationName), times(4));
    }
  }

  @Test
  public void retry_then_fail() {
    try (MockedStatic<DestinationAccessor> mockedDestinationAccessor =
        mockStatic(DestinationAccessor.class)) {
      mockedDestinationAccessor
          .when(() -> DestinationAccessor.getDestination("invalidDestination"))
          .thenThrow(new RuntimeException("Destination not found"));

      ConnectorException exception =
          assertThrows(
              ConnectorException.class,
              () -> DestinationProvider.getDestination("invalidDestination", DestinationType.HTTP));

      assertEquals(ErrorCodes.DESTINATION_ERROR.name(), exception.getErrorCode());
      mockedDestinationAccessor.verify(
          () -> DestinationAccessor.getDestination("invalidDestination"), times(10));
    }
  }
}
