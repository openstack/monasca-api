---
layout: page
permalink: /api/monitoring/
title: HP Cloud Monitoring API
description: "HP Cloud Monitoring API Specifications"
keywords: "monitoring, maas"
product: monitoring

---

# HP Cloud Monitoring API Specifications

**Date:**  5th August, 2013

**Document Version:** 1.4.1
 
## 1. Overview # {#Section1_}

This document describes the HP Cloud Monitoring API, which allows you to monitor resources in HP's Cloud.

### 1.1 API Maturity Level ## {#Section1_1}

**Maturity Level**: Private Beta 

**Service activation required, please refer to the Monitoring service information on hpcloud.com for instructions on how to request activation for your account.**

**Version API Status**: BETA

### 1.2 Document Revision History ## {#Section1_2}

|Doc. Version|Date|Description|
|:---------------|:-------|:--------------|
|0.1|01/17/2013|Initial draft|
|0.2|02/22/2013|Compute only release|
|0.3|02/28/2013|Added Volume metrics and cleanup|
|1.0|03/04/2013|Document 1.0 release|
|1.1|03/09/2013|API Keys desc and cleanup|
|1.2|04/25/2013|User defined metrics and cleanup|
|1.3|05/01/2013|Advanced alarm expressions and cleanup|
|1.3.1|05/06/2013|Cleanup|
|1.3.2|05/09/2013|Modified dimensions handling for user defined metrics and cleanup|
|1.3.3|05/13/2013|Namespace and dimension changes|
|1.3.4|05/22/2013|Timestamp and Subject optional for user defined metrics|
|1.3.5|05/24/2013|Zero value metrics change|
|1.4|07/11/2013|Rate based metrics|
|1.4.1|07/15/2013|Enhanced volume metrics|

## 2. Architecture View # {#Section2_}

Monitoring as a Service provides APIs for managing metric consumption endpoints, metric subscriptions, alarms, contact methods, and publishing user defined metrics.

### 2.1 Overview ## {#ArchitectureOverview}

The Monitoring API provides a RESTful JSON interface for interacting with Monitoring as a Service and managing monitoring related resources.

The API supports a number of operations. These include:

