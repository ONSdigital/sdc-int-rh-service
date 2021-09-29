# Manual Testing

To manually test RH:

1) **Queue setup**

Ensure that the steps described in [PUBSUB.md](PUBSUB.md) have been completed first, and navigate to the python pubsub project's `python-pubsub/samples/snippets` directory

2) **Receiving Messages**

The Easiest way currently to populate the topics is to modify the `publisher.py` class in the python pubsub project.
In the `publish_messages` method, replace the for loop with the following snippet (maintaining indentation)

    with open('<EVENT_JSON_FILE>', 'r') as file:
      data = file.read()
      data = data.encode("utf-8")
      future = publisher.publish(topic_path, data)
      print(future.result())

There is a `json` file in `src/test/resources/message/impl` for each event typ that RHSvc can receive, named `PackageFixture.<EVENT>.json`
Replace `<EVENT_JSON_FILE>` in the above python snippet with the filepath of the json file for the event type you wish to test (listed below).
You'll need the full path to the file, not just the file name.

    Event Type                 |Topic                            | Example file
    ---------------------------+---------------------------------+--------------------------------------------------
    CASE_UPDATE                |event_case-update                | PackageFixture.CaseEvent.json
    UAC_UPDATE                 |event_uac-update                 | PackageFixture.UacEvent.json
    SURVEY_UPDATE              |event_survey-update              | PackageFixture.SurveyUpdateEvent.json
    COLLECTION_EXERCISE_UPDATE |event_collection-exercise-update | PackageFixture.CollectionExerciseUpdateEvent.json

You can then use `python publisher.py <DUMMY_PROJECT_NAME> publish <TOPIC_NAME>` using the topic names listed above to send messages to different topics.

You should see an acknowledgement in the RHSvc logs, and the message should now appear in your GCP project's firestore.

3) **Sending Messages**

Ensure that you have successfully received a `CASE_UPDATE` and matching `UAC_UPDATE` event from the above steps, and that both appear in firestore

Create a subscription for the `event_uac-authenticate` topic using

    `python subscriber.py local create event_uac-authenticate fake_subscription`

In a new terminal window run:

    $(gcloud beta emulators pubsub env-init)
    python3 subscriber.py local receive fake_subscription

Generate respondent authenticated event using the `uacHash` from the previously sent `UAC_UPDATE` event and hitting the `uacs` endpoint

       $ curl -s -H "Content-Type: application/json" "http://localhost:8071/uacs/<UAC_HASH>"

To calculate the sha256 value for a uac:

    $ echo -n "w4nwwpphjjptp7fn" | shasum -a 256
    8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4  -


Confirm that the curl command, from the previous step, returned a 200 status.

Also verify that it contains a line such as:
"caseStatus": "OK",

In the terminal window listening to the subscription you created, check to see if a message was received

Format the event text and make sure it looks like:

	{
	  "event": {
	    "type": "RESPONDENT_AUTHENTICATED",
	    "source": "RESPONDENT_HOME",
	    "channel": "RH",
	    "dateTime": "2019-06-24T10:38:07.550Z",
	    "transactionId": "66cc1a1a-c4cc-4442-b7a4-4f86857d1aae"
	  },
	  "payload": {
	    "response": {
	      "questionnaireId": "1110000009",
	      "caseId": "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4"
	    }
	  }
	}

Modified endpoint /cases/uprn/ - method name changed from
getHHCaseByUPRN to
getLatestValidNonHICaseByUPRN
to reflect the fact that it will return non HI cases now with latest valid address record.

## Copyright
Copyright (C) 2021 Crown Copyright (Office for National Statistics)
