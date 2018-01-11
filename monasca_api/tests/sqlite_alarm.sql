PRAGMA synchronous = OFF;
PRAGMA journal_mode = MEMORY;
BEGIN TRANSACTION;
CREATE TABLE `alarm_state` (
  `name` varchar(20) NOT NULL,
  PRIMARY KEY (`name`)
);
CREATE TABLE `alarm_definition_severity` (
  `name` varchar(20) NOT NULL,
  PRIMARY KEY (`name`)
);
CREATE TABLE `notification_method_type` (
  `name` varchar(20) NOT NULL,
  PRIMARY KEY (`name`)
);
CREATE TABLE `notification_method` (
  `id` varchar(36) NOT NULL,
  `tenant_id` varchar(36) NOT NULL,
  `name` varchar(250) DEFAULT NULL,
  `type` varchar(20) NOT NULL,
  `address` varchar(512) DEFAULT NULL,
  `period` int NOT NULL DEFAULT 0,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `alarm_definition` (
  `id` varchar(36) NOT NULL,
  `tenant_id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `description` varchar(255) DEFAULT NULL,
  `expression` longtext NOT NULL,
  `severity` varchar(20) NOT NULL,
  `match_by` varchar(255) DEFAULT '',
  `actions_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `alarm` (
  `id` varchar(36) NOT NULL,
  `alarm_definition_id` varchar(36) NOT NULL DEFAULT '',
  `state` varchar(20) NOT NULL,
  `lifecycle_state` varchar(50) DEFAULT NULL,
  `link` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `state_updated_at` datetime,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `alarm_action` (
  `alarm_definition_id` varchar(36) NOT NULL,
  `alarm_state` varchar(20) NOT NULL,
  `action_id` varchar(36) NOT NULL,
  PRIMARY KEY (`alarm_definition_id`,`alarm_state`,`action_id`)
);
CREATE TABLE `alarm_metric` (
  `alarm_id` varchar(36) NOT NULL,
  `metric_definition_dimensions_id` binary(20) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  PRIMARY KEY (`alarm_id`,`metric_definition_dimensions_id`)
);
CREATE TABLE `metric_definition` (
  `id` binary(20) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `name` varchar(255) NOT NULL,
  `tenant_id` varchar(36) NOT NULL,
  `region` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
);
CREATE TABLE `metric_definition_dimensions` (
  `id` binary(20) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `metric_definition_id` binary(20) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `metric_dimension_set_id` binary(20) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  PRIMARY KEY (`id`)
);
CREATE TABLE `metric_dimension` (
  `dimension_set_id` binary(20) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `name` varchar(255) NOT NULL DEFAULT '',
  `value` varchar(255) NOT NULL DEFAULT ''
);
CREATE TABLE `sub_alarm_definition` (
  `id` varchar(36) NOT NULL,
  `alarm_definition_id` varchar(36) NOT NULL DEFAULT '',
  `function` varchar(10) NOT NULL,
  `metric_name` varchar(100) DEFAULT NULL,
  `operator` varchar(5) NOT NULL,
  `threshold` double NOT NULL,
  `period` int(11) NOT NULL,
  `periods` int(11) NOT NULL,
  `is_deterministic` tinyint(1) NOT NULL DEFAULT(0),
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
);
CREATE TABLE `sub_alarm_definition_dimension` (
  `sub_alarm_definition_id` varchar(36) NOT NULL DEFAULT '',
  `dimension_name` varchar(255) NOT NULL DEFAULT '',
  `value` varchar(255) DEFAULT NULL
);
CREATE TABLE `sub_alarm` (
  `id` varchar(36) NOT NULL,
  `alarm_id` varchar(36) NOT NULL DEFAULT '',
  `sub_expression_id` varchar(36) NOT NULL DEFAULT '',
  `expression` longtext NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
);

insert into `alarm_state` values ('UNDETERMINED');
insert into `alarm_state` values ('OK');
insert into `alarm_state` values ('ALARM');

insert into `alarm_definition_severity` values ('LOW');
insert into `alarm_definition_severity` values ('MEDIUM');
insert into `alarm_definition_severity` values ('HIGH');
insert into `alarm_definition_severity` values ('CRITICAL');

insert into `notification_method_type` values ('EMAIL');
insert into `notification_method_type` values ('WEBHOOK');
insert into `notification_method_type` values ('PAGERDUTY');

END TRANSACTION;
