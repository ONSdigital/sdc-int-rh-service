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

# To run entirely locally, simply ensure GOOGLE_CLOUD_PROJECT and DOCKER_GCP_CREDENTIALS are not set, then run this script with no additional env vars.

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "1/3 Checking environment variables ..."
if [ -z "$GOOGLE_CLOUD_PROJECT" ]
then
  echo "Using local firestore emulator and dummy local GCP project"
else
  export VAR_FIRESTORE_EMULATOR_HOST=DUMMY_DISABLE_EMULATOR  # Switch off firestore emulator
  echo "Running against GCP project: $GOOGLE_CLOUD_PROJECT"
fi

if [ -z "$DOCKER_GCP_CREDENTIALS" ]
then
  echo "Using local fake service account credentials"
fi

echo "2/3 Starting dependencies and RH Service ..."
COMPOSE_IGNORE_ORPHANS=True docker compose -f $SCRIPT_DIR/docker-compose-rh-service.yml up -d

echo "3/3 Setting up PubSub ..."
$SCRIPT_DIR/../scripts/pubsub-setup.sh
