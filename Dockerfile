ARG CAMUNDA_CONNECTORS_VERSION=0.0.0
FROM camunda/connectors:${CAMUNDA_CONNECTORS_VERSION}

COPY target/odata-*-with-dependencies.jar /opt/custom

