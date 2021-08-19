# Running RH Service in Docker

If you want to locally run the RH Service without compiling from source then
it's probably easiest to do this by running it in Docker.


## Prerequisits

### Rabbit MQ Queues created

To get RHUI + RHSVC working you'll need to make sure that Rabbit has the required queues.
These can be created in the web interface (http://localhost:46672/#/queues) after
running 'rh-service-up.sh'.

See the RH Service Readme for a list of the required queues.

### Docker pull permissions

If Docker pull commands fail with a permissions error run the following and re-attempt:

    gcloud auth configure-docker europe-west2-docker.pkg.dev

### Google credentials

The docker compose file for RH depends on the $GOOGLE_APPLICATION_CREDENTIALS environment
variable. This must point to a file containing your Google credentials.

Depending on how you've set this up you may need to refresh your credentials by running 
'gcloud auth application-default login' before attemping to start the services.

If your credentials are not valid you can see that the attempt to do an intial write to 
the Firestore startup collection fails. In this circumstance the /info endpoint also 
seems to produce an empty response.

The command to look at the rh-service logs is 'docker logs rh-service'. 

### RH-UI is running

As you would expect RHUI needs to be running for Cucumber tests, etc.


## Starting & Stopping RH Service

To start and stop the RH Service and it's dependencies (rabbit MQ & mock-envoy) you
can run scripts within the RH/docker directory.

To bring up the services run the following. Note that the script will pull down
the required docker images if they are not already cached on your machine. 

    cd docker
    ./rh-service-up.sh
    
To stop the services:

    cd docker
    ./rh-service-stop.sh


## Confirming execution

From the command line you can confirm that all of the services are running. 
This is especially important for RHSVC as it's not always very obvious if it has
been started successfully: 

    # Rabbit
    curl -s http://localhost:46672/#/queues | grep title
    
    # mock-envoy
    curl -s "http://localhost:8181/info" | jq
    
    # rh-service
    # This gives an empty response if not started with valid Google credentials
    curl -s "http://localhost:8071/info" | jq


## Running with specific releases

If you want to run with a specific release or development build for mock-case or rh-service
the required versions can be set near the top of the rh-docker-service.sh script.

The ordering for doing this should be:

1. Run rh-service-stop.sh to stop existing services.

1. Ammend the versions required in ./rh-service-up.sh

1. Run ./rh-service-stop.sh to bring up the new versions. The 'docker pull' command in the script
will download the images if required.
