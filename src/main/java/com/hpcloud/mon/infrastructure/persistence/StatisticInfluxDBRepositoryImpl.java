package com.hpcloud.mon.infrastructure.persistence;

import com.google.inject.Inject;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.domain.model.statistic.StatisticRepository;
import com.hpcloud.mon.domain.model.statistic.Statistics;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Serie;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class StatisticInfluxDBRepositoryImpl implements StatisticRepository {

    private static final Logger logger = LoggerFactory.getLogger(StatisticInfluxDBRepositoryImpl.class);

    private final MonApiConfiguration config;
    private final InfluxDB influxDB;

    public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis();

    @Inject
    public StatisticInfluxDBRepositoryImpl(MonApiConfiguration config) {
        this.config = config;

        this.influxDB = InfluxDBFactory.connect(this.config.influxDB.getUrl(), this.config.influxDB.getUser(),
                this.config.influxDB.getPassword());

    }

    @Override
    public List<Statistics> find(String tenantId, String name, Map<String, String> dimensions,
                                 DateTime startTime, @Nullable DateTime endTime,
                                 List<String> statistics, int period) {

        String statsPart = buildStatsPart(statistics);
        String timePart = buildTimePart(startTime, endTime);
        String dimsPart = buildDimPart(dimensions);
        String periodPart = buildPeriodPart(period);

        String query = String.format("select time %1$s from %2$s where tenant_id = '%3$s' %4$s %5$s %6$s",
                statsPart, name, tenantId, timePart, dimsPart, periodPart);

        logger.debug("Query string: {}", query);

        List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);

        List<Statistics> statisticsList = new LinkedList<Statistics>();

        // Should only be one serie -- name.
        for (Serie serie : result) {
            Statistics stat = new Statistics();
            stat.setName(serie.getName());
            List<String> colNamesList = new LinkedList<>(statistics);
            colNamesList.add(0, "timestamp");
            stat.setColumns(colNamesList);
            stat.setDimensions(dimensions);
            List<List<Object>> valObjArryArry = new LinkedList<List<Object>>();
            stat.setStatistics(valObjArryArry);
            Object[][] pointsArryArry = serie.getPoints();
            for (int i = 0; i < pointsArryArry.length; i++) {
                List<Object> valObjArry = new ArrayList<>();
                // First column is always time.
                valObjArry.add(DATETIME_FORMATTER.print((long) pointsArryArry[i][0]));
                for (int j = 1; j < statistics.size() + 1; j++) {
                    valObjArry.add(pointsArryArry[i][j]);
                }
                valObjArryArry.add(valObjArry);
            }
            statisticsList.add(stat);
        }

        return statisticsList;
    }

    private String buildPeriodPart(int period) {

        String s = "";
        if (period >= 1) {
            s += String.format("group by time(%1$ds)", period);
        }

        return s;
    }

    private String buildDimPart(Map<String, String> dims) {

        String s = "";
        if (dims != null) {
            for (String colName : dims.keySet()) {
                if (s.length() > 0) {
                    s += " and";
                }
                s += String.format(" %1$s = '%2$s'", colName, dims.get(colName));
            }

            if (s.length() > 0) {
                s = " and " + s;
            }
        }
        return s;
    }

    private String buildTimePart(DateTime startTime, DateTime endTime) {

        String s = "";

        if (startTime != null) {
            s += String.format(" and time > %1$ds", startTime.getMillis() / 1000);
        }

        if (endTime != null) {
            s += String.format(" and time < %1$ds", endTime.getMillis() / 1000);
        }

        return s;
    }

    private String buildStatsPart(List<String> statistics) {

        String s = "";
        for (String statistic : statistics) {
            s += ",";
            if (statistic.trim().toLowerCase().equals("avg")) {
                s += " mean(value)";
            } else {
                s += " " + statistic + "(value)";
            }
        }

        return s;
    }
}
