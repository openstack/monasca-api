# Monasca API

Date: July 17, 2014

Document Version: v2.0

# Overview
This document describes the Monasca API v2.0, which supports Monitoring as a Service (MONaaS).
The API consists of five main resources:

1. Versions
2. Metrics
3. Measurements
4. Statistics
5. Notification Methods
6. Alarms

# Common Request Headers
This section documents the common request headers that are used in requests.

## Common Http Request Headers
The standard Http request headers that are used in requests.

* Accept - Internet media types that are acceptable in the response. Must be application/json.
* Content-Type - The Internet media type of the request body. Used with POST and PUT requests. Must be application/json.
* X-Requested-With
* Origin

## Non-standard request headers
The non-standard request headers that are used in requests.

* X-Auth-Token - Keystone auth token.

# Common Responses
* links

# Versions
The versions resource supplies operations for accessing information about supported versions of the API.
## List Versions
### GET /v2.0/versions
Lists the supported versions.

#### Success Response
##### Status code
* 200 - OK

##### Response data
Returns an array of the supported versions.

```
[  
   {  
      "id":"v2.0",
      "links":[  
         {  
            "rel":"self",
            "href":"http://192.168.10.4:8080/v2.0"
         }
      ],
      "status":"CURRENT",
      "updated":"2014-07-18T03:25:02.423Z"
   }
]
```

## Get Version
### Get /v2.0/versions/{version_id}
Gets detail about the specified version.
#### Success Response
##### Status code
* 200 - OK

##### Response data
Returns detail about the specified version.

```
{  
   "id":"v2.0",
   "links":[  
      {  
         "rel":"self",
         "href":"http://192.168.10.4:8080/v2.0/"
      }
   ],
   "status":"CURRENT",
   "updated":"2014-07-18T03:25:02.423Z"
}
```

# Metrics
The metrics resource allows metrics to be created and queried.

## Create Metric

### POST /v2.0/metrics
Create metrics.

#### Headers
* X-Auth-Token (required)
* Content-Type (required)

#### URL Parameters
None

#### Body
Consists of a single metric or an array of metrics. A metric has the following properties:

* name (required) - The name of the metric of type string(64).
* dimensions (optional) -  A dictionary consisting of (key, value) pairs of type string(255) that are used to uniquely identify a metric.
* timestamp (required) - The timestamp in seconds from the Epoch.
* value (required) - A float

The name and dimensions are used to uniquely identify a metric.

#### Request Data

##### Single metric
POST a single metric.

```
POST /v2.0/metrics HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache

{ "name": "name1", "dimensions": { "key1": "value1", "key2": "value2" }, "timestamp": 1405630174, "value": 1.0 }
```

##### Array of metrics
POST an array of metrics.

```
POST /v2.0/metrics HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache

    [ { "name": "name1", "dimensions": { "key1": "value1", "key2": "value2" }, "timestamp": 1405630174, "value": 1.0 }, { "name": "name2", "dimensions": { "key1": "value1", "key2": "value2" }, "timestamp": 1405630174, "value": 2.0 } ]
```

#### Success Response
###### Status Code
* 204 - No Content

##### Response Data
This request does not return a response body.

#### Error Response
##### Status Code
* 401 - Unauthorized

## List metrics

### GET /v2.0/metrics
Get metrics

#### Headers
* X-Auth-Token (string)

#### URL Parameters
* name (required) - The name of the metric of type string(64).
* dimensions (optional) - A dictionary consisting of (key, value) pairs of type string(255) that are used to uniquely identify a metric.

#### Request Data
```
GET /v2.0/metrics?name=metric1&dimensions=key1:value1 HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache
```

#### Success Response

##### Status Code
* 200 - OK

##### Response Data
````
[  
   {  
      "name":"name1",
      "dimensions":{  
         "key1":"value1"
      }
   },
   {  
      "name":"name2",
      "dimensions":{  
         "key1":"value1"
      }
   }
]
````

