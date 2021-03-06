#!/bin/bash

#
# This script lists the topics and subscriptions currently held by the pubsub emulator
#
# It takes a single argument which points at a pre-existing python-pubsub directory.
# For example:
#    export python_pubsub_root=~/sdc/source/python-pubsub
#    sdc-int-rh-service/scripts/list_pubsub_topics.sh $python_pubsub_root 
#
# This script assumes that you are running python3. If this is not the case then you'll
# need to do one off:
#   - setup pyenv so you can switch to python3
#   - locally edit the script to use python3

set -e

PUBSUB_DIR=$1
[ -z "$PUBSUB_DIR" ] && echo 'Error: Usage list_pubsub_topics.sh <pubsub-emulator-dir>' && exit 1;

[ -z "$PUBSUB_EMULATOR_HOST" ] && echo 'Error: Emulator environment setup not done. Run: $(gcloud beta emulators pubsub env-init)' && exit 1;


cd $PUBSUB_DIR/samples/snippets

echo "   H O S T"
echo "$PUBSUB_EMULATOR_HOST"

echo "   T O P I C S"
python publisher.py local list | sed 's/name: //g' | sed 's/"//g' | grep -E ".*[a-z].*" --color=never

echo "   S U B S C R I P T I O N S"
python subscriber.py local list-in-project
