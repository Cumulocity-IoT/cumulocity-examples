To use Notifications2Api you need to:
1. Implement your own NotificationListener (one or more) that will handle messages.
2. Autowire Notifications2Api in your Microservice
3. Subscribe and enjoy!

You also need to set **C8Y.notifications2.websocketUrl** property to point to our pulsar websocket proxy.

In this small service you can dynamically subscribe/unsubscribe to different topics using REST API. 
The listener will only output received messages to logs. 

See Notifications2Api javadocs for more details. 