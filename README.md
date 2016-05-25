# Tarbela

Tarbela is a reliable generic event publisher.

## Which problem does this solve?

In a microservice architecture, some types of events need to be sent (to an event consumer application, or to some event bus which distributes it to the consumers) whenever some data is changing. If sending the event at the same time as updating the data in the database, we have the distributed transaction problem: Do we first send the event or first commit the change to the database? In either case we get problems when the second one fails for some reason.

## How does this work?

Our approach here is that the event producer commits both the event and whatever data change caused the event in the same transaction to the same database (but usually different tables). Then Tarbela (which is a separate application) fetches those events, tries to publish them to the event sink, and if that publishing was successful, informs the producer about the success (so the same event doesn't get sent again the next time).

![](docs/tarbela-architecture-overview.png)

The Producer needs to implement a Tarbela-specific "Event producer API" (defined in this project as [an OpenAPI definition](src/main/resources/api/event-producer-api.yaml)).
For each type of event sink, Tarbela knows those sink's API. *(Note: Currently just [Nakadi](https://github.com/zalando/nakadi) is supported, but other sinks are planned to be added.)*

Tarbela's configuration lists all the producers and sinks with their types, URLs and needed authentication (i.e. which OAuth scopes need to be present in tokens to be sent there). *(Note: Currently we just support one Producer and one Consumer. We still have to figure out how to configure multiple ones.)*

Tarbela itself has no (mutable) state.


## What do I need to configure?

Tarbela needs some pieces of configuration to do its job. If you are using the pre-built docker image (or a building the image as described below), you can pass those as environment variables. If you build your own image, you can configure the properties in a Spring YAML config file, like the application-dev.yml.

Tarbela currently is supposed to run on the [STUPS platform](https://stups.io/) on AWS. *(TODO: figure out how much of this actually depends on STUPS, and if we can support non-STUPS alternatives.)*

Setting | Description | Environment variable, Spring property |
--------|----------------------|-----------------|-------------
Access Token URI | The Authentication Server URI where Tarbela can obtain OAuth2 access tokens for accessing the resource server. | `ACCESS_TOKEN_URI`, tokens.accessTokenUri
Token Info URI | The Authentication Server URI where Tarbela can check which scopes an OAuth Token actually has. *(TODO: is this actually necessary?)* | `TOKEN_INFO_URI`, tokens.tokenInfoUri
Credentials Directory | A directory where Tarbela (or actually the [Tokens](https://github.com/zalando-stups/tokens) library we are using internally) can get the credentials for fetching the access tokens. Those credentials are supposed to be rotated by [berry](https://github.com/zalando-stups/berry) regularly. When running in a Stups-Setup on Taupage, Taupage takes care of this. | `CREDENTIALS_DIR`, tokens.credentialsDirectory
Event Producer URI | The URI implementing the producer API. (This is the full URI, including the `/events` part, but without any query parameters.) *In later versions, we'll have a different way of configuring this, to support several producers.* | `PRODUCER_EVENTS_URI`, producer.events.uri
Event Sink URI | The URI implementing the event submission part of Nakadi's API. This is a template of the form `https://nakadi.example.org/event-types/{type}/events`. The `{type}` part will be replaced by the name of the event type for each event when trying to submit some events. *In later versions, we'll have a different way of configuring this, to support several sinks.* | `NAKADI_SUBMISSION_URI_TEMPLATE`, nakadi.submission.uriTemplate



##How to build

    $ mvn clean package
    
This will build, run all unit and integration tests, and package everything up as a jar file (target/tarbela.jar).

    $ mvn clean install
    
This will build, run all unit and integration tests, package everything up as a jar file, and install that in the local maven repository.

##How to run

    $ # set env variables first, see above
    $ mvn spring-boot:run 

##How to build a docker image

Build tarbela:

    $ mvn clean package -U

Build scm-source.json (with information about the commit being used):

    $ ./scm-source.sh

Build docker image:

    $ docker build -t registry/tarbela:0.1 .

Show images:

    $ docker images

Run with docker with example env variable configuration (adapt to your use case, of course):

    $ docker run -it -e ACCESS_TOKEN_URI='...' \
                     -e TOKEN_INFO_URI='...' \
                     -e CREDENTIALS_DIR='meta/credentials' \
                     -e NAKADI_SUBMISSION_URI_TEMPLATE='https://nakadi.example.org/event-types/{type}/events' \
                     -e PRODUCER_EVENTS_URI='https://my-event-producer.example.org/events' \
                     registry/tarbela:0.1

Push docker image:

    $ docker push registry/tarbela:0.1


## ToDo lists

There are some things we still need to do.

Those are mainly to be done by the maintainers:

* We should push our released versions to the Zalando open source docker registry, and document here how to use the images from there. → [Issue #31](https://github.com/zalando/tarbela/issues/31)
* Describe how to run on the [STUPS platform](https://stups.io/) in AWS (i.e. example Senza configuration). → [Issue #32](https://github.com/zalando/tarbela/issues/32)
* Figure out how much we actually depend on Stups, and potentially describe how to run in a non-Stups setting. → [Issue #33](https://github.com/zalando/tarbela/issues/33)

Other wishes for future versions – here we welcome contributions:

* **Allow fetching from several event sources.**  The basic infrastructure for this is there (just have more EventRetrievers), but we need to figure out a way to configure this (preferably without having to change the docker image for each event source change/addition). → [#34](https://github.com/zalando/tarbela/issues/34)

* **Allow sending events to not just one event sink, but one of several ones.**  The Producer API has a "Sink identifier" in its channel definition, which would be mapped to the sink's URL by configuration. In the first step this would mean supporting several Nakadi installations (or installations of other software following the same event submission API), later we could add other sink types. → [#35](https://github.com/zalando/tarbela/issues/35)

* **Better error handling.**  Currently whenever a bunch of events couldn't be sent, we just set an ERROR status for the events which had failures, and ignore the aborted ones – those will then be tried again the next time. → Some discussion is in [Issue #8](https://github.com/zalando/tarbela/issues/8)

* **Follow Ordering constraints.**  Currently, we are sending events of each type in the same order they are in the producer's list  – but whenever one bunch of events failed, we continue with the next ones (and try the same bunch again later). This can produce out-of-order delivery of events. For some event types this is not a problem, but it might one for others. I guess an ordering requirement needs to be either configurable, or part of the producer API. → Some discussion is in [Issue #8](https://github.com/zalando/tarbela/issues/8)


## License

The MIT License (MIT)
Copyright © 2016 Zalando SE, https://tech.zalando.com

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
