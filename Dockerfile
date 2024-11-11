ARG CAMUNDA_CONNECTORS_VERSION=8.6.4
FROM camunda/connectors:${CAMUNDA_CONNECTORS_VERSION}

COPY target/odata-*-with-dependencies.jar /opt/custom

