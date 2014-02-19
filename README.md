# Overview

`mon-api` is a RESTful API server that is designed with a layered architecture [layered architecture](http://en.wikipedia.org/wiki/Multilayered_architecture).

## Usage

```
mvn package
java -jar target/mon-api.jar server config-file.yml
```

## Design Overview

### Architectural layers

* Resource
  * Serves as the entrypoint into the service. 
  * Responsible for handling web service requests, and performing structural request validation.
* Application
  * Responsible for providing application level implementations for specific use cases.
* Domain
  * Contains the technology agnostic core domain model and domain service definitions.
  * Responsible for defining state transitions.
* Infrastructure
  * Contains technology specific implementations of domain services.