CREATE SCHEMA MonAlarms;

CREATE TABLE MonAlarms.StateHistory(
    id AUTO_INCREMENT,
    tenant_id VARCHAR,
    alarm_id VARCHAR,
    metrics VARCHAR (65000),
    old_state VARCHAR,
    new_state VARCHAR,
    sub_alarms VARCHAR (65000),
    reason VARCHAR(65000),
    reason_data VARCHAR(65000),
    time_stamp TIMESTAMP NOT NULL
) PARTITION BY EXTRACT('year' FROM time_stamp)*10000 + EXTRACT('month' FROM time_stamp)*100 + EXTRACT('day' FROM time_stamp);
