FROM registry.opensource.zalan.do/stups/openjdk:8u91-b14-1-22

MAINTAINER Zalando SE

COPY target/tarbela.jar /tarbela.jar
COPY scm-source.json /scm-source.json

EXPOSE 7979

CMD java $(java-dynamic-memory-optsyok  40) $(appdynamics-agent) -jar /tarbela.jar