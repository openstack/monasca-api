drop table if exists monasca.metric_map;

drop table if exists monasca.measurements;

drop table if exists monasca.alarm_state_history;

drop schema if exists monasca;

create schema monasca
with replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

use monasca;

create table monasca.metric_map (
    tenant_id text,
    region text,
    metric_hash blob,
    metric_map map<text, text>,
primary key ((tenant_id, region), metric_hash)
);

create index on monasca.metric_map (entries(metric_map));

create table monasca.measurements (
    tenant_id text,
    region text,
    metric_hash blob,
    time_stamp timestamp,
    value double,
    value_meta text,
primary key ((tenant_id, region, metric_hash), time_stamp)
);

create table monasca.alarm_state_history (
    tenant_id text,
    alarm_id text,
    metrics text,
    new_state text,
    old_state text,
    reason text,
    reason_data text,
    sub_alarms text,
    time_stamp timestamp,
primary key ((tenant_id), alarm_id, time_stamp)
);

