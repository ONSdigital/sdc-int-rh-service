#!/bin/sh
set -e

# 
# This script stops the docker containers started by 'rh-service-up.sh'
# 

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

docker compose -f $SCRIPT_DIR/docker-compose-mock-envoy.yml stop

docker compose -f $SCRIPT_DIR/docker-compose-rh-service.yml stop
