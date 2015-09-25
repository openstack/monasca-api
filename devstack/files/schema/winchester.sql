/*
* (C) Copyright 2015 Hewlett Packard Enterprise Development Company LP
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
* implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

DROP DATABASE IF EXISTS `winchester`;
CREATE DATABASE IF NOT EXISTS `winchester`;
USE `winchester`;

/*
 * To require ssl connections add 'REQUIRE SSL' to the end of all grant statements
 */
GRANT ALL ON winchester.* TO 'notification'@'%' IDENTIFIED BY 'password';
GRANT ALL ON winchester.* TO 'notification'@'localhost' IDENTIFIED BY 'password';
GRANT ALL ON winchester.* TO 'monapi'@'%' IDENTIFIED BY 'password';
GRANT ALL ON winchester.* TO 'monapi'@'localhost' IDENTIFIED BY 'password';
GRANT ALL ON winchester.* TO 'thresh'@'%' IDENTIFIED BY 'password';
GRANT ALL ON winchester.* TO 'thresh'@'localhost' IDENTIFIED BY 'password';


CREATE TABLE alembic_version (
    version_num VARCHAR(32) NOT NULL
);

-- Running upgrade None -> 3ab6d7bf80cd

CREATE TABLE event_type (
    id INTEGER NOT NULL AUTO_INCREMENT,
    `desc` VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE (`desc`)
);

CREATE TABLE event (
    id INTEGER NOT NULL AUTO_INCREMENT,
    message_id VARCHAR(50),
    generated DECIMAL(20, 6),
    event_type_id INTEGER,
    PRIMARY KEY (id),
    FOREIGN KEY(event_type_id) REFERENCES event_type (id),
    UNIQUE (message_id)
);

CREATE TABLE trait (
    event_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    type INTEGER,
    t_string VARCHAR(255),
    t_float FLOAT,
    t_int INTEGER,
    t_datetime DECIMAL(20, 6),
    PRIMARY KEY (event_id, name),
    FOREIGN KEY(event_id) REFERENCES event (id)
);

INSERT INTO alembic_version (version_num) VALUES ('3ab6d7bf80cd');

-- Running upgrade 3ab6d7bf80cd -> 44289d1492e6

CREATE TABLE stream (
    id INTEGER NOT NULL AUTO_INCREMENT,
    first_event DECIMAL(20, 6) NOT NULL,
    last_event DECIMAL(20, 6) NOT NULL,
    expire_timestamp DECIMAL(20, 6),
    fire_timestamp DECIMAL(20, 6),
    name VARCHAR(255) NOT NULL,
    state INTEGER NOT NULL,
    state_serial_no INTEGER NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX ix_stream_expire_timestamp ON stream (expire_timestamp);

CREATE INDEX ix_stream_fire_timestamp ON stream (fire_timestamp);

CREATE INDEX ix_stream_name ON stream (name);

CREATE INDEX ix_stream_state ON stream (state);

CREATE TABLE dist_trait (
    stream_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    type INTEGER,
    dt_string VARCHAR(255),
    dt_float FLOAT,
    dt_int INTEGER,
    dt_datetime DECIMAL(20, 6),
    dt_timerange_begin DECIMAL(20, 6),
    dt_timerange_end DECIMAL(20, 6),
    PRIMARY KEY (stream_id, name),
    FOREIGN KEY(stream_id) REFERENCES stream (id)
);

CREATE INDEX ix_dist_trait_dt_datetime ON dist_trait (dt_datetime);

CREATE INDEX ix_dist_trait_dt_float ON dist_trait (dt_float);

CREATE INDEX ix_dist_trait_dt_int ON dist_trait (dt_int);

CREATE INDEX ix_dist_trait_dt_string ON dist_trait (dt_string);

CREATE INDEX ix_dist_trait_dt_timerange_begin ON dist_trait (dt_timerange_begin);

CREATE INDEX ix_dist_trait_dt_timerange_end ON dist_trait (dt_timerange_end);

CREATE TABLE streamevent (
    stream_id INTEGER NOT NULL,
    event_id INTEGER NOT NULL,
    PRIMARY KEY (stream_id, event_id),
    FOREIGN KEY(event_id) REFERENCES event (id),
    FOREIGN KEY(stream_id) REFERENCES stream (id)
);

CREATE INDEX ix_event_generated ON event (generated);

CREATE INDEX ix_event_message_id ON event (message_id);

CREATE INDEX ix_event_type_id ON event (event_type_id);

CREATE INDEX ix_trait_t_datetime ON trait (t_datetime);

CREATE INDEX ix_trait_t_float ON trait (t_float);

CREATE INDEX ix_trait_t_int ON trait (t_int);

CREATE INDEX ix_trait_t_string ON trait (t_string);

UPDATE alembic_version SET version_num='44289d1492e6';
