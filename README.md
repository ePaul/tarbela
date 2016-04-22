# Tarbela

The goal of the project is to provide a scalable, reliable and generic message publisher which supports the following features:

+ defines remote REST API to be implemented by event producers, and uses it for pulling messages from a remote event table

+ pulls pending messages on a scheduled basis and directs the messages to its designated target (e.g. Nakadi)

+ evaluates responses and detect errors when trying to send messages

+ marks messages as sent or dead letters after a defined time of unsuccessful sending attempts, using the provided REST API
  
+ provides configuration for source queues and a possible target messaging system

+ Tarbela itself is stateless, just uses a configuration defining from which URIs to fetch the events



 
We are currently in a pre-alpha phase, there is nothing usable yet here.