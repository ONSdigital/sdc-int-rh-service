#!/bin/sh
set -e

#
# This script starts the services needed to run RH locally.
# It is intended to be used by developers who have a local RHUI and want to standup the RH-service for:
#   - local RHUI development
#   - or, to run the RH Cucumber tests.
#
# Prerequisites:
#   - RabbitMQ is running.
#   - Rabbit queues have been created (see RH-service Readme.doc).
#   - RH-ui is running.
#

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

docker compose -f $SCRIPT_DIR/docker-compose-mock-envoy.yml up -d

docker compose -f $SCRIPT_DIR/docker-compose-rh-service.yml up -d
