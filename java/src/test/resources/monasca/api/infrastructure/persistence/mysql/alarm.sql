CREATE TABLE `alarm` (
  `id` varchar(36) NOT NULL,
  `alarm_definition_id` varchar(36) NOT NULL DEFAULT '',
  `state` varchar(20) NOT NULL check state in ('UNDETERMINED','OK','ALARM'),
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tenant_id` (`alarm_definition_id`)
);

CREATE TABLE `alarm_definition` (
  `id` varchar(36) NOT NULL,
  `tenant_id` varchar(36) NOT NULL,
  `name` varchar(250) DEFAULT NULL,
  `description` varchar(250) DEFAULT NULL,
  `expression` mediumtext,
  `severity` varchar(20) NOT NULL check severity in ('LOW','MEDIUM','HIGH','CRITICAL'),
  `match_by` varchar(255) DEFAULT '',
  `actions_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
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
  `is_deterministic` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `sub_alarm_definition_dimension` (
  `sub_alarm_definition_id` varchar(36) NOT NULL DEFAULT '',
  `dimension_name` varchar(50) NOT NULL DEFAULT '',
  `value` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`sub_alarm_definition_id`,`dimension_name`)
);

CREATE TABLE `sub_alarm` (
  `id` varchar(36) NOT NULL,
  `alarm_id` varchar(36) NOT NULL DEFAULT '',
  `expression` mediumtext NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
);

CREATE TABLE `alarm_action` (
  `alarm_definition_id` varchar(36) NOT NULL,
  `alarm_state` varchar(20) NOT NULL check alarm_state in ('UNDETERMINED','OK','ALARM'),
  `action_id` varchar(36) NOT NULL DEFAULT '',
  PRIMARY KEY (`alarm_definition_id`,`alarm_state`,`action_id`)
);
