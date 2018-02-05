---
-- # Copyright 2017 FUJITSU LIMITED
---

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

---
-- enum tables
---

CREATE TABLE alarm_state (
  name character varying(20) NOT NULL,
  CONSTRAINT alarm_state_pkey PRIMARY KEY (name)
);

CREATE TABLE alarm_definition_severity (
  name character varying(20) NOT NULL,
  CONSTRAINT alarm_definition_severity_pkey PRIMARY KEY (name)
);

CREATE TABLE notification_method_type (
  name character varying(20) NOT NULL,
  CONSTRAINT notification_method_type_pkey PRIMARY KEY (name)
);

---
-- tables
---

CREATE TABLE alarm (
    id character varying(36) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    lifecycle_state character varying(50),
    link character varying(512),
    state character varying(20) NOT NULL,
    state_updated_at timestamp without time zone,
    alarm_definition_id character varying(36) NOT NULL
);

CREATE TABLE alarm_action (
    action_id character varying(36) NOT NULL,
    alarm_state character varying(20) NOT NULL,
    alarm_definition_id character varying(36) NOT NULL
);

CREATE TABLE alarm_definition (
    id character varying(36) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    actions_enabled boolean NOT NULL,
    deleted_at timestamp without time zone,
    description character varying(255),
    expression text NOT NULL,
    match_by character varying(255),
    name character varying(255) NOT NULL,
    severity character varying(20) NOT NULL,
    tenant_id character varying(36) NOT NULL
);

CREATE TABLE alarm_metric (
    metric_definition_dimensions_id bytea NOT NULL,
    alarm_id character varying(36) NOT NULL
);

CREATE TABLE metric_definition (
    id bytea NOT NULL,
    name character varying(255) NOT NULL,
    region character varying(255) NOT NULL,
    tenant_id character varying(36) NOT NULL
);

CREATE TABLE metric_definition_dimensions (
    id bytea NOT NULL,
    metric_definition_id bytea NOT NULL,
    metric_dimension_set_id bytea NOT NULL
);

CREATE TABLE metric_dimension (
    dimension_set_id bytea NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(255) NOT NULL
);

CREATE TABLE notification_method (
    id character varying(36) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    address character varying(512) NOT NULL,
    name character varying(250),
    tenant_id character varying(36) NOT NULL,
    type character varying(20) NOT NULL,
    period integer NOT NULL
);

CREATE TABLE sub_alarm (
    id character varying(36) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    expression text NOT NULL,
    alarm_id character varying(36) NOT NULL,
    sub_expression_id character varying(36),
    state character varying(20) NOT NULL DEFAULT 'OK'
);

CREATE TABLE sub_alarm_definition (
    id character varying(36) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    function character varying(10) NOT NULL,
    metric_name character varying(100),
    operator character varying(5) NOT NULL,
    period integer NOT NULL,
    periods integer NOT NULL,
    threshold double precision NOT NULL,
    is_deterministic boolean NOT NULL,
    alarm_definition_id character varying(36) NOT NULL
);

CREATE TABLE sub_alarm_definition_dimension (
    dimension_name character varying(255) NOT NULL,
    value character varying(255),
    sub_alarm_definition_id character varying(36) NOT NULL
);

---
-- primary keys
---
ALTER TABLE ONLY alarm_action
    ADD CONSTRAINT alarm_action_pkey PRIMARY KEY (action_id, alarm_definition_id, alarm_state);

ALTER TABLE ONLY alarm_definition
    ADD CONSTRAINT alarm_definition_pkey PRIMARY KEY (id);

ALTER TABLE ONLY alarm_metric
    ADD CONSTRAINT alarm_metric_pkey PRIMARY KEY (alarm_id, metric_definition_dimensions_id);

ALTER TABLE ONLY alarm
    ADD CONSTRAINT alarm_pkey PRIMARY KEY (id);

ALTER TABLE ONLY metric_definition_dimensions
    ADD CONSTRAINT metric_definition_dimensions_pkey PRIMARY KEY (id);

