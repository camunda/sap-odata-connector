IP_ADDRESS=$(ifconfig | grep 'inet ' | grep -v '127.0.0.1' | awk '{print $2}' | head -n 1)

docker run --rm --name=odata-connector \
    -e ZEEBE_CLIENT_SECURITY_PLAINTEXT=true \
    -e ZEEBE_CLIENT_BROKER_GATEWAY-ADDRESS=$IP_ADDRESS:26500 \
    -e SERVER_PORT=9898 \
    -e CAMUNDA_CONNECTOR_POLLING_ENABLED=false \
    -e CAMUNDA_CLIENT_MODE=simple \
    -e CAMUNDA_CLIENT_AUTH_USERNAME=demo \
    -e CAMUNDA_CLIENT_AUTH_PASSWORD=demo \
    -e CAMUNDA_CLIENT_OPERATE_BASE-URL=http://$IP_ADDRESS:8081 \
    -e destinations='[{"name":"localMockServer","url":"http://'$IP_ADDRESS':4004","Authentication":"BasicAuthentication","User":"alice","Password":"admin"}]' \
        odata-connector:0.1.0