#!/bin/bash

set -e

PUBSUB_DIR=$1
[ -z "$PUBSUB_DIR" ] && echo 'Error: Usage list_pubsub_topics.sh <pubsub-emulator-dir>' && exit 1;

[ -z "$PUBSUB_EMULATOR_HOST" ] && echo 'Error: Emulator environment not done. Run: $(gcloud beta emulators pubsub env-init)' && exit 1;


cd $PUBSUB_DIR/samples/snippets

echo "T O P I C S"
python publisher.py local list

echo "S U B S C R I P T I O N S"
python subscriber.py local list-in-project
