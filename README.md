[![codecov](https://codecov.io/gh/ONSdigital/sdc-int-rh-service/branch/main/graph/badge.svg)](https://codecov.io/gh/ONSdigital/sdc-int-rh-service)

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

You will need to complete the PubSub setup steps detailed in [PUBSUB.md](docs/PUBSUB.md) and the first step in the [Manual Testing](#manual-testing) section.
Messages that are published to either the `event_case-update` or `event_uac-update` topic will be received by sdc-int-rh-service and stored in either the case_schema or the uac_schema (as appropriate) of the relevant Google Firestore datastore.

The project to use is given by the Application Default Credentials (These are the credential associated with the service account that your app engine app runs as - to set these up please follow the steps given in the previous section).

## PubSub

Pubsub setup instructions can be found in [PUBSUB.md](docs/PUBSUB.md)

## Firestore

RH service uses a Firestore datastore. If running locally you'll need to create this for your GCP account. When you go into the 'Firestore' section let it create a database for you in 'Native' mode.

More details about RH Firestore can be found in [FIRESTORE.md](docs/FIRESTORE.md)

## Manual Testing

Instructions for testing the sending and receiving of events can be found in [MANUAL_TESTING.md](docs/MANUAL_TESTING.md)
    
## Docker image build

Is switched off by default for clean deploy. Switch on with;

* mvn dockerfile:build -Dskip.dockerfile=false

    
## Copyright
Copyright (C) 2019 Crown Copyright (Office for National Statistics)