+ [Version Operations](#ServiceVersionOps) - Provides information about the supported Monitoring API versions.

+ [Version Operations Details](#ServiceDetailsVersion)

+ [Endpoint Operations](#ServiceEndpointOps) - The endpoint resource represents an endpoint from which metrics can be consumed.

+ [Endpoint Operations Details](#ServiceDetailsEndpoint)

+ [Subscription Operations](#ServiceSubscriptionOps) - The subscription resource represents a subscription to consume metrics.

+ [Subscription Operations Details](#ServiceDetailsSubscription)

+ [Notification Operations](#ServiceNotificationOps) - The notification method resource represents a method through which notifications can be sent.

+ [Notification Operations Details](#ServiceDetailsNotification)

+ [Alarm Operations](#ServiceAlarmOps) - The alarm resource identifies a particular metric scoped by namespace, type and dimensions, which should trigger a set of actions when the value of the metric exceeds a threshold.

+ [Alarm Operations Details](#ServiceDetailsAlarm)

+ [Metric Operations](#ServiceMetricsOps) - The metric resource allows the insertion of user defined metrics.

+ [Metrics Operations Details](#ServiceDetailsMetrics)

#### 2.1.1 High Level and Usage ### {#HighLevel}

There are 5 major operations (besides Version information):

+ Endpoints specify the connection for consuming metric data to the RabbitMQ (AMQP) message queue. (Only one Endpoint can be created at a time per tenant. At least one Dimension must be specified.)
+ Subscriptions specify what monitoring data is to be streamed. (You must create an Endpoint to use with the subscription.)
+ Notifications specify the method(s) in which a user is contacted by alarms.
+ Alarms specify user defined exceptional conditions that the user feels the need to be notified about. (You must create a Notification method to use with the Alarm. At least one Dimension must be specified.)
+ Metrics specify user defined metrics for collection through a subscription and for alarming.

With those 5 operations, there are 3 different major activities, besides activating your [account](#AccountActivation) in section 2.1.1.1. Refer to [Metrics](#ServiceDetailsCreateMetric) for user defined metrics. [Alarm Setup](#AlarmSetup) is information about setting up an alarm to notify on a logged metric. [Subscription Setup](#SubscriptionSetup)) is information about setting up a subscription feed to locally log monitoring data.

##### 2.1.1.1 Activating Your Account Example #### {#AccountActivation}

[Account Creation Details](#Accounts)

Before creating Alarms or Subscriptions, you must first get a valid auth token. Go to the above link, verify that you have access with the API Keys page, and then use the Curl Example. Replace both the tenantName and username values with your account tenentName and login username. Replace password with your account password.

Save the returned information. The access:token:id value is required to access the API.

##### 2.1.1.2 Setting Up an Alarm Example #### {#AlarmSetup}

Alarms give direct notification on if a measured value passes a user defined threshold.

[Notification Creation Details](#ServiceDetailsCreateNotification)

Alarms need a way in which to contact you, so we need to create a notification.

From the above link, use the Curl Example, changing the X-Auth-Token value with the access:token:id value you retrieved when activating your account. Change "name" to your descriptive name, "type" with how you are to be accessed (this is a strict enumeration value, use only the values given in the Data Parameters section), and "address" with your email address or phone number. Save the returned json output.
	    
[Alarm Creation Details](#ServiceDetailsCreateAlarm)

From the above link, use the Curl Example, changing the X-Auth-Token value with the access:token:id value you retrieved when activating your account. Changes: "name" to a description of the alarm. "expression" to a user defined expression for when an alarm should trigger, as in [Alarm Expressions](#AlarmExpressions). "alarm_actions" is a list of id values from creating notifications and will be the notifications activated if the alarm expression is true. The alarm should be active immediately after.

##### 2.1.1.3 Setting Up a Subscription Example #### {#SubscriptionSetup}

Subscriptions give a continuous feed of monitoring metrics data.

[Endpoint Creation Details](#ServiceDetailsCreateEndpoint)

Subscriptions need a way to connect you to the RabbitMQ (AMQP) queue, so we need to create an endpoint.

From the above link, use the Curl Example, changing the X-Auth-Token value with the access:token:id value you retrieved when activating your account. Save the returned json output. "uri", "amqp_username", "amqp_password", and "amqp_queue" are the required values for accessing the RabbitMQ (AMQP) queue. (How to setup the queue connection is beyond the scope of this document. There are tutorials at [RabbitMQ](http://www.rabbitmq.com).)

[Subscription Creation Details](#ServiceDetailsCreateSubscription)

From the above link, use the Curl Example, changing the X-Auth-Token value with the access:token:id value you retrieved when activating your account. Change "endpoint_id" to the id value returned from the Endpoint call, "instance_id" with your hpcs.compute instance id, "az" with your availability zone number, and "instance_uuid" with the UUID for the instance. The data feed should start soon after.

The metric json format (HP Cloud Metrics JSON) received is the same format used for creating user defined metrics in 4.4.6.1 [Create a New Metric Message](#ServiceDetailsCreateMetric).

#### 2.1.2 Namespaces, Dimensions, and Metrics ### {#Metrics}

The Monitoring API makes use of several metric related pre-defined constants throughout.

##### 2.1.2.1 Namespaces #### {#Namespaces}

Defines the high level logical partition to monitor.  This restricts what metrics will be used.

Namespaces may only use the characters from: a-z A-Z 0-9 . \_.

*Note: All namespaces starting with hpcs. are reseved by HP and cannot be used for user defined namespaces. Namespaces of compute and volume are also currently resevered and return an error.*

*Supported Namespaces*

+ hpcs.compute
+ hpcs.volume
+ user defined (See [Metrics Operations Details](#ServiceDetailsMetrics)

##### 2.1.2.2 Dimensions #### {#Dimensions}

Places restrictions on the namespace to further narrow what is monitored.

Dimensions may only use the characters from: a-z A-Z 0-9 . \_. The dimension value may only use the characters from: a-z A-Z 0-9 . - \_.

*Note: Each dimension type can only be used once per call.  All 3 types are required to be used for hpcs.compute and hpcs.volume namespaces. For hpcs.volume, the dimensions are for the instance the volume is attached to. User defined namespaces do not require dimensions.*

*Required hpcs.compute Dimensions*

+ instance_id (compute instance id)
+ az (avaliability zone)
+ instance_uuid (nova instance unique id)

*Required hpcs.volume Dimensions*

+ instance_id (compute instance id)
+ az (avaliability zone)
+ instance_uuid (nova instance unique id)
+ disk (disk read/written from/to, normally vda)

##### 2.1.2.3 Metrics #### {#Metrics}

Each namespace represents a service that has its own metric types. These are described below:

Metric type names and subjects may only use the characters from: a-z A-Z 0-9 . \_.

The Subject field gives a description of where exactly the metric came from based on the supplied dimensions.

Delta measurement types give a count from the last measurement point. Measurement points are normally at either 60 second or 300 second intervals, depending upon the measurement rate you have contracted for.

*hpcs.compute Metric Types*

Gauge and Delta types:

|Metric Type|Measurement Type|Unit|Subject|Description|
|:----------|:---|:---|:------|:----------|
|cpu_total_utilization|Gauge|Percent|(no subject)|Total percentage of CPU used for all cores|
|disk_read_ops|Delta|Operations|Disk Name|Delta of read requests from a disk|
|disk_write_ops|Delta|Operations|Disk Name|Delta of write requests from a disk|
|disk_read_bytes|Delta|Bytes|Disk Name|Delta of bytes read from a disk|
|disk_write_bytes|Delta|Bytes|Disk Name|Delta of bytes written to a disk|
|net_in_bytes|Delta|Bytes|Interface Name|Delta of receive bytes on a network interface |
|net_out_bytes|Delta|Bytes|Interface Name|Delta of transfer bytes on a network interface |
|net_in_dropped|Delta|Packets|Interface Name|Delta of receive packets dropped on a network interface|
|net_out_dropped|Delta|Packets|Interface Name|Delta of transfer packets dropped on a network interface|
|net_in_packets|Delta|Packets|Interface Name|Delta of receive packets on a network interface|
|net_out_packets|Delta|Packets|Interface Name|Delta of transfer packets on a network interface |
|net_in_errors|Delta|Errors|Interface Name|Delta of receive packet errors on a network interface|
|net_out_errors|Delta|Errors|Interface Name|Delta of transfer packet errors on a network interface|

Counter types:

|Metric Type|Measurement Type|Unit|Subject|Description|
|:----------|:---|:---|:------|:----------|
|cpu_total_time|Counter|Nanoseconds|(no subject)|Total CPU time used in nanoseconds|
|disk_read_ops_count|Counter|Operations|Disk Name|Number of read requests from a disk|
|disk_write_ops_count|Counter|Operations|Disk Name|Number of write requests from a disk|
|disk_read_bytes_count|Counter|Bytes|Disk Name|Number of bytes read from a disk|
|disk_write_bytes_count|Counter|Bytes|Disk Name|Number of bytes written to a disk|
|net_in_bytes_count|Counter|Bytes|Interface Name|Number of receive bytes on a network interface |
|net_out_bytes_count|Counter|Bytes|Interface Name|Number of transfer bytes on a network interface |
|net_in_dropped_count|Counter|Packets|Interface Name|Number of receive packets dropped on a network interface|
|net_out_dropped_count|Counter|Packets|Interface Name|Number of transfer packets dropped on a network interface|
|net_in_packets_count|Counter|Packets|Interface Name|Number of receive packets on a network interface|
|net_out_packets_count|Counter|Packets|Interface Name|Number of transfer packets on a network interface |
|net_in_errors_count|Counter|Errors|Interface Name|Number of receive packet errors on a network interface|
|net_out_errors_count|Counter|Errors|Interface Name|Number of transfer packet errors on a network interface|

*hpcs.volume Metric Types*

|Metric Type|Measurement Type|Unit|Description|
|:----------|:---|:---|:----------|
|volume_read_ops|Delta|Operations|Read operations for volume attached to an instance|
|volume_write_ops|Delta|Operations|Write operations for volume attached to an instance|
|volume_read_bytes|Delta|Bytes|Read bytes for volume attached to an instance|
|volume_write_bytes|Delta|Bytes|Write bytes for volume attached to an instance|
|volume_read_time|Delta|Seconds|Read time spent for volume attached to an instance|
|volume_write_time|Delta|Seconds|Write time spent for volume attached to an instance|
|volume_idle_time|Delta|Seconds|Idle time spent for volume attached to an instance|

*User Defined Metrics*

See [Metrics Operations Details](#ServiceDetailsMetrics).

### 2.2 Alarm Expressions ## {#AlarmExpressions}

The alarm expression syntax allows the creation of simple to complex alarms to handle a wide variety of needs. Alarm expressions are evaluated every 60 seconds.

Go to [Alarm](#ServiceDetailsAlarm) to see the Rest interface.

An alarm expression is a boolean equation which if it evaluates to true with the incoming metrics, will then trigger a notification to be sent. 

Syntax:

At the highest level, you have an expression, which is made up of one or more subexpressions, joined by boolean logic. Parenthesis can be used for separators. In a BNF style format:

	expression
		: subexpression
		| '(' expression ')'
		| expression logical_operator expression

The logical_operators are: 'and' (also &&), 'or' (also ||).

Each subexpression is made up of several parts with a couple of options:

	subexpression
		: metric relational_operator threshold_value
		| function '(' metric ',' period ')' relational_operator threshold_value ('times' periods)?
		
The relational_operators are: 'lt' (also <), 'gt' (also >), 'lte' (also <=), 'gte' (also >=).

Threshold values are always in the same units as the metric that they are being compared to.

The first subexpression shows a direct comparison of a metric to a threshold_value, done every 60 seconds.

	Example:
	
	hpcs.compute:cpu_total_utilization:{instance_id=123} > 90

The second subexpression shows a function operation done on all collected metrics within a specific period and compared to a threshold_value, then triggered if it happens periods number of times.

	Example:
	
	avg(hpcs.compute:cpu_total_utilization:{instance_id=123},60) > 90 times 3
	
Note that period is the number of seconds for the measurement to be done on. They can only be in a multiple of 60. Periods is how many times in a row that this expression must be true before triggering the alarm. Both period and periods are optional and default to 60 and 1 respectively.

Functions work on all metric measurements during the period time frame.

+ min (returns the minimum of all the values)
+ max (returns the maximum of all the values)
+ sum (returns the sum of all the values)
+ count (returns the number of metric observations)
+ avg (returns the average of all the values)

The metric is a complex identifier that says the namespace, metric type, an optional subject, and the dimensions.

	metric
		: namespace ':' metric_type (':' subject)? (':' '{' dimensions '}')?

	Examples:
	
	hpcs.compute:disk_read_ops:vda:{instance_id=123,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66}
	
	hpcs.compute:cpu_total_utilization:{instance_id=123}
	
	userspace:usermetric
	
The first example shows a namespace of 'hpcs.compute', see [Namespaces](#Namespaces). The metric_type is 'disk_read_ops', see [Metrics](#Metrics). The optional subject name is 'vda'. The dimensions are 'instance_id=123', 'az=2' and 'instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66'. This fully describes where the metric comes from, what namespace it belongs to, and what metric type was measured.

The second example shows leaving out 2 of the dimensions and the subject.

The third, very simple, example is for targeting a user defined namespace and metric type. It leaves off the subject and dimensions (which are optional for user defined metrics).
	
Larger example:

	( avg(hpcs.compute:cpu_total_utilization:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66}) > 90 ) or ( avg(hpcs.compute:disk_read_ops:vda:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66},120) > 1000 times 3 )
	
This example shows 2 subexpressions. If the cpu_total_utilization is on average greater than 90 for a 60 second period for that instance then the alarm will trigger. If not that, then if the disk_read_ops for that same instance is on average greater than to 1000 for a 120 second period for 3 periods in a row, then the alarm will also trigger.

User defined metric example:

	count(userspace:usermetric:usersubject:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66},600) > 7 times 5

This example shows 1 subexpression. It does a count of how many times the user publishes a user defined metric in a 600 second period. If more than 7 occur in that period and this happens 5 times in a row, then the alarm will trigger.

### 2.3 Faults ## {#Faults}

When an fault occurs at request time, the system will return an HTTP error response code denoting the type of fault. The system will also return additional information about the fault in the body of the response.

*Fault Response*

JSON

	{  
		"fault_element": {
			"code": HTTP error code (integer),
			"message": "Fault information...",
			"details": "Fault Details…",
			"internal_code": "Internal error log code"
		}
	}

The error code is returned in the body of the response for convenience. The message section returns a human-readable message that is appropriate for display to the end user. The details section is optional and may contain extra information. The internal_code section is optional monitoring logging information to further identify the cause of the fault.

The root element of the fault (the fault_element value) may change depending on the type of fault. The following is a list of possible elements along with their associated HTTP error codes.

|Fault Element|HTTP Error Code|
|:------------|:--------------|
|server_error|500|
|bad_request|400|
|unauthorized|401|
|forbidden|403|
|not_found|404|
|conflict|409|
|unprocessable_entity|422|

## 3. Account-level View # {#Section3_}

Requests to the Monitoring API are required to present a valid token which must be obtained from HP Cloud Identity Service prior to making a Monitoring API request.

More information about the HP Cloud Identity Service:

[Introduction to HP Cloud Identity Service](https://community.hpcloud.com/article/identity-service-introduction)

[HP Cloud Identity Service Overview](https://community.hpcloud.com/knowledge-base-categories/cloud-identity-service)

### 3.1 Accounts ## {#Accounts}

**Verifying Access**

Log into your cloud account from the Console page. Select API Keys in the upper right. From this page you can retrieve your Project/Tenant ID and Project. Lower down on the page it will show "Monitoring" along with the region and base monitoring URI. If the monitoring section does not exist, then contact help to activate your access to monitoring before continuing.

**Requesting a Token**

***Request URI***

	curl -i -X POST \
	https://region-a.geo-1.identity.hpcloudsvc.com:35357/v2.0/tokens \
	-H "Content-Type: application/json" -H "User-Agent: python-novaclient"

***Request Body***

	{"auth": {"tenantName": "tenant@domain.com", "passwordCredentials": {"username": "tenant@domain.com", "password": "changeit"}}}

***Response Body***

	{
		"access": {
	  		"token": {
	    		"expires": "2012-04-05T04:28:29.405Z",
	    		"id": "HPAuth_4f7c6456e4b01a25ab011e74",
	    		"tenant": {
					"id": "123456789",
					"name": "tenant@domain.com"
				}
			},
			"user": {
			"id": "987654321",
			"name": "tenant@hp.com",
			"roles": [
			{
				...
			},
		},
		"serviceCatalog": [
			...
		{
			"name": "MaaS",
			"type": "maas",
			"endpoints": [
			{
				"tenantId": "12345678901234",
				"publicURL": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/",
				"region": "region-a.geo-1",
				"versionId": "1.0",
			}
		]
		}
	]
	}}

***Accessing Monitoring***

The endpoint for accessing the Monitoring API can be obtained from the service catalog returned in your Keystone authentication request.

	https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/

A request can then be made against resources at that endpoint by supplying the access/token/id from your Keystone authentication request as in your Monitoring API request header as an X-Auth-Token:

**Sample Request**

	> GET /v1.0/ HTTP/1.1
	> Host: 15.185.167.67:8779
	> Accept: application/json
	> Content-Type: application/json
	> X-Auth-Token: HPAuth_4f7c6456e4b01a25ab011e74

**Curl Example**

*Replace tenantName with your Project value from the API Keys web page. Replace username and password with your credentials for logging in to HP Cloud.*

	$ curl -i -X POST \
	  https://region-a.geo-1.identity.hpcloudsvc.com:35357/v2.0/tokens \
	  -H "Content-Type: application/json" -H "User-Agent: python-novaclient" \
	  -d '{"auth": {"tenantName": "tenant@domain.com", "passwordCredentials": {"username": "tenant@domain.com", "password": "changeit"}}}'

### 3.2 Regions and Availability Zones ## {#AccountsRegions}

**Region(s)**: region-a

**Future Expansion**: region-b

## 4. REST API Specifications # {#Section4_}
The HP Cloud Monitoring API is implemented using a RESTful web service interface. All requests to authenticate and operate against the Monitoring API should be performed using SSL over HTTP (HTTPS) on TCP port 443.

### 4.1 Service API Operations ## {#Service}

**Host**: https://region-a.geo-1.monitoring.hpcloudsvc.com

**BaseUri**: {Host}/v1.0

### Version Operations ## {#ServiceVersionOps}

| Resource | Operation                                          | HTTP Method | Path                            | JSON/XML Support? | Privilege Level |
| :------- | :------------------------------------------------- | :---------- | :------------------------------ | :---------------- | :-------------- |
| Version | [List all versions](#ServiceDetailsListVersion) | GET | {Host}/ | Y/N ||
| Version | [Get a specific version](#ServiceDetailsSpecificVersion) | GET | {Host}/*version_id* | Y/N ||

### Endpoint Operations ## {#ServiceEndpointOps}

| Resource | Operation                                          | HTTP Method | Path                            | JSON/XML Support? | Privilege Level |
| :------- | :------------------------------------------------- | :---------- | :------------------------------ | :---------------- | :-------------- |
| Endpoint | [Create a new endpoint](#ServiceDetailsCreateEndpoint) | POST | {BaseUri}/endpoints | Y/N ||
| Endpoint | [List all endpoints](#ServiceDetailsListEndpoint) | GET | {BaseUri}/endpoints | Y/N ||
| Endpoint | [Get a specific endpoint](#ServiceDetailsSpecificEndpoint) | GET | {BaseUri}/endpoints/*endpoint_id* | Y/N ||
| Endpoint | [Delete a specific endpoint](#ServiceDetailsDeleteEndpoint) | DELETE | {BaseUri}/endpoints/*endpoint_id* | Y/N ||
| Endpoint | [Reset the password for a specific endpoint](#ServiceDetailsResetPasswordEndpoint) | POST | {BaseUri}/endpoints/*endpoint_id*/reset-password | Y/N ||

### Subscription Operations ## {#ServiceSubscriptionOps}

| Resource | Operation                                          | HTTP Method | Path                            | JSON/XML Support? | Privilege Level |
| :------- | :------------------------------------------------- | :---------- | :------------------------------ | :---------------- | :-------------- |
| Subscription | [Create a new subscription](#ServiceDetailsCreateSubscription) | POST | {BaseUri}/subscriptions | Y/N ||
| Subscription | [List all subscriptions](#ServiceDetailsListSubscription) | GET | {BaseUri}/subscriptions | Y/N ||
| Subscription | [Get a specific subscription](#ServiceDetailsSpecificSubscription) | GET | {BaseUri}/subscriptions/*subscription_id* | Y/N ||
| Subscription | [Delete a specific subscription](#ServiceDetailsDeleteSubscription) | DELETE | {BaseUri}/subscriptions/*subscription_id* | Y/N ||

### Notification Method Operations ## {#ServiceNotificationOps}

| Resource | Operation                                          | HTTP Method | Path                            | JSON/XML Support? | Privilege Level |
| :------- | :------------------------------------------------- | :---------- | :------------------------------ | :---------------- | :-------------- |
| Notification Method | [Create a new notification method](#ServiceDetailsCreateNotification) | POST | {BaseUri}/notification-methods | Y/N ||
| Notification Method | [List all notification methods](#ServiceDetailsListNotification) | GET | {BaseUri}/notification-methods | Y/N ||
| Notification Method | [Get a specific notification method](#ServiceDetailsSpecificNotification) | GET | {BaseUri}/notification-methods/*notification_method_id* | Y/N ||
| Notification Method | [Delete a specific notification method](#ServiceDetailsDeleteNotification) | DELETE | {BaseUri}/notification-methods/*notification_method_id* | Y/N ||

### Alarm Operations ## {#ServiceAlarmOps}

| Resource | Operation                                          | HTTP Method | Path                            | JSON/XML Support? | Privilege Level |
| :------- | :------------------------------------------------- | :---------- | :------------------------------ | :---------------- | :-------------- |
| Alarm | [Create a new alarm](#ServiceDetailsCreateAlarm) | POST | {BaseUri}/alarms | Y/N ||
| Alarm | [List all alarms](#ServiceDetailsListAlarm) | GET | {BaseUri}/alarms | Y/N ||
| Alarm | [Get a specific alarm](#ServiceDetailsSpecificAlarm) | GET | {BaseUri}/alarms/*alarm_id* | Y/N ||
| Alarm | [Delete a specific alarm](#ServiceDetailsDeleteAlarm) | DELETE | {BaseUri}/alarms/*alarm_id* | Y/N ||  

### Metrics Operations ## {#ServiceMetricsOps}

| Resource | Operation                                          | HTTP Method | Path                            | JSON/XML Support? | Privilege Level |
| :------- | :------------------------------------------------- | :---------- | :------------------------------ | :---------------- | :-------------- |
| Metrics | [Create a new user defined metric](#ServiceDetailsCreateMetric) | POST | {BaseUri}/metrics | Y/N ||

### 4.2 Common Requests ## {#CommonRequests}

#### 4.2.1 Common Request Headers ### {#CommonRequestHeaders}

*Http standard request headers*

**Accept** - Internet media types that are acceptable in the response. HP Cloud Monitoring supports the media types application/xml and application/json.

**Content-Length** - The length of the request body in octets (8-bit bytes).

**Content-Type** - The Internet media type of the request body. Used with POST and PUT requests. Must be either application/xml or application/json.

**Host** - The domain name of the server hosting HP Cloud Monitoring.

*Non-standard request headers*

**X-Auth-Token** - HP Cloud authorization token.

*Example*

	POST /v1.0/subscriptions HTTP/1.1
	Host: region-a.geo-1.monitoring.hpcloudsvc.com
	Content-Type: application/json
	Accept: application/json
	X-Auth-Token: HPAuth_2895c13b1118e23d977f6a21aa176fd2bd8a10e04b74bd8e353216072968832a
	Content-Length: 85

### 4.3 Common Responses ## {#CommonResponses}

#### 4.3.1 Common Response Headers ### {#CommonResponseHeaders}

*Http standard response headers*

**Content-Length** - The length of the response body in octets (8-bit bytes).

**Content-Type** - Internet media type of the response body.

**Date** - The date and time that the response was sent.

*Example*

	HTTP/1.1 200 OK
	Content-Length: 1135
	Content-Type: application/json; charset=UTF-8
	Date: Tue, 30 Oct 2012 16:22:35 GMT

### 4.4 Service API Operation Details ## {#ServiceDetails}

#### 4.4.1 Versions ### {#ServiceDetailsVersion}

Provides information about the supported Monitoring API versions.

##### 4.4.1.1 List All Versions #### {#ServiceDetailsListVersion}
###### GET /

Lists all versions. 

**Request Data**

	GET / HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

|Call Specific Attributes|Type|Description|
|:---------|:---|:----------|
|id|string|API version number.|
|rel|string|Relationship type.|
|href|string|Hypermedia reference.|
|status|string|Status of the API, if it is active and/or current.|
|updated|timestamp|Internal timestamp of when the API was released.|

JSON

	{  
	  "versions": [
	    {
	      "id": "1.0",
	      "links" : [
	        {
	          "rel": "self",
	          "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/1.0"
	        }
	      ],
	      "status": "CURRENT",
	      "updated": "1361977272958"
	    }
	  ]
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts).*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com \
	  -H "X-Auth-Token: ${Auth_Token}"

##### 4.4.1.2 Get a Specific Version #### {#ServiceDetailsSpecificVersion}
###### GET /{version_id}

Gets the details of a specific version identified by {version_id}.

**Request Data**

	GET /v1.0 HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.1.1 List All Versions](#ServiceDetailsListVersion).

JSON

	{  
	  "version": {
	    "id": "1.0",
	    "links" : [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0"
	      }
	    ],
	    "status": "CURRENT",
	    "updated": "2012-09-25T00:00:00Z"
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation. |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts).*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0 \
	  -H "X-Auth-Token: ${Auth_Token}"

#### 4.4.2 Endpoint ### {#ServiceDetailsEndpoint}

The endpoint resource represents an endpoint from which metrics can be consumed.

*Note: The amqp_password is not retrievable after endpoint creation. If the password is lost, then the password reset operation must be performed.*

*Note: Only one Endpoint can be created at a time per tenant.*

##### 4.4.2.1 Create a New Endpoint #### {#ServiceDetailsCreateEndpoint}
###### POST /endpoints

Creates a new endpoint for metric consumption. AMQP and URI information needs to be retained for accessing the message queue.

**Request Data**

	POST /v1.0/endpoints HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

	HTTP/1.1 201 Created
	Content-Type: application/json
	Content-Length: 337
	Location: /v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c

**Status Code**

201 - Created

**Response Data**

|Attributes|Type|Description|
|:---------|:---|:----------|
|id|string|Return value id of the call.|
|rel|string|Relationship type.|
|href|string|Hypermedia reference.|
|uri|string|URI of the amqp router.|
|meta|array|Amqp router information.|
|amqp_password|string|Needed to access the amqp router.|
|amqp_username|string|Needed to access the amqp router.|
|amqp_queue|string|Needed to access the amqp router.|

JSON

	{
	  "endpoint": {
	    "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "links": [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c"
	      }
	    ]
	    "uri": "amqp://region-a.geo-1.amqp-monitoring.hpcloudsvc.com:5672/385937540",
	    "meta": {
	      "amqp_password":"GnbV94wW3MF90",
	      "amqp_username": "385937540",
	      "amqp_queue": "metrics-67892236969703",
	    }
	  }
	}
	
**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 409 | Conflict | An endpoint for this tenant already exists. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts).*

	$ curl -i -X POST \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints \
	  -H "X-Auth-Token: ${Auth_Token}"

##### 4.4.2.2 List All Endpoints #### {#ServiceDetailsListEndpoint}
###### GET /endpoints

Lists all endpoints. Password information is not present.

**Request Data**

	GET /v1.0/endpoints HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.2.1 Create a New Endpoint](#ServiceDetailsCreateEndpoint).

JSON

	{
	  "endpoints": [
	    {
	      "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	      "links": [
	        {
	          "rel": "self",
	          "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c"
	        }
	      ],
	      "uri": "amqp://region-a.geo-1.amqp-monitoring.hpcloudsvc.com:5672/385937540"
	    },
	  ]
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts).*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints \
	  -H "X-Auth-Token: ${Auth_Token}" 

##### 4.4.2.3 Get a Specific Endpoint #### {#ServiceDetailsSpecificEndpoint}
###### GET /endpoints/{endpoint_id}

Gets the details of a specific endpoint identified by {endpoint_id}. Password information is not present.

**Request Data**

	GET /v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.2.1 Create a New Endpoint](#ServiceDetailsCreateEndpoint).

JSON

	{
	  "endpoint": {
	    "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "links": [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c"
	      }
	    ],
	    "uri": "amqp://region-a.geo-1.amqp-monitoring.hpcloudsvc.com:5672/385937540",
	    "meta": {
	      "amqp_username": "385937540",
	      "amqp_queue": "metrics-67892236969703"
	    }
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /endpoints/ with the actual endpoint id.*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}"
	  
##### 4.4.2.4 Delete a Specific Endpoint #### {#ServiceDetailsDeleteEndpoint}
###### DELETE /endpoints/{endpoint_id}

Deletes a specific endpoint identified by {endpoint_id}.

**Request Data**

	DELETE /v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

204 - No Content

**Response Data**

This call does not provide a response body.

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /endpoints/ with the actual endpoint id.*

	$ curl -i -X DELETE \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}"

##### 4.4.2.4 Reset the Password for a Specific Endpoint #### {#ServiceDetailsResetPasswordEndpoint}
###### POST /endpoints/{endpoint_id}/reset-password

Resets the password for a specific endpoint identified by {endpoint_id}.

**Request Data**

	POST /v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c/reset-password HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

|Call Specific Attributes|Type|Description|
|:---------|:---|:----------|
|password|string|Newly created amqp_password|

	{
	  "password": "mEfOy34qJV"
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value between /endpoints/ and /reset-password with the actual endpoint id.*

	$ curl -i -X POST \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/endpoints/eabe9e32-6ce0-4a36-9750-df415606b44c/reset-password \
	  -H "X-Auth-Token: ${Auth_Token}"
	  
#### 4.4.3 Subscription ### {#ServiceDetailsSubscription}

The subscription resource represents a subscription to consume metrics.

The metric json format received is the same format used for creating user defined metrics in 4.4.6.1 [Create a New Metric Message](#ServiceDetailsCreateMetric).

*Note: Only basicConsume functionality is allowed. You cannot create queues or exchanges.*

##### 4.4.3.1 Create a New Subscription #### {#ServiceDetailsCreateSubscription}

###### POST /subscriptions

Creates a new subscription to consume metrics.

**Request Data**

	POST /v1.0/subscriptions HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Content-Type: application/json
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

|Attributes|Type|Description|
|:---------|:---|:----------|
|endpoint_id|string|The id of an endpoint to subscribe to or view.|
|namespace|string|The namespace of the metrics to receive. See [Namespaces](#Namespaces).|
|dimensions|dictionary|The dimensions of metrics to receive. May be empty for user defined namespaces. See [Dimensions](#Dimensions).|

JSON

	{
	  "subscription": {
	    "endpoint_id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "namespace": "hpcs.compute",
	    "dimensions": {
	      "instance_id": "392633",
	      "az": 2,
	      "instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"
	    }
	  }
	}

**Success Response**

	HTTP/1.1 201 Created
	Content-Type: application/json
	Content-Length: 337
	Location: /v1.0/subscriptions/cdace7b4-8bea-404c-848c-860754a76fb7

**Status Code**

201 - Created

**Response Data**

|Attributes|Type|Description|
|:---------|:---|:----------|
|id|string|Return value id of the call.|
|rel|string|Relationship type.|
|href|string|Hypermedia reference.|
|endpoint_id|string|The id of an endpoint to subscribe to or view.|
|namespace|string|The namespace of the metrics to receive. See [Namespaces](#Namespaces).|
|dimensions|dictionary|The dimensions of metrics to receive. See [Dimensions](#Dimensions).|

JSON

	{
	  "subscriptions": {
	    "id": "cdace7b4-8bea-404c-848c-860754a76fb7",
	    "links": [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions/cdace7b4-8bea-404c-848c-860754a76fb7"
	      }
	    ]
	    "endpoint_id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "namespace": "hpcs.compute",
	    "dimensions": {
	      "instance_id": "392633",
	      "az": 2,
	      "instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"
	    }
	    "meta": {}
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 409 | Conflict | A Subscription for this combination of endpoint_id, namespace, and dimension already exists for this tenant. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the values for endpoint_id, namespace, instance_id, az, and instance_uuid.*

	$ curl -i -X POST \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions \
	  -H "Content-Type:application/json" -H "Accept:application/json" \
	  -H "X-Auth-Token: ${Auth_Token}" \
	  -d '{"subscription": {"endpoint_id": "4d159ef6-0b6a-439b-a5bf-07459e1005b8", "namespace": "hpcs.compute", "dimensions": {"instance_id": "392633","az": 2,"instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"}}}'

##### 4.4.3.2 List All Subscriptions #### {#ServiceDetailsListSubscription}

###### GET /subscriptions

Lists all subscriptions.

**Request Data**

	GET /v1.0/subscriptions HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.3.1 Create a New Subscription](#ServiceDetailsCreateSubscription).

JSON

	{
	  "subscriptions": [
	    {
	      "id": "cdace7b4-8bea-404c-848c-860754a76fb7",
	      "links": [
	        {
	          "rel": "self",
	          "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions/cdace7b4-8bea-404c-848c-860754a76fb7"
	        }
	      ]
	      "endpoint_id": "36351ef0-3ff3-11e2-a25f-0800200c9a66",
	      "namespace": "hpcs.compute",
	      "dimensions": {
	        "instance_id": "392633",
	        "az": 2,
	        "instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"
	      }
	    },
	    {
	      "id": "abce9e32-6ce0-4a36-9750-df415606babc",
	      "links": [
	        {
	          "rel": "self",
	          "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions/abce9e32-6ce0-4a36-9750-df415606babc"
	        }
	      ]
	      "endpoint_id": "3d713b90-3ff3-11e2-a25f-0800200c9a66",
	      "namespace": "hpcs.compute",
	      "dimensions": {
	        "instance_id": "392633",
	        "az": 2,
	        "instance_uuid": "42ff1110-7c86-12e2-b92a-0800200c9a65"
	      }
	    }
	  ]
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts).*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions \
	  -H "X-Auth-Token: ${Auth_Token}" 

##### 4.4.3.3 Get a Specific Subscription #### {#ServiceDetailsSpecificSubscription}

###### GET /subscriptions/{subscription_id}

Gets the details of a specific subscription identified by {subscription_id}.

**Request Data**

	GET /v1.0/subscriptions/eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.3.1 Create a New Subscription](#ServiceDetailsCreateSubscription).

JSON

	{
	  "subscription": {
	    "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "links": [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions/eabe9e32-6ce0-4a36-9750-df415606b44c"
	      }
	    ]
	    "endpoint_id": "36351ef0-3ff3-11e2-a25f-0800200c9a66",
	    "namespace": "hpcs.compute",
	    "dimensions": {
	      "instance_id": "392633",
	      "az": 2,
	      "instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"
	    },
	    "meta": {}
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /subscriptions/ with the actual subscriptions id.*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}" 

##### 4.4.3.4 Delete a Specific Subscription #### {#ServiceDetailsDeleteSubscription}

###### DELETE /subscriptions/{subscription_id}

Deletes a specific subscription identified by {subscription_id}.

**Request Data**

	DELETE /v1.0/subscriptions/eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

204 - No Content

**Response Data**

This call does not provide a response body.

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /subscriptions/ with the actual subscriptions id.*

	$ curl -i -X DELETE \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/subscriptions/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}" 

#### 4.4.4 Notification Method ### {#ServiceDetailsNotification}
The notification method resource represents a method through which notifications can be sent.

*Note: SMS is currently only supported for US customers.*

##### 4.4.4.1 Create a New Notification Method #### {#ServiceDetailsCreateNotification}

###### POST /notification-methods

Creates a new notification method through which notifications can be sent when an alarm is triggered.

**Request Data**

	POST /v1.0/notification-methods HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Content-Type: application/json
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

|Attributes|Type|Description|
|:---------|:---|:----------|
|name|string (250)|A descriptive name.|
|type|enumeration|The type of notification method (SMS, EMAIL).|
|address|string (100)|The address / number to notify.|

JSON

	{
	  "notification_method": {
	    "name": "Joe's Email",
	    "type": "EMAIL",
	    "address": "joe@mail.com"
	  }
	}

**Success Response**

	HTTP/1.1 201 Created
	Content-Type: application/json
	Content-Length: 337
	Location: /v1.0/notification-methods/acb8ad2b-6ce0-4a36-9750-a78bc7da87a2

**Status Code**

201 - Created

**Response Data**

|Attributes|Type|Description|
|:---------|:---|:----------|
|id|string|Return value id of the call.|
|rel|string|Relationship type.|
|href|string|Hypermedia reference.|
|name|string|A descriptive name.|
|type|string / enumeration|The type of notification method (SMS, EMAIL).|
|address|string|The address / number to notify.|

JSON

	{
	  "notification_method": {
	    "id": "acb8ad2b-6ce0-4a36-9750-a78bc7da87a2",
	    "links" : [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods/acb8ad2b-6ce0-4a36-9750-a78bc7da87a2"
	      }
	    ],
	    "name": "Joe's Email",
	    "type": "EMAIL",
	    "address": "joe@mail.com"
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the values for name, type, and address*

	$ curl -i -X POST \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods \
	  -H "Content-Type:application/json" -H "Accept:application/json" \
	  -H "X-Auth-Token: ${Auth_Token}" \
	  -d '{"notification_method": {"name": "Joe'\''s Email", "type": "EMAIL", "address": "joe@mail.com"}}'

##### 4.4.4.2 List All Notification Methods #### {#ServiceDetailsListNotification}
###### GET /notification-methods

Lists all notification methods.

**Request Data**

	GET /v1.0/notification-methods HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.4.1 Create a New Notification Method](#ServiceDetailsCreateNotification).

JSON

	{
	  "notification_methods": [
	    {
	      "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	      "links" : [
	        {
	          "rel": "self",
	          "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods/eabe9e32-6ce0-4a36-9750-df415606b44c"
	        }
	      ],
	      "name": "Joe's Email",
	      "type": "EMAIL",
	      "address": "joe@mail.com"
	    },
	    {
	      "id": "acb8ad2b-6ce0-4a36-9750-a78bc7da87a2",
	      "links" : [
	        {
	          "rel": "self",
	          "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods/acb8ad2b-6ce0-4a36-9750-a78bc7da87a2"
	        }
	      ],
	      "name": "Joe's Phone",
	      "type": "SMS",
	      "address": "12063823454"
	    }
	  ]
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts).*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods \
	  -H "X-Auth-Token: ${Auth_Token}" 

##### 4.4.4.3 Get a Specific Notification Method #### {#ServiceDetailsSpecificNotification}
###### GET /notification-methods/{notification_method_id}

Gets the details of a specific notification method identified by {notification_method_id}.

**Request Data**

	GET /v1.0/notification-methods/eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.4.1 Create a New Notification Method](#ServiceDetailsCreateNotification).

JSON

	{
	  "notification_method": {
	    "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "links" : [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods/eabe9e32-6ce0-4a36-9750-df415606b44c"
	      }
	    ],
	    "name": "Joe's Email",
	    "type": "EMAIL",
	    "address": "joe@mail.com"
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /notification-methods/ with the actual notification-methods id.*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}"

##### 4.4.4.4 Delete a Specific Notification Method #### {#ServiceDetailsDeleteNotification}
###### DELETE /notification-methods /{notification_method_id}

Deletes a specific notification method identified by {notification_method_id}.

**Request Data**

	DELETE /v1.0/notification-methods /eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

204 - No Content

**Response Data**

This call does not provide a response body.

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /notification-methods/ with the actual notification-methods id.*

	$ curl -i -X DELETE \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/notification-methods/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}"

#### 4.4.5 Alarm ### {#ServiceDetailsAlarm}

The alarm resource identifies a particular metric scoped by namespace, metric type, and dimensions, which should trigger a set of actions when the value of the metric exceeds a threshold as determined by an expression. Alarm polling is done once every 60 seconds.

*Note: See [Alarm Expressions](#AlarmExpressions) for how to write the expression.*

**State Lifecycle**

|State|Definition|
|:----|:---------|
|UNDETERMINED|The state of the alarm cannot be determined. An alarm starts in this state and can re-enter this state if it doesn't receive metric values for a while|
|OK|The threshold is not passed, no notification is sent|
|ALARM|The threshold has been passed, a notification has been sent|

##### 4.4.5.1 Create a New Alarm #### {#ServiceDetailsCreateAlarm}

###### POST /alarms

Creates a new alarm.

**Request Data**

	POST /v1.0/alarms HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Content-Type: application/json
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

|Attributes|Type|Description|
|:---------|:---|:----------|
|name|string (250)|A descriptive name.|
|expression|string|An expression, which if true, triggers a notification to be sent. See [Alarm Expressions](#AlarmExpressions) for how to write an expression.|
|alarm_actions|array|Methods through which notifications (notification id) should be sent when transitioning to an ALARM state.|

JSON

	{
	  "alarm": {
	    "name": "Disk exceeds 1k operations per measurement period",
	    "expression": "avg(hpcs.compute:disk_read_ops:VDA:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66}) >= 1000",
	    "alarm_actions": [
	      "036609b0-3d6b-11e2-a25f-0800200c9a66",
	      "1221dba0-3d6b-11e2-a25f-0800200c9a66"
	    ]
	  }
	}

**Success Response**

	HTTP/1.1 201 Created
	Content-Type: application/json
	Content-Length: 337
	Location: /v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c

**Status Code**

201 - Created

**Response Data**

|Attributes|Type|Description|
|:---------|:---|:----------|
|id|string|Return value id of the call.|
|rel|string|Relationship type.|
|href|string|Hypermedia reference.|
|name|string|A descriptive name.|
|expression|string|An expression, which if true, triggers a notification to be sent. See [Alarm Expressions](#AlarmExpressions) for how to write an expression.|
|state|string|The alarm state. See State Lifecycle above.|
|alarm_actions|array|Methods through which notifications (notification id) should be sent when transitioning to an ALARM state.|

JSON

	{
	  "alarm": {
	    "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "links": [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c"
	      }
	    ],
	    "name": "Disk exceeds 1k operations per measurement period",
	    "expression": "avg(hpcs.compute:disk_read_ops:VDA:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66}) >= 1000",
	    "state": "UNDETERMINED",
	    "alarm_actions": [
	      "036609b0-3d6b-11e2-a25f-0800200c9a66",
	      "1221dba0-3d6b-11e2-a25f-0800200c9a66"
	    ]
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 409 | Conflict | An Alarm for this combination of values already exists for this tenant. |
| 422 | Unprocessable Entity | The metric json contained errors. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the values for name, expression, alarm_actions.*

	$ curl -i -X POST \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/alarms \
	  -H "Content-Type:application/json" -H "Accept:application/json" \
	  -H "X-Auth-Token: ${Auth_Token}" \
	  -d '{"alarm": {"name": "Disk exceeds 1k operations per measurement period", "expression": "avg(hpcs.compute:disk_read_ops:VDA:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66}) >= 1000", "alarm_actions": ["036609b0-3d6b-11e2-a25f-0800200c9a66", "1221dba0-3d6b-11e2-a25f-0800200c9a66"]}}'

##### 4.4.5.2 List All Alarms #### {#ServiceDetailsListAlarm}
###### GET /alarms

Lists all alarms.

**Request Data**

	GET /v1.0/alarms HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.5.1 Create a New Alarm](#ServiceDetailsCreateAlarm).

JSON

	{
	  "alarms": [
	    {
	    "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "links": [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c"
	      }
	    ],
	    "name": "Disk exceeds 1k operations per measurement period",
	    "expression": "avg(hpcs.compute:disk_read_ops:VDA:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66}) >= 1000",
	    "state": "OK",
	    }
	  ]
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts).*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/alarms \
	  -H "X-Auth-Token: ${Auth_Token}"

##### 4.4.5.3 Get a Specific Alarm #### {#ServiceDetailsSpecificAlarm}

###### GET /alarms/{alarm_id}

Gets the details of a specific alarms identified by {alarm_id}.

**Request Data**

	GET /v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body.

**Success Response**

**Status Code**

200 - OK

**Response Data**

Common attributes described in [4.4.5.1 Create a New Alarm](#ServiceDetailsCreateAlarm).

JSON

	{
	  "alarm": {
	    "id": "eabe9e32-6ce0-4a36-9750-df415606b44c",
	    "links": [
	      {
	        "rel": "self",
	        "href": "https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c"
	      }
	    ],
	    "name": "Disk exceeds 1k operations per measurement period",
	    "expression": "avg(hpcs.compute:disk_read_ops:VDA:{instance_id=392633,az=2,instance_uuid=31ff6820-7c86-11e2-b92a-0800200c9a66}) >= 1000",
	    "state": "OK",
	    "alarm_actions": [
	      "036609b0-3d6b-11e2-a25f-0800200c9a66",
	      "1221dba0-3d6b-11e2-a25f-0800200c9a66"
	    ]
	  }
	}

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /alarms/ with the actual alarm id.*

	$ curl -i -X GET \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}" 

##### 4.4.5.4 Delete a Specific Alarm #### {#ServiceDetailsDeleteAlarm}

###### DELETE /alarms/{alarm_id}

Deletes a specific alarm identified by {alarm_id}.

**Request Data**

	DELETE /v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

This call does not require a request body. 

**Success Response**

**Status Code**

204 - No Content

**Response Data**

This call does not provide a response body.

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation.      |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 404 | Not Found | Requested resource cannot be found. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the value after /alarms/ with the actual alarm id.*

	$ curl -i -X DELETE \ 
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/alarms/eabe9e32-6ce0-4a36-9750-df415606b44c \
	  -H "X-Auth-Token: ${Auth_Token}"

#### 4.4.6 Metrics ### {#ServiceDetailsMetrics}

The metrics resource allows the insertion of user defined metrics.

*Note: You must have a subscription running at the time of the new metric message post call in order to see the message*

##### 4.4.6.1 Create a New Metric Message #### {#ServiceDetailsCreateMetric}

###### POST /metrics

Creates a new metric.

**Request Data**

	POST /v1.0/metrics HTTP/1.1
	Host: https://region-a.geo-1.monitoring.hpcloudsvc.com
	Content-Type: application/json
	Accept: application/json
	X-Auth-Token: ${Auth_Token}

**Data Parameters**

|Call Specific Attributes|Type|Description|
|:---------|:---|:----------|
|namespace|string (64)|User defined namespace (cannot conflict with a reserved namespace). See [Namespaces](#Namespaces).|
|type|string (64)|User defined metric type name (similar to the predefined metric types: [Metric Types](#Metrics)).|
|dimensions|dictionary|(Optional) The dimensions of metrics to receive (each dimension type can only be used once in this call). User defined metrics do not require dimensions. See [Dimensions](#Dimensions).|
|timestamp|integer (long)|(Optional) Unix format timestamp for the message. This must be within 2 minutes into the future or 2 weeks into the past of the current UTC time. If not included, the current UTC time is inserted.|
|value|double|Metric value. Must be between 8.515920e-109 and 1.174271e+108 or zero.|
|time_values|[ [ long, double ] ]|Use instead of the value field to give a list of timestamp / value pairs.|

JSON

The HP Cloud Metrics JSON json format is used for publishing new user defined metrics and for receiving all metrics from a subscription.

***Single Metric Message***

	{
		"namespace": "user_namespace"
		"type": "user_metric",
		"dimensions": {
		  "instance_id": "392633",
		  "az": 2,
		  "instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"
		},
		"timestamp": 123123123,
		"value": 123.0
	}
	
***Multiple Metric Message***

	{
		"namespace": "user_namespace"
		"type": "user_metric",
		"dimensions": {
		  "instance_id": "392633",
		  "az": 2,
		  "instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"
		},
		"timestamp": 123123123,
		"time_values": [
		  [1366128488, 123.0], [1366128504, 0.0], [1366128494, 234.0]
		]
	}

**Success Response**

**Status Code**

204 - No Content

**Response Data**

This call does not provide a response body.

**Error Response**

**Status Code**

| Status Code | Description | Reasons |
| :-----------| :-----------| :-------|
| 400 | Bad Request | Malformed request in URI or request body. |
| 401 | Unauthorized | The caller does not have the privilege required to perform the operation. |
| 403 | Forbidden | Disabled or suspended user making the request or requested operation is forbidden. |
| 409 | Conflict | The namespace name conflicts with a reserved namespace. |
| 422 | Unprocessable Entity | The metric json contained errors. |
| 500 | Server Error | The server encountered a problem while processing the request. |

**Curl Example**

*Replace ${Auth_Token} with the token returned from section [Accounts](#Accounts). Replace the values for namespace, type, dimensions, timestamp, .*

	$ curl -i -X POST \
	  https://region-a.geo-1.monitoring.hpcloudsvc.com/v1.0/metrics \
	  -H "Content-Type:application/json" -H "Accept:application/json" \
	  -H "X-Auth-Token: ${Auth_Token}" \
	  -d '{"namespace": "user_namespace", "type": "user_metric", "dimensions": {"instance_id": "392633","az": 2,"instance_uuid": "31ff6820-7c86-11e2-b92a-0800200c9a66"}, "timestamp": 1366178760, "value": 127}'

## 5. Glossary # {#Section5_}

* Namespace - A required classification for a metric.
* Dimension - A required classification for a metric narrowing the namespace classification. A metric may be classified by multiple dimensions.
