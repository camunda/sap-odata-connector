# Camunda-SAP integration: outbound protocol connectors

OData and RFC protocol connectors to r/w data from/to SAP S/4 and R/4.

## infrastructure prerequisites

- BTP subaccount w/ cf environment activated
- BTP Destination Service instance pointing to the SAP system,  
    with authorization via a technical user (no principal propagation support yet)
- (optional) in case of on-premise SAP system
  - cloud connector setup and connected to above BTP subaccount
  - BTP Connectivity Service instance 

## setup

## development