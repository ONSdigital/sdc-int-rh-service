#!/bin/sh
set -e

#
# This script starts the services needed to run RH locally.
# It is intended to be used by developers who have a local RHUI and want to standup the RH-service for:
#   - local RHUI development
#   - or, to run the RH Cucumber tests.
#
# Prerequisites. See Readme.md in this directory.
#

MOCK_ENVOY_VERSION="europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/mock-envoy:latest"
RH_SERVICE_VERSION="europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/rh-service:latest"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"


echo "1/3 Pulling images ..."
docker pull $MOCK_ENVOY_VERSION
docker pull $RH_SERVICE_VERSION

echo "2/3 Tagging images ..."
docker tag $MOCK_ENVOY_VERSION mock-envoy
docker tag $RH_SERVICE_VERSION rh-service

echo "3/3 Starting services ..."
docker compose -f $SCRIPT_DIR/docker-compose-rabbit-mq.yml up -d --no-recreate
docker compose -f $SCRIPT_DIR/docker-compose-mock-envoy.yml up -d
docker compose -f $SCRIPT_DIR/docker-compose-rh-service.yml up -d
