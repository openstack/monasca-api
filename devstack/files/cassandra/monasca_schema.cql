// (C) Copyright 2017 SUSE LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
// implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// version 1.0

drop schema if exists monasca;

// replication factor is set to 1 for devstack installation

create schema monasca with replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

create table monasca.measurements (
    metric_id blob,
    region text static,
    tenant_id text static,
    metric_name text static,
    dimensions frozen<list<text>> static,
    time_stamp timestamp,
    value double,
    value_meta text,
    primary key (metric_id, time_stamp)
)
WITH CLUSTERING ORDER BY (time_stamp ASC);

create table monasca.metrics (
    region text,
    tenant_id text,
    metric_name text,
    dimensions frozen<list<text>>,
    dimension_names frozen<list<text>>,
    metric_id blob,
    created_at timestamp,
    updated_at timestamp,
    primary key ((region, tenant_id, metric_name), dimensions, dimension_names)
);

CREATE CUSTOM INDEX metrics_created_at_index ON monasca.metrics (created_at)
USING 'org.apache.cassandra.index.sasi.SASIIndex';

CREATE CUSTOM INDEX metrics_updated_at_index ON monasca.metrics (updated_at)
USING 'org.apache.cassandra.index.sasi.SASIIndex';

create table monasca.dimensions (
    region text,
    tenant_id text,
    name text,
    value text,
    primary key ((region, tenant_id, name), value)
);

create table monasca.dimensions_metrics (
    region text,
    tenant_id text,
    dimension_name text,
    dimension_value text,
    metric_name text,
    primary key ((region, tenant_id, dimension_name, dimension_value), metric_name)
);

create table monasca.metrics_dimensions (
    region text,
    tenant_id text,
    dimension_name text,
    dimension_value text,
    metric_name text,
    primary key ((region, tenant_id, metric_name), dimension_name, dimension_value)
);

create table monasca.alarm_state_history (
    tenant_id text,
    alarm_id text,
    time_stamp timestamp,
    metric text,
    old_state text,
    new_state text,
    reason text,
    reason_data text,
    sub_alarms text,
    primary key ((tenant_id, alarm_id), time_stamp)
);

