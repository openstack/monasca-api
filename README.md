# Overview

`monasca-api` is a RESTful API server that is designed with a layered architecture [layered architecture](http://en.wikipedia.org/wiki/Multilayered_architecture).

The full API Specification can be found in [docs/monasca-api-spec.md](docs/monasca-api-spec.md)

## Build

Requires monasca-common from https://github.com/stackforge/monasca-common. Download and do mvn install.

```
mvn clean install
```

## Usage

```
java -jar target/monasca-api.jar server config-file.yml
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
  
## Documentation

* API Specification: [/docs/monasca-api-spec.md](/docs/monasca-api-spec.md).


python monasca api implementation
=================================

To install the python api implementation, git clone the source and run the
following command::

    sudo python setup.py install

If it installs successfully, you will need to make changes to the following
two files to reflect your system settings, especially where kafka server is
located::

    /etc/monasca/monasca.ini
    /etc/monasca/monasca.conf

Once the configurations are modified to match your environment, you can start
up the server by following the following instructions.

To start the server, run the following command:

    Running the server in foreground mode
    gunicorn -k eventlet --worker-connections=2000 --backlog=1000
             --paste /etc/monasca/monasca.ini

    Running the server as daemons
    gunicorn -k eventlet --worker-connections=2000 --backlog=1000
             --paste /etc/monasca/monasca.ini -D

To check if the code follows python coding style, run the following command
from the root directory of this project

    tox -e pep8
    
To run all the unit test cases, run the following command from the root
directory of this project

    tox -e py27   (or -e py26, -e py33)


# License

Copyright (c) 2014 Hewlett-Packard Development Company, L.P.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.
See the License for the specific language governing permissions and
limitations under the License.
