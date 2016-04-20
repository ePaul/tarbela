# tarbela

The goal of the project is to provide a scalable, reliable and generic message producer which supports the following features:

+ defines remote rest api for pulling messages from a remote queue

+ pulls pending messages on a scheduled basis and directs the messages to its designated target queue

+ evaluates responses and detect errors when trying to send messages

+ marks messages as send or dead letters messages after a defined time of unsuccessful sending attempts
  
+ provides configuration for source queues and a possible target messaging system



 
