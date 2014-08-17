/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.hpcloud.mon.infrastructure.persistence.influxdb;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.common.model.metric.MetricDefinition;
import com.hpcloud.mon.domain.model.metric.MetricDefinitionRepository;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Serie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MetricDefinitionInfluxDbRepositoryImpl implements MetricDefinitionRepository {
  private static final Logger logger = LoggerFactory.getLogger
      (AlarmStateHistoryInfluxDbRepositoryImpl.class);

  private final MonApiConfiguration config;
  private final InfluxDB influxDB;

  @Inject
  public MetricDefinitionInfluxDbRepositoryImpl(MonApiConfiguration config, InfluxDB influxDB) {
    this.config = config;
    this.influxDB = influxDB;
  }

  @Override
  public List<MetricDefinition> find(String tenantId, String name, Map<String,
      String> dimensions) throws Exception {

    String namePart = name == null ? "/.*/" : Utils.SQLSanitizer.sanitize(name);

    List<MetricDefinition> metricDefinitionList = new ArrayList<>();

    // If no dimensions given, then find everything.
    if (dimensions == null || dimensions.isEmpty()) {

      // First find all time series (measurements) with all their dimensions.
      String query = String.format("select * from %1$s where tenant_id = '%2$s' limit 1",
          namePart, Utils.SQLSanitizer.sanitize(tenantId));

      logger.debug("Query string: {}", query);

      List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query,
          TimeUnit.SECONDS);

      // Group time series names under their unique set of dimensions.
      Map<TreeSet, List<String>> columnsToNamesMap = new HashMap<TreeSet, List<String>>();
      for (Serie serie : result) {
        TreeSet<String> columnsSet = new TreeSet<String>();
        for (String columnName : serie.getColumns()) {

          // Filter out everything but the dimensions.
          if (columnName.equalsIgnoreCase("time")) {
            continue;
          } else if (columnName.equalsIgnoreCase("sequence_number")) {
            continue;
          } else if (columnName.equalsIgnoreCase("value")) {
            continue;
          } else if (columnName.equalsIgnoreCase("tenant_id")) {
            continue;
          } else if (columnName.equalsIgnoreCase("region")) {
            continue;
          } else {
            columnsSet.add(columnName);
          }
        }

        if (columnsToNamesMap.containsKey(columnsSet)) {
          columnsToNamesMap.get(columnsSet).add(serie.getName());
        } else {
          List<String> nameList = new LinkedList<String>();
          nameList.add(serie.getName());
          columnsToNamesMap.put(columnsSet, nameList);
        }
      }

      // For each set of unique dimensions, issue a query for all time series with that
      // unique set of dimensions.
      for (TreeSet columnsSet : columnsToNamesMap.keySet()) {
        List nameList = columnsToNamesMap.get(columnsSet);

        String groupByPart = "";

        // Could be time series with no dimensions.
        if (columnsSet.size() > 0) {
          groupByPart = "group by " + Joiner.on(",").join(columnsSet);
        }
        String namesPart = Joiner.on(",").join(nameList);

        // Can use any aggregate function.  We chose max.
        String query2 = String.format("Select max(value) from %1$s where tenant_id = '%2$s' %3$s",
            namesPart, Utils.SQLSanitizer.sanitize(tenantId), groupByPart);

        logger.debug("Query string: {}", query2);

        List<Serie> result2 = this.influxDB.Query(this.config.influxDB.getName(), query2,
            TimeUnit.SECONDS);

        for (Serie serie : result2) {

          // Each set of points is a unique measurement definition.
          final String[] colNames = serie.getColumns();
          final List<Map<String, Object>> rows = serie.getRows();

          for (Map<String, Object> row : rows) {

            MetricDefinition metricDefinition = new MetricDefinition();
            metricDefinition.name = serie.getName();

            Map<String, String> dimMap = new HashMap<String, String>();
            // time and max(value) are always the first columns. Skip them.
            for (int i = 2; i < colNames.length; ++i) {
              Object dimValue = row.get(colNames[i]);
              if (dimValue != null) {
                dimMap.put((String) colNames[i], (String) dimValue);
              }
            }
            metricDefinition.setDimensions(dimMap);
            metricDefinitionList.add(metricDefinition);
          }
        }
      }
      // Dimensions given, so we can use dimensions to query exactly what is needed.
    } else {

      String dimsPart = Utils.WhereClauseBuilder.buildDimsPart(dimensions);

      String query = String.format("select first(value) from %1$s where tenant_id = '%2$s' " +
          "%3$s", namePart, Utils.SQLSanitizer.sanitize(tenantId), dimsPart);

      logger.debug("Query string: {}", query);

      List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query,
          TimeUnit.SECONDS);

      for (Serie serie : result) {

        MetricDefinition metricDefinition = new MetricDefinition();
        metricDefinition.name = serie.getName();
        metricDefinition.setDimensions(dimensions == null ? new HashMap<String,
            String>() : dimensions);
        metricDefinitionList.add(metricDefinition);
      }
    }

    return metricDefinitionList;
  }
}
