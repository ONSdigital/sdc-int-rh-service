#!/bin/sh
set -e

# 
# This script stops the docker containers started by 'rh-service-up.sh'
# 

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "1/1 Stopping services"
docker compose -f $SCRIPT_DIR/docker-compose-rh-service.yml stop
docker compose -f $SCRIPT_DIR/docker-compose-mock-envoy.yml stop
docker compose -f $SCRIPT_DIR/docker-compose-rabbit-mq.yml stop
