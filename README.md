[![codecov](https://codecov.io/gh/ONSdigital/sdc-int-rh-service/branch/main/graph/badge.svg?token=LTKekdoBFG)](https://codecov.io/gh/ONSdigital/sdc-int-rh-service)

# Respondent Home Data Service

This repository contains the Respondent Data service. This microservice is a RESTful web service implemented
using [Spring Boot](http://projects.spring.io/spring-boot/). It manages respondent data, where a Respondent Data object
represents an expected response from the Respondent Data service, which provides all the data that is required by
Respondent Home in order for it to verify the respondent's UAC code and connect them to the relevant EQ questionnaire.

## Set Up

Do the following steps to set up the code to run locally:

* Install Java 17 locally
* Install Docker: Sign in to docker hub and then install docker by downloading it
  from https://hub.docker.com/editions/community/docker-ce-desktop-mac
* Install maven
* Clone the following git repositories:
* https://github.com/ONSdigital/sdc-int-common
* https://github.com/ONSdigital/sdc-int-rh-service
* Make sure that you have a suitable settings.xml file in your local .m2 directory
* Run a mvn clean install for each of the cloned repos in turn. This will install each of them into your local maven
  repository.

NB. For more detailed information about any of the above steps please see the following confluence article:
https://collaborate2.ons.gov.uk/confluence/display/SDC/How+to+Set+Up+a+Mac+for+Census+Integration+Development+Work

Do the following steps to set up the Application Default Credential (see the next section to understand what this is
used for):

* Install the Google SDK locally
* Create a Google Cloud Platform project
* Open the Google Cloud Platform console, which can be found at the following
  location: https://console.cloud.google.com/getting-started?pli=1
* In the left hand panel choose 'APIs & Services' > 'Credentials'
* Use the following instructions to set up a service account and create an environment
  variable: https://cloud.google.com/docs/authentication/getting-started
  NB. The instructions will lead to a .json file being downloaded, which can be used for setting up credentials. You
  should move this to a suitable location for pointing your code at.
* To set up the GOOGLE_APPLICATION_CREDENTIALS environment variable you will need to point to the .json file using a
  command similar to this:
* export GOOGLE_APPLICATION_CREDENTIALS="/users/ellacook/Documents/census-int-code/<filename>.json"
* Once that is done then you can use the following command to tell your applications to use those credentials as your
  application default credentials:
* gcloud auth application-default login
  *NB. Running the above command will first prompt you to hit 'Y' to continue and will then open up the Google Login
  page, where you need to select your login account and then click 'Allow'.
* It should then open Google Cloud with the following message displayed: You are now authenticated with the Google Cloud
  SDK!

NB. For more detailed information about setting up the Application Default Credential please see the following
confluence article:
https://collaborate2.ons.gov.uk/confluence/display/SDC/How+to+Set+Up+Google+Cloud+Platform+Locally

For initial PubSub emulator setup:

* Clone https://github.com/googleapis/python-pubsub
* Install the emulator

  gcloud components install pubsub-emulator gcloud components update

* Install python dependencies, in the above repo navigate to `samples/snipets` and run:

  pip install -r requirements.txt

Finally, create an environment variable to hold the name of your Google Cloud Platform project, using the export command
at the terminal (in your census-rh-service repo):
export GOOGLE_CLOUD_PROJECT="<name of your project>" e.g. export GOOGLE_CLOUD_PROJECT="census-rh-ellacook1"

## Running

### Docker Compose

A [docker compose file](/docker/docker-compose-rh-service.yml) and [helper script](/docker/rh-service-up.sh) are
provided for running dependencies and the RH service locally.

Start them with

```shell
make up
```

This will default to running entirely locally against emulators.

You can bring the containers down with

```shell
make down
```

If you have made local code changes, you can test and rebuild locally with

```shell
make build
```

Which will build to the same image tag that the docker compose file uses, so that you are running your image with
changes in the docker compose. You can reset this image to the remote, release version with `make docker-refresh`.

### Locally

#### Intellij

An Intellij spring run config is provided which should be picked up automatically, to use it choose the spring
boot `Local RHSvcApplication` run config in the Run/Debug Configurations menu. Start the dependencies in docker compose
using default local config with `make up`, then stop the dockerized RH service with `docker stop rh-service`. You should
now be able to run the local configuration in Intellij, allowing you to quickly run and debug changes within the IDE.

#### Elsewhere

To run locally, either through terminal or through an IDE, you must set your profile to local so that
the `application-local.yml` is picked up.

Make sure you run RH with the VM arguments:

```shell
  -Dspring.profiles.active=local --add-opens java.base/java.lang=ALL-UNNAMED
```

There are several ways of running this service

* Run the service in Eclipse. Run the RHSvcApplication class and update the 'Environment' section to set a value for
  GOOGLE_CLOUD_PROJECT.

* Docker. To run the RH Service and its required mocks you can run a docker compose script. See
  the [Readme](docker/README.md) in the docker directory for details.

* Alternatively, run a Maven build from the command line:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
* A third way requires that you first create a JAR file using the following mvn command (after moving into the same
  directory as the pom.xml):
    ```bash
    mvn clean package
    ```

This will create the JAR file in the Target directory. You can then right-click on the JAR file (in Intellij) and
choose 'Run'.

You will need to complete the PubSub setup steps detailed in [PUBSUB.md](docs/PUBSUB.md) and the first step in
the [Manual Testing](#manual-testing) section.

The project to use is given by the Application Default Credentials (These are the credential associated with the service
account that your app engine app runs as - to set these up please follow the steps given in the previous section).

## Running the junit driven tests

There are unit tests and integration tests that can be run from maven (or an IDE of your choice). Some of the
integration tests make use of [TestContainers](https://www.testcontainers.org/) which can be used for testing against a
firestore emulator, for example. Since TestContainers relies on a docker environment, then docker should be available to
the environment that the integration tests are run. Following normal maven conventions, the unit test classes are
suffixed with **Test** and the integration test classes are suffixed with **IT**.

### Running both unit and integration tests using maven

Any of the following methods will run both sets of tests:

```sh
  mvn clean install
  mvn clean install -Dskip.integration.tests=false
  mvn clean verify
```

### Running just the unit tests using maven

Any of the following methods will run the unit tests without running the integration tests:

```sh
  mvn clean install -Dskip.integration.tests=true
  mvn clean install -DskipITs
  mvn clean test
```

### Running just the integration tests using maven

Any of the following methods will run the integration tests without running the unit tests:

```sh
  mvn clean verify -Dtest=SomePatternThatDoesntMatchAnything -DfailIfNoTests=false
  mvn failsafe:integration-test
```

### Excluding the database integration tests when running from an IDE

Configure your test run in your IDE, such that Junit5 excludes the following tag: "firestore".

## PubSub

Pubsub setup instructions can be found in [PUBSUB.md](docs/PUBSUB.md)

## Firestore

RH service uses a Firestore datastore. If running locally you'll need to create this for your GCP account. When you go
into the 'Firestore' section let it create a database for you in 'Native' mode.

More details about RH Firestore can be found in [FIRESTORE.md](docs/FIRESTORE.md)

## Manual Testing

Instructions for testing the sending and receiving of events can be found in [MANUAL_TESTING.md](docs/MANUAL_TESTING.md)

## Docker image build

*NOTE: The dockerfile plugin implementation is broken, use `make build` to test and build the docker image,
or `make docker` to do a quick compile and build for now.*

Is switched off by default for clean deploy. Switch on with;

* mvn dockerfile:build -Dskip.dockerfile=false

## Copyright

Copyright (C) 2019 Crown Copyright (Office for National Statistics)
