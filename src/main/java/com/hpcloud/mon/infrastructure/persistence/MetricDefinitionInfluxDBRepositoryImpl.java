/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpcloud.mon.infrastructure.persistence;

import com.google.inject.Inject;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MetricDefinitionInfluxDBRepositoryImpl implements MetricDefinitionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AlarmStateHistoryInfluxDBRepositoryImpl.class);

    private final MonApiConfiguration config;
    private final InfluxDB influxDB;

    @Inject
    public MetricDefinitionInfluxDBRepositoryImpl(MonApiConfiguration config) {
        this.config = config;
        this.influxDB = InfluxDBFactory.connect(this.config.influxDB.getUrl(), this.config.influxDB.getUser(),
                this.config.influxDB.getPassword());
    }

    @Override
    public List<MetricDefinition> find(String tenantId, String name, Map<String, String> dimensions) {

        String dimWhereClause = buildDimWherePart(dimensions);

        // name is not used in the query.
        String query = String.format("select first(value) from /.*/ where tenant_id = '%1$s' %2$s", tenantId, dimWhereClause);

        logger.debug("Query string: {}", query);

        List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.SECONDS);

        List<MetricDefinition> metricDefinitionList = new ArrayList<>();
        for (Serie serie : result) {

            MetricDefinition metricDefinition = new MetricDefinition();
            metricDefinition.name = serie.getName();
            metricDefinition.setDimensions(dimensions == null ? new HashMap<String, String>() : dimensions);
            metricDefinitionList.add(metricDefinition);
        }

        return metricDefinitionList;
    }

    private String buildDimWherePart(Map<String, String> dimensions) {

        String dimWhereClause = "";
        boolean first = true;
        if (dimensions != null) {
            for (String colName : dimensions.keySet()) {
                if (first) {
                    first = false;
                } else {
                    dimWhereClause += " and";
                }
                dimWhereClause += String.format(" %1$s = '%2$s'", colName, dimensions.get(colName));

            }
            if (dimWhereClause.length() > 0) {
                dimWhereClause = String.format(" and %1$s", dimWhereClause);
            }
        }
        return dimWhereClause;
    }
}
