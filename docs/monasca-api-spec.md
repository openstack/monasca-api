# Monasca API

Date: July 18, 2014

Document Version: v2.0

# Overview
This document describes the Monasca API v2.0, which supports Monitoring as a Service (MONaaS). The Monasca API provides a RESTful JSON interface for interacting with and managing monitoring related resources.

The API consists of six main resources:

1. Versions  - Provides information about the supported versions of the API.
2. Metrics - Provides for storage and retrieval of metrics.
3. Measurements - Operations for querying measurements of metrics.
4. Statistics -  Operations for evaluating statistics of metrics.
5. Notification Methods - Represents a method, such as email, which can be associated with an alarm via an action. When an alarm is triggered notification methods associated with the alarm are triggered.
5. Alarms - Provides CRUD operations for alarms and querying the alarm state history.  

Before using the API, you must first get a valid auth token from Keystone. All API operations require an auth token specified in the header of the http request.

## Metric Name and Dimensions
A metric is uniquely identified by a name and set of dimensions.

### Name
Defines the name of a metric. A name is of type string(64).

### Dimensions
A dictionary of (key, value) pairs. The key and value are of type string(255). Dimensions may
only use the characters from: a-z A-Z 0-9 . _ - % = & ? : /.

## Alarm Expressions
The alarm expression syntax allows the creation of simple or complex alarms to handle a wide variety of needs. Alarm expressions are evaluated every 60 seconds.

An alarm expression is a boolean equation which if it evaluates to true with the incoming metrics, will then trigger a notification to be sent.

### Syntax

At the highest level, you have an expression, which is made up of one or more subexpressions, joined by boolean logic. Parenthesis can be used for separators. In a BNF style format:

```
expression
    : subexpression
    | '(' expression ')'
    | expression logical_operator expression
```

The logical_operators are: `and` (also `&&`), `or` (also `||`).

Each subexpression is made up of several parts with a couple of options:

```
subexpression
    : metric relational_operator threshold_value
    | function '(' metric ',' period ')' relational_operator threshold_value ('times' periods)? 
```

The relational_operators are: `lt` (also `<`), `gt` (also `>`), `lte` (also `<=`), `gte` (also `>=`).

Threshold values are always in the same units as the metric that they are being compared to.

The first subexpression shows a direct comparison of a metric to a threshold_value, done every 60 seconds.

#### Simple Example
In this example the metric uniquely identified with the name=cpu_perc and dimension hostname=host.domain.com is compared to the threshold 95.

```
cpu_perc{hostname=host.domain.com} > 95
```

#### More Complex Example
In this example the average of the same metric as in the previous example is evaluated over a 90 second period for 3 times.

```
avg(cpu_perc{hostname=host.domain.com}, 85) > 90 times 3
```

Note that period is the number of seconds for the measurement to be done on. They can only be in a multiple of 60. Periods is how many times in a row that this expression must be true before triggering the alarm. Both period and periods are optional and default to 60 and 1 respectively.

Functions work on all metric measurements during the period time frame.

* min (returns the minimum of all the values)
* max (returns the maximum of all the values)
* sum (returns the sum of all the values)
* count (returns the number of metric observations)
* avg (returns the average of all the values)

The metric is a complex identifier that says the name and optional dimensions.

#### Compound alarm example
In this example a compound alarm expression is evaluated involving two thresholds.

```
avg(cpu_perc{hostname=hostname.domain.com}) > 90 or avg(disk_read_ops{hostname=hostname.domain.com, device=vda, 120) > 1000
```

# Common Request Headers
This section documents the common request headers that are used in requests.

## Common Http Request Headers
The standard Http request headers that are used in requests.

* Content-Type - The Internet media type of the request body. Used with POST and PUT requests. Must be `application/json` or `application/json-patch+json`.
* Accept - Internet media types that are acceptable in the response. Must be application/json.
* X-Requested-With
* Origin

## Non-standard request headers
The non-standard request headers that are used in requests.

* X-Auth-Token (string, required) - Keystone auth token

