up:
	./docker/rh-service-up.sh

down:
	./docker/rh-service-stop.sh

test:
	mvn clean install

build: test docker-build

compile:
	mvn clean install -DskipITs -DskipTests

docker: compile docker-build
	docker build . -t europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/rh-service:latest

docker-build:
	docker build . -t europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/rh-service:latest

docker-refresh:
	docker rmi europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/rh-service:latest
	docker pull europe-west2-docker.pkg.dev/ons-ci-int/int-docker-release/rh-service:latest
