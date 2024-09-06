VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
NAME=$(mvn help:evaluate -Dexpression=project.name -q -DforceStdout)
CAMUNDA_CONNECTORS_VERSION=8.5.7

docker build \
  -t "$NAME:$VERSION" \
  --build-arg CAMUNDA_CONNECTORS_VERSION=$CAMUNDA_CONNECTORS_VERSION \
  .