# Common Responses
The Monasca API utilizes HTTP response codes to inform clients of the success or failure of each request. Clients should use the HTTP response code to trigger error handling if necessary (many errors can be resolved by retrying the request after a short delay). This section discusses various API error responses.

* 200 - A request succeeded.
* 201 - A resource has been successfully created.
* 204 - No content
* 400 - Bad request
* 401 - Unauthorized
* 404 - Not found
* 409 - Conflict
* 422 - Unprocessable entity

# Versions
The versions resource supplies operations for accessing information about supported versions of the API.

## List Versions
Lists the supported versions of the Monasca API.

### GET /v2.0/versions

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
None.

#### Request Body
None.

#### Request Examples
```
GET / HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Accept: application/json
Cache-Control: no-cache
```

### Response
#### Status code
* 200 - OK

#### Response Body
Returns a JSON array of supported versions.

#### Response Examples
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
Gets detail about the specified version of the Monasca API.

### Get /v2.0/versions/{version_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
* version_id (string, required) - Version ID of API

#### Query Parameters
None.

#### Request Body
None.

#### Request Examples
```
GET /v2.0/ HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status code
* 200 - OK

#### Response Body
Returns a JSON version object with details about the specified version.

#### Response Examples
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
Create metrics.

### POST /v2.0/metrics

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json

#### Path Parameters
None.

#### Query Parameters
None.

#### Request Body
Consists of a single metric object or an array of metric objects. A metric has the following properties:

* name (string(64), required) - The name of the metric.
* dimensions ({string(255): string(255)}, optional) - A dictionary consisting of (key, value) pairs used to uniquely identify a metric.
* timestamp (string, required) - The timestamp in seconds from the Epoch.
* value (float, required) - Value of the metric.

The name and dimensions are used to uniquely identify a metric.

#### Request Examples

##### Single metric
POST a single metric.

```
POST /v2.0/metrics HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache

{  
   "name":"name1",
   "dimensions":{  
      "key1":"value1",
      "key2":"value2"
   },
   "timestamp":1405630174,
   "value":1.0
}
```

##### Array of metrics
POST an array of metrics.

```
POST /v2.0/metrics HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache

