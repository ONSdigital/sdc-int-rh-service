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
  
  ```
  gcloud components install pubsub-emulator
  gcloud components update
  ```

Finally, create an environment variable to hold the name of your Google Cloud Platform project, using the export command at the terminal (in your census-rh-service repo):
export GOOGLE_CLOUD_PROJECT="<name of your project>" e.g. export GOOGLE_CLOUD_PROJECT="census-rh-ellacook1"

## Running

To run locally, either through terminal or through an IDE, you must set your profile to local so that the `application-local.yml` is picked up.

Use this flag to do this: 

    -Dspring.profiles.active=local

There are two ways of running this service

* The first way is from the command line after moving into the same directory as the pom.xml:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
* The second way requires that you first create a JAR file using the following mvn command (after moving into the same directory as the pom.xml):
    ```bash
    mvn clean package
    ```
This will create the JAR file in the Target directory. You can then right-click on the JAR file (in Intellij) and choose 'Run'.

You will need to complete the PubSub steps below and the first step in the `## Manual Testing` section.
Messages that are published to either the `event_case-update` or `event_uac-update` topic will be received by sdc-int-rh-service and stored in either the case_schema or the uac_schema (as appropriate) of the relevant Google Firestore datastore.
The project to use is given by the Application Default Credentials (These are the credential associated with the service account that your app engine app runs as - to set these up please follow the steps given in the previous section).


## PubSub

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

      Topic                          | Subscription
    ---------------------------------+--------------------------------
      event_case-update              | event_case-update_rh
      event_uac-update               | event_uac-update_rh
      event_uac-authenticate         | N/A
      event_survey-launch            | N/A
      event_fulfilment               | N/A

    python publisher.py local create event_case-update
    python publisher.py local create event_uac-update
    python publisher.py local create event_uac-authenticate
    python publisher.py local create event_survey-launch
    python publisher.py local create event_fulfilment

    python subscriber.py local create event_case-update event_case-update_rh
    python subscriber.py local create event_uac-update event_uac-update_rh

You can test pubsub with the steps detailed in the `## Manual testing` section below.

Further help as well as the source of all pubsub commands in this README can be found here:

https://cloud.google.com/pubsub/docs/emulator

## Firestore

RH service uses a Firestore datastore. If running locally you'll need to create this for your GCP account. When you go into the 'Firestore' section let it create a database for you in 'Native' mode.

### Firestore contention

Firestore will throw a contention exception when overloaded, with the expectation that the application will retry with
an exponential backoff. See this page for details: https://cloud.google.com/storage/docs/request-rate

CR-555 added datastore backoff support to RH service. It uses Spring @Retryable annotation in RespondentDataRepositoryImpl to
do an exponential backoff for DataStoreContentionException exceptions. If the maximum number of retries is used without
success then the 'doRecover' method throws the failing exception.


The explicit handling of contention exceptions has some advantages:

  - Keeps log files clean when contention is happening, as an exception is only logged once both layers of retries have been exhausted.
  - Easy analysis on the quantity of contention, as a custom retry listener does a single line log entry on completion of the retries.
  - Allows large number of retries for only the expected contention. Any other issue will only have the standard 3 or so retries.
  - Google state that contention can happen on reads too. If we were to get this during load testing then it's easy to
   apply the proven retryable annotation to read methods.

#### Contention logging

The logging for different contention circumstances is as follows.
  
**No contention**

Logging will show the arrival of the case/uac but there is no further logging if successful.

**Initial contention and then success**

A warning is logged when the object is finally stored:

    2019-12-11 08:45:32.258  INFO  50306 --- [enerContainer-1] u.g.o.c.i.r.e.i.CaseEventReceiverImpl    : Entering acceptCaseEvent
    2019-12-11 08:45:44.713  WARN  50087 --- [enerContainer-1] u.g.o.c.i.r.r.impl.CustomRetryListener   : writeCollectionCase: Transaction successful after 19 attempts
    
There is no logging of the contention exceptions or for each retry.

**Continual contention with retries exhausted**

If attempts to store the object result in continued contention and all retries are used then this is also logged 
as a warning.

    2019-12-11 09:16:35.362  INFO  50306 --- [enerContainer-1] u.g.o.c.i.r.e.i.CaseEventReceiverImpl    : Entering acceptCaseEvent
    2019-12-11 09:18:12.336  WARN  50306 --- [enerContainer-1] u.g.o.c.i.r.r.impl.CustomRetryListener   : writeCollectionCase: Transaction failed after 30 attempts

    
#### Contention backoff times

The retry of Firestore puts is controlled by the following properties:

**backoffInitial** Is the number of milliseconds that the initial backoff will wait for.

**backoffMultiplier** This controls the increase in the wait time for each subsequent failure and retry.

**backoffMax** Is the maximum amount of time that we want to wait before retrying.

**backoffMaxAttempts** This limits the number of times we are going to attempt the opertation before throwing an exception.

The default values for these properties has been set to give a very slow rate escalation. This should mean that the
number of successful Firestore transactions is just a fraction below the actual maximum possible rate. The shallow
escalation also means that we will try many times, and combined with a relatively high maximum wait, will mean 
that we should hopefully never see a transaction (which is failing due to contention) going back to PubSub. 

Under extreme contention RH should slow down to the extent that each RH thread is only doing one Firestore add per
minute. This should mean that RH is submitting requests 100's of times slower than Firestore can handle.

#### Contention backoff times

