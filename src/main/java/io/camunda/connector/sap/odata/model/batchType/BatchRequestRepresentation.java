package io.camunda.connector.sap.odata.model.batchType;

import lombok.Getter;
import lombok.Setter;

/**
 * runtime equivalent of the connector template's batch request options see
 * src/test/resources/batch.json for an example
 */
@Getter
@Setter
public class BatchRequestRepresentation {
  public enum EntryKind {
    BATCH("batch"),
    CHANGESET("changeset");

    private final String value;

    EntryKind(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }
  }

  private EntryKind type;
  private Request[] requests;
}
