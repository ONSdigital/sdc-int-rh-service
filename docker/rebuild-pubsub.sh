#!/bin/sh
set -e

#
# This is a utility script which recreates the running pubsub emulator.
#
# It basically restarts the pubsub emulator and creates the latest version of the 
# topics and subscriptions.
# This is also useful when a malformed event is endless consumed by RH.
#

PUBSUB_EMULATOR_VERSION="europe-west2-docker.pkg.dev/ons-ci-int/int-docker-ci/pubsubemu:latest"

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

[ -z "$DOCKER_GCP_CREDENTIALS" ] && echo "Error: DOCKER_GCP_CREDENTIALS must be set" && exit 1;
[ -z "$GOOGLE_CLOUD_PROJECT" ] && echo "Need to set GOOGLE_CLOUD_PROJECT" && exit 1;

docker stop pubsub-emulator
docker compose -f $SCRIPT_DIR/docker-compose-pubsub-emulator.yml up -d
$SCRIPT_DIR/../scripts/pubsub-setup.sh 
