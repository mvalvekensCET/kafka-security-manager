FROM nexus.devops.flexpond.net:18078/flexpond-openjdk11:2.1.0
COPY ./target/scala*/kafka-security-manager-*.jar /srv/application/app.jar
ADD ./target/universal/kafka-security-manager-0.8.tgz /srv/application
ENTRYPOINT ["/srv/application/kafka-security-manager-0.8/bin/kafka-security-manager"]