#### Error Response

##### Status Code
* 401 - Unauthorized

# Measurements

## List measurements

### GET /v2.0/metrics/measurements
Get measurements for metrics.

#### Headers
* X-Auth-Token (string)

#### URL Parameters
* name (required) - A metric name to filter metrics by.
* dimensions (optional) - A dictionary to filter metrics by.
* start_time (required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (optional) - The end time in ISO 8601 combined date and time format in UTC.
* limit (optional) - The number of metrics to return.

#### Request Data
```
GET /v2.0/metrics/measurements?name=cpu_user_perc&dimensions=hostname:devstack&start_time=2014-07-18T03:00:00Z HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

#### Success Response
* name: A name of a metric.
* dimensions: The dimensions of a metric.
* columns - An array of column names corresponding to the columns in measurements.
* measurements - A two dimensional array of measurements for each timestamp.


##### Status Code
* 200 - OK

##### Response Data
```
[  
   {  
      "name":"cpu_user_perc",
      "dimensions":{  
         "hostname":"devstack"
      },
      "columns":[  
         "id",
         "timestamp",
         "value"
      ],
      "measurements":[  
         [  
            6254100001,
            "2014-07-18T03:24:25Z",
            2.54
         ],
         [  
            6248030001,
            "2014-07-18T03:23:50Z",
            2.21
         ],
         [  
            6246680001,
            "2014-07-18T03:23:14Z",
            3.17
         ],
         [  
            6242570001,
            "2014-07-18T03:22:38Z",
            2.12
         ]
      ]
   }
]
```

#### Error Response

##### Status Code
* 401 - Unauthorized

# Statistics

## List statistics

### GET /v2.0/metrics/statistics
Get statistics for metrics.

#### Headers
* X-Auth-Token (string)

#### URL Parameters
* name (required) - A metric name to filter metrics by.
* dimensions (optional) - A dictionary to filter metrics by.
* statistics (required) - A comma separate list of statistics to return. Valid statistics are avg, min, max, sum and count.
* start_time (required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (optional) - The end time in ISO 8601 combined date and time format in UTC.
* period (optional) - The time period to aggregate measurements by. Default is 300 seconds.

#### Request Data
```
GET /v2.0/metrics/statistics?name=cpu_user_perc&dimensions=hostname:devstack&start_time=2014-07-18T03:00:00Z&statistics=avg,min,max,sum,count HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache
```

#### Success Response
Returns an array of statistics for each unique metric with the following parameters:

* name: A name of a metric.
* dimensions: The dimensions of a metric.
* columns - An array of column names corresponding to the columns in statistics.
* statistics - A two dimensional array of statistics for each period.


##### Status Code
* 200 - OK

##### Response Data
```
[  
   {  
      "name":"cpu_user_perc",
      "dimensions":{  
         "hostname":"devstack"
      },
      "columns":[  
         "timestamp",
         "avg",
         "min",
         "max",
         "sum",
         "count"
      ],
      "statistics":[  
         [  
            "2014-07-18T03:20:00Z",
            2.765,
            1.95,
            4.93,
            22.119999999999997,
            8.0
         ],
         [  
            "2014-07-18T03:10:00Z",
            2.412941176470588,
            1.71,
            4.09,
            41.019999999999996,
            17.0
         ],
         [  
            "2014-07-18T03:00:00Z",
            2.1135294117647065,
            1.62,
            3.85,
            35.93000000000001,
            17.0
         ]
      ]
   }
]
```

#### Error Response

##### Status Code
* 401 - Unauthorized

# Notification Methods

## Create Notification Method

### POST /v2.0/notification-methods
Creates a new notification method through which notifications can be sent to when an alarm state transition occurs. Notification methods are associated with alarms when an alarm is created.

#### Headers
* X-Auth-Token (string)

#### URL Parameters

None.

#### Body
* name (required) - A descriptive name of the notifcation method.
* type (required) - The type of notification method (EMAIL).
* address (required) - The address / number to notify.

#### Request Data

```
POST /v2.0/notification-methods HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{ "name": "Name of notification method", "type": "EMAIL", "address": "john.doe@hp.com" }
```

##### Response Data
Returns the notification method that was created consisting of the following parameters:

* id - The ID of the notification method that was created.
* links - An array of links where a link consists of the following:
	* rel - Relationship type
	* href - Hypermedia reference
* name
* type
* address

```
{  
   "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
   "links":[  
      {  
         "rel":"self",
         "href":"http://192.168.10.4:8080/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
      }
   ],
   "name":"Name of notification method",
   "type":"EMAIL",
   "address":"john.doe@hp.com"
}
```

## List Notification Methods

### GET /v2.0/notification-methods
List all notification methods.

#### Headers
* X-Auth-Token (string)

#### URL Parameters
None

#### Body
None

#### Request Data
```
GET /v2.0/notification-methods HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

#### Success Response

##### Status Code
* 200 - OK

##### Response Data
Returns an array of notification methods.

```
[  
   {  
      "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
      "links":[  
         {  
            "rel":"self",
            "href":"http://192.168.10.4:8080/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
         }
      ],
      "name":"Name of notification method",
      "type":"EMAIL",
      "address":"john.doe@hp.com"
   },
   {  
      "id":"c60ec47e-5038-4bf1-9f95-4046c6e9a759",
      "links":[  
         {  
            "rel":"self",
            "href":"http://192.168.10.4:8080/v2.0/notification-methods/c60ec47e-5038-4bf1-9f95-4046c6e9a759"
         }
      ],
      "name":"Name of notification method",
      "type":"EMAIL",
      "address":"jane.doe@hp.com"
   }
]
```

## Get Notification Method
### GET /v2.0/notification-methods/{notification_method_id}
Get the details of a specific notification method.

#### Headers
* X-Auth-Token (string)

#### URL Parameters
* notification_method_id - ID of the notification method

#### Body
None

##### Request Data
```
http://192.168.10.4:8080/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508
```

#### Success Response

##### Status Code
* 200 - OK

##### Response Data
Returns the specified notification method

```
{  
   "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
   "links":[  
      {  
         "rel":"self",
         "href":"http://192.168.10.4:8080/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
      }
   ],
   "name":"Name of notification method",
   "type":"EMAIL",
   "address":"john.doe@hp.com"
}
```

#### Error Response

##### Status Code

* 404 - Not Found

## Update Notification Method

### PUT /v2.0/notification-methods/{notification_method_id}
Update the specified notification method.

#### Headers
* X-Auth-Token (string)

#### URL Parameters
* notification_method_id - ID of the notification method to update.

#### Body
* name (required) - A descriptive name of the notifcation method.
* type (required) - The type of notification method (EMAIL).
* address (required) - The address / number to notify.

#### Request data
````
PUT /v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508 HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{ "name": "New name of notification method", "type": "EMAIL", "address": "jane.doe@hp.com" }
````

#### Success Response

##### Status Code
* 200 - OK

#### Response data
````
{  
   "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
   "links":[  
      {  
         "rel":"self",
         "href":"http://192.168.10.4:8080/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
      }
   ],
   "name":"New name of notification method",
   "type":"EMAIL",
   "address":"jane.doe@hp.com"
}
````

## Delete Notification Method

### DELETE /v2.0/notification-methods/{notification_method_id}
Delete the specified notification method.

#### Headers
* X-Auth-Token (string)

#### URL Parameters
* notification_method_id - ID of the notification method to delete

##### Request Data
```
DELETE /v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508 HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

#### Success Response

###### Status Code
* 204 - No Content

##### Response Data
This request does not return a response body.

#### Error Response

##### Status Code
* 401 - Unauthorized

# Alarms

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