[  
   {  
      "name":"name1",
      "dimensions":{  
         "key1":"value1",
         "key2":"value2"
      },
      "timestamp":1405630174,
      "value":1.0
   },
   {  
      "name":"name2",
      "dimensions":{  
         "key1":"value1",
         "key2":"value2"
      },
      "timestamp":1405630174,
      "value":2.0
   }
]
```

### Response
#### Status Code
* 204 - No Content

#### Response Body
This request does not return a response body.

## List metrics
Get metrics

#### GET /v2.0/metrics

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* name (string(64), optional) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics?name=metric1&dimensions=key1:value1 HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of metric definition objects with the following fields:

* name (string)
* dimensions ({string(255): string(255)})

#### Response Examples
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

# Measurements
Operations for accessing measurements of metrics.

## List measurements
Get measurements for metrics.

### GET /v2.0/metrics/measurements

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* name (string(64), optional) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`
* start_time (string, required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.
* limit (integer, optional) - The maximum number of metrics to return.

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/measurements?name=cpu_user_perc&dimensions=hostname:devstack&start_time=2014-07-18T03:00:00Z HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of measurements objects for each unique metric with the following fields:

* name (string(64)) - A name of a metric.
* dimensions ({string(255): string(255)}) - The dimensions of a metric.
* columns (array[string]) - An array of column names corresponding to the columns in measurements.
* measurements (array[array[]]) - A two dimensional array of measurements for each timestamp.

#### Response Examples
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
            6248030003,
            "2014-07-18T03:23:50Z",
            2.21
         ],
         [  
            6246680007,
            "2014-07-18T03:23:14Z",
            3.17
         ],
         [  
            6242570022,
            "2014-07-18T03:22:38Z",
            2.12
         ]
      ]
   }
]
```

# Statistics
Operations for calculating statistics of metrics.

## List statistics
Get statistics for metrics.

### GET /v2.0/metrics/statistics

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* name (string(64), required) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`
* statistics (string, required) - A comma separate array of statistics to evaluate. Valid statistics are avg, min, max, sum and count.
* start_time (string, required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.
* period (integer, optional) - The time period to aggregate measurements by. Default is 300 seconds.

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/statistics?name=cpu_user_perc&dimensions=hostname:devstack&start_time=2014-07-18T03:00:00Z&statistics=avg,min,max,sum,count HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache
```

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of statistic objects for each unique metric with the following fields:

* name (string(64)) - A name of a metric.
* dimensions ({string(255): string(255)}) - The dimensions of a metric.
* columns (array[string]) - An array of column names corresponding to the columns in statistics.
* statistics (array[array[]]) - A two dimensional array of statistics for each period.

#### Response Examples
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

# Notification Methods
Operations for working with notification methods.

## Create Notification Method
Creates a notification method through which notifications can be sent to when an alarm state transition occurs. Notification methods can be associated with zero or many alarms. 

### POST /v2.0/notification-methods

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
None.

#### Request Body
* name (string(250), required) - A descriptive name of the notifcation method.
* type (string(100), required) - The type of notification method (`EMAIL`).
* address (string(100), required) - The address / number to notify.

#### Request Examples
```
POST /v2.0/notification-methods HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{  
   "name":"Name of notification method",
   "type":"EMAIL",
   "address":"john.doe@hp.com"
}
```

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON notification method object with the following fields:

* id (string) - ID of notification method
* links ([link]) 
* name (string) - Name of notification method
* type (string) - Type of notification method
* address (string) - Address of notification method

#### Response Examples
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
List all notification methods.

### GET /v2.0/notification-methods

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
None.

#### Body
None.

#### Request Examples
```
GET /v2.0/notification-methods HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of notification method objects with the following fields:

* id (string) - ID of notification method
* links ([link]) 
* name (string) - Name of notification method
* type (string) - Type of notification method
* address (string) - Address of notification method

#### Response Examples
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
Get the details of a specific notification method.

### GET /v2.0/notification-methods/{notification_method_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
* notification_method_id (string, required) - ID of the notification method

#### Query Parameters
None.

#### Request Body
None.

#### Request Examples
```
GET http://192.168.10.4:8080/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508
```

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON notification method object with the following fields:

* id (string) - ID of notification method
* links ([link]) 
* name (string) - Name of notification method
* type (string) - Type of notification method
* address (string) - Address of notification method

#### Response Examples
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

## Update Notification Method
Update the specified notification method.

### PUT /v2.0/notification-methods/{notification_method_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
* Accept (string) - application/json

#### Path Parameters
* notification_method_id (string, required) - ID of the notification method to update.

#### Query Parameters
None.

#### Request Body
* name (string(250), required) - A descriptive name of the notifcation method.
* type (string(100), required) - The type of notification method (`EMAIL`).
* address (string(100), required) - The address / number to notify.

#### Request Examples
````
PUT /v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508 HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{  
   "name":"New name of notification method",
   "type":"EMAIL",
   "address":"jane.doe@hp.com"
}
````

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON notification method object with the following fields:

* id (string) - ID of notification method
* links ([link]) 
* name (string) - Name of notification method
* type (string) - Type of notification method
* address (string) - Address of notification method

#### Response Examples
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
Delete the specified notification method.

### DELETE /v2.0/notification-methods/{notification_method_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token

#### Path Parameters
* notification_method_id (string, required) - ID of the notification method to delete

#### Query Parameters
None.

#### Request Body
None.

#### Request Examples
```
DELETE /v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508 HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response

#### Status Code
* 204 - No Content

#### Response Body
This request does not return a response body.

# Alarms
Operations for working with alarms.

## Create Alarm
Create an alarm.

### POST /v2.0/alarms

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
None.

#### Request Body
Consists of an alarm definition. An alarm has the following properties:

* name (string(255), required) - A unique name of the alarm. Note, the name must be unique.
* description (string(255), optional) -  A description of an alarm.
* expression (string, required) - An alarm expression.
* alarmActions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* okActions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undeterminedActions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.
* severity (string, optional) - Severity of an alarm. Must be either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`. Default is `LOW`. 

#### Request Examples
```
POST /v2.0/alarms HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{  
   "name":"Average CPU percent greater than 10",
   "description":"The average CPU percent is greater than 10",
   "severity":"LOW",
   "expression":"(avg(cpu_user_perc{hostname=devstack}) > 10)",
   "ok_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "alarm_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "undetermined_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ]
}
```

### Response
#### Status Code
* 201 - Created

#### Response Body
Returns a JSON array of alarm objects with the following fields:

* id (string) - ID of alarm that was created.
* links ([link]) - An array of Links to the alarm.
* name (string) - Name of alarm.
* description (string) - Description of alarm.
* expression (string) - The alarm expression.
* expression_data (JSON object) - The alarm expression as a JSON object.
* state (string) - State of alarm. Either `OK`, `ALARM` or `UNDETERMINED`. The initial state of an alarm is `UNDETERMINED`. 
* severity (string) - The severity of an alarm. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true the alarm is enable else the alarm is disabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.

#### Response Examples
```
{  
   "id":"b461d659-577b-4d63-9782-a99194d4a472",
   "links":[  
      {  
         "rel":"self",
         "href":"http://192.168.10.4:8080/v2.0/alarms/b461d659-577b-4d63-9782-a99194d4a472"
      },
      {  
         "rel":"history",
         "href":"http://192.168.10.4:8080/v2.0/alarms/b461d659-577b-4d63-9782-a99194d4a472/history"
      }
   ],
   "name":"Average CPU percent greater than 10",
   "description":"The average CPU percent is greater than 10",
   "expression":"(avg(cpu_user_perc{hostname=devstack}) > 10)",
   "expression_data":{  
      "function":"AVG",
      "metric_name":"cpu_user_perc",
      "dimensions":{  
         "hostname":"devstack"
      },
      "operator":"GT",
      "threshold":10.0,
      "period":60,
      "periods":1
   },
   "state":"UNDETERMINED",
   "severity":"LOW",
   "actions_enabled":true,
   "alarm_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "ok_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "undetermined_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ]
}
```

## List Alarms
List alarms

### GET /v2.0/alarms

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* name (string(255), optional) - Name of alarm to filter by.
* dimensions (string, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`
* state (string, optional) - State of alarm to filter by. Must be either `UNDETERMINED`, `OK` or `ALARM`.

#### Request Body
None.
"
#### Request Examples
```
GET /v2.0/alarms?name=CPU percent greater than 10&dimensions=hostname:devstack&state=UNDETERMINED HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response"
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of alarm objects with the following fields:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* name (string) - Name of alarm.
* description (string) - Description of alarm.
* expression (string) - The alarm expression.
* expression_data (JSON object) - The alarm expression as a JSON object.
* state (string) - State of alarm. Either `OK`, `ALARM` or `UNDETERMINED`. The initial state of an alarm is `UNDETERMINED`. 
* severity (string) - The severity of an alarn. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true the alarm is enable else the alarm is disabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.

#### Response Examples
```
[  
   {  
      "id":"f9935bcc-9641-4cbf-8224-0993a947ea83",
      "links":[  
         {  
            "rel":"self",
            "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
         },
         {  
            "rel":"history",
            "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/history"
         }
      ],
      "name":"CPU percent greater than 10",
      "description":"Release the hounds",
      "expression":"(avg(cpu_user_perc{hostname=devstack}) > 10)",
      "expression_data":{  
         "function":"AVG",
         "metric_name":"cpu_user_perc",
         "dimensions":{  
            "hostname":"devstack"
         },
         "operator":"GT",
         "threshold":10.0,
         "period":60,
         "periods":1
      },
      "state":"UNDETERMINED",
      "severity":"CRITICAL",
      "actions_enabled":true,
      "alarm_actions":[  
         "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
      ],
      "ok_actions":[  
         "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
      ],
      "undetermined_actions":[  
         "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
      ]
   }
]
```

## List Alarms State History
List alarm state history for alarms.

### GET /v2.0/alarms/state-history

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* dimensions (string, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`
* start_time (string, optional) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.

#### Request Body
None.

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of alarm state transition objects with the following fields:

* alarm_id (string) - Alarm ID.
* old_state (string) - The old state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* new_state (string) - The new state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* reason (string) - The reason for the state transition.
* reason_data (string) - The reason for the state transition as a JSON object.
* timestamp (string) - The time in ISO 8601 combined date and time format in UTC when the state transition occurred.

#### Response Examples
```
[
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:38:15.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:42.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu_user_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:18.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu_user_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:20.000Z"
    }
]
```

## Get Alarm
Get the specified alarm.

### GET /v2.0/alarms/{alarm_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
* alarm_id (string, required) - Alarm ID

#### Query Parameters
None.

#### Request Body
None.

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON alarm object with the following fields:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* name (string) - Name of alarm.
* description (string) - Description of alarm.
* expression (string) - The alarm expression.
* expression_data (JSON object) - The alarm expression as a JSON object.
* state (string) - State of alarm. Either `OK`, `ALARM` or `UNDETERMINED`. The initial state of an alarm is `UNDETERMINED`. 
* severity (string) - The severity of an alarm. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true the alarm is enable else the alarm is disabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.

#### Response Examples
```
{
    "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
        },
        {
            "rel": "history",
            "href": "http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/history"
        }
    ],
    "name": "CPU percent greater than 10",
    "description": "Release the hounds",
    "expression": "(avg(cpu_user_perc{hostname=devstack}) > 10)",
    "expression_data": {
        "function": "AVG",
        "metric_name": "cpu_user_perc",
        "dimensions": {
            "hostname": "devstack"
        },
        "operator": "GT",
        "threshold": 10,
        "period": 60,
        "periods": 1
    },
    "state": "UNDETERMINED",
    "severity": "CRITICAL",
    "actions_enabled": true,
    "alarm_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ],
    "ok_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ],
    "undetermined_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ]
}
```

## Update Alarm
Update/Replace the entire state of the specified alarm.

### PUT /v2.0/alarms/{alarm_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
* Accept (string) - application/json

#### Path Parameters
* alarm_id (string, required)

#### Query Parameters
None.

#### Request Body
Consists of an alarm definition. An alarm has the following properties:

* name (string(255), required) - A name of the alarm.
* description (string(255), optional) -  A description of an alarm.
* expression (string, required) - An alarm expression.
* state (string, required) - State of alarm to set. Must be either `OK`, `ALARM` or `UNDETERMINED`.
* enabled (boolean, required)
* alarmActions ([string(50)], optional) 
* okActions ([string(50)], optional)
* undeterminedActions ([string(50)], optional)
* severity (string, optional) - Severity of an alarm. Must be either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.

If optional parameters are not specified they will be reset to their default state.

#### Request Examples
```
PUT /v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache

