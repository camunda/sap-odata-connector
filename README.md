# Camunda-SAP integration: outbound protocol connectors

OData and RFC protocol connectors to r/w data from/to SAP S/4 and ECC.

## infrastructure prerequisites

- BTP subaccount w/ cf environment activated
- BTP Destination Service instance pointing to the SAP system,
    with authorization via a technical user (no principal propagation support yet)
- (optional) in case of on-premise SAP system
  - cloud connector setup and connected to above BTP subaccount
  - BTP Connectivity Service instance

## setup

## development

### RFC protocol outbound connector

- explicitly add `maven` dependencies to `pom.xml` for
  - `s4hana-connectivity`   
    ```xml
    <dependency>
      <groupId>com.sap.cloud.sdk.s4hana</groupId>
      <artifactId>s4hana-connectivity</artifactId>
      <version>${version.cloud-sdk}</version>
    </dependency>
    ```
  - `rfc`
     ```xml
    <dependency>
      <groupId>com.sap.cloud.sdk.s4hana</groupId>
      <artifactId>rfc</artifactId>
      <version>${version.cloud-sdk}</version>
    </dependency>
    ```
- `<dest-name>.jcoDestination` in `/` for mocking destination resolution
- `sapjco3.jar` and `libsapjco3.dylib` in `/` for having the JCo library available at runtime

### OData sample backend

There's a Node.js-based OData v2 + v4 backend located in `/cap-bookshop`.
It is intended for dev-time and mandatory for running the unit tests.

First, get yourself [Node.js >= 20](https://nodejs.org/en/download/package-manager/current).
Then get going via

```shell
$> cd cap-bookshop
# deps of the mockserver backend
$> npm i
# global install of cds-dk is required for the cds run command
$> npm i -g @sap/cds-dk
$> cds run
[cds] - loaded model from 5 file(s):

  srv/user-service.cds
  srv/cat-service.cds
  srv/admin-service.cds
  db/schema.cds
  node_modules/@sap/cds/common.cds

[cds] - connect to db > sqlite { url: ':memory:' }
  > init from db/init.js
  > init from db/data/sap.capire.bookshop-Genres.csv
  > init from db/data/sap.capire.bookshop-Books.texts.csv
  > init from db/data/sap.capire.bookshop-Books.csv
  > init from db/data/sap.capire.bookshop-AuthorsByMultKeyDateTime.csv
  > init from db/data/sap.capire.bookshop-AuthorsByMultKey.csv
  > init from db/data/sap.capire.bookshop-AuthorsByDateTimeKey.csv
  > init from db/data/sap.capire.bookshop-Authors.csv
/> successfully deployed to in-memory database.

[cds] - using auth strategy { kind: 'mocked', impl: 'node_modules/@sap/cds/lib/auth/basic-auth' }

[cds] - using new OData adapter
[cds] - serving AdminService { impl: 'srv/admin-service.js', path: '/admin' }
[cds] - serving CatalogService { impl: 'srv/cat-service.js', path: '/browse' }
[cds] - serving UserService { impl: 'srv/user-service.js', path: '/user' }

[cds] - server listening on { url: 'http://localhost:4004' }
[cds] - launched at 8/27/2024, 4:16:11 PM, version: 8.1.1, in: 321.312ms
[cds] - [ terminate with ^C ]
```

After the mockserver is up and running, `mvn test` can be run in the root directory to execute the unit tests.
