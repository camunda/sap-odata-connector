# Camunda-SAP integration: SAP OData protocol outbound Connector

Camunda Connector to interact with an SAP S/4 and ECC system via OData v2 + v4.

## development hints

- c8.7, either locally or SaaS
- have a `destinations` environment variable point to 
  - the local mockserver (see below)
  - the SAP system, including credentials
```shell
export destinations='[{"name":"localMockServer","url":"http://localhost:4004",Authentication:"BasicAuthentication","User":"alice","Password":"admin"}, {"name":"s4","url":"https:<sap-app-server>",Authentication:"BasicAuthentication","User":"<user>","Password":"<pwd>","sap-client":"<mandant>"}]'
```
- source code formatting is done with `maven-spotless-plugin` upon build/compile

### OData sample backend

There's a Node.js-based OData v2 + v4 backend located in `/cap-bookshop`.
It is intended for dev-time and mandatory for running the unit tests.

First, get [Node.js >= 20](https://nodejs.org/en/download/package-manager/current).
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

## Release cutting

:warning: Publishing of docker images and GH releases is only done upon changes to `pom.xml` in a push to this branch.

- create release branch: `release/8.x`
- adjust version in `/src/pom.xml`
- in `.github/workflows/build-and-publish.yml`:
    - adjust `on.push.branches` to the release branch
    - adjust `CAMUNDA_CONNECTORS_VERSION`
- in `.github/workflows/build-and-test.yml`:
    - adjust `on.pull_request.branches` to the release branch
- (soon) (adjust secrets in both GH and .yaml to point to an 8.7 cluster)
    - adjust `secrets.C8x_...` to the target cluster version (and eventually create those gh secrets)
