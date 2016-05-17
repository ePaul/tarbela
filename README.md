# Tarbela

The goal of the project is to provide a scalable, reliable and generic message publisher which supports the following features:

+ defines remote REST API to be implemented by event producers, and uses it for pulling messages from a remote event table

+ pulls pending messages on a scheduled basis and directs the messages to its designated target (e.g. Nakadi)

+ evaluates responses and detect errors when trying to send messages

+ marks messages as sent or dead letters after a defined time of unsuccessful sending attempts, using the provided REST API
  
+ provides configuration for source queues and a possible target messaging system

+ Tarbela itself is stateless, just uses a configuration defining from which URIs to fetch the events

 
We are currently in a pre-alpha phase, there is nothing usable yet here.


##How to build

    $ mvn clean install

##How to run

    $ # set env variables first
    $ mvn spring-boot:run 

##How to build a docker image

Build tarbela:

    $ mvn clean package -U

Build scm-source.json:

    $ ./scm-source.sh

Build docker image:

    $ docker build -t registry/tarbela:0.1 .

Show images:

    $ docker images

Run with docker with example env variable configuration:

    $ docker run -it -e ACCESS_TOKEN_URI='https://token.services.auth.zalando.com/oauth2/access_token?realm=/services' \
                     -e TOKEN_INFO_URI='https://info.services.auth.zalando.com/oauth2/tokeninfo' \
                     -e CREDENTIALS_DIR='meta/credentials' \
                     -e PRODUCER_EVENTS_URI='https://warehouse-allocation-staging.wholesale.zalan.do/events' \
                     registry/tarbela:0.1
    
Push docker image:

    $ docker push registry/tarbela:0.1
