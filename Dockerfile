FROM openjdk:17

RUN groupadd -g 985 rhsvc && \
    useradd -r -u 985 -g rhsvc rhsvc
USER rhsvc
COPY target/rhsvc-0.0.0.jar /opt/rhsvc.jar

ENTRYPOINT [ "java", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "-jar", "/opt/rhsvc.jar" ]
