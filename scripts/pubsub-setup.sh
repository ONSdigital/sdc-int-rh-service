#! /bin/bash

$(gcloud beta emulators pubsub env-init)

python publisher.py local create event_case-update
python publisher.py local create event_uac-update
python publisher.py local create event_uac-authenticate
python publisher.py local create event_survey-launch
python publisher.py local create event_fulfilment

python subscriber.py local create event_case-update event_case-update_rh
python subscriber.py local create event_uac-update event_uac-update_rh
