CREATE SCHEMA MonMetrics;

CREATE TABLE MonMetrics.Measurements (
    id AUTO_INCREMENT,
    metric_definition_id BINARY(20) NOT NULL,
    time_stamp TIMESTAMP NOT NULL,
    value FLOAT NOT NULL,
    PRIMARY KEY(id)
) PARTITION BY EXTRACT('year' FROM time_stamp)*10000 + EXTRACT('month' FROM time_stamp)*100 + EXTRACT('day' FROM time_stamp);

CREATE TABLE MonMetrics.Definitions (
    id BINARY(20) NOT NULL,
    name VARCHAR NOT NULL,
    tenant_id VARCHAR(14) NOT NULL,
    region VARCHAR NOT NULL,
    PRIMARY KEY(id),
    CONSTRAINT MetricsDefinitionsConstraint UNIQUE(id, name, tenant_id, region)
);

CREATE TABLE MonMetrics.Dimensions (
    metric_definition_id BINARY(20) NOT NULL,
    name VARCHAR NOT NULL,
    value VARCHAR NOT NULL,
    CONSTRAINT MetricsDimensionsConstraint UNIQUE(metric_definition_id, name, value)
);

CREATE PROJECTION Measurements_DBD_1_rep_MonMetrics /*+createtype(D)*/
(
 id ENCODING AUTO, 
 metric_definition_id ENCODING RLE, 
 time_stamp ENCODING DELTAVAL, 
 value ENCODING AUTO
)
AS
 SELECT id, 
        metric_definition_id, 
        time_stamp, 
        value
 FROM MonMetrics.Measurements 
 ORDER BY metric_definition_id,
          time_stamp,
          id
UNSEGMENTED ALL NODES;

CREATE PROJECTION Definitions_DBD_2_rep_MonMetrics /*+createtype(D)*/
(
 id ENCODING RLE, 
 name ENCODING AUTO,
 tenant_id ENCODING RLE, 
 region ENCODING RLE
)
AS
 SELECT id, 
        name, 
        tenant_id, 
        region
 FROM MonMetrics.Definitions 
 ORDER BY id,
          tenant_id,
          region,
          name
UNSEGMENTED ALL NODES;

CREATE PROJECTION Dimensions_DBD_4_rep_MonMetrics /*+createtype(D)*/
(
 metric_definition_id ENCODING RLE, 
 name ENCODING AUTO, 
 value ENCODING AUTO
)
AS
 SELECT metric_definition_id, 
        name, 
        value
 FROM MonMetrics.Dimensions 
 ORDER BY metric_definition_id,
          name
UNSEGMENTED ALL NODES;

select refresh('MonMetrics.Measurements, MonMetrics.Definitions, MonMetrics.Dimensions');