To help tune the contention backoff configuration (see 'cloudStorage' section of application properties) here is a noddy program to help:

    public class RetryTimes {
      public static void main(String[] args) {
        long nextWaitTime = 100;
        double multiplier = 1.20;
        long maxWaitTime = 26000;
    
        int iterations = 0;
        long totalWaitTime = 0;
    
        System.out.println("iter wait   total");
    
        while (nextWaitTime < maxWaitTime) {
          iterations++;
          totalWaitTime += nextWaitTime;
      
          System.out.printf("%2d %,6d %,6d\n", iterations, nextWaitTime, totalWaitTime);
      
          nextWaitTime = (long) (nextWaitTime * multiplier);
        }
      }
    }
 
This helps show the backoff time escalation and the maximum run time for a transaction:
    
    iter wait   total
     1    100    100
     2    120    220
     3    144    364
     4    172    536
     5    206    742
     6    247    989
     7    296  1,285
     8    355  1,640
     9    426  2,066
    10    511  2,577
    11    613  3,190
    12    735  3,925
    13    882  4,807
    14  1,058  5,865
    15  1,269  7,134
    16  1,522  8,656
    17  1,826 10,482
    18  2,191 12,673
    19  2,629 15,302
    20  3,154 18,456
    21  3,784 22,240
    22  4,540 26,780
    23  5,448 32,228
    24  6,537 38,765
    25  7,844 46,609
    26  9,412 56,021
    27 11,294 67,315
    28 13,552 80,867

## Manual testing

To manually test RH:

1) **Queue setup**

Ensure that the steps in the `## PubSub` section above have been completed first, and navigate to the python pubsub project's `python-pubsub/samples/snippets` directory

2) **UAC Data**

The Easiest way currently to populate the topics is to modify the `publisher.py` class in the python pubsub project.
In the `publish_messages` method, replace the for loop with 
    
    data = <EVENT_JSON>
    # Data must be a bytestring
    data = data.encode("utf-8")
    # When you publish a message, the client returns a future.
    future = publisher.publish(topic_path, data)
    print(future.result())

You can then use `python publisher.py <DUMMY_PROJECT_NAME> publish <TOPIC_NAME>` to send messages to different topics


Submit the UAC data (see UAC.java) by swapping `<EVENT_JSON>` from above to the json below (including the tripple quotes)

	{
	  "event": {
	    "type": "UAC_UPDATE",
	    "source": "CASE_SERVICE",
	    "channel": "RM",
	    "dateTime": "2011-08-12T20:17:46.384Z",
	    "transactionId": "c45de4dc-3c3b-11e9-b210-d663bd873d93"
	  },
	  "payload": {
	    "uac": {
	      "uacHash": "8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4",
	      "active": true,
	      "questionnaireId": "1110000009",
	      "caseType": "HH",
	      "region": "E",
	      "caseId": "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4",
	      "collectionExerciseId": "a66de4dc-3c3b-11e9-b210-d663bd873d93",
	      "formType": "H"
	    }
	  }
	}

And send `python publisher.py local publish event_uac-update`

3) **Case data**

Submit the case (see CollectionCase.java) by swapping `<EVENT_JSON>` from above to the json below (including the tripple quotes)

	"""
    {
	  "event": {
	    "type": "CASE_UPDATE",
	    "source": "CASE_SERVICE",
	    "channel": "RM",
	    "dateTime": "2011-08-12T20:17:46.384Z",
	    "transactionId": "c45de4dc-3c3b-11e9-b210-d663bd873d93"
	  },
	  "payload": {
	    "collectionCase": {
	      "id": "dc4477d1-dd3f-4c69-b181-7ff725dc9fa4",
	      "caseRef": "10000000010",
	      "caseType": "HH",
	      "survey": "CENSUS",
	      "collectionExerciseId": "a66de4dc-3c3b-11e9-b210-d663bd873d93",
	      "address": {
	        "addressLine1": "1 main street",
	        "addressLine2": "upper upperingham",
	        "addressLine3": "",
	        "townName": "upton",
	        "postcode": "UP103UP",
	        "region": "E",
	        "latitude": "50.863849",
	        "longitude": "-1.229710",
	        "uprn": "123456",
	        "arid": "XXXXX",
	        "addressType": "HH",
	        "estabType": "XXX"
	      },
	      "contact": {
	        "title": "Ms",
	        "forename": "jo",
	        "surname": "smith",
	        "email": "me@example.com",
	        "telNo": "+447890000000"
	      },
	      "actionableFrom": "2011-08-12T20:17:46.384Z",
	      "handDelivery": "false"
	    }
	  }
	}
    """

And send And send `python publisher.py local publish event_case-update`


4) **Create a subscription for the `event_uac-authenticate` topic**


    `python subscriber.py local create event_uac-authenticate fake_subscription`

5) **Listen to that subscription**

In a new terminal window run:

    $(gcloud beta emulators pubsub env-init)
    python3 subscriber.py local receive fake_subscription

6) **Generate respondent authenticated event**

If you know the case id which matches the stored UAC hash then you can supply it in the UACS get request:
  
       $ curl -s -H "Content-Type: application/json" "http://localhost:8071/uacs/8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4"

To calculate the sha256 value for a uac:

    $ echo -n "w4nwwpphjjptp7fn" | shasum -a 256
    8a9d5db4bbee34fd16e40aa2aaae52cfbdf1842559023614c30edb480ec252b4  -


7) **Check the get request results**

Firstly confirm that the curl command returned a 200 status.

Also verify that it contains a line such as:
"caseStatus": "OK",

8) **Check the respondent authenticated event in the terminal window listening to the subscription**

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
 
 
 
