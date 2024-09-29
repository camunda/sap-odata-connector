package io.camunda.connector.sap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@Disabled
public class ODataS4Test {
  // test these boundary conditions:
  // - OData v4 CRU(put + patch)D with key referenced in REST manner
  // fex: /WarrantyClaim/{WrntyClaimHeaderUUID}
  // https://api.sap.com/api/WARRANTYCLAIM_0001/path/get_WarrantyClaim__WrntyClaimHeaderUUID_

  // - OData v4 bound action with SAP__self.<action> in path
  // fex: /WarrantyClaim/{WrntyClaimHeaderUUID}/SAP__self.ProcessSupplierClaim

}
