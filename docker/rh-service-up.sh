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

PUBSUB_EMULATOR_VERSION="europe-west2-docker.pkg.dev/ons-ci-int/int-docker-ci/pubsubemu:latest"
MOCK_ENVOY_VERSION="europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/mock-envoy:latest"
RH_SERVICE_VERSION="europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/rh-service:latest"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "1/5 Checking environment variables"
export GCP_METRICS_EXPORT=true
if [ -z "$GOOGLE_CLOUD_PROJECT" ]
then
  echo "Using local firestore emulator and dummy local GCP project"
  export GOOGLE_CLOUD_PROJECT=dummy-local
  export FIRESTORE_EMULATOR_HOST=rh-firestore-emulator:8540
  export GCP_METRICS_EXPORT=false
fi

if [ -z "$DOCKER_GCP_CREDENTIALS" ]
then
  echo "Using local fake service account credentials"
  export DOCKER_GCP_CREDENTIALS=./fake-service-account.json
fi

echo "2/5 Pulling images ..."
docker pull $PUBSUB_EMULATOR_VERSION
docker pull $MOCK_ENVOY_VERSION
docker pull $RH_SERVICE_VERSION

echo "3/5 Tagging images ..."
docker tag $PUBSUB_EMULATOR_VERSION pubsub-emulator
docker tag $MOCK_ENVOY_VERSION mock-envoy
docker tag $RH_SERVICE_VERSION rh-service

echo "4/5 Starting pubsub emulator ..."
docker compose -f $SCRIPT_DIR/docker-compose-pubsub-emulator.yml up -d
$SCRIPT_DIR/../scripts/pubsub-setup.sh 

echo "5/5 Starting mocks and RH Service ..."
docker compose -f $SCRIPT_DIR/docker-compose-mock-envoy.yml up -d
docker compose -f $SCRIPT_DIR/docker-compose-rh-service.yml up -d
