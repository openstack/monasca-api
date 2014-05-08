## /v2.0/statistics
Operations for accessing statistics
### GET /v2.0/statistics
Get statistics
#### Parameters
* X-Tenant-Id (string)
* name (string)
* dimensions (string)
* start_time (string)
* end_time (string)
* statistics (string)
* period (string)


## /v2.0/metrics
Operations for accessing metrics
### POST /v2.0/metrics
Create metrics
#### Parameters
* X-Tenant-Id (string)
* X-Roles (string)
* tenant_id (string)
* body (array)

### GET /v2.0/metrics
Get metrics
#### Parameters
* X-Tenant-Id (string)
* name (string)
* dimensions (string)


## /v2.0/alarms
Operations for working with alarms
### POST /v2.0/alarms
Create alarm
#### Parameters
* X-Tenant-Id (string)
* body (CreateAlarmCommand)

### GET /v2.0/alarms
List alarms
#### Parameters
* X-Tenant-Id (string)
* dimensions (string)
* state (string)


### GET /v2.0/alarms/{alarm_id}/state-history
Get alarm state history
#### Parameters
* X-Tenant-Id (string)
* alarm_id (string)


### GET /v2.0/alarms/{alarm_id}
Get alarm
#### Parameters
* X-Tenant-Id (string)
* alarm_id (string)

#### Responses
* 400: Invalid ID supplied
* 404: Alarm not found

### DELETE /v2.0/alarms/{alarm_id}
Delete alarm
#### Parameters
* X-Tenant-Id (string)
* alarm_id (string)

### PUT /v2.0/alarms/{alarm_id}
Update alarm
#### Parameters
* X-Tenant-Id (string)
* alarm_id (string)
* body (UpdateAlarmCommand)


## /v2.0/notification-methods
Operations for working with notification methods
### POST /v2.0/notification-methods
Create notification method
#### Parameters
* X-Tenant-Id (string)
* body (CreateNotificationMethodCommand)

### GET /v2.0/notification-methods
List notification methods
#### Parameters
* X-Tenant-Id (string)


### GET /v2.0/notification-methods/{notification_method_id}
Get notification method
#### Parameters
* X-Tenant-Id (string)
* notification_method_id (string)

### DELETE /v2.0/notification-methods/{notification_method_id}
Delete notification method
#### Parameters
* X-Tenant-Id (string)
* notification_method_id (string)

### PUT /v2.0/notification-methods/{notification_method_id}
Update notification method
#### Parameters
* X-Tenant-Id (string)
* notification_method_id (string)
* body (CreateNotificationMethodCommand)


## /v2.0/measurements
Operations for accessing measurements
### GET /v2.0/measurements
Get measurements
#### Parameters
* X-Tenant-Id (string)
* name (string)
* dimensions (string)
* start_time (string)
* end_time (string)


## /
Operations for accessing versions
## Statistics

* columns: array
* name: string


## CreateMetricCommand

* timestamp: integer
* name: string
* value: number

## MetricDefinition

* name: string


## CreateAlarmCommand

* name: string
* undeterminedActions: array
* alarmActions: array
* okActions: array
* expression: string
* description: string

## Alarm

An alarm is a devops's best friend

* undeterminedActions: array
* links: array
* alarmActions: array
* description: string
* okActions: array
* expression: string
* id: string
* actionsEnabled: boolean
* name: string

## AlarmStateHistory

* reasonData: string
* alarmId: string
* reason: string

## Link

* href: string
* rel: string

## UpdateAlarmCommand

* name: string
* undeterminedActions: array
* alarmActions: array
* okActions: array
* expression: string
* actionsEnabled: boolean
* description: string


## NotificationMethod

* id: string
* name: string
* links: array
* address: string

## Link

* href: string
* rel: string

## CreateNotificationMethodCommand

* name: string
* address: string


## Measurements

* columns: array
* name: string


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