ALTER TABLE ONLY metric_definition
    ADD CONSTRAINT metric_definition_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notification_method
    ADD CONSTRAINT notification_method_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sub_alarm_definition_dimension
    ADD CONSTRAINT sub_alarm_definition_dimension_pkey PRIMARY KEY (dimension_name, sub_alarm_definition_id);

ALTER TABLE ONLY sub_alarm_definition
    ADD CONSTRAINT sub_alarm_definition_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sub_alarm
    ADD CONSTRAINT sub_alarm_pkey PRIMARY KEY (id);

---
-- indexes
---

CREATE INDEX alarm_id ON alarm_metric USING btree (alarm_id);
CREATE INDEX deleted_at ON alarm_definition USING btree (deleted_at);
CREATE INDEX dimension_set_id ON metric_dimension USING btree (dimension_set_id);
CREATE INDEX metric_definition_dimensions_id ON alarm_metric USING btree (metric_definition_dimensions_id);
CREATE INDEX metric_definition_id ON metric_definition_dimensions USING btree (metric_definition_id);
CREATE UNIQUE INDEX metric_dimension_key ON metric_dimension USING btree (dimension_set_id, name);
CREATE INDEX metric_dimension_set_id ON metric_definition_dimensions USING btree (metric_dimension_set_id);
CREATE INDEX tenant_id ON alarm_definition USING btree (tenant_id);

---
-- foreign key constraints
---
ALTER TABLE ONLY alarm_action
    ADD CONSTRAINT fk_alarm_action_alarm_definition FOREIGN KEY (alarm_definition_id) REFERENCES alarm_definition(id);

ALTER TABLE ONLY sub_alarm
    ADD CONSTRAINT fk_sub_alarm_sub_alarm_definition FOREIGN KEY (sub_expression_id) REFERENCES sub_alarm_definition(id);

ALTER TABLE ONLY alarm
    ADD CONSTRAINT fk_alarm_alarm_definition FOREIGN KEY (alarm_definition_id) REFERENCES alarm_definition(id);

ALTER TABLE ONLY sub_alarm_definition_dimension
    ADD CONSTRAINT fk_sub_alarm_def_dim_sub_alarm_def FOREIGN KEY (sub_alarm_definition_id) REFERENCES sub_alarm_definition(id) ON DELETE CASCADE;

ALTER TABLE ONLY alarm_metric
    ADD CONSTRAINT fk_alarm_metric_alamr FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE;

ALTER TABLE ONLY sub_alarm_definition
    ADD CONSTRAINT fk_sub_alarm_def_alarm_def FOREIGN KEY (alarm_definition_id) REFERENCES alarm_definition(id) ON DELETE CASCADE;

ALTER TABLE ONLY sub_alarm
    ADD CONSTRAINT fk_sub_alarm_alarm FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE;

ALTER TABLE ONLY alarm
    ADD CONSTRAINT fk_alarm_state FOREIGN KEY (state) REFERENCES alarm_state (name);

ALTER TABLE ONLY alarm_action
    ADD CONSTRAINT fk_alarm_action_state FOREIGN KEY (alarm_state) REFERENCES alarm_state (name);

ALTER TABLE ONLY alarm_definition
    ADD CONSTRAINT fk_alarm_definition_severity FOREIGN KEY (severity) REFERENCES alarm_definition_severity (name);

ALTER TABLE ONLY notification_method
    ADD CONSTRAINT fk_alarm_noticication_method_type FOREIGN KEY (type) REFERENCES notification_method_type (name);

---
-- data for enum tables
---
insert into alarm_state values ('UNDETERMINED');
insert into alarm_state values ('OK');
insert into alarm_state values ('ALARM');

insert into alarm_definition_severity values ('LOW');
insert into alarm_definition_severity values ('MEDIUM');
insert into alarm_definition_severity values ('HIGH');
insert into alarm_definition_severity values ('CRITICAL');

insert into notification_method_type values ('EMAIL');
insert into notification_method_type values ('WEBHOOK');
insert into notification_method_type values ('PAGERDUTY');
