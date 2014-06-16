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

        String dimWhereClause = "";
        String dimColNames = "";
        boolean first = true;
        if (dimensions != null) {
            for (String colName : dimensions.keySet()) {
                if (first) {
                    first = false;
                } else {
                    dimWhereClause += " and";
                    dimColNames += ",";
                }
                dimWhereClause += String.format(" %1$s = '%2$s'", colName, dimensions.get(colName));
                dimColNames += colName;

            }
            if (dimWhereClause.length() > 0) {
                dimWhereClause = String.format(" and %1$s", dimWhereClause);
            }
        }
        String query = String.format("select %1$s from /.*/ where tenant_id = '%2$s' %3$s", dimColNames, tenantId, dimWhereClause);

        logger.debug("Query string: {}", query);

        List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.SECONDS);

        List<MetricDefinition> metricDefinitionList = new ArrayList<>();
        for (Serie serie : result) {

            MetricDefinition metricDefinition = new MetricDefinition();
            metricDefinition.name = serie.getName();
            metricDefinition.setDimensions(dimensions);
            metricDefinitionList.add(metricDefinition);
        }

        return metricDefinitionList;
    }
}
