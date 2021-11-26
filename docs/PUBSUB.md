# PubSub

Set an environment variable in your .bashrc to point the pubsub utilities at the pubsub port:

    export PUBSUB_EMULATOR_HOST="localhost:9808"
    
To start pubsub

    gcloud beta emulators pubsub start --project=<fake_project_id>

`<fake_project_id>` Can be anything as it's not a real project, however the `application.yml` and all commands below are expecting it to be `local`, so be sure to change them if you use something else.

    gcloud beta emulators pubsub start --project=local

Switch to a new temrinal window and run the following to set up environment variables. You will need to run this in every terminal that you wish to use to interact with the emulator.

    $(gcloud beta emulators pubsub env-init)

From the python pubsub project's `python-pubsub/samples/snippets` directory run the following commands to create a topic and subscription:

Topic: `python publisher.py <DUMMY_PROJECT_NAME> create <TOPIC_NAME>`

Subcription: `python subscriber.py <DUMMY_PROJECT_NAME> create <TOPIC_NAME> <SUBSCRIPTION_NAME>`

**Note: Python3 is required for this project, if your default `python` points to python 2, use `python3` in the above commands instead**

All commands below will assume that your dummy project name is `local`

Create the following topics/subscriptions:

      Topic                            | Subscription
    -----------------------------------+--------------------------------
      event_case-update                | event_case-update_rh
      event_uac-update                 | event_uac-update_rh
      event_survey-update              | event_survey-update_rh
      event_collection-exercise-update | event_collection-exercise-update_rh
      event_uac-authentication         | N/A
      event_eq-launch                  | N/A
      event_fulfilment                 | N/A
      event_new-case                   | N/A

    python publisher.py local create event_case-update
    python publisher.py local create event_uac-update
    python publisher.py local create event_survey-update
    python publisher.py local create event_collection-exercise-update
    python publisher.py local create event_uac-authentication
    python publisher.py local create event_eq-launch
    python publisher.py local create event_fulfilment
    python publisher.py local create event_new-case

    python subscriber.py local create event_case-update event_case-update_rh
    python subscriber.py local create event_uac-update event_uac-update_rh
    python subscriber.py local create event_survey-update event_survey-update_rh
    python subscriber.py local create event_collection-exercise-update event_collection-exercise-update_rh

There's a script to create the above topics and subscriptions in the scripts folder of this project called `pubsub-setup.sh`. It must be run from the `source/snipets of the python pubsub project.

You can now run rhsvc locally with pubsub.

Further help as well as the source of all pubsub commands in this README can be found here:

https://cloud.google.com/pubsub/docs/emulator

## Copyright
Copyright (C) 2021 Crown Copyright (Office for National Statistics)
