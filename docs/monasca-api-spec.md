# Monasca API

Date: November 5, 2014

Document Version: v2.0

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**  *generated with [DocToc](https://github.com/thlorenz/doctoc)*

- [Overview](#overview)
  - [Metric Name and Dimensions](#metric-name-and-dimensions)
    - [Name](#name)
    - [Dimensions](#dimensions)
    - [Text Representation](#text-representation)
  - [Measurement](#measurement)
    - [Value Meta](#value-meta)
  - [Alarm Definitions and Alarms](#alarm-definitions-and-alarms)
    - [Deterministic or non-deterministic alarms](#deterministic-or-non-deterministic-alarms)
  - [Alarm Definition Expressions](#alarm-definition-expressions)
    - [Syntax](#syntax)
      - [Simple Example](#simple-example)
      - [More Complex Example](#more-complex-example)
      - [Compound alarm example](#compound-alarm-example)
      - [Deterministic alarm example](#deterministic-alarm-example)
      - [Non-deterministic alarm with deterministic sub expressions](#non-deterministic-alarm-with-deterministic-sub-expressions)
    - [Changing Alarm Definitions](#changing-alarm-definitions)
  - [Notification Methods](#notification-methods)
- [Common Request Headers](#common-request-headers)
  - [Common Http Request Headers](#common-http-request-headers)
  - [Non-standard request headers](#non-standard-request-headers)
- [Common Responses](#common-responses)
- [Paging](#paging)
  - [Offset](#offset)
  - [Limit](#limit)
- [JSON Results](#json-results)
- [Versions](#versions)
  - [List Versions](#list-versions)
    - [GET /](#get-)
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
        - [Single metric with value_meta](#single-metric-with-value_meta)
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
  - [List dimension values](#list-dimension-values)
      - [GET /v2.0/metrics/dimensions/names/values](#get-v20metricsdimensionsnamesvalues)
      - [Headers](#headers-4)
      - [Path Parameters](#path-parameters-4)
      - [Query Parameters](#query-parameters-4)
      - [Request Body](#request-body-4)
      - [Request Examples](#request-examples-4)
    - [Response](#response-4)
      - [Status Code](#status-code-2)
      - [Response Body](#response-body-4)
      - [Response Examples](#response-examples-3)
- [Measurements](#measurements)
  - [List measurements](#list-measurements)
    - [GET /v2.0/metrics/measurements](#get-v20metricsmeasurements)
      - [Headers](#headers-5)
      - [Path Parameters](#path-parameters-5)
      - [Query Parameters](#query-parameters-5)
      - [Request Body](#request-body-5)
      - [Request Examples](#request-examples-5)
    - [Response](#response-5)
      - [Status Code](#status-code-3)
      - [Response Body](#response-body-5)
      - [Response Examples](#response-examples-4)
- [Metric Names](#metric-names)
  - [List names](#list-names)
    - [GET /v2.0/metrics/names](#get-v20metricsnames)
      - [Headers](#headers-6)
      - [Path Parameters](#path-parameters-6)
      - [Query Parameters](#query-parameters-6)
      - [Request Body](#request-body-6)
      - [Request Examples](#request-examples-6)
    - [Response](#response-6)
      - [Status Code](#status-code-4)
      - [Response Body](#response-body-6)
      - [Response Examples](#response-examples-5)
- [Statistics](#statistics)
  - [List statistics](#list-statistics)
    - [GET /v2.0/metrics/statistics](#get-v20metricsstatistics)
      - [Headers](#headers-7)
      - [Path Parameters](#path-parameters-7)
      - [Query Parameters](#query-parameters-7)
      - [Request Body](#request-body-7)
      - [Request Examples](#request-examples-7)
    - [Response](#response-7)
      - [Status Code](#status-code-5)
      - [Response Body](#response-body-7)
      - [Response Examples](#response-examples-6)
- [Notification Methods](#notification-methods-1)
  - [Create Notification Method](#create-notification-method)
    - [POST /v2.0/notification-methods](#post-v20notification-methods)
      - [Headers](#headers-8)
      - [Path Parameters](#path-parameters-8)
      - [Query Parameters](#query-parameters-8)
      - [Request Body](#request-body-8)
      - [Request Examples](#request-examples-8)
    - [Response](#response-8)
      - [Status Code](#status-code-6)
      - [Response Body](#response-body-8)
      - [Response Examples](#response-examples-7)
  - [List Notification Methods](#list-notification-methods)
    - [GET /v2.0/notification-methods](#get-v20notification-methods)
      - [Headers](#headers-9)
      - [Path Parameters](#path-parameters-9)
      - [Query Parameters](#query-parameters-9)
      - [Request Body](#request-body-9)
      - [Request Examples](#request-examples-9)
    - [Response](#response-9)
      - [Status Code](#status-code-7)
      - [Response Body](#response-body-9)
      - [Response Examples](#response-examples-8)
  - [Get Notification Method](#get-notification-method)
    - [GET /v2.0/notification-methods/{notification_method_id}](#get-v20notification-methodsnotification_method_id)
      - [Headers](#headers-10)
      - [Path Parameters](#path-parameters-10)
      - [Query Parameters](#query-parameters-10)
      - [Request Body](#request-body-10)
      - [Request Examples](#request-examples-10)
    - [Response](#response-10)
      - [Status Code](#status-code-8)
      - [Response Body](#response-body-10)
      - [Response Examples](#response-examples-9)
  - [Update Notification Method](#update-notification-method)
    - [PUT /v2.0/notification-methods/{notification_method_id}](#put-v20notification-methodsnotification_method_id)
      - [Headers](#headers-11)
      - [Path Parameters](#path-parameters-11)
      - [Query Parameters](#query-parameters-11)
      - [Request Body](#request-body-11)
      - [Request Examples](#request-examples-11)
    - [Response](#response-11)
      - [Status Code](#status-code-9)
      - [Response Body](#response-body-11)
      - [Response Examples](#response-examples-10)
  - [Patch Notification Method](#patch-notification-method)
    - [PATCH /v2.0/notification-methods/{notification_method_id}](#patch-v20notification-methodsnotification_method_id)
      - [Headers](#headers-12)
      - [Path Parameters](#path-parameters-12)
      - [Query Parameters](#query-parameters-12)
      - [Request Body](#request-body-12)
      - [Request Examples](#request-examples-12)
    - [Response](#response-12)
      - [Status Code](#status-code-10)
      - [Response Body](#response-body-12)
      - [Response Examples](#response-examples-11)
  - [Delete Notification Method](#delete-notification-method)
    - [DELETE /v2.0/notification-methods/{notification_method_id}](#delete-v20notification-methodsnotification_method_id)
      - [Headers](#headers-13)
      - [Path Parameters](#path-parameters-13)
      - [Query Parameters](#query-parameters-13)
      - [Request Body](#request-body-13)
      - [Request Examples](#request-examples-13)
    - [Response](#response-13)
      - [Status Code](#status-code-11)
      - [Response Body](#response-body-13)
  - [List supported Notification Method Types](#list-supported-notification-method-types)
    - [GET /v2.0/notification-methods/types/](#get-v20notification-methodstypes)
      - [Headers](#headers-14)
      - [Query Parameters](#query-parameters-14)
      - [Request Body](#request-body-14)
      - [Request Examples](#request-examples-14)
    - [Response](#response-14)
      - [Status Code](#status-code-12)
      - [Response Body](#response-body-14)
      - [Response Examples](#response-examples-12)
- [Alarm Definitions](#alarm-definitions)
  - [Create Alarm Definition](#create-alarm-definition)
    - [POST /v2.0/alarm-definitions](#post-v20alarm-definitions)
      - [Headers](#headers-15)
      - [Path Parameters](#path-parameters-14)
      - [Query Parameters](#query-parameters-15)
      - [Request Body](#request-body-15)
      - [Request Examples](#request-examples-15)
    - [Response](#response-15)
      - [Status Code](#status-code-13)
      - [Response Body](#response-body-15)
      - [Response Examples](#response-examples-13)
  - [List Alarm Definitions](#list-alarm-definitions)
    - [GET /v2.0/alarm-definitions](#get-v20alarm-definitions)
      - [Headers](#headers-16)
      - [Path Parameters](#path-parameters-15)
      - [Query Parameters](#query-parameters-16)
      - [Request Body](#request-body-16)
      - [Request Examples](#request-examples-16)
    - [Response](#response-16)
      - [Status Code](#status-code-14)
      - [Response Body](#response-body-16)
      - [Response Examples](#response-examples-14)
  - [Get Alarm Definition](#get-alarm-definition)
    - [GET /v2.0/alarm-definitions/{alarm_definition_id}](#get-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-17)
      - [Path Parameters](#path-parameters-16)
      - [Query Parameters](#query-parameters-17)
      - [Request Body](#request-body-17)
    - [Response](#response-17)
      - [Status Code](#status-code-15)
      - [Response Body](#response-body-17)
      - [Response Examples](#response-examples-15)
  - [Update Alarm Definition](#update-alarm-definition)
    - [PUT /v2.0/alarm-definitions/{alarm_definition_id}](#put-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-18)
      - [Path Parameters](#path-parameters-17)
      - [Query Parameters](#query-parameters-18)
      - [Request Body](#request-body-18)
      - [Request Examples](#request-examples-17)
    - [Response](#response-18)
      - [Status Code](#status-code-16)
      - [Response Body](#response-body-18)
      - [Response Examples](#response-examples-16)
  - [Patch Alarm Definition](#patch-alarm-definition)
    - [PATCH /v2.0/alarm-definitions/{alarm_definition_id}](#patch-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-19)
      - [Path Parameters](#path-parameters-18)
      - [Query Parameters](#query-parameters-19)
      - [Request Body](#request-body-19)
      - [Request Examples](#request-examples-18)
    - [Response](#response-19)
      - [Status Code](#status-code-17)
      - [Response Body](#response-body-19)
      - [Response Examples](#response-examples-17)
  - [Delete Alarm Definition](#delete-alarm-definition)
    - [DELETE /v2.0/alarm-definitions/{alarm_definition_id}](#delete-v20alarm-definitionsalarm_definition_id)
      - [Headers](#headers-20)
      - [Path Parameters](#path-parameters-19)
      - [Query Parameters](#query-parameters-20)
      - [Request Body](#request-body-20)
      - [Request Examples](#request-examples-19)
    - [Response](#response-20)
      - [Status Code](#status-code-18)
      - [Response Body](#response-body-20)
- [Alarms](#alarms)
  - [List Alarms](#list-alarms)
    - [GET /v2.0/alarms](#get-v20alarms)
      - [Headers](#headers-21)
      - [Path Parameters](#path-parameters-20)
      - [Query Parameters](#query-parameters-21)
      - [Request Body](#request-body-21)
      - [Request Examples](#request-examples-20)
    - [Response](#response-21)
      - [Status Code](#status-code-19)
      - [Response Body](#response-body-21)
      - [Response Examples](#response-examples-18)
  - [List Alarms State History](#list-alarms-state-history)
    - [GET /v2.0/alarms/state-history](#get-v20alarmsstate-history)
      - [Headers](#headers-22)
      - [Path Parameters](#path-parameters-21)
      - [Query Parameters](#query-parameters-22)
      - [Request Body](#request-body-22)
    - [Response](#response-22)
      - [Status Code](#status-code-20)
      - [Response Body](#response-body-22)
      - [Response Examples](#response-examples-19)
  - [Get Alarm](#get-alarm)
    - [GET /v2.0/alarms/{alarm_id}](#get-v20alarmsalarm_id)
      - [Headers](#headers-23)
      - [Path Parameters](#path-parameters-22)
      - [Query Parameters](#query-parameters-23)
      - [Request Body](#request-body-23)
    - [Response](#response-23)
      - [Status Code](#status-code-21)
      - [Response Body](#response-body-23)
      - [Response Examples](#response-examples-20)
  - [Update Alarm](#update-alarm)
    - [PUT /v2.0/alarms/{alarm_id}](#put-v20alarmsalarm_id)
      - [Headers](#headers-24)
      - [Path Parameters](#path-parameters-23)
      - [Query Parameters](#query-parameters-24)
      - [Request Body](#request-body-24)
      - [Request Examples](#request-examples-21)
    - [Response](#response-24)
      - [Status Code](#status-code-22)
      - [Response Body](#response-body-24)
      - [Response Examples](#response-examples-21)
  - [Patch Alarm](#patch-alarm)
    - [PATCH /v2.0/alarms/{alarm_id}](#patch-v20alarmsalarm_id)
      - [Headers](#headers-25)
      - [Path Parameters](#path-parameters-24)
      - [Query Parameters](#query-parameters-25)
      - [Request Body](#request-body-25)
      - [Request Examples](#request-examples-22)
    - [Response](#response-25)
      - [Status Code](#status-code-23)
      - [Response Body](#response-body-25)
      - [Response Examples](#response-examples-22)
  - [Delete Alarm](#delete-alarm)
    - [DELETE /v2.0/alarms/{alarm_id}](#delete-v20alarmsalarm_id)
      - [Headers](#headers-26)
      - [Path Parameters](#path-parameters-25)
      - [Query Parameters](#query-parameters-26)
      - [Request Body](#request-body-26)
      - [Request Examples](#request-examples-23)
    - [Response](#response-26)
      - [Status Code](#status-code-24)
      - [Response Body](#response-body-26)
  - [List Alarm State History](#list-alarm-state-history)
    - [GET /v2.0/alarms/{alarm_id}/state-history](#get-v20alarmsalarm_idstate-history)
      - [Headers](#headers-27)
      - [Path Parameters](#path-parameters-26)
      - [Query Parameters](#query-parameters-27)
      - [Request Body](#request-body-27)
      - [Request Data](#request-data)
    - [Response](#response-27)
      - [Status Code](#status-code-25)
      - [Response Body](#response-body-27)
      - [Response Examples](#response-examples-23)
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
Defines the name of a metric. A name is of type string(255). The name may include any characters except the following: `> < = { } ( ) , ' " \ ; &`. Note that JSON does allow control characters (such as `\n`), however these should not be used in metric names.

### Dimensions
A dictionary of (key, value) pairs. The key and value are of type string(255). Dimension keys may not begin with '_' (underscore). The dimension key and value strings may include any characters except the following: `> < = { } ( ) , ' " \ ; &`. Note that JSON does allow control characters (such as `\n`), however these should not be used in dimension keys or values. Dimension keys and values must not be empty.

### Text Representation
In this document, metrics will be represented in the form `name{name=value,name=value}` where name is the metric name and the name=value pairs in the curly braces are the dimensions. For example, `cpu.idle_perc{service=monitoring,hostname=mini-mon}` represents a metric with the name "cpu.idle_perc" and the dimensions "service=monitoring" and "hostname=mini-mon".

## Measurement
A measurement is a value with a timestamp for a specific Metric. The value is represented by a double, e.g. 42.0 or 42.42.

### Value Meta
Optionally, a measurement may also contain extra data about the value which is known as value meta. Value meta is a set of name/value pairs that add textual data to the value of the measurement.  The value meta will be returned from the API when the measurement is read. Only measurements that were written with value meta will have the key value pairs when read from the API. The value meta is ignored when computing statistics such as average on measurements.

For an example of how value meta is used, imagine this metric: http_status{url: http://localhost:8070/healthcheck, hostname=devstack, service=object-storage}.  The measurements for this metric have a value of either 1 or 0 depending if the status check succeeded. If the check fails, it would be helpful to have the actual http status code and error message if possible. So instead of just a value, the measurement will be something like:
{Timestamp=now(), value=1, value_meta{http_rc=500, error=“Error accessing MySQL”}}

Up to 16 separate key/value pairs of value meta are allowed per measurement. The keys are required and are trimmed of leading and trailing whitespace and have a maximum length of 255 characters. The value is a string and value meta (with key, value and '{"":""}' combined) has a maximum length of 2048 characters. The value can be an empty string. Whitespace is not trimmed from the values.

## Alarm Definitions and Alarms

Alarm Definitions are policies that specify how Alarms should be created. By using Alarm Definitions, the user doesn't have to create individual alarms for each system or service. Instead, a small number of Alarm Definitions can be managed and Monasca will create Alarms for systems and services as they appear.

An Alarm Definition has an expression for evaluating one or more metrics to determine if there is a problem. Depending on the Alarm Definition expression and match_by value, Monasca will create one or more Alarms depending on the measurements that are received. The match_by parameter specifies which dimension or dimensions should be used to determine if one or more alarms will be created.

An example is the best way to show this. Imagine two Alarm Definitions have been created:

Alarm Definition 1 has an expression of `avg(cpu.idle_perc{service=monitoring}) < 20` and the match_by parameter is not set.  Alarm Definition 2 has an expression of `min(cpu.idle_perc{service=monitoring}) < 10` and the match_by parameter is set to `hostname`.

When a measurement for the metric cpu.idle_perc{service=monitoring,hostname=mini-mon} is first received after the Metric Definitions have been created, an Alarm is created for both Alarm Definitions. The metric is added to both Alarms. The following set of Alarm Definitions and Alarm would exist:

Alarm Definition 1:
```
Alarm 1 - Metrics: cpu.idle_perc{service=monitoring,hostname=mini-mon}
```

Alarm Definition 2:
```
Alarm 1 - Metrics: cpu.idle_perc{service=monitoring,hostname=mini-mon}
```

Now, when a measurement for the metric cpu.idle_perc{service=monitoring,hostname=devstack} is received, the two Alarm Definitions define different behaviors. Since the value for the hostname dimension is different from the value for the existing Alarm from Alarm Definition 2, and Alarm Definition 2 has specified a match_by parameter on `hostname`, a new Alarm will be created.  Alarm Definition 1 does not have a value for match_by, so this metric is added to the existing Alarm. This gives us the following set of Alarm Definitions and Alarms:

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

Now if another system is configured into the monitoring service, then when a measurement is received for its cpu.idle_perc metric, that metric will be added to the Alarm for Alarm Definition 1 and a new Alarm will be created for Alarm Definition 2, all without any user intervention. The system will be monitored without requiring the user to explicitly add alarms for the new system as other monitoring systems require.

If an Alarm Definition expression has multiple subexpressions, for example, `avg(cpu.idle_perc{service=monitoring}) < 10 or avg(cpu.user_perc{service=monitoring}) > 60` and a match_by value set, then the metrics for both subexpressions must have the same value for the dimension specified in match_by. For example, assume this Alarm Definition:

Expression `avg(cpu.idle_perc{service=monitoring}) < 10 or avg(cpu.user_perc{service=monitoring}) > 60` and match_by is `hostname`

Now assume a measurement for each of these four metrics is received by Monasca:

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

Note that the value of match_by, "hostname", is used to match the metrics between the subexpressions, hence the name 'match_by'.

As a negative example, assume a measurement for the below metric is received by Monasca:

```
cpu.idle_perc{service=nova,hostname=nova1}
```

This metric does not have the service=monitoring dimension, so it will not match the Alarm Definition and no Alarm will be created or metric added to an existing alarm.

An Alarm will only get created when measurements are seen for metrics that match all subexpressions in the Alarm Definition.  If match_by is set, then each metric must have a value for at least one of the values in match_by. If match_by is not set, only one Alarm will be created for an Alarm Definition.

The value of the match_by parameter can also be a list, for example, `hostname,device`. In that case, Alarms will be created and metrics added based on all values of match_by.

For example, assume the Alarm Definition with the expression `max(disk.space_used_perc{service=monitoring}) > 90` and match_by set to `hostname`. This will create one alarm for each system that contains all of the metrics for each device. If instead, the match_by is set to `hostname,device`, then a separate alarm will be created for each device in each system.

To illustrate, assume a measurement for each of these four metrics is received by Monasca:
```
disk.space_used_perc{device:/dev/sda1,hostname=mini-mon}
disk.space_used_perc{device:tmpfs,hostname=mini-mon}
disk.space_used_perc{device:/dev/sda1,hostname=devstack}
disk.space_used_perc{device:tmpfs,hostname=devstack}
```

Given the expression  `max(disk.space_used_perc) > 90` and match_by set to `hostname`, this will create two alarms:

```
Alarm 1 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=mini-mon}, disk.space_used_perc{device:tmpfs,hostname=mini-mon}
Alarm 2 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=devstack}, disk.space_used_perc{device:tmpfs,hostname=devstack}
```

If instead, match_by is set to `hostname,device`, then four alarms will be created:

```
Alarm 1 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=mini-mon}
Alarm 2 - Metrics: disk.space_used_perc{device:tmpfs,hostname=mini-mon}
Alarm 3 - Metrics: disk.space_used_perc{device:/dev/sda1,hostname=devstack}
Alarm 4 - Metrics: disk.space_used_perc{device:tmpfs,hostname=devstack}
```

The second value of match_by will create an Alarm for each device. For each device that fills up, a separate Alarm will be triggered. The first value of match_by will give you less Alarms to display in the dashboard but if an Alarm has already triggered for one device and another device fills up, the Alarm won't be triggered again.

If desired, an Alarm Definition can be created that exactly matches a set of metrics. The match_by should not be set. Only one Alarm will be created for that Alarm Definition.

Alarms have a state that is set by the Threshold Engine based on the incoming measurements.

* UNDETERMINED - No measurements have been received for at least one of the subexpressions in any period for a least 2 * periods (see below for definition of period and periods
* OK - Measurements have been received and the Alarm Definition Expression evaluates to false for the given measurements
* ALARM - Measurements have been received and the Alarm Definition Expression evaluates to true for the given measurements

The Alarms are evaluated and their state is set once per minute.

Alarms contain three fields that may be edited via the API. These are the alarm state, lifecycle state, and the link. The alarm state is updated by Monasca as measurements are evaluated, and can be changed manually as necessary. The lifecycle state and link fields are not maintained or updated by Monasca, instead these are provided for storing information related to external tools.

### Deterministic or non-deterministic alarms

By default all alarm definitions are assumed to be **non-deterministic**.
There are 3 possible states such alarms can transition to: *OK*, *ALARM*,
*UNDETERMINED*. On the other hand, alarm definitions can be also
**deterministic**. In that case alarm is allowed to transition only: *OK*
and *ALARM* state.

Following expression ```avg(cpu.user_perc{hostname=compute_node_1}) > 10``` means that potential
alarm and transition to *ALARM* state is restricted to specific machine. If for some reason that
host would crash and stay offline long enough, there would be no measurements received from it.
In this case alarm will transition to *UNDETERMINED* state.

On the other hand, some metrics are irregular and look more like events. One case is
metric created only if something critical happens in the system.
For example an error in log file or deadlock in database.
If non-deterministic alarm definition would be created using expression ```count(log.error{component=mysql}) >= 1)```,
that alarm could stay in *UNDETERMINED* state for most of its lifetime.
However, from operator point of view, if there are no errors related to MySQL, everything works correctly.
Answer to that situation is creating *deterministic* alarm definition
using expression ```count(log.error{component=mysql}, deterministic) >= 1```.

The deterministic alarm's main trait is preventing from transition to *UNDETERMINED* state.
The alarm should be *OK* if no data is received. Also such alarms transition to *OK* immediately when created,
rather than to *UNDETERMINED* state.

Finally, it must be mentioned that alarm definition can be composed of multiple expressions and
that *deterministic* is actually part of it. The entire alarm definition is considered *deterministic*
only if all of its expressions are such. Otherwise the alarm is *non-deterministic*.

For example:
```
avg(disk.space_used_perc{hostname=compute_node_1}) >= 99
    and
count(log.error{hostname=compute_node_1,component=kafka},deterministic) >= 1
```
potential alarm will transition to *ALARM* state if there is no usable disk space left and kafka starts to report errors regarding
inability to save data to it. Second expression is *deterministic*, however entire alarm will be kept in *UNDETERMINED* state
until such situation happens.

On the other hand, expression like this:
```
avg(disk.space_used_perc{hostname=compute_node_1},deterministic) >= 99
    and
count(log.error{hostname=compute_node_1,component=kafka},deterministic) >= 1
```
makes entire alarm *deterministic*. In other words - *all parts of alarm's expression
must be marked as deterministic in order for entire alarm to be considered such*.
Having definition like one above, potential alarm will stay in *OK* state as long as there is enough
disk space left at *compute_node_1* and there are no errors reported from *kafka* running
at the same host.

## Alarm Definition Expressions
The alarm definition expression syntax allows the creation of simple or complex alarm definitions to handle a wide variety of needs. Alarm expressions are evaluated every 60 seconds.

An alarm expression is a boolean equation which is used to evaluate the state of an alarm based on the received measurements. If the expression evaluates to true the state of the alarm to be set to ALARM. If it evaluates to false, the state of the alarm will be set to OK.

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
    ::= <function> '(' <metric> [',' deterministic] [',' period] ')' <relational_operator> threshold_value ['times' periods]
    | '(' expression ')'

````
Period must be an integer multiple of 60. The default period is 60 seconds.

Expression is by default **non-deterministic** (i.e. when expression does
not contain *deterministic* keyword). If however **deterministic**
option would be desired, it is enough to have *deterministic* keyword
inside expression.

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

#### Deterministic alarm example
In this example alarm is created with one expression which is deterministic

```
count(log.error{}, deterministic) > 1
```

#### Non-deterministic alarm with deterministic sub expressions
In this example alarm's expression is composed of 3 parts where two of them
are marked as **deterministic**. However entire expression is non-deterministic because
of the 3rd expression.

```
count(log.error{}, deterministic) > 1 or count(log.warning{}, deterministic) > 1 and avg(cpu.user_perc{}) > 10
```

### Changing Alarm Definitions

Once an Alarm Definition has been created, the value for match_by and any metrics in the expression cannot be changed. This is because those fields control the metrics used to create Alarms and Alarms may already have been created. The function, operator, period, periods and any boolean operators can change, but not the metrics in subexpressions or the number of subexpressions.  All other fields in an Alarm Definition can be changed.

The only option to change metrics or match_by is to delete the existing Alarm Definition and create a new one. Deleting an Alarm Definition will delete all Alarms associated with it.

## Notification Methods
Notification methods are resources used to specify a notification name, type and address that notifications can be sent to. After a notification method has been created, it can be associated with actions in alarm definitions, such that when an alarm state transition occurs, one or more notifications can be sent.

Currently, notification method types of email, PagerDuty and webhooks are supported. In the case of email, the address is the email address. In the case of PagerDuty, the address is the PagerDuty Service API Key. In the case of a webhook, the address is the URL of the webhook.

# Common Request Headers
This section documents the common request headers that are used in requests.

## Common Http Request Headers
The standard Http request headers that are used in requests.

* Content-Type - The Internet media type of the request body. Used with POST and PUT requests. Must be `application/json`.
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

# Paging
The Monasca API implements a paging mechanism to allow users to 'page' through result sets returned from the API. The paging functionality is limited to resources that return unbounded lists of results. This permits the user to consume as much data from the API as is needed without placing undo memory consumption burdens on the Monasca API Server. The paging mechanism is accomplished by allowing the user to specify an offset and a limit in the request URL as query parameters.

For example:

```
"http://192.168.10.4:8070/v2.0/metrics/measurements?offset=2015-03-03T05%3A21%3A55Z&limit=1000&name=cpu.system_perc&dimensions=hostname%3Adevstack&start_time=2014-07-18T03%3A00%3A00Z"

```

Results sets that would otherwise return more results if there had not been a limit will include a next link with the offset prepopulated. The user only need use the next link to get the next set of results.

If no limit is specified in the request URL, then a server-wide configurable limit is applied.


## Offset
Offsets can be either integer offsets, string offsets (including hexadecimal numbers), or timestamp offsets. The use of either integer, string, or timestamp is determined by the resource being queried.

For example, an integer offset would look like this:

```
offset=999

```
Integer offsets are zero based.

A string offset would look like this:

```
offset=c60ec47e-5038-4bf1-9f95-4046c6e9a759

```

A hexadecimal string offset would look like this:

```

offset=01ce0acc66131296c8a17294f39aee44ea8963ec

```

A timestamp offset would look like this:

```
offset=2104-01-01T00:00:01Z

```

A dimension value offset would look as follows:

```
offset=dimensionValue2

```

Different resources use different offset types because of the internal implementation of different resources depends on different types of mechanisms for indexing and identifying resources. The type and form of the offsets for each resource can be determined by referring to the examples in each resource section below.

The offset is determined by the ID of the last element in the result list. Users wishing to manually create a query URL can use the ID of the last element in the previously returned result set as the offset. The proceeding result set will return all elements with an ID greater than the offset up to the limit. The automatically generated offset in the next link does exactly this; it uses the ID in the last element.

The offset can take the form of an integer, string, or timestamp, but the user should treat the offset as an opaque reference. When using offsets in manually generated URLs, users enter them as strings that look like integers, timestamps, or strings. Future releases may change the type and form of the offsets for each resource.

## Limit
The Monasca API has a server-wide default limit that is applied. Users may specifiy their own limit in the URL, but the server-wide limit may not be exceeded. The Monasca server-wide limit is configured in the Monasca API config file as maxQueryLimit. Users may specify a limit up to the maxQueryLimit.

```
limit=1000

```
# JSON Results
All Monasca API results are in the form of JSON. For resources that return a list of elements, the JSON object returned will contain a 'links' array and an 'elements' array.

The 'links' array will contain a 'self' element that is the original URL of the request that was used to generate the result. The 'links' array may also contain a 'next' element if the number of elements in the result would exceed the query limit. The 'next' link can be used to query the Monasca API for the next set of results, thus allowing the user to page through lengthy data sets.

The 'elements' array will contain the items from the resource that match the query parameters. Each element will have an 'id' element. The 'id' element of the last item in the elements list is used as the offset in the 'next' link.

For example:

```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/metrics&limit=2"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/metrics?offset=1&limit=2"
        }
    ],
    "elements": [
        {
            "id": 0,
            "name": "name1",
            "dimensions": {
                "key1": "value1"
            }
        },
        {
            "id": 1,
            "name": "name2",
            "dimensions": {
                "key1": "value1"
            }
        }
    ]
}
```

# Versions
The versions resource supplies operations for accessing information about supported versions of the API.

## List Versions
Lists the supported versions of the Monasca API.

### GET /

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
Host: 192.168.10.4:8070
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Accept: application/json
Cache-Control: no-cache
```

### Response
#### Status code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of supported versions.

#### Response Examples
```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/"
        }
    ],
    "elements": [
        {
            "id": "v2.0",
            "links": [
                {
                    "rel": "self",
                    "href": "http://192.168.10.4:8070/v2.0"
                }
            ],
            "status": "CURRENT",
            "updated": "2014-07-18T03:25:02.423Z"
        }
    ]
}
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
Host: 192.168.10.4:8070
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
         "href":"http://192.168.10.4:8070/v2.0/"
      }
   ],
   "status":"CURRENT",
   "updated":"2014-07-18T03:25:02.423Z"
}
```
___

# Metrics
The metrics resource allows metrics to be created and queried. The `X-Auth-Token` is used to derive the tenant that submits metrics. Metrics are stored and scoped to the tenant that submits them, or if the `tenant_id` query parameter is specified and the tenant has the `monitoring-delegate` role, the metrics are stored using the specified tenant ID.  Note that several of the GET methods also support the tenant_id query parameter, but the `monasca-admin` role is required to get cross-tenant metrics, statistics, etc..

## Create Metric
Create metrics.

### POST /v2.0/metrics

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json

#### Path Parameters
None.

#### Query Parameters
* tenant_id (string, optional, restricted) - Tenant ID to create metrics on behalf of. This parameter can be used to submit metrics from one tenant, to another. Normally, this parameter is used when the Agent is being run as an operational monitoring tenant, such as monitoring OpenStack infrastructure, and needs to submit metrics for an OpenStack resource, such as a VM, but those metrics need to be accessible to the tenant that owns the resource. Usage of this query parameter is restricted to users with the `monitoring-delegate` role.

#### Request Body
Consists of a single metric object or an array of metric objects. A metric has the following properties:

* name (string(255), required) - The name of the metric.
* dimensions ({string(255): string(255)}, optional) - A dictionary consisting of (key, value) pairs used to uniquely identify a metric.
* timestamp (string, required) - The timestamp in milliseconds from the Epoch.
* value (float, required) - Value of the metric. Values with base-10 exponents greater than 126 or less than -130 are truncated.
* value_meta ({string(255): string}(2048), optional) - A dictionary consisting of (key, value) pairs used to add information about the value. Value_meta key value combinations must be 2048 characters or less including '{"":""}' 7 characters total from every json string.

The name and dimensions are used to uniquely identify a metric.

#### Request Examples

##### Single metric
POST a single metric.

```
POST /v2.0/metrics HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache

{
   "name":"name1",
   "dimensions":{
      "key1":"value1",
      "key2":"value2"
   },
   "timestamp":1405630174123,
   "value":1.0
}
```

##### Single metric with value_meta
POST a single metric with value_meta.

```
POST /v2.0/metrics HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache

{
   "name":"name1",
   "dimensions":{
      "key1":"value1",
      "key2":"value2"
   },
   "timestamp":1405630174123,
   "value":1.0,
   "value_meta":{
      "key1":"value1",
      "key2":"value2"
   }
}
```

##### Array of metrics
POST an array of metrics.

```
POST /v2.0/metrics HTTP/1.1
Host: 192.168.10.4:8070
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
      "timestamp":1405630174123,
      "value":1.0
   },
   {
      "name":"name2",
      "dimensions":{
         "key1":"value1",
         "key2":"value2"
      },
      "timestamp":1405630174123,
      "value":2.0,
      "value_meta":{
         "key1":"value1",
         "key2":"value2"
      }
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
* tenant_id (string, optional, restricted) - Tenant ID to from which to get metrics. This parameter can be used to get metrics from a tenant other than the tenant the request auth token is scoped to. Usage of this query parameter is restricted to users with the the monasca admin role, as defined in the monasca api configuration file, which defaults to `monasca-admin`.
* name (string(255), optional) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`, leaving the value empty `key1,key2:value2` will return all values for that key, multiple values for a key may be specified as `key1:value1|value2|...,key2:value4,...`
* start_time (string, optional) - The start time in ISO 8601 combined date and time format in UTC.  This is useful for only listing metrics that have measurements since the specified start_time.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.  Combined with start_time, this can be useful to only list metrics that have measurements in between the specified start_time and end_time.
* offset (integer (InfluxDB) or hexadecimal string (Vertica), optional)
* limit (integer, optional)

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics?name=metric1&dimensions=key1:value1 HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of metric definition objects with the following fields:

* name (string)
* dimensions ({string(255): string(255)})

#### Response Examples
````
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/metrics"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/metrics?offset=1"
        }
    ],
    "elements": [
        {
            "id": 0,
            "name": "name1",
            "dimensions": {
                "key1": "value1"
            }
        },
        {
            "id": 1,
            "name": "name2",
            "dimensions": {
                "key1": "value1"
            }
        }
    ]
}
````
___

## List dimension values
Get dimension values

#### GET /v2.0/metrics/dimensions/names/values

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* tenant_id (string, optional, restricted) - Tenant ID to from which to get dimension values. This parameter can be used to get dimension values from a tenant other than the tenant the request auth token is scoped to. Usage of this query parameter is restricted to users with the the monasca admin role, as defined in the monasca api configuration file, which defaults to `monasca-admin`.
* metric_name (string(255), optional) - A metric name to filter dimension values by.
* dimension_name (string(255), required) - A dimension name to filter dimension values by.
* offset (string(255), optional) - The dimension values are returned in alphabetic order, and the offset is the dimension name after which to return in the next pagination request.
* limit (integer, optional)

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/dimensions/names/values?dimension_name=dimension_name HTTP/1.1
Host: 192.168.10.4:8080
Content-Type: application/json
X-Auth-Token: 27feed73a0ce4138934e30d619b415b0
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of metric definition objects with the following fields:

* dimension_name (string)
* metric_name (string)
* values (list of strings)

#### Response Examples
````
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8080/v2.0/metrics/dimensions/names/values?dimension_name=dimension_name"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8080/v2.0/metrics/dimensions/names/values?dimension_name=dimension_name&offset=dimensionValue2"
        }
    ],
    "elements": [
        {
            "id": "b63d9bc1e16582d0ca039616ce3c870556b3095b",
            "metric_name": "metric_name",
            "dimension_name": "dimension_name",
            "values": [
                "dimensonValue1",
                "dimensonValue2"
            ]
        }
    ]
}
````
___

# Measurements
Operations for accessing measurements of metrics.

## List measurements
Get measurements for metrics.

If `group_by` is not specified, metrics must be fully qualified with name and dimensions so that only measurements are returned for a single metric. If the metric name and dimensions given do not resolve to a single metric, an error will be displayed asking the user to further qualify the metric with a name and additional dimensions.

If users do not wish to see measurements for a single metric, but would prefer to have measurements from multiple metrics combined, a 'merge_metrics' flag can be specified. when 'merge_metrics' is set to true (**merge_metrics=true**), all meaurements for all metrics that satisfy the query parameters will be merged into a single list of measurements.

### GET /v2.0/metrics/measurements

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* tenant_id (string, optional, restricted) - Tenant ID from which to get measurements from. This parameter can be used to get metrics from a tenant other than the tenant the request auth token is scoped to. Usage of this query parameter is restricted to users with the monasca admin role, as defined in the monasca api configuration file, which defaults to `monasca-admin`.
* name (string(255), required) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`
* start_time (string, required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.
* offset (timestamp, optional)
* limit (integer, optional)
* merge_metrics (boolean, optional) - allow multiple metrics to be combined into a single list of measurements.
* group_by (string, optional) - list of columns to group the metrics to be returned. For now, the only valid value is '*'.

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/measurements?name=cpu.system_perc&dimensions=hostname:devstack&start_time=2015-03-00T00:00:01Z HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of measurements objects for each unique metric with the following fields:

* name (string(255)) - A name of a metric.
* dimensions ({string(255): string(255)}) - The dimensions of a metric.
* columns (array[string]) - An array of column names corresponding to the columns in measurements.
* measurements (array[array[]]) - A two dimensional array of measurements for each timestamp. The timestamp is in ISO 8601 combined date and time format, with millisecond resolution.

#### Response Examples
```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/metrics/measurements?start_time=2015-03-00T00%3A00%3A00Z&name=cpu.system_perc&dimensions=hostname%3Adevstack"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/metrics/measurements?offset=2015-03-03T05%3A24%3A55Z&name=cpu.system_perc&dimensions=hostname%3Adevstack&start_time=2015-03-00T00%3A00%3A00Z"
        }
    ],
    "elements": [
        {
            "id": "2015-03-03T05:24:55Z",
            "name": "http_status",
            "dimensions": {
                "url": "http://localhost:8774/v2.0",
                "hostname": "devstack",
                "service": "compute"
            },
            "columns": [
                "timestamp",
                "value",
                "value_meta"
            ],
            "measurements": [
                [
                    "2015-03-03T05:22:28.123Z",
                    0,
                    {}
                ],
                [
                    "2015-03-03T05:23:12.123Z",
                    0,
                    {}
                ],
                [
                    "2015-03-03T05:24:55.123Z",
                    1,
                    {
                        "rc": "404",
                        "error": "Not Found"
                    }
                ]
            ]
        }
    ]
}
```
___

# Metric Names
Operations for accessing names of metrics.

## List names
Get names for metrics.

### GET /v2.0/metrics/names

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* tenant_id (string, optional, restricted) - Tenant ID from which to get metric names. This parameter can be used to get metric names from a tenant other than the tenant the request auth token is scoped to. Usage of this query parameter is restricted to users with the monasca admin role, as defined in the monasca api configuration file, which defaults to `monasca-admin`.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`
* offset (integer, optional)
* limit (integer, optional)

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/names HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of metric name objects for each unique metric name (not including dimensions) with the following fields:

* name (string(255)) - A name of a metric.

#### Response Examples
```
{
    "elements": [
        {
            "name":"name1"
        },
        {
            "name":"name2"
        }
    ],
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/metrics/names?offset=tenantId%3region%26name1%26dimensionKey1%3DdimensionValue1%26dimensionKey2%3DdimensionValue2"
        },
        {
            "rel": "next"
            "href": http://192.168.10.4:8070/v2.0/metrics/names?offset=tenantId%3region%26name3%26dimensionKey1%3DdimensionValue1%26dimensionKey2%3DdimensionValue2
        }
    ]
}
```
___

# Statistics
Operations for calculating statistics of metrics.

If `group_by` is not specified, then metrics must be fully qualified with name and dimensions so that only statistics are returned for a single metric. If the metric name and dimensions given do not resolve to a single metric, an error will be displayed asking the user to further qualify the metric with a name and additional dimensions.

If users do not wish to see statistics for a single metric, but would prefer to have statistics from multiple metrics combined, a 'merge_metrics' flag can be specified. when 'merge_metrics' is set to true (**merge_metrics=true**), all statistics for all metrics that satisfy the query parameters will be merged into a single list of statistics.

## List statistics
Get statistics for metrics.

### GET /v2.0/metrics/statistics

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Accept (string) - application/json

#### Path Parameters
None.

#### Query Parameters
* tenant_id (string, optional, restricted) - Tenant ID from which to get statistics. This parameter can be used to get statistics from a tenant other than the tenant the request auth token is scoped to. Usage of this query parameter is restricted to users with the monasca admin role, as defined in the monasca api configuration file, which defaults to `monasca-admin`.
* name (string(255), required) - A metric name to filter metrics by.
* dimensions (string, optional) - A dictionary to filter metrics by specified as a comma separated array of (key, value) pairs as `key1:value1,key2:value2, ...`
* statistics (string, required) - A comma separate array of statistics to evaluate. Valid statistics are avg, min, max, sum and count.
* start_time (string, required) - The start time in ISO 8601 combined date and time format in UTC.
* end_time (string, optional) - The end time in ISO 8601 combined date and time format in UTC.
* period (integer, optional) - The time period to aggregate measurements by. Default is 300 seconds.
* offset (timestamp, optional)
* limit (integer, optional)
* merge_metrics (boolean, optional) - allow multiple metrics to be combined into a single list of statistics.
* group_by (string, optional) - list of columns to group the metrics to be returned. For now, the only valid value is '*'.

#### Request Body
None.

#### Request Examples
```
GET /v2.0/metrics/statistics?name=cpu.system_perc&dimensions=hostname:devstack&start_time=2014-07-18T03:00:00Z&statistics=avg,min,max,sum,count HTTP/1.1
Host: 192.168.10.4:8070
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache
```

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of statistic objects for each unique metric with the following fields:

* name (string(255)) - A name of a metric.
* dimensions ({string(255): string(255)}) - The dimensions of a metric.
* columns (array[string]) - An array of column names corresponding to the columns in statistics.
* statistics (array[array[]]) - A two dimensional array of statistics for each period.

#### Response Examples
```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/metrics/statistics?start_time=2014-07-18T03%3A00%3A00Z&name=cpu.system_perc&dimensions=hostname%3Adevstack&statistics=avg%2Cmin%2Cmax%2Csum%2Ccount"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/metrics/statistics?offset=2014-07-18T03%3A22%3A00Z&name=cpu.system_perc&dimensions=hostname%3Adevstack&start_time=2014-07-18T03%3A00%3A00Z&statistics=avg%2Cmin%2Cmax%2Csum%2Ccount"
        }
    ],
    "elements": [
        {
            "id": "2014-07-18T03:22:00Z",
            "name": "cpu.system_perc",
            "dimensions": {
                "hostname": "devstack"
            },
            "columns": [
                "timestamp",
                "avg",
                "min",
                "max",
                "sum",
                "count"
            ],
            "statistics": [
                [
                    "2014-07-18T03:20:00Z",
                    2.765,
                    1.95,
                    4.93,
                    22.119999999999997,
                    8
                ],
                [
                    "2014-07-18T03:21:00Z",
                    2.412941176470588,
                    1.71,
                    4.09,
                    41.019999999999996,
                    17
                ],
                [
                    "2014-07-18T03:22:00Z",
                    2.1135294117647065,
                    1.62,
                    3.85,
                    35.93000000000001,
                    17
                ]
            ]
        }
    ]
}
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
* type (string(100), required) - The type of notification method (`EMAIL`, `WEBHOOK`, or `PAGERDUTY` ).
* address (string(100), required) - The email/url address to notify.
* period (integer, optional) - The interval in seconds to periodically send the notification. Only can be set as a non zero value for WEBHOOK methods. Allowed periods for Webhooks by default are 0, 60. You can change allow periods for webhooks in the api config. The notification will continue to be sent at the defined interval until the alarm it is associated with changes state.

#### Request Examples
```
POST /v2.0/notification-methods HTTP/1.1
Host: 192.168.10.4:8070
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
* 201 - Created

#### Response Body
Returns a JSON notification method object with the following fields:

* id (string) - ID of notification method
* links ([link])
* name (string) - Name of notification method
* type (string) - Type of notification method
* address (string) - Address of notification method
* period (integer) - Period of notification method

#### Response Examples
```
{
   "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
   "links":[
      {
         "rel":"self",
         "href":"http://192.168.10.4:8070/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
      }
   ],
   "name":"Name of notification method",
   "type":"EMAIL",
   "address":"john.doe@hp.com",
   "period":0
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
* offset (string, optional)
* limit (integer, optional)
* sort_by (string, optional) - Comma separated list of fields to sort by, defaults to 'id'. Fields may be followed by 'asc' or 'desc' to set the direction, ex 'address desc'
Allowed fields for sort_by are: 'id', 'name', 'type', 'address', 'created_at', 'updated_at'

#### Request Body
None.

#### Request Examples
```
GET /v2.0/notification-methods HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of notification method objects with the following fields:

* id (string) - ID of notification method
* links ([link])
* name (string) - Name of notification method
* type (string) - Type of notification method
* address (string) - Address of notification method
* period (integer) - Period of notification method

#### Response Examples
```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/notification-methods"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/notification-methods?offset=c60ec47e-5038-4bf1-9f95-4046c6e9a759"
        }
    ],
    "elements": [
        {
            "id": "35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
            "links": [
                {
                    "rel": "self",
                    "href": "http://192.168.10.4:8070/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
                }
            ],
            "name": "Name of notification method",
            "type": "EMAIL",
            "address": "john.doe@hp.com",
            "period": 0
        },
        {
            "id": "c60ec47e-5038-4bf1-9f95-4046c6e9a759",
            "links": [
                {
                    "rel": "self",
                    "href": "http://192.168.10.4:8070/v2.0/notification-methods/c60ec47e-5038-4bf1-9f95-4046c6e9a759"
                }
            ],
            "name": "Name of notification method",
            "type": "WEBHOOK",
            "address": "http://localhost:3333",
            "period": 1
        }
    ]
}
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
GET http://192.168.10.4:8070/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508
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
* period (integer) - Period of notification method

#### Response Examples
```
{
   "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
   "links":[
      {
         "rel":"self",
         "href":"http://192.168.10.4:8070/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
      }
   ],
   "name":"Name of notification method",
   "type":"EMAIL",
   "address":"john.doe@hp.com",
   "period": 0
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
* type (string(100), required) - The type of notification method (`EMAIL`, `WEBHOOK`, or `PAGERDUTY` ).
* address (string(100), required) - The email/url address to notify.
* period (integer, required) - The interval in seconds to periodically send the notification. Only can be set as a non zero value for WEBHOOK methods. Allowed periods for Webhooks by default are 0, 60. You can change allow periods for webhooks in the api config. The notification will continue to be sent at the defined interval until the alarm it is associated with changes state.

#### Request Examples
````
PUT /v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508 HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{
   "name":"New name of notification method",
   "type":"EMAIL",
   "address":"jane.doe@hp.com",
   "period":0
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
* period (integer) - Period of notification method

#### Response Examples
````
{
   "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
   "links":[
      {
         "rel":"self",
         "href":"http://192.168.10.4:8070/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
      }
   ],
   "name":"New name of notification method",
   "type":"EMAIL",
   "address":"jane.doe@hp.com",
   "period":0
}
````
___

## Patch Notification Method
Patch the specified notification method.

### PATCH /v2.0/notification-methods/{notification_method_id}

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
* Accept (string) - application/json

#### Path Parameters
* notification_method_id (string, required) - ID of the notification method to update.

#### Query Parameters
None.

#### Request Body
* name (string(250), optional) - A descriptive name of the notifcation method.
* type (string(100), optional) - The type of notification method (`EMAIL`, `WEBHOOK`, or `PAGERDUTY` ).
* address (string(100), optional) - The email/url address to notify.
* period (integer, optional) - The interval in seconds to periodically send the notification. Only can be set as a non zero value for WEBHOOK methods. Allowed periods for Webhooks by default are 0, 60. You can change allow periods for webhooks in the api config. The notification will continue to be sent at the defined interval until the alarm it is associated with changes state.

#### Request Examples
````
PATCH /v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508 HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{
   "name":"New name of notification method",
   "type":"EMAIL",
   "address":"jane.doe@hp.com",
   "period":0
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
* period (integer) - Period of notification method

#### Response Examples
````
{
   "id":"35cc6f1c-3a29-49fb-a6fc-d9d97d190508",
   "links":[
      {
         "rel":"self",
         "href":"http://192.168.10.4:8070/v2.0/notification-methods/35cc6f1c-3a29-49fb-a6fc-d9d97d190508"
      }
   ],
   "name":"New name of notification method",
   "type":"EMAIL",
   "address":"jane.doe@hp.com",
   "period":0
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
Host: 192.168.10.4:8070
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

## List supported Notification Method Types
List supported notification method types.

### GET /v2.0/notification-methods/types/

#### Headers
* X-Auth-Token (string, required) - Keystone auth token


#### Query Parameters
None.

#### Request Body
None.

#### Request Examples
````
GET /v2.0/notification-methods/types
Host: 192.168.10.4:8070
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

````

### Response

#### Status Code
* 200 - OK

#### Response Body
Returns a JSON list which has list of notification types supported

* type (string) - List of notification methods


#### Response Examples
````
{
   "links":[
      {
         "rel":"self",
         "href":"http://192.168.10.6:8070/v2.0/notification-methods/types"
      }
   ],
   "elements":[
      {
         "type":"EMAIL"
      },
      {

         "type":"PAGERDUTY"
      },
      {
         "type":"WEBHOOK"
      }
   ]
}
````
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
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{
   "name":"Average CPU percent greater than 10",
   "description":"The average CPU percent is greater than 10",
   "expression":"(avg(cpu.user_perc{hostname=devstack}) > 10)",
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

To create deterministic definition following request should be sent:
```
POST /v2.0/alarm-definitions HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache

{
   "name":"Average CPU percent greater than 10",
   "description":"The average CPU percent is greater than 10",
   "expression":"(avg(cpu.user_perc{hostname=devstack},deterministic) > 10)",
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
Returns a JSON object of alarm definition objects with the following fields:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* deterministic (boolean) - Is the underlying expression deterministic ? **Read-only**, computed from *expression*
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
         "href":"http://192.168.10.4:8070/v2.0/alarm-definitions/b461d659-577b-4d63-9782-a99194d4a472"
      }
   ],
   "name":"Average CPU percent greater than 10",
   "description":"The average CPU percent is greater than 10",
   "expression":"(avg(cpu.user_perc{hostname=devstack}) > 10)",
   "deterministic": false,
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
* dimensions (string, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`, leaving the value empty `key1,key2:value2` will return all values for that key, multiple values for a key may be specified as `key1:value1|value2|...,key2:value4,...`
* severity (string, optional) - One or more severities to filter by, separated with `|`, ex. `severity=LOW|MEDIUM`.
* offset (integer, optional)
* limit (integer, optional)
* sort_by (string, optional) - Comma separated list of fields to sort by, defaults to 'id', 'created_at'. Fields may be followed by 'asc' or 'desc' to set the direction, ex 'severity desc'
Allowed fields for sort_by are: 'id', 'name', 'severity', 'updated_at', 'created_at'

#### Request Body
None.

#### Request Examples
```
GET /v2.0/alarm-definitions?name=CPU percent greater than 10&dimensions=hostname:devstack&state=UNDETERMINED HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of alarm objects with the following fields:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* deterministic (boolean) - Is the underlying expression deterministic ? **Read-only**, computed from *expression*
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
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/alarm-definitions?name=CPU%20percent%20greater%20than%2010&dimensions=hostname:devstack&state=UNDETERMINED"
        },
        {
            "rel": "next",
            "href": "http://localhost:8070/v2.0/alarm-definitions?offset=f9935bcc-9641-4cbf-8224-0993a947ea83&name=CPU%20percent%20greater%20than%2010&dimensions=hostname:devstack&state=UNDETERMINED"
        }
    ],
    "elements": [
        {
            "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
            "links": [
                {
                    "rel": "self",
                    "href": "http://192.168.10.4:8070/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
                }
            ],
            "name": "CPU percent greater than 10",
            "description": "Release the hounds",
            "expression": "(avg(cpu.user_perc{hostname=devstack}) > 10)",
            "deterministic": false,
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
            "match_by": [
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
        },
        {
            "id": "g9323232-6543-4cbf-1234-0993a947ea83",
            "links": [
                {
                    "rel": "self",
                    "href": "http://192.168.10.4:8070/v2.0/alarm-definitions/g9323232-6543-4cbf-1234-0993a947ea83"
                }
            ],
            "name": "Log error count exceeds 1000",
            "description": "Release the cats",
            "expression": "(count(log.error{hostname=devstack}, deterministic) > 1000)",
            "deterministic": true,
            "expression_data": {
                "function": "AVG",
                "metric_name": "log.error",
                "dimensions": {
                    "hostname": "devstack"
                },
                "operator": "GT",
                "threshold": 1000,
                "period": 60,
                "periods": 1
            },
            "match_by": [
                "hostname"
            ],
            "severity": "CRITICAL",
            "actions_enabled": true,
            "alarm_actions": [],
            "ok_actions": [],
            "undetermined_actions": []
        }
    ]
}
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
Returns a JSON alarm definition object with the following fields:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* deterministic (boolean) - Is the underlying expression deterministic ? **Read-only**, computed from *expression*
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
            "href": "http://192.168.10.4:8070/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
        }
    ],
    "name": "CPU percent greater than 10",
    "description": "Release the hounds",
    "expression": "(avg(cpu.user_perc{hostname=devstack}) > 10)",
    "deterministic": false,
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
* description (string(255), required) -  A description of an alarm definition.
* expression (string, required) - An alarm expression.
* match_by ([string], required) - The metric dimensions to use to create unique alarms. This MUST be the same as the existing value for match_by
* severity (string, required) - Severity of an alarm definition. Must be either `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
* alarm_actions ([string(50)], required)
* ok_actions ([string(50)], required)
* undetermined_actions ([string(50)], required)
* actions_enabled (boolean, required) If actions should be enabled (set to true) or ignored (set to false)

See Changing Alarm Definitions for restrictions on changing expression and match_by.

#### Request Examples
```
PUT /v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8070
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
   "severity": "LOW",
   "alarm_actions":[
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "ok_actions":[
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "undetermined_actions":[
      "c60ec47e-5038-4bf1-9f95-4046c6e9a759"
   ],
   "actions_enabled": true
}
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON alarm definition object with the following parameters:

* id (string) - ID of alarm definition.
* links ([link]) - Links to alarm definition.
* name (string) - Name of alarm definition.
* description (string) - Description of alarm definition.
* expression (string) - The alarm definition expression.
* deterministic (boolean) - Is the underlying expression deterministic ? **Read-only**, computed from *expression*
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
            "href": "http://192.168.10.4:8070/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
        }
    ],
    "name": "CPU percent greater than 15",
    "description": "Release the hounds",
    "expression": "(avg(cpu.user_perc{hostname=devstack}) > 15)",
    "deterministic": false,
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
Update selected parameters of the specified alarm definition, and enable/disable its actions.

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
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
Host: 192.168.10.4:8070
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
* deterministic (boolean) - Is the underlying expression deterministic ? **Read-only**, computed from *expression*
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
            "href": "http://192.168.10.4:8070/v2.0/alarm-definitions/f9935bcc-9641-4cbf-8224-0993a947ea83"
        }
    ],
    "name": "CPU percent greater than 15",
    "description": "Release the hounds",
    "expression": "(avg(cpu.user_perc{hostname=devstack}) > 15)",
    "deterministic": false,
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
Host: 192.168.10.4:8070
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 204 - No content

#### Response Body
None.
___

# Alarms
Operations for working with alarms.

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
* metric_dimensions ({string(255): string(255)}, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1, ...`, leaving the value empty `key1,key2:value2` will return all values for that key, multiple values for a key may be specified as `key1:value1|value2|...,key2:value4,...`
* state (string, optional) - State of alarm to filter by, either `OK`, `ALARM` or `UNDETERMINED`.
* severity (string, optional) - One or more severities to filter by, separated with `|`, ex. `severity=LOW|MEDIUM`.
* lifecycle_state (string(50), optional) - Lifecycle state to filter by.
* link (string(512), optional) - Link to filter by.
* state_updated_start_time (string, optional) - The start time in ISO 8601 combined date and time format in UTC.
* offset (integer, optional)
* limit (integer, optional)
* sort_by (string, optional) - Comma separated list of fields to sort by, defaults to 'alarm_id'. Fields may be followed by 'asc' or 'desc' to set the direction, ex 'severity desc'
Allowed fields for sort_by are: 'alarm_id', 'alarm_definition_id', 'alarm_definition_name', 'state', 'severity', 'lifecycle_state', 'link', 'state_updated_timestamp', 'updated_timestamp', 'created_timestamp'

#### Request Body
None.
"
#### Request Examples
```
GET /v2.0/alarms?metric_name=cpu.system_perc&metric_dimensions=hostname:devstack&state=UNDETERMINED HTTP/1.1
Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of alarm objects with the following fields:

* id (string) - ID of alarm.
* links ([link]) - Links to alarm.
* alarm_definition (JSON object) - Summary of alarm definition.
* metrics ({string, string(255): string(255)}) - The metrics associated with the alarm.
* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.
* lifecycle_state (string) - Lifecycle state of alarm.
* link (string) - Link to an external resource related to the alarm.
* state_updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the state was last updated.
* updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when any field was last updated.
* created_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the alarm was created.

#### Response Examples
```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/alarms?metric_name=cpu.system_perc&metric_dimensions=hostname%3Adevstack&state=UNDETERMINED"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/alarms?offset=f9935bcc-9641-4cbf-8224-0993a947ea83&metric_name=cpu.system_perc&metric_dimensions=hostname%3Adevstack&state=UNDETERMINED"
        }
    ],
    "elements": [
        {
            "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
            "links": [
                {
                    "rel": "self",
                    "href": "http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
                },
                {
                    "rel": "state-history",
                    "href": "http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
                }
            ],
            "alarm_definition": {
                "severity": "LOW",
                "id": "b7e5f472-7aa5-4254-a49a-463e749ae817",
                "links": [
                    {
                        "href": "http://192.168.10.4:8070/v2.0/alarm-definitions/b7e5f472-7aa5-4254-a49a-463e749ae817",
                        "rel": "self"
                    }
                ],
                "name": "high cpu and load"
            },
            "metrics": [
                {
                    "name": "cpu.system_perc",
                    "dimensions": {
                        "hostname": "devstack"
                    }
                }
            ],
            "state": "OK",
            "lifecycle_state":"OPEN",
            "link":"http://somesite.com/this-alarm-info",
            "state_updated_timestamp": "2015-03-20T21:04:49.000Z",
            "updated_timestamp":"2015-03-20T21:04:49.000Z",
            "created_timestamp": "2015-03-20T21:03:34.000Z"
        }
    ]
}
```
___

##Get Alarm Counts
Get the number of alarms that match the criteria.

###GET /v2.0/alarms/count

####Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
* Accept (string) - application/json

####Path Parameters
None

####Query Parameters
* alarm_definition_id (string, optional) - Alarm definition ID to filter by.
* metric_name (string(255), optional) - Name of metric to filter by.
* metric_dimensions ({string(255): string(255)}, optional) - Dimensions of metrics to filter by specified as a comma separated array of (key, value) pairs as `key1:value1,key1:value1,...`
* state (string, optional) - State of alarm to filter by, either `OK`, `ALARM` or `UNDETERMINED`.
* severity (string, optional) - One or more severities to filter by, separated with `|`, ex. `severity=LOW|MEDIUM`.
* lifecycle_state (string(50), optional) - Lifecycle state to filter by.
* link (string(512), optional) - Link to filter by.
* state_updated_start_time (string, optional) - The start time in ISO 8601 combined date and time format in UTC.
* offset (integer, optional)
* limit (integer, optional)
* group_by (string, optional) – a list of fields to group the results by as ```field1,field2,…```
The group_by field is limited to `alarm_definition_id`, `name`, `state`, `severity`, `link`, `lifecycle_state`, `metric_name`, `dimension_name`, `dimension_value`.
If any of the fields `metric_name`, `dimension_name`, or `dimension_value` are specified, the sum of the resulting counts is not guaranteed to equal the total number of alarms in the system. Alarms with multiple metrics may be included in multiple counts when grouped by any of these three fields.

####Request Body
None

####Request Examples
```
GET /v2.0/alarms/count?metric_name=cpu.system_perc&metric_dimensions=hostname:devstack&group_by=state,lifecycle_state
HTTP/1.1 Host: 192.168.10.4:8070
Content-Type: application/json
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

###Response
####Status Code
* 200 OK

####Response Body
Returns a JSON object containing the following fields:
* links ([link]) - Links to alarms count resource
* columns ([string]) - List of the column names, in the order they were returned
* counts ([array[]]) - A two dimensional array of the counts returned

####Response Example
```
{
       "links": [
           {
               "rel": "self",
               "href": "http://192.168.10.4:8070/v2.0/alarms/count?metric_name=cpu.system_perc&metric_dimensions=hostname%3Adevstack&group_by=state,lifecycle_state"
           }
       ],
       "columns": ["count", "state", "lifecycle_state"],
       "counts": [
           [124, "ALARM", "ACKNOWLEDGED"],
           [12, "ALARM", "RESOLVED"],
           [235, "OK", "OPEN"],
           [61, "OK", "RESOLVED"],
           [13, "UNDETERMINED", "ACKNOWLEDGED"],
           [1, "UNDETERMINED", "OPEN"],
           [2, "UNDETERMINED", "RESOLVED"],
       ]
   }
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
* offset (timestamp, optional) - The offset in ISO 8601 combined date and time format in UTC.
* limit (integer, optional)

#### Request Body
None.

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of alarm state transition objects with the following fields:

* id - Alarm State Transition ID.
* alarm_id (string) - Alarm ID.
* metrics ({string, string, string(255): string(255)}) - The metrics associated with the alarm state transition.
* old_state (string) - The old state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* new_state (string) - The new state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* reason (string) - The reason for the state transition.
* reason_data (string) - The reason for the state transition as a JSON object.
* timestamp (string) - The time in ISO 8601 combined date and time format in UTC when the state transition occurred.
* sub_alarms ({{string, string, string(255): string(255), string, string, string, string, boolean}, string, [string]) - The sub-alarms stated of when the alarm state transition occurred.

#### Response Examples
```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/alarms/state-history?dimensions=hostname%3Adevstack"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/alarms/state-history?offset=1424451007004&dimensions=hostname%3Adevstack"
        }
    ],
    "elements": [
        {
            "id": "1424451007002",
            "alarm_id": "bc7f388d-3522-47bd-b4ae-41567090ab72",
            "metrics": [
                {
                    "id": null,
                    "name": "cpu.system_perc",
                    "dimensions": {
                        "hostname": "devstack"
                    }
                }
            ],
            "old_state": "UNDETERMINED",
            "new_state": "OK",
            "reason": "The alarm threshold(s) have not been exceeded for the sub-alarms: avg(cpu.system_perc{hostname=devstack}) > 15.0 with the values: [1.5]",
            "reason_data": "{}",
            "timestamp": "2015-02-20T16:50:07.000Z",
            "sub_alarms": [
                {
                    "sub_alarm_expression": {
                        "function": "AVG",
                        "metric_name": "cpu.system_perc",
                        "dimensions": {
                            "hostname": "devstack"
                        },
                        "operator": "GT",
                        "threshold": 15,
                        "period": 60,
                        "periods": 1,
                        "deterministic": false
                    },
                    "sub_alarm_state": "OK",
                    "current_values": [
                        1.5
                    ]
                }
            ]
        },
        {
            "id": "1424451007003",
            "alarm_id": "5ec51b06-193b-49f7-bcf7-b80d11010137",
            "metrics": [
                {
                    "id": null,
                    "name": "mysql.performance.slow_queries",
                    "dimensions": {
                        "component": "mysql",
                        "service": "mysql",
                        "hostname": "devstack"
                    }
                }
            ],
            "old_state": "ALARM",
            "new_state": "OK",
            "reason": "The alarm threshold(s) have not been exceeded for the sub-alarms: avg(mysql.performance.slow_queries) > 10.0 times 3 with the values: [29.23069852941176, 20.146139705882355, 7.536764705882352]",
            "reason_data": "{}",
            "timestamp": "2015-02-20T16:12:07.000Z",
            "sub_alarms": [
                {
                    "sub_alarm_expression": {
                        "function": "AVG",
                        "metric_name": "mysql.performance.slow_queries",
                        "dimensions": {},
                        "operator": "GT",
                        "threshold": 10,
                        "period": 60,
                        "periods": 3,
                        "deterministic": false
                    },
                    "sub_alarm_state": "OK",
                    "current_values": [
                        29.23069852941176,
                        20.146139705882355,
                        7.536764705882352
                    ]
                }
            ]
        },
        {
            "id": "1424451007004",
            "alarm_id": "5ec51b06-193b-49f7-bcf7-b80d11010137",
            "metrics": [
                {
                    "id": null,
                    "name": "mysql.performance.slow_queries",
                    "dimensions": {
                        "component": "mysql",
                        "service": "mysql",
                        "hostname": "devstack"
                    }
                }
            ],
            "old_state": "OK",
            "new_state": "ALARM",
            "reason": "Thresholds were exceeded for the sub-alarms: avg(mysql.performance.slow_queries) > 10.0 times 3 with the values: [36.32720588235294, 29.23069852941176, 20.146139705882355]",
            "reason_data": "{}",
            "timestamp": "2015-02-20T16:11:07.000Z",
            "sub_alarms": [
                {
                    "sub_alarm_expression": {
                        "function": "AVG",
                        "metric_name": "mysql.performance.slow_queries",
                        "dimensions": {},
                        "operator": "GT",
                        "threshold": 10,
                        "period": 60,
                        "periods": 3,
                        "deterministic": false
                    },
                    "sub_alarm_state": "ALARM",
                    "current_values": [
                        36.32720588235294,
                        29.23069852941176,
                        20.146139705882355
                    ]
                }
            ]
        }
    ]
}

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
* alarm_definition (JSON object) - Summary of alarm definition.
* metrics ({string, string(255): string(255)}) - The metrics associated with the alarm.
* state (string) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.
* lifecycle_state (string) - Lifecycle state of alarm.
* link (string) - Link to an external resource related to the alarm.
* state_updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the state was last updated.
* updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when any field was last updated.
* created_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the alarm was created.

#### Response Examples
```
{
   "id":"f9935bcc-9641-4cbf-8224-0993a947ea83",
   "links":[
      {
         "rel":"self",
         "href":"http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
      },
      {
         "rel":"state-history",
         "href":"http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
      }
   ],
   "alarm_definition":
   {
      "id":"ad837fca-5564-4cbf-523-0117f7dac6ad",
      "name":"Average CPU percent greater than 10",
      "severity":"LOW",
      "links":[
         {
            "rel":"self",
            "href":"http://192.168.10.4:8070/v2.0/alarm-definitions/ad837fca-5564-4cbf-523-0117f7dac6ad
         }
      ]
   },
   "metrics":[{
      "name":"cpu.system_perc",
      "dimensions":{
         "hostname":"devstack"
      }
   }],
   "state":"OK",
   "lifecycle_state":"OPEN",
   "link":"http://somesite.com/this-alarm-info",
   "state_updated_timestamp": "2015-03-20T21:04:49.000Z",
   "updated_timestamp": "2015-03-20T21:04:49.000Z",
   "created_timestamp": "2015-03-20T21:03:34.000Z"
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
* lifecycle_state (string(50)) - Lifecycle state of alarm.
* link (string(512)) - Link to an external resource related to the alarm.

#### Request Examples
```
PUT /v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8070
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache

{
  "state":"OK",
  "lifecycle_state":"OPEN",
  "link":"http://pagerduty.com/"
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
* lifecycle_state (string) - Lifecycle state of alarm.
* link (string) - Link to an external resource related to the alarm.
* state_updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the state was last updated.
* updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when any field was last updated.
* created_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the alarm was created.

#### Response Examples
```
{
  "id":"f9935bcc-9641-4cbf-8224-0993a947ea83",
  "links":[
     {
        "rel":"self",
        "href":"http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
     },
     {
        "rel":"state-history",
        "href":"http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
     }
  ],
  "alarm_definition_id":"ad837fca-5564-4cbf-523-0117f7dac6ad",
  "metrics":[{
     "name":"cpu.system_perc",
     "dimensions":{
        "hostname":"devstack"
     }
  }],
  "state":"OK",
  "lifecycle_state":"OPEN",
  "link":"http://somesite.com/this-alarm-info",
  "state_updated_timestamp": "2015-03-20T21:04:49.000Z",
  "updated_timestamp": "2015-03-20T21:04:49.000Z",
  "created_timestamp": "2015-03-20T21:03:34.000Z"
}
```
___

## Patch Alarm
### PATCH /v2.0/alarms/{alarm_id}
Update select parameters of the specified alarm, set the alarm state and enable/disable it. To set lifecycle_state or link field to `null`, use an UPDATE Alarm request with `"lifecycle_state":null` and/or `"link":null`.

#### Headers
* X-Auth-Token (string, required) - Keystone auth token
* Content-Type (string, required) - application/json
* Accept (string) - application/json

#### Path Parameters
* alarm_id (string, required) - Alarm ID

#### Query Parameters
None.

#### Request Body
Consists of an alarm with the following mutable properties:

* state (string, optional) - State of alarm, either `OK`, `ALARM` or `UNDETERMINED`.
* lifecycle_state (string(50), optional) - Lifecycle state of alarm.
* link (string(512), optional) - Link to an external resource related to the alarm.

#### Request Examples
```
PATCH /v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83 HTTP/1.1
Host: 192.168.10.4:8070
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Content-Type: application/json
Cache-Control: no-cache

{
  "lifecycle_state":"OPEN",
  "link":"http://somesite.com/this-alarm-info"
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
* lifecycle_state (string) - Lifecycle state of the alarm.
* link (string) - Link to an external resource related to the alarm.
* state_updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the state was last updated.
* updated_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when any field was last updated.
* created_timestamp - Timestamp in ISO 8601 combined date and time format in UTC when the alarm was created.

#### Response Examples
```
{
    "id": "f9935bcc-9641-4cbf-8224-0993a947ea83",
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83"
        },
        {
            "rel": "state-history",
            "href": "http://192.168.10.4:8070/v2.0/alarms/f9935bcc-9641-4cbf-8224-0993a947ea83/state-history"
        }
    ],
    "alarm_definition_id": "ad837fca-5564-4cbf-523-0117f7dac6ad",
    "metrics": [
        {
            "name": "cpu.system_perc",
            "dimensions": {
                "hostname": "devstack"
            }
        }
    ],
    "state": "OK"
    "lifecycle_state":"OPEN",
    "link":"http://somesite.com/this-alarm-info",
    "state_updated_timestamp": "2015-03-20T21:04:49.000Z",
    "updated_timestamp": "2015-03-20T21:04:49.000Z",
    "created_timestamp": "2015-03-20T21:03:34.000Z"
}

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
Host: 192.168.10.4:8070
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
* offset (timestamp, optional) - The offset in ISO 8601 combined date and time format in UTC.
* limit (integer, optional)

#### Request Body
None.

#### Request Data
```
GET /v2.0/alarms/37d1ddf0-d7e3-4fc0-979b-25ac3779d9e0/state-history HTTP/1.1
Host: 192.168.10.4:8070
X-Auth-Token: 2b8882ba2ec44295bf300aecb2caa4f7
Cache-Control: no-cache
```

### Response
#### Status Code
* 200 - OK

#### Response Body
Returns a JSON object with a 'links' array of links and an 'elements' array of alarm state transition objects with the following fields:

* id - Alarm State Transition ID.
* alarm_id (string) - Alarm ID.
* metrics ({string, string, string(255): string(255)}) - The metrics associated with the alarm state transition.
* old_state (string) - The old state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* new_state (string) - The new state of the alarm. Either `OK`, `ALARM` or `UNDETERMINED`.
* reason (string) - The reason for the state transition.
* reason_data (string) - The reason for the state transition as a JSON object.
* timestamp (string) - The time in ISO 8601 combined date and time format in UTC when the state transition occurred.
* sub_alarms ({{string, string, string(255): string(255), string, string, string, string, boolean}, string, [string]) - The sub-alarms stated of when the alarm state transition occurred.

#### Response Examples
```
{
    "links": [
        {
            "rel": "self",
            "href": "http://192.168.10.4:8070/v2.0/alarms/37d1ddf0-d7e3-4fc0-979b-25ac3779d9e0/state-history"
        },
        {
            "rel": "next",
            "href": "http://192.168.10.4:8070/v2.0/alarms/37d1ddf0-d7e3-4fc0-979b-25ac3779d9e0/state-history?offset=1424452147006"
        }
    ],
    "elements": [
        {
            "id": "1424452147003",
            "alarm_id": "37d1ddf0-d7e3-4fc0-979b-25ac3779d9e0",
            "metrics": [
                {
                    "id": null,
                    "name": "cpu.idle_perc",
                    "dimensions": {
                        "hostname": "devstack"
                    }
                }
            ],
            "old_state": "OK",
            "new_state": "ALARM",
            "reason": "Thresholds were exceeded for the sub-alarms: avg(cpu.idle_perc) < 10.0 times 3 with the values: [0.0, 0.0, 0.0]",
            "reason_data": "{}",
            "timestamp": "2015-02-20T17:09:07.000Z",
            "sub_alarms": [
                {
                    "sub_alarm_expression": {
                        "function": "AVG",
                        "metric_name": "cpu.idle_perc",
                        "dimensions": {},
                        "operator": "LT",
                        "threshold": 10,
                        "period": 60,
                        "periods": 3,
                        "deterministic": false
                    },
                    "sub_alarm_state": "ALARM",
                    "current_values": [
                        0,
                        0,
                        0
                    ]
                }
            ]
        },
        {
            "id": "1424452147004",
            "alarm_id": "37d1ddf0-d7e3-4fc0-979b-25ac3779d9e0",
            "metrics": [
                {
                    "id": null,
                    "name": "cpu.idle_perc",
                    "dimensions": {
                        "hostname": "devstack"
                    }
                }
            ],
            "old_state": "ALARM",
            "new_state": "OK",
            "reason": "The alarm threshold(s) have not been exceeded for the sub-alarms: avg(cpu.idle_perc) < 10.0 times 3 with the values: [0.0, 0.0, 72.475]",
            "reason_data": "{}",
            "timestamp": "2015-02-20T17:02:07.000Z",
            "sub_alarms": [
                {
                    "sub_alarm_expression": {
                        "function": "AVG",
                        "metric_name": "cpu.idle_perc",
                        "dimensions": {},
                        "operator": "LT",
                        "threshold": 10,
                        "period": 60,
                        "periods": 3,
                        "deterministic": false
                    },
                    "sub_alarm_state": "OK",
                    "current_values": [
                        0,
                        0,
                        72.475
                    ]
                }
            ]
        },
        {
            "id": "1424452147005",
            "alarm_id": "37d1ddf0-d7e3-4fc0-979b-25ac3779d9e0",
            "metrics": [
                {
                    "id": null,
                    "name": "cpu.idle_perc",
                    "dimensions": {
                        "hostname": "devstack"
                    }
                }
            ],
            "old_state": "OK",
            "new_state": "ALARM",
            "reason": "Thresholds were exceeded for the sub-alarms: avg(cpu.idle_perc) < 10.0 times 3 with the values: [0.0, 0.0, 0.0]",
            "reason_data": "{}",
            "timestamp": "2015-02-20T16:56:07.000Z",
            "sub_alarms": [
                {
                    "sub_alarm_expression": {
                        "function": "AVG",
                        "metric_name": "cpu.idle_perc",
                        "dimensions": {},
                        "operator": "LT",
                        "threshold": 10,
                        "period": 60,
                        "periods": 3,
                        "deterministic": false
                    },
                    "sub_alarm_state": "ALARM",
                    "current_values": [
                        0,
                        0,
                        0
                    ]
                }
            ]
        },
        {
            "id": "1424452147006",
            "alarm_id": "37d1ddf0-d7e3-4fc0-979b-25ac3779d9e0",
            "metrics": [
                {
                    "id": null,
                    "name": "cpu.idle_perc",
                    "dimensions": {
                        "hostname": "devstack"
                    }
                }
            ],
            "old_state": "UNDETERMINED",
            "new_state": "OK",
            "reason": "The alarm threshold(s) have not been exceeded",
            "reason_data": "{}",
            "timestamp": "2015-02-20T15:02:30.000Z",
            "sub_alarms": []
        }
    ]
}

```
___

# License
(C) Copyright 2014-2016 Hewlett Packard Enterprise Development LP

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
