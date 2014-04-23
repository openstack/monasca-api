<img src="http://images4.fanpop.com/image/photos/20400000/Rainbow-Dash-my-little-pony-friendship-is-magic-20416585-555-375.jpg"/>

# Overview

`mon-api` is a RESTful API server that is designed with a layered architecture [layered architecture](http://en.wikipedia.org/wiki/Multilayered_architecture).

## Usage

```
mvn package
java -jar target/mon-api.jar server config-file.yml
```

## Design Overview

### Architectural layers

Requests flow through the following architectural layers from top to bottom:

* Resource
  * Serves as the entrypoint into the service. 
  * Responsible for handling web service requests, and performing structural request validation.
* Application
  * Responsible for providing application level implementations for specific use cases.
* Domain
  * Contains the technology agnostic core domain model and domain service definitions.
  * Responsible for upholding invariants and defining state transitions.
* Infrastructure
  * Contains technology specific implementations of domain services.
  
## Docs and Test Interface

When running mon-api the API docs along with the API test interface can be accessed via:

[http://localhost:8080/swagger-ui/](http://localhost:8080/swagger-ui/)