{  
   "name":"CPU percent greater than 15",
   "description":"Release the hounds",
   "expression":"(avg(cpu_user_perc{hostname=devstack}) > 15)",
   "state":"UNDETERMINED",
   "actions_enabled":true,
   "alarm_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "ok_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "undetermined_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ]
}
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON alarm object with the following parameters:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* name (string) - Name of alarm.
* description (string) - Description of alarm.
* expression (string) - The alarm expression.
* expression_data (JSON object) - The alarm expression as a JSON object.
* state (string) - State of alarm. Either `OK`, `ALARM` or `UNDETERMINED`. The initial state of an alarm is `UNDETERMINED`. 
* severity (string) - The severity of an alarm. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true the alarm is enable else the alarm is disabled.
* alarm_actions (array[string(50)] - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* ok_actions (array[string(50)] - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undetermined_actions (array[string(50)] - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.

#### Response Examples
```
{
    "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
        },
        {
            "rel": "history",
            "href": "http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/history"
        }
    ],
    "name": "CPU percent greater than 15",
    "description": "Release the hounds",
    "expression": "(avg(cpu_user_perc{hostname=devstack}) > 15)",
    "expression_data": {
        "function": "AVG",
        "metric_name": "cpu_user_perc",
        "dimensions": {
            "hostname": "devstack"
        },
        "operator": "GT",
        "threshold": 15,
        "period": 60,
        "periods": 1
    },
    "state": "UNDETERMINED",
    "severity": "CRITICAL",
    "actions_enabled": true,
    "alarm_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ],
    "ok_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ],
    "undetermined_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ]
}
```

## Update Alarm
### PATCH /v2.0/alarms/{alarm_id}
Update select parameters of the specified alarm, set the alarm state and enable/disable it.

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json-patch+json
* Accept (string) - application/json

#### Path Parameters
* alarm_id (string, required) - Alarm ID

#### Query Parameters
None.

#### Request Body
Consists of an alarm with the following properties:

* name (string(255), optional) - A name of the alarm.
* description (string(255), optional) -  A description of an alarm.
* expression (string, optional) - An alarm expression.
* state (string, optional) - State of alarm to set. Must be either `UNDETERMINED`, `OK` or `ALARM`.
* enabled (boolean, optional)
* alarm_actions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* ok_actions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undetermined_actions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.

Only the parameters that are specified will be updated.

#### Request Examples
```
PATCH /v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json-patch+json
Cache-Control: no-cache

{  
   "name":"CPU percent greater than 15",
   "description":"Release the hounds",
   "expression":"(avg(cpu_user_perc{hostname=devstack}) > 15)",
   "state":"UNDETERMINED",
   "actions_enabled":true,
   "severity":"CRITICAL",
   "alarm_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "ok_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "undetermined_actions":[  
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ]
}
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON alarm object with the following fields:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* name (string) - Name of alarm.
* description (string) - Description of alarm.
* expression (string) - The alarm expression.
* expression_data (JSON object) - The alarm expression as a JSON object.
* state (string) - State of alarm. Either `OK`, `ALARM` or `UNDETERMINED`. The initial state of an alarm is `UNDETERMINED`. 
* severity (string) - The severity of an alarm. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true the alarm is enable else the alarm is disabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.

#### Response Examples
```
{
    "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
        },
        {
            "rel": "history",
            "href": "http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/history"
        }
    ],
    "name": "CPU percent greater than 15",
    "description": "Release the hounds",
    "expression": "(avg(cpu_user_perc{hostname=devstack}) > 15)",
    "expression_data": {
        "function": "AVG",
        "metric_name": "cpu_user_perc",
        "dimensions": {
            "hostname": "devstack"
        },
        "operator": "GT",
        "threshold": 15,
        "period": 60,
        "periods": 1
    },
    "state": "UNDETERMINED",
    "severity": "CRITICAL",
    "actions_enabled": true,
    "alarm_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ],
    "ok_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ],
    "undetermined_actions": [
        "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
    ]
}
```

## Delete Alarm
Delete the specified alarm.

### DELETE /v2.0/alarms/{alarm_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token

#### Path Parameters
* alarm_id (string, required) - Alarm ID

#### Query Parameters
None.

#### Request Body
None.

#### Request Examples
```
DELETE /v2.0/alarms/b461d659-577b-4d63-9782-a99194d4a472 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 204 - No content

#### Response Body
None.

## List Alarm State History
List the alarm state history for the specified alarm.

### GET /v2.0/alarms/{alarm_id}/state-history

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
* alarm_id (string, required)

#### Query Parameters
None.

#### Request Body
None.

#### Request Data
```
GET /v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of alarm state transition objects with the following fields:

* alarm_id (string) - Alarm ID.
* old_state (string) - The old state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* new_state (string) - The new state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* reason (string) - The reason for the state transition.
* reason_data (string) - The reason for the state transition as a JSON object.
* timestamp (string) - The time in ISO 8601 combined date and time format in UTC when the state transition occurred.

#### Response Examples
```
[
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:38:15.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:42.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu_user_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:18.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu_user_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:20.000Z"
    }
]
```

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

