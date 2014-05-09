
# Monitoring API Overview
 
## 1. Overview

This document describes the Monitoring API, which allows you to monitor resources.

## 2. Architecture View

Monitoring as a Service provides APIs for managing metric consumption endpoints, metric subscriptions, alarms, contact methods, and publishing user defined metrics.

### 2.1 Overview

The Monitoring API provides a RESTful JSON interface for interacting with and managing monitoring related resources.

The API supports a number of resources. These include:

+ Versions - Provides information about the supported Monitoring API versions.

+ Metrics: The metric resource allows the insertion of user defined metrics.

+ Measurements: Operations for accessing measurements.

+ Statistics: Operations for accessing statistics about metrics.

+ Alarms: The alarm resource identifies a one or more metrics scoped by name and dimensions, which should trigger a set of actions when the value of a threshold is exceeded.

+ Notification Methods: The notification method resource represents a method through which notifications can be sent.


#### 2.1.1 High Level and Usage

Before using the API, you must first get a valid auth token. All API operations require anauth token specified in the header 

##### 2.1.1.1 Sending metrics

##### 2.1.1.2 Creating an alarm

#### 2.1.2 Name, Dimensions, and Metrics

##### 2.1.2.1 Name

Defines the name of a metric. 

##### 2.1.2.2 Dimensions

Places restrictions on the namespace to further narrow what is monitored.

Dimensions may only use the characters from: a-z A-Z 0-9 . \_. The dimension value may only use the characters from: a-z A-Z 0-9 . - \_.

### 2.2 Alarm Expressions

The alarm expression syntax allows the creation of simple to complex alarms to handle a wide variety of needs. Alarm expressions are evaluated every 60 seconds.

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
	
	cpu_perc:{hostname=host.domain.com} > 95

The second subexpression shows a function operation done on all collected metrics within a specific period and compared to a threshold_value, then triggered if it happens periods number of times.

	Example:
	
	avg(cpu_perc:{hostname=host.domain.com},85) > 90 times 3
	
Note that period is the number of seconds for the measurement to be done on. They can only be in a multiple of 60. Periods is how many times in a row that this expression must be true before triggering the alarm. Both period and periods are optional and default to 60 and 1 respectively.

Functions work on all metric measurements during the period time frame.

+ min (returns the minimum of all the values)
+ max (returns the maximum of all the values)
+ sum (returns the sum of all the values)
+ count (returns the number of metric observations)
+ avg (returns the average of all the values)

The metric is a complex identifier that says the name and optional dimensions.

	metric
		: name ':' '{' dimensions '}')?

	Examples:
	
	cpu_perc:{hostname=host.domain.com}
	
Larger example:

	(avg(cpu_perc:{hostname=hostname.domain.com}) > 90 ) or ( avg(disk_read_ops:{hostname=hostname.domain.com,device=vda,120) > 1000 times 3)
