# Camunda-SAP integration: SAP OData protocol outbound Connector

Camunda Connector to interact with an SAP S/4 and ECC system via OData v2 + v4.

## development

- source code formatting is done with `maven-spotless-plugin` upon build/compile

### testing

- these spring profiles activate different test suites
  - no active profile or "unit" or "default" &rarr; 'ODataStandardTest', 'ErrorCodesTest', 'DestinationProviderTest'
  - "integration-s4" &rarr; 'ODataS4Test' (only applicable if `destinations` env var is pointing to an actual S/4 system - which is done in the gh action)
  - "integration-c8" + Camunda 8 minor version + {"saas" | "sm" }
     - e.g. "integration-c86-saas" &rarr; activates matching config file (e.g. `src/test/resources/application-integration-c86-saas.yaml`) and
     - runs 'C8Test' pointing to a specific c8 saas or sm cluster

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
