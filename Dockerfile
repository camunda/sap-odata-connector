ARG CAMUNDA_CONNECTORS_VERSION=0.23.2
FROM camunda/connectors:${CAMUNDA_CONNECTORS_VERSION}

COPY target/odata-*-with-dependencies.jar /opt/custom

