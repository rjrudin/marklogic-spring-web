# marklogic-spring-web

The intent behind this library is to provide plumbing for a Spring MVC webapp - in particular, a Spring Boot webapp 
(which uses Spring MVC) - that talks to MarkLogic. The following use cases are addressed so far:

1. Via Spring Security, authenticating a user against MarkLogic by making a simple request to the root REST API endpoint to see if 
the user is authorized to connect to the REST API.
1. Proxy every "/v1/**" request to the Java middle tier to MarkLogic, using the username/password that was captured when authenticating
via Spring Security. The proxying is done via Spring's RestTemplate. 

This library is used by [slush-marklogic-spring-boot](https://github.com/rjrudin/slush-marklogic-spring-boot) for
connecting to MarkLogic within a Spring Boot middle tier. But you can use it in any middle tier that uses
Spring Security.