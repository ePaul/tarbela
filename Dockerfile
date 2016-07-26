FROM registry.opensource.zalan.do/stups/openjdk:8-29

MAINTAINER Zalando SE

COPY target/tarbela.jar /tarbela.jar
COPY scm-source.json /scm-source.json

EXPOSE 7979

ENTRYPOINT java $(java-dynamic-memory-opts  40) $(appdynamics-agent) -jar /tarbela.jar