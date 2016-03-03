# marklogic-spring-web

The intent behind this library is to provide plumbing for a Spring MVC webapp - in particular, a Spring Boot webapp 
(which uses Spring MVC) - that talks to MarkLogic. The following use cases are addressed so far:

1. Via Spring Security, authenticating a user against MarkLogic by making a simple request to the root REST API endpoint to see if 
the user is authorized to connect to the REST API.
1. Proxy every "/v1/**" request to the Java middle tier to MarkLogic, using the username/password that was captured when authenticating
via Spring Security. The proxying is done via Spring's RestTemplate. 
1. Provide basic implementations of the endpoint of what I'll call the "Slush UI" - i.e. the UI that's used by 
https://github.com/marklogic/slush-marklogic-node . That project uses a Node middle tier; the intent of this project is to make it
easy to reuse that UI with a Spring middle tier, and particularly a Spring Boot middle tier. 
