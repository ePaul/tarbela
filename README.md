# tarbela

The goal of the project is to provide a scalable, reliable and generic message producer which supports the following features:

+ defines remote rest api and uses it for pulling messages from a remote event table

+ pulls pending messages on a scheduled basis and directs the messages to its designated target (e.g. Nakadi)

+ evaluates responses and detect errors when trying to send messages

+ marks messages as send using the provided rest api or dead letters messages after a defined time of unsuccessful sending attempts
  
+ provides configuration for source queues and a possible target messaging system

+ Tarbela itself is stateless, just uses a configuration defining from which URIs to fetch the events



 
