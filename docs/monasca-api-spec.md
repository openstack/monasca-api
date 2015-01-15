# Monasca API

Date: November 5, 2014

Document Version: v2.0

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](http://doctoc.herokuapp.com/)*

- [Overview](#overview)
  - [Metric Name and Dimensions](#metric-name-and-dimensions)
    - [Name](#name)
    - [Dimensions](#dimensions)
    - [Text Representation](#text-representation)
  - [Alarm Definitions and Alarms](#alarm-definitions-and-alarms)
  - [Alarm Definition Expressions](#alarm-definition-expressions)
    - [Syntax](#syntax)
      - [Simple Example](#simple-example)
      - [More Complex Example](#more-complex-example)
      - [Compound alarm example](#compound-alarm-example)
    - [Changing Alarm Definitions](#changing-alarm-definitions)
- [Common Request Headers](#common-request-headers)
  - [Common Http Request Headers](#common-http-request-headers)
  - [Non-standard request headers](#non-standard-request-headers)
- [Common Responses](#common-responses)
- [Versions](#versions)
  - [List Versions](#list-versions)
    - [GET](#get)
      - [Headers](#headers)
      - [Path Parameters](#path-parameters)
      - [Query Parameters](#query-parameters)
      - [Request Body](#request-body)
      - [Request Examples](#request-examples)
    - [Response](#response)
      - [Status code](#status-code)
      - [Response Body](#response-body)
      - [Response Examples](#response-examples)
  - [Get Version](#get-version)
    - [Get /{version_id}](#get-version_id)
      - [Headers](#headers-1)
      - [Path Parameters](#path-parameters-1)
      - [Query Parameters](#query-parameters-1)
      - [Request Body](#request-body-1)
      - [Request Examples](#request-examples-1)
    - [Response](#response-1)
      - [Status code](#status-code-1)
      - [Response Body](#response-body-1)
      - [Response Examples](#response-examples-1)
- [Metrics](#metrics)
  - [Create Metric](#create-metric)
    - [POST /v2.0/metrics](#post-v20metrics)
      - [Headers](#headers-2)
      - [Path Parameters](#path-parameters-2)
      - [Query Parameters](#query-parameters-2)
      - [Request Body](#request-body-2)
      - [Request Examples](#request-examples-2)
        - [Single metric](#single-metric)
        - [Array of metrics](#array-of-metrics)
    - [Response](#response-2)
      - [Status Code](#status-code)
      - [Response Body](#response-body-2)
  - [List metrics](#list-metrics)
      - [GET /v2.0/metrics](#get-v20metrics)
      - [Headers](#headers-3)
      - [Path Parameters](#path-parameters-3)
      - [Query Parameters](#query-parameters-3)
      - [Request Body](#request-body-3)
      - [Request Examples](#request-examples-3)
    - [Response](#response-3)
      - [Status Code](#status-code-1)
      - [Response Body](#response-body-3)
      - [Response Examples](#response-examples-2)
- [Measurements](#measurements)
  - [List measurements](#list-measurements)
    - [GET /v2.0/metrics/measurements](#get-v20metricsmeasurements)
      - [Headers](#headers-4)
      - [Path Parameters](#path-parameters-4)
      - [Query Parameters](#query-parameters-4)
      - [Request Body](#request-body-4)
      - [Request Examples](#request-examples-4)
    - [Response](#response-4)
      - [Status Code](#status-code-2)
      - [Response Body](#response-body-4)
      - [Response Examples](#response-examples-3)
- [Statistics](#statistics)
  - [List statistics](#list-statistics)
    - [GET /v2.0/metrics/statistics](#get-v20metricsstatistics)
      - [Headers](#headers-5)
      - [Path Parameters](#path-parameters-5)
      - [Query Parameters](#query-parameters-5)
      - [Request Body](#request-body-5)
      - [Request Examples](#request-examples-5)
    - [Response](#response-5)
      - [Status Code](#status-code-3)
      - [Response Body](#response-body-5)
      - [Response Examples](#response-examples-4)
- [Notification Methods](#notification-methods)
  - [Create Notification Method](#create-notification-method)
    - [POST /v2.0/notification-methods](#post-v20notification-methods)
      - [Headers](#headers-6)
      - [Path Parameters](#path-parameters-6)
      - [Query Parameters](#query-parameters-6)
      - [Request Body](#request-body-6)
      - [Request Examples](#request-examples-6)
    - [Response](#response-6)
      - [Status Code](#status-code-4)
      - [Response Body](#response-body-6)
      - [Response Examples](#response-examples-5)
  - [List Notification Methods](#list-notification-methods)
    - [GET /v2.0/notification-methods](#get-v20notification-methods)
      - [Headers](#headers-7)
      - [Path Parameters](#path-parameters-7)
      - [Query Parameters](#query-parameters-7)
      - [Request Body](#request-body-7)
      - [Request Examples](#request-examples-7)
    - [Response](#response-7)
      - [Status Code](#status-code-5)
      - [Response Body](#response-body-7)
      - [Response Examples](#response-examples-6)
  - [Get Notification Method](#get-notification-method)
    - [GET /v2.0/notification-methods/{notification_method_id}](#get-v20notification-methodsnotification_method_id)
      - [Headers](#headers-8)
      - [Path Parameters](#path-parameters-8)
      - [Query Parameters](#query-parameters-8)
      - [Request Body](#request-body-8)
      - [Request Examples](#request-examples-8)
    - [Response](#response-8)
      - [Status Code](#status-code-6)
      - [Response Body](#response-body-8)
      - [Response Examples](#response-examples-7)
  - [Update Notification Method](#update-notification-method)
    - [PUT /v2.0/notification-methods/{notification_method_id}](#put-v20notification-methodsnotification_method_id)
      - [Headers](#headers-9)
      - [Path Parameters](#path-parameters-9)
      - [Query Parameters](#query-parameters-9)
      - [Request Body](#request-body-9)
      - [Request Examples](#request-examples-9)
    - [Response](#response-9)
      - [Status Code](#status-code-7)
      - [Response Body](#response-body-9)
      - [Response Examples](#response-examples-8)
  - [Delete Notification Method](#delete-notification-method)
    - [DELETE /v2.0/notification-methods/{notification_method_id}](#delete-v20notification-methodsnotification_method_id)
      - [Headers](#headers-10)
      - [Path Parameters](#path-parameters-10)
      - [Query Parameters](#query-parameters-10)
      - [Request Body](#request-body-10)
      - [Request Examples](#request-examples-10)
    - [Response](#response-10)
      - [Status Code](#status-code-8)
      - [Response Body](#response-body-10)
- [Alarm Definitions](#alarm-definitions)
  - [Create Alarm Definition](#create-alarm-definition)
    - [POST /v2.0/alarm-definitions](#post-v20alarm-definitions)
      - [Headers](#headers-11)
      - [Path Parameters](#path-parameters-11)
      - [Query Parameters](#query-parameters-11)
      - [Request Body](#request-body-11)
      - [Request Examples](#request-examples-11)
    - [Response](#response-11)
      - [Status Code](#status-code-9)
      - [Response Body](#response-body-11)
      - [Response Examples](#response-examples-9)
  - [List Alarm Definitions](#list-alarm-definitions)
    - [GET /v2.0/alarm-definitions](#get-v20alarm-definitions)
      - [Headers](#headers-12)
      - [Path Parameters](#path-parameters-12)
      - [Query Parameters](#query-parameters-12)
      - [Request Body](#request-body-12)
      - [Request Examples](#request-examples-12)
    - [Response](#response-12)
      - [Status Code](#status-code-10)
      - [Response Body](#response-body-12)
      - [Response Examples](#response-examples-10)
  - [Get Alarm Definition](#get-alarm-definition)
    - [GET /v2.0/alarm-definitions/{alarm_definition_id}](#get-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-13)
      - [Path Parameters](#path-parameters-13)
      - [Query Parameters](#query-parameters-13)
      - [Request Body](#request-body-13)
    - [Response](#response-13)
      - [Status Code](#status-code-11)
      - [Response Body](#response-body-13)
      - [Response Examples](#response-examples-11)
  - [Update Alarm Definition](#update-alarm-definition)
    - [PUT /v2.0/alarm-definitions/{alarm_definition_id}](#put-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-14)
      - [Path Parameters](#path-parameters-14)
      - [Query Parameters](#query-parameters-14)
      - [Request Body](#request-body-14)
      - [Request Examples](#request-examples-13)
    - [Response](#response-14)
      - [Status Code](#status-code-12)
      - [Response Body](#response-body-14)
      - [Response Examples](#response-examples-12)
  - [Patch Alarm Definition](#patch-alarm-definition)
    - [PATCH /v2.0/alarm-definitions/{alarm_definition_id}](#patch-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-15)
      - [Path Parameters](#path-parameters-15)
      - [Query Parameters](#query-parameters-15)
      - [Request Body](#request-body-15)
      - [Request Examples](#request-examples-14)
    - [Response](#response-15)
      - [Status Code](#status-code-13)
      - [Response Body](#response-body-15)
      - [Response Examples](#response-examples-13)
  - [Delete Alarm Definition](#delete-alarm-definition)
    - [DELETE /v2.0/alarm-definitions/{alarm_definition_id}](#delete-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-16)
      - [Path Parameters](#path-parameters-16)
      - [Query Parameters](#query-parameters-16)
      - [Request Body](#request-body-16)
      - [Request Examples](#request-examples-15)
    - [Response](#response-16)
      - [Status Code](#status-code-14)
      - [Response Body](#response-body-16)
  - [List Alarms](#list-alarms)
    - [GET /v2.0/alarms](#get-v20alarms)
      - [Headers](#headers-17)
      - [Path Parameters](#path-parameters-17)
      - [Query Parameters](#query-parameters-17)
      - [Request Body](#request-body-17)
      - [Request Examples](#request-examples-16)
    - [Response](#response-17)
      - [Status Code](#status-code-15)
      - [Response Body](#response-body-17)
      - [Response Examples](#response-examples-14)
  - [List Alarms State History](#list-alarms-state-history)
    - [GET /v2.0/alarms/state-history](#get-v20alarmsstate-history)
      - [Headers](#headers-18)
      - [Path Parameters](#path-parameters-18)
      - [Query Parameters](#query-parameters-18)
      - [Request Body](#request-body-18)
    - [Response](#response-18)
      - [Status Code](#status-code-16)
      - [Response Body](#response-body-18)
      - [Response Examples](#response-examples-15)
  - [Get Alarm](#get-alarm)
    - [GET /v2.0/alarms/{alarm_id}](#get-v20alarmsalarm_id)
      - [Headers](#headers-19)
      - [Path Parameters](#path-parameters-19)
      - [Query Parameters](#query-parameters-19)
      - [Request Body](#request-body-19)
    - [Response](#response-19)
      - [Status Code](#status-code-17)
      - [Response Body](#response-body-19)
      - [Response Examples](#response-examples-16)
  - [Update Alarm](#update-alarm)
    - [PUT /v2.0/alarms/{alarm_id}](#put-v20alarmsalarm_id)
      - [Headers](#headers-20)
      - [Path Parameters](#path-parameters-20)
      - [Query Parameters](#query-parameters-20)
      - [Request Body](#request-body-20)
      - [Request Examples](#request-examples-17)
    - [Response](#response-20)
      - [Status Code](#status-code-18)
      - [Response Body](#response-body-20)
      - [Response Examples](#response-examples-17)
  - [Patch Alarm](#patch-alarm)
    - [PATCH /v2.0/alarms/{alarm_id}](#patch-v20alarmsalarm_id)
      - [Headers](#headers-21)
      - [Path Parameters](#path-parameters-21)
      - [Query Parameters](#query-parameters-21)
      - [Request Body](#request-body-21)
      - [Request Examples](#request-examples-18)
    - [Response](#response-21)
      - [Status Code](#status-code-19)
      - [Response Body](#response-body-21)
      - [Response Examples](#response-examples-18)
  - [Delete Alarm](#delete-alarm)
    - [DELETE /v2.0/alarms/{alarm_id}](#delete-v20alarmsalarm_id)
      - [Headers](#headers-22)
      - [Path Parameters](#path-parameters-22)
      - [Query Parameters](#query-parameters-22)
      - [Request Body](#request-body-22)
      - [Request Examples](#request-examples-19)
    - [Response](#response-22)
      - [Status Code](#status-code-20)
      - [Response Body](#response-body-22)
  - [List Alarm State History](#list-alarm-state-history)
    - [GET /v2.0/alarms/{alarm_id}/state-history](#get-v20alarmsalarm_idstate-history)
      - [Headers](#headers-23)
      - [Path Parameters](#path-parameters-23)
      - [Query Parameters](#query-parameters-23)
      - [Request Body](#request-body-23)
      - [Request Data](#request-data)
    - [Response](#response-23)
      - [Status Code](#status-code-21)
      - [Response Body](#response-body-23)
      - [Response Examples](#response-examples-19)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# Overview
This document describes the Monasca API v2.0, which supports Monitoring as a Service (MONaaS). The Monasca API provides a RESTful JSON interface for interacting with and managing monitoring related resources.

The API consists of six main resources:

1. Versions  - Provides information about the supported versions of the API.
2. Metrics - Provides for storage and retrieval of metrics.
3. Measurements - Operations for querying measurements of metrics.
4. Statistics -  Operations for evaluating statistics of metrics.
5. Notification Methods - Represents a method, such as email, which can be associated with an alarm definition via an action. When an alarm is triggered notification methods associated with the alarm definition are triggered.
5. Alarm Definitions - Provides CRUD operations for alarm definitions.
6. Alarms - Provides CRUD operations for alarms, and querying the alarm state history.

Before using the API, you must first get a valid auth token from Keystone. All API operations require an auth token specified in the header of the http request.

## Metric Name and Dimensions
A metric is uniquely identified by a name and set of dimensions.

### Name
Defines the name of a metric. A name is of type string(100).

### Dimensions
A dictionary of (key, value) pairs. The key and value are of type string(255). The first character in the dimension is restricted to the following: `a-z A-Z 0-9 _ / \ $`. 
However, the next characters may be any character except for the following: `; } { = , & ) ( "`. If one of the restricted characters is needed, this can be achieved by double quoting the dimensions. 

### Text Representation
In this document, metrics will be represented in the form `name{name=value,name=value}` where name is the metric name and the name=value pairs in the curly braces are the dimensions. For example, `cpu.idle_perc{service=monitoring,hostname=mini-mon}` represents a metric with the name "cpu.idle_perc" and the dimensions "service=monitoring" and "hostname=mini-mon".

## Alarm Definitions and Alarms

Alarm Definitions are policies that specify how Alarms should be created. By using Alarm Definitions, the user doesn't have to create individual alarms for each system or service. Instead, a small number of Alarm Definitions can be managed and Monasca will create Alarms for systems and services as they appear.

An Alarm Definition has an expression for evaluating one or more metrics to determine if there is a problem. Depending on the Alarm Definition expression and match_by value, Monasca will create one or more Alarms depending on the Metrics that are received. The match_by parameter specifies which dimension or dimensions should be used to determine if one or more alarms will be created.

An example is the best way to show this. Imagine two Alarm Definitions have been created:

Alarm Definition 1 has an expression of `avg(cpu.idle_perc{service=monitoring}) < 20` and the match_by parameter is not set.  Alarm Definition 2 has an expression of `min(cpu.idle_perc{service=monitoring}) < 10` and the match_by parameter is set to `hostname`.

When the metric cpu.idle_perc{service=monitoring,hostname=mini-mon} is first received after the Metric Definitions have been created, an Alarm is created for both Alarm Definitions. The metric is added to both Alarms. The following set of Alarm Definitions and Alarm would exist:

Alarm Definition 1:
```
Alarm 1 - Metrics: cpu.idle_perc{service=monitoring,hostname=mini-mon}
```

Alarm Definition 2:
```
Alarm 1 - Metrics: cpu.idle_perc{service=monitoring,hostname=mini-mon}
```

Now, when the metric cpu.idle_perc{service=monitoring,hostname=devstack} is received, the two Alarm Definitions define different behaviors. Since the value for the hostname dimension is different than the value for the existing Alarm from Alarm Definition 2, an new Alarm will be created.  Alarm Definition 1 does not have a value for match_by, so this metric is added to the existing Alarm. This gives us the following set of Alarm Definitions and Alarms:

Alarm Definition 1:
```
Alarm 1 - Metrics: cpu.idle_perc{service=monitoring,hostname=mini-mon} and cpu.idle_perc{service=monitoring,hostname=devstack}
```

Alarm Definition 2:
```
Alarm 1 - Metrics: cpu.idle_perc{service=monitoring,hostname=mini-mon}
Alarm 2 - Metrics: cpu.idle_perc{service=monitoring,hostname=devstack}
```

Alarm Definition 1 is evaluating the status of the monitoring service as a whole, while Alarm Definition 2 evaluates each system in the service.

Now if another system is configured into the monitoring service, then its cpu.idle_perc metric will be added to the Alarm for Alarm Definition 1 and a new Alarm will be created for Alarm Definition 2, all without any user intervention. The system will be monitored without requiring the user to explicitly add alarms for the new system as other monitoring systems require.

If an Alarm Definition expression has multiple subexpressions, for example, `avg(cpu.idle_perc{service=monitoring}) < 10 or avg(cpu.user_perc{service=monitoring}) > 60` and a match_by value set, then the metrics for both subexpressions must have the same value for the dimension specified in match_by. For example, assume this Alarm Definition:

Expression `avg(cpu.idle_perc{service=monitoring}) < 10 or avg(cpu.user_perc{service=monitoring}) > 60` and match_by is `hostname`

Now assume four metrics are received by Monasca:

```
cpu.idle_perc{service=monitoring,hostname=mini-mon}
cpu.idle_perc{service=monitoring,hostname=devstack}
cpu.user_perc{service=monitoring,hostname=mini-mon}
cpu.user_perc{service=monitoring,hostname=devstack}
```

This will cause two Alarms to be created, one for each unique value of hostname. One Alarm will have the metrics:

```
avg(cpu.idle_perc{service=monitoring,hostname=mini-mon}) and avg(cpu.user_perc{service=monitoring,hostname=mini-mon})
```

and another will have the metrics:

```
avg(cpu.idle_perc{service=monitoring,hostname=devstack}) and avg(cpu.user_perc{service=monitoring,hostname=devstack})
```

Note that the value of match_by, "hostname", is used to match the metrics between the subexpressions, hence the name match_by.

An Alarm will only get created when metrics are seen that match all subexpressions in the Alarm Definition.  If match_by is set, then each metric must have a value for at least one of the values in match_by. If match_by is not set, only one Alarm will be created for an Alarm Definition.

The value of the match_by parameter can also be a list, for example, `hostname,device`. In that case, Alarms will be created based and metrics added based on all values of match_by.

For example, assume the Alarm Definition with the expression `max(disk.space_used_perc{service=monitoring}) > 90` and match_by set to `hostname`. This will create one alarm for each system that contains all of the metrics for each device. If instead, the match_by is set to `hostname,device`, then a separate alarm will be created for each device in each system.

To illustrate, assume these four metrics are received by Monasca:
```
disk.space_used_perc{device:/dev/sda1,hostname=mini-mon}
disk.space_used_perc{device:tmpfs,hostname=mini-mon}
disk.space_used_perc{device:/dev/sda1,hostname=devstack}
disk.space_used_perc{device:tmpfs,hostname=devstack}
```

Given the expression  `max(disk.space_used_perc{service=monitoring}) > 90` and match_by set to `hostname`, this will create two alarms:

```
Alarm 1 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=mini-mon}, disk.space_used_perc{device:tmpfs,hostname=mini-mon}
Alarm 2 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=devstack}, disk.space_used_perc{device:tmpfs,hostname=devstack}
```

If instead, match_by is set to `hostname,device`, then four alarms will be created:

```
Alarm 1 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=mini-mon}
Alarm 2 - Metrics: disk.space_used_perc{device:tmpfs,hostname=devstack}
Alarm 3 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=devstack}
Alarm 4 - Metrics: disk.space_used_perc{device:tmpfs,hostname=devstack}
```

The second value of match_by will create an Alarm for each device. For each device that fills up, a separate Alarm will be triggered. The first value of match_by will give you less Alarms to display in the dashboard but if an Alarm has already triggered for one device and another device fills up, the Alarm won't be triggered again.

If desired, an Alarm Definition can be created that exactly matches a set of metrics. The match_by should not be set. Only one Alarm will be created for that Alarm Definition.

Alarms have a state that is set by the Threshold Engine based on the incoming metrics. The states are:
	UNDETERMINED - No metrics for at least one of the subexpressions has been received in (period + 2) times periods (see below for definition of period and periods
	OK - Metrics have been received and the Alarm Definition Expression evaluates to false for the given metrics
	ALARM - Metrics have been received and the Alarm Definition Expression evaluates to true for the given metrics

The Alarms are evaluated and their state is set once per minute.

## Alarm Definition Expressions
The alarm definition expression syntax allows the creation of simple or complex alarm definitions to handle a wide variety of needs. Alarm expressions are evaluated every 60 seconds.

An alarm expression is a boolean equation which if it evaluates to true with the incoming metrics, will then trigger a notification to be sent.

### Syntax

At the highest level, you have an expression, which is made up of one or more subexpressions, joined by boolean operators. Parenthesis can be used around groups of subexpressions to indicated higher precedence. In a BNF style format where items enclosed in [] are optional, '*' means zero or more times, and '|' means or.

````
<expression> 
	::= <subexpression> [(and | or) <subexpression>]*
````

More formally, taking boolean operator precedence into account, where 'and' has higher precedence than 'or', results in the following.

````
<expression>
    ::= <and_expression> <or_logical_operator> <expression> 
    | <and_expression>

<and_expression>
	::= <sub_expression> <and_logical_operator> <and_expression> 
	| <sub_expression>
	
````
Each subexpression is made up of several parts with a couple of options:

````
<sub_expression> 
    ::= <function> '(' <metric> [',' period] ')' <relational_operator> threshold_value ['times' periods] 
	| '(' expression ')'     

````
Period must be an integer multiple of 60.  The default period is 60 seconds.

The logical_operators are: `and` (also `&&`), `or` (also `||`).

````
<and_logical_operator> ::= 'and' | '&&'
<or_logical_operator> ::= 'or' | '||'
````

A metric can be a metric name only or a metric name followed by a list of dimensions. The dimensions further qualify the metric name.


````
<metric>
	::=  metric_name
	| metric_name '{' <dimension_list> '}'
	
````

Any number of dimensions can follow the metric name.

````
<dimension_list>
	::= <dimension>
	| <dimension> ',' <dimension_list>

````

A dimension is simply a key-value pair.

````
<dimension>
	::= dimension_name '=' dimension_value
	
````

The relational_operators are: `lt` (also `<`), `gt` (also `>`), `lte` (also `<=`), `gte` (also `>=`).


````
<relational_operator>
	::= 'lt' | '<' | 'gt' | '>' | 'lte' | '<=' | 'gte' | '>='
	
````
The list of available statistical functions include the following.

```
<function>
	::= 'min' | 'max' | 'sum' | 'count' | 'avg' 
	
```

where 'avg' is the arithmetic average. Note, threshold values are always in the same units as the metric that they are being compared to.


#### Simple Example
In this example the metric uniquely identified with the name `cpu.system_perc` and dimension `hostname=host.domain.com` is compared to the threshold 95.

```
cpu.system_perc{hostname=host.domain.com} > 95
```

#### More Complex Example
In this example the average of the same metric as in the previous example is evaluated over a 120 second period for 3 times so that the expression will evaluate to true if the average is greater than 95 for a total of 360 seconds.

```
avg(cpu.system_perc{hostname=host.domain.com}, 120) > 95 times 3
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
avg(cpu.system_perc{hostname=hostname.domain.com}) > 90 or avg(disk_read_ops{hostname=hostname.domain.com, device=vda}, 120) > 1000
```

### Changing Alarm Definitions

Once an Alarm Definition has been created, the value for match_by and any metrics in the expression cannot be changed. This is because those fields control the metrics used to create Alarms and Alarms may already have been created. The function, operator, period, periods and any boolean operators can change, but not the metrics in subexpressions or the number of subexpressions.  All other fields in an Alarm Definition can be changed.

The only option to change metrics or match_by is to delete the existing Alarm Definition and create a new one. Deleting an Alarm Definition will delete all Alarms associated with it.

# Common Request Headers
This section documents the common request headers that are used in requests.

## Common Http Request Headers
The standard Http request headers that are used in requests.

* Content-Type - The Internet media type of the request body. Used with POST and PUT requests. Must be `application/json` or `application/json-patch+json`.
* Accept - Internet media types that are acceptable in the response. Must be application/json.
* X-Requested-With (optional) - Which headers are requested to be allowed. Filled in by browser as part of the CORS protocol.
* Origin (optional) - The origin of page that is requesting cross origin access. Filled in by browser as part of the CORS protocol.

## Non-standard request headers
The non-standard request headers that are used in requests.

* X-Auth-Token (string, required) - Keystone auth token

# Common Responses
The Monasca API utilizes HTTP response codes to inform clients of the success or failure of each request. Clients should use the HTTP response code to trigger error handling if necessary. This section discusses various API error responses.

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

### GET

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
___

## Get Version
Gets detail about the specified version of the Monasca API.

### Get /{version_id}

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
___

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
* tenant_id (string, optional, restricted) - Tenant ID to create metric on behalf of. Usage of this query parameter requires the `monitoring-delegate` role.

#### Request Body
Consists of a single metric object or an array of metric objects. A metric has the following properties:

* name (string(100), required) - The name of the metric.
* dimensions ({string(255): string(255)}, optional) - A dictionary consisting of (key, value) pairs used to uniquely identify a metric.
* timestamp (string, required) - The timestamp in seconds from the Epoch.
* value (float, required) - Value of the metric. Values with base-10 exponents greater than 126 or less than -130 are truncated.

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
___

## List metrics
Get metrics

#### GET /v2.0/metrics

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* name (string(100), optional) - A metric name to filter metrics by.
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
___

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
* name (string(100), optional) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`
* start_time (string, required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/measurements?name=cpu.system_perc&dimensions=hostname:devstack&start_time=2014-07-18T03:00:00Z HTTP/1.1
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

* name (string(100)) - A name of a metric.
* dimensions ({string(255): string(255)}) - The dimensions of a metric.
* columns (array[string]) - An array of column names corresponding to the columns in measurements.
* measurements (array[array[]]) - A two dimensional array of measurements for each timestamp.

#### Response Examples
```
[  
   {  
      "name":"cpu.system_perc",
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
___

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
* name (string(100), required) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`
* statistics (string, required) - A comma separate array of statistics to evaluate. Valid statistics are avg, min, max, sum and count.
* start_time (string, required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.
* period (integer, optional) - The time period to aggregate measurements by. Default is 300 seconds.

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/statistics?name=cpu.system_perc&dimensions=hostname:devstack&start_time=2014-07-18T03:00:00Z&statistics=avg,min,max,sum,count HTTP/1.1
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

* name (string(100)) - A name of a metric.
* dimensions ({string(255): string(255)}) - The dimensions of a metric.
* columns (array[string]) - An array of column names corresponding to the columns in statistics.
* statistics (array[array[]]) - A two dimensional array of statistics for each period.

#### Response Examples
```
[  
   {  
      "name":"cpu.system_perc",
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
___

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
* name (string(250), required) - A descriptive name of the notification method.
* type (string(100), required) - The type of notification method (`EMAIL` or `WEBHOOK` ).
* address (string(100), required) - The email/url address to notify.

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
___

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

#### Request Body
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
___

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
___

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
* type (string(100), required) - The type of notification method (`EMAIL` or `WEBHOOK` ).
* address (string(100), required) - The email/url address to notify.

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
___

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
___

# Alarm Definitions
Operations for working with alarm definitions.

## Create Alarm Definition
Create an alarm definition.

### POST /v2.0/alarm-definitions

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
* match_by ([string], optional) - The metric dimensions to use to create unique alarms
* severity (string, optional) - Severity of an alarm. Must be either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`. Default is `LOW`.
* alarm_actions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `ALARM` state.
* ok_actions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `OK` state.
* undetermined_actions ([string(50)], optional) - Array of notification method IDs that are invoked when the alarm transitions to the `UNDETERMINED` state.

#### Request Examples
```
POST /v2.0/alarm-definitions HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{  
   "name":"Average CPU percent greater than 10",
   "description":"The average CPU percent is greater than 10",
   "expression":"(avg(cpu,user_perc{hostname=devstack}) > 10)",
   "match_by":[
     "hostname"
   ],
   "severity":"LOW",
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
Returns a JSON array of alarm definition objects with the following fields:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* expression_data (JSON object) - The alarm definition expression as a JSON object.
* match_by ([string]) - The metric dimensions to match to the alarm dimensions
* severity (string) - The severity of an alarm definition. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true actions for all alarms related to this definition are enabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `UNDETERMINED` state.

#### Response Examples
```
{  
   "id":"b461d659-577b-4d63-9782-a99194d4a472",
   "links":[  
      {  
         "rel":"self",
         "href":"http://192.168.10.4:8080/v2.0/alarm-definitions/b461d659-577b-4d63-9782-a99194d4a472"
      }
   ],
   "name":"Average CPU percent greater than 10",
   "description":"The average CPU percent is greater than 10",
   "expression":"(avg(cpu.user_perc{hostname=devstack}) > 10)",
   "expression_data":{  
      "function":"AVG",
      "metric_name":"cpu.user_perc",
      "dimensions":{  
         "hostname":"devstack"
      },
      "operator":"GT",
      "threshold":10.0,
      "period":60,
      "periods":1
   },
   "match_by":[
     "hostname"
   ],
   "severity":"LOW",
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
___
## List Alarm Definitions
List alarm definitions.

### GET /v2.0/alarm-definitions

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* name (string(255), optional) - Name of alarm to filter by.
* dimensions (string, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`

#### Request Body
None.

#### Request Examples
```
GET /v2.0/alarm-definitions?name=CPU percent greater than 10&dimensions=hostname:devstack&state=UNDETERMINED HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of alarm objects with the following fields:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* expression_data (JSON object) - The alarm definition expression as a JSON object.
* match_by ([string]) - The metric dimensions to use to create unique alarms
* severity (string) - The severity of an alarm definition. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true actions for all alarms related to this definition are enabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `UNDETERMINED` state.

#### Response Examples
```
[  
   {  
      "id":"f9935bcc-9641-4cbf-8224-0993a947ea83",
      "links":[  
         {  
            "rel":"self",
            "href":"http://192.168.10.4:8080/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
         }
      ],
      "name":"CPU percent greater than 10",
      "description":"Release the hounds",
      "expression":"(avg(cpu.user_perc{hostname=devstack}) > 10)",
      "expression_data":{  
         "function":"AVG",
         "metric_name":"cpu.user_perc",
         "dimensions":{  
            "hostname":"devstack"
         },
         "operator":"GT",
         "threshold":10.0,
         "period":60,
         "periods":1
      },
	  "match_by":[
	    "hostname"
	  ],
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
___

## Get Alarm Definition
Get the specified alarm definition.

### GET /v2.0/alarm-definitions/{alarm_definition_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
* alarm_definition_id (string, required) - Alarm Definition ID

#### Query Parameters
None.

#### Request Body
None.

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON alarm object with the following fields:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* expression_data (JSON object) - The alarm definition expression as a JSON object.
* match_by ([string]) - The metric dimensions to use to create unique alarms
* severity (string) - The severity of an alarm definition. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true actions for all alarms related to this definition are enabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `UNDETERMINED` state.

#### Response Examples
```
{
    "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8080/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
        }
    ],
    "name": "CPU percent greater than 10",
    "description": "Release the hounds",
    "expression": "(avg(cpu.user_perc{hostname=devstack}) > 10)",
    "expression_data": {
        "function": "AVG",
        "metric_name": "cpu.user_perc",
        "dimensions": {
            "hostname": "devstack"
        },
        "operator": "GT",
        "threshold": 10,
        "period": 60,
        "periods": 1
    },
    "match_by":[
      "hostname"
    ],
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
___

## Update Alarm Definition
Update/Replace the specified alarm definition.

### PUT /v2.0/alarm-definitions/{alarm_definition_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
* Accept (string) - application/json

#### Path Parameters
* alarm_definition_id (string, required)

#### Query Parameters
None.

#### Request Body
Consists of an alarm definition. An alarm has the following properties:

* name (string(255), required) - A name of the alarm definition.
* description (string(255), optional) -  A description of an alarm definition.
* expression (string, required) - An alarm expression.
* match_by ([string], optional) - The metric dimensions to use to create unique alarms. If specified, this MUST be the same as the existing value for match_by
* severity (string, optional) - Severity of an alarm definition. Must be either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* alarm_actions ([string(50)], optional) 
* ok_actions ([string(50)], optional)
* undetermined_actions ([string(50)], optional)

If optional parameters are not specified they will be reset to their default state.

Only the parameters that are specified will be updated. See Changing Alarm Definitions for restrictions on changing expression and match_by.

#### Request Examples
```
PUT /v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache

{  
   "name":"CPU percent greater than 15",
   "description":"Release the hounds",
   "expression":"(avg(cpu.user_perc{hostname=devstack}) > 15)",
   "match_by":[
     "hostname"
   ],
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

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* expression_data (JSON object) - The alarm definition expression as a JSON object.
* match_by ([string]) - The metric dimensions to use to create unique alarms
* severity (string) - The severity of an alarm definition. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true actions for all alarms related to this definition are enabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `UNDETERMINED` state.

#### Response Examples
```
{
    "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8080/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
        }
    ],
    "name": "CPU percent greater than 15",
    "description": "Release the hounds",
    "expression": "(avg(cpu.user_perc{hostname=devstack}) > 15)",
    "expression_data": {
        "function": "AVG",
        "metric_name": "cpu.user_perc",
        "dimensions": {
            "hostname": "devstack"
        },
        "operator": "GT",
        "threshold": 15,
        "period": 60,
        "periods": 1
    },
    "match_by":[
      "hostname"
    ]
    "severity": "CRITICAL",
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
___

## Patch Alarm Definition
### PATCH /v2.0/alarm-definitions/{alarm_definition_id}
Update select parameters of the specified alarm definition, and enable/disable its actions.

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json-patch+json
* Accept (string) - application/json

#### Path Parameters
* alarm_definition_id (string, required) - Alarm Definition ID

#### Query Parameters
None.

#### Request Body
Consists of an alarm with the following properties:

* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* match_by ([string], optional) - The metric dimensions to use to create unique alarms. If specified, this MUST be the same as the existing value for match_by
* severity (string) - The severity of an alarm definition. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true actions for all alarms related to this definition are enabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `UNDETERMINED` state.

Only the parameters that are specified will be updated. See Changing Alarm Definitions for restrictions on changing expression and match_by.

#### Request Examples
```
PATCH /v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json-patch+json
Cache-Control: no-cache

{  
   "name":"CPU percent greater than 15",
   "description":"Release the hounds",
   "expression":"(avg(cpu.user_perc{hostname=devstack}) > 15)",
   "match_by":[
     "hostname"
   ],
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
Returns a JSON alarm definition object with the following fields:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* expression_data (JSON object) - The alarm definition expression as a JSON object.
* match_by ([string]) - The metric dimensions to use to create unique alarms
* severity (string) - The severity of an alarm definition. Either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* actions_enabled (boolean) - If true actions for all alarms related to this definition are enabled.
* alarm_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `ALARM` state.
* ok_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `OK` state.
* undetermined_actions ([string]) - Array of notification method IDs that are invoked when the alarms for this definition transition to the `UNDETERMINED` state.

#### Response Examples
```
{
    "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8080/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
        }
    ],
    "name": "CPU percent greater than 15",
    "description": "Release the hounds",
    "expression": "(avg(cpu.user_perc{hostname=devstack}) > 15)",
    "expression_data": {
        "function": "AVG",
        "metric_name": "cpu.user_perc",
        "dimensions": {
            "hostname": "devstack"
        },
        "operator": "GT",
        "threshold": 15,
        "period": 60,
        "periods": 1
    },
    "match_by":[
      "hostname"
    ],
    "severity": "CRITICAL",
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
___

## Delete Alarm Definition
Delete the specified alarm definition.

### DELETE /v2.0/alarm-definitions/{alarm_definition_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token

#### Path Parameters
* alarm_id (string, required) - Alarm Definition ID

#### Query Parameters
None.

#### Request Body
None.

#### Request Examples
```
DELETE /v2.0/alarm-definitions/b461d659-577b-4d63-9782-a99194d4a472 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 204 - No content

#### Response Body
None.
___

## List Alarms
List alarms

### GET /v2.0/alarms

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters

* alarm_definition_id (string, optional) - Alarm definition ID to filter by.
* metric_name (string(255), optional) - Name of metric to filter by.
* metric_dimensions ({string(255): string(255)}, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`
* state (string) - State of alarm to filter by, either `OK`, `ALARM` or `UNDETERMINED`.

#### Request Body
None.
"
#### Request Examples
```
GET /v2.0/alarms?metric_name=cpu.system_perc&metric_dimensions=hostname:devstack&state=UNDETERMINED HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON array of alarm objects with the following fields:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* alarm_definition_id (string) - Name of alarm.
* metrics ({string, string(255): string(255)}) - The metrics associated with the alarm.
* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.

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
            "rel":"state-history",
            "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
         }
      ],
      "alarm_definition": {
         "severity": "LOW", 
         "id": "b7e5f472-7aa5-4254-a49a-463e749ae817", 
         "links": [
            {
               "href": "http://192.168.10.4:8080/v2.0/alarm-definitions/b7e5f472-7aa5-4254-a49a-463e749ae817", 
               "rel": "self"
            }
      ], 
      "name": "high cpu and load"
    }
      "metrics":[{
         "name":"cpu.system_perc",
         "dimensions":{  
            "hostname":"devstack"
         }
      }],
      "state":"OK"
   }
]
```
___

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
* metric_name (string(255), optional) - Name of metric to filter by.
* metric_dimensions ({string(255): string(255)}, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`
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
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:38:15.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:42.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu.system_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:18.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu.system_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:20.000Z"
    }
]
```
___

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
* alarm_definition_id (string) - Name of alarm.
* description (string) - ID of the alarm definition.
* metrics ({string, string(255): string(255)}) - The metrics associated with the alarm.
* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.

#### Response Examples
```
{  
   "id":"f9935bcc-9641-4cbf-8224-0993a947ea83",
   "links":[  
      {  
         "rel":"self",
         "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
      },
      {  
         "rel":"state-history",
         "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
      }
   ],
   "alarm_definition_id":"ad837fca-5564-4cbf-523-0117f7dac6ad",
   "metrics":[{
      "name":"cpu.system_perc",
      "dimensions":{  
         "hostname":"devstack"
      }
   }],
   "state":"OK"
}
```
___

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
Consists of an alarm definition. An alarm has the following mutable properties:

* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.

#### Request Examples
```
PUT /v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache

{  
  "state":"OK"
}
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON alarm object with the following parameters:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* alarm_definition_id (string) - Name of alarm.
* description (string) - ID of the alarm definition.
* metrics ({string, string(255): string(255)}) - The metrics associated with the alarm.
* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.

#### Response Examples
```
{  
  "id":"f9935bcc-9641-4cbf-8224-0993a947ea83",
  "links":[  
     {  
        "rel":"self",
        "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
     },
     {  
        "rel":"state-history",
        "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
     }
  ],
  "alarm_definition_id":"ad837fca-5564-4cbf-523-0117f7dac6ad",
  "metrics":[{
     "name":"cpu.system_perc",
     "dimensions":{  
        "hostname":"devstack"
     }
  }],
  "state":"OK"
}
```
___

## Patch Alarm
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
Consists of an alarm with the following mutable properties:

* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.

#### Request Examples
```
PATCH /v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8080
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json-patch+json
Cache-Control: no-cache

{  
  "state":"OK"
}
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON alarm object with the following fields:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* alarm_definition_id (string) - Name of alarm.
* description (string) - ID of the alarm definition.
* metrics ({string, string(255): string(255)}) - The metrics associated with the alarm.
* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.

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
            "rel":"state-history",
            "href":"http://192.168.10.4:8080/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
         }
      ],
      "alarm_definition_id":"ad837fca-5564-4cbf-523-0117f7dac6ad",
      "metrics":[{
         "name":"cpu.system_perc",
         "dimensions":{  
            "hostname":"devstack"
         }
      }],
      "state":"OK"
   }
]
```
___

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
___

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
* metric_name (string(255), optional) - Name of metric to filter by.
* metric_dimensions ({string(255): string(255)}, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`
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
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:38:15.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:42.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu.system_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:37:18.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "ALARM",
        "new_state": "UNDETERMINED",
        "reason": "No data was present for the sub-alarms: [avg(cpu.system_perc{hostname=devstack}) > 15.0]",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:26.000Z"
    },
    {
        "alarm_id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
        "metric_name": "cpu.system_perc",
        "metric_dimensions": {
          "hostname": "devstack"
        },
        "old_state": "UNDETERMINED",
        "new_state": "ALARM",
        "reason": "Alarm state updated via API",
        "reason_data": "{}",
        "timestamp": "2014-07-19T03:26:20.000Z"
    }
]
```
___

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

