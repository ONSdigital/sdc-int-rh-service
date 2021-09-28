# Respondent Home Data Service
This repository contains the Respondent Data service. This microservice is a RESTful web service implemented using [Spring Boot](http://projects.spring.io/spring-boot/). It manages respondent data, where a Respondent Data object represents an expected response from the Respondent Data service, which provides all the data that is required by Respondent Home in order for it to verify the respondent's UAC code and connect them to the relevant EQ questionnaire.

## Set Up
Do the following steps to set up the code to run locally:
* Install Java 11 locally
* Install Docker: Sign in to docker hub and then install docker by downloading it from https://hub.docker.com/editions/community/docker-ce-desktop-mac
* Install maven
* Clone the following git repositories:
* https://github.com/ONSdigital/sdc-int-common
* https://github.com/ONSdigital/sdc-int-rh-service
* Make sure that you have a suitable settings.xml file in your local .m2 directory
* Run a mvn clean install for each of the cloned repos in turn. This will install each of them into your local maven repository.

NB. For more detailed information about any of the above steps please see the following confluence article:
https://collaborate2.ons.gov.uk/confluence/display/SDC/How+to+Set+Up+a+Mac+for+Census+Integration+Development+Work

Do the following steps to set up the Application Default Credential (see the next section to understand what this is used for):
* Install the Google SDK locally
* Create a Google Cloud Platform project
* Open the Google Cloud Platform console, which can be found at the following location: https://console.cloud.google.com/getting-started?pli=1
* In the left hand panel choose 'APIs & Services' > 'Credentials'
* Use the following instructions to set up a service account and create an environment variable: https://cloud.google.com/docs/authentication/getting-started
NB. The instructions will lead to a .json file being downloaded, which can be used for setting up credentials. You should move this to a suitable location for pointing your code at.
* To set up the GOOGLE_APPLICATION_CREDENTIALS environment variable you will need to point to the .json file using a command similar to this:
* export GOOGLE_APPLICATION_CREDENTIALS="/users/ellacook/Documents/census-int-code/<filename>.json"
* Once that is done then you can use the following command to tell your applications to use those credentials as your application default credentials:
* gcloud auth application-default login
*NB. Running the above command will first prompt you to hit 'Y' to continue and will then open up the Google Login page, where you need to select your login account and then click 'Allow'.
* It should then open Google Cloud with the following message displayed: You are now authenticated with the Google Cloud SDK!

NB. For more detailed information about setting up the Application Default Credential please see the following confluence article:
https://collaborate2.ons.gov.uk/confluence/display/SDC/How+to+Set+Up+Google+Cloud+Platform+Locally

For initial PubSub emulator setup:

* Clone https://github.com/googleapis/python-pubsub
* Install the emulator

    gcloud components install pubsub-emulator
    gcloud components update

* Install python dependencies, in the above repo navigate to `samples/snipets` and run:

    pip install -r requirements.txt

Finally, create an environment variable to hold the name of your Google Cloud Platform project, using the export command at the terminal (in your census-rh-service repo):
export GOOGLE_CLOUD_PROJECT="<name of your project>" e.g. export GOOGLE_CLOUD_PROJECT="census-rh-ellacook1"

## Running

To run locally, either through terminal or through an IDE, you must set your profile to local so that the `application-local.yml` is picked up.

Make sure you run RH with the VM argument: 

    -Dspring.profiles.active=local

There are several ways of running this service

* Run the service in Eclipse. Run the RHSvcApplication class and update the 'Environment' section to set a value for GOOGLE_CLOUD_PROJECT.

* Docker. To run the RH Service and its required mocks you can run a docker compose script. See the [Readme](docker/README.md) in the docker directory for details.

* Alternatively, run a Maven build from the command line:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
* A third way requires that you first create a JAR file using the following mvn command (after moving into the same directory as the pom.xml):
    ```bash
    mvn clean package
    ```
This will create the JAR file in the Target directory. You can then right-click on the JAR file (in Intellij) and choose 'Run'.

You will need to complete the PubSub setup steps detailed in [PUBSUB.md](docs/PUBSUB.md) and the first step in the `## Manual Testing` section.
Messages that are published to either the `event_case-update` or `event_uac-update` topic will be received by sdc-int-rh-service and stored in either the case_schema or the uac_schema (as appropriate) of the relevant Google Firestore datastore.

The project to use is given by the Application Default Credentials (These are the credential associated with the service account that your app engine app runs as - to set these up please follow the steps given in the previous section).

## PubSub

Pubsub setup instructions can be found in [PUBSUB.md](docs/PUBSUB.md)

## Firestore

Firestore setup instructions can be found in [FIRESTORE.md](docs/FIRESTORE.md)

## Manual testing

To manually test RH:

1) **Queue setup**

Ensure that the steps in the `## PubSub` section above have been completed first, and navigate to the python pubsub project's `python-pubsub/samples/snippets` directory

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

## Docker image build

Is switched off by default for clean deploy. Switch on with;

* mvn dockerfile:build -Dskip.dockerfile=false

    
## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)
 
 
 
