package com.hpcloud.mon.infrastructure.persistence;

import com.google.inject.Inject;
import com.hpcloud.mon.MonApiConfiguration;
import com.hpcloud.mon.domain.model.measurement.MeasurementRepository;
import com.hpcloud.mon.domain.model.measurement.Measurements;
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

public class MeasurementInfluxDBRepositoryImpl implements MeasurementRepository {

    private static final Logger logger = LoggerFactory.getLogger(MeasurementInfluxDBRepositoryImpl.class);

    private final MonApiConfiguration config;
    private final InfluxDB influxDB;

    public static final DateTimeFormatter DATETIME_FORMATTER = ISODateTimeFormat.dateTimeNoMillis();

    @Inject
    public MeasurementInfluxDBRepositoryImpl(MonApiConfiguration config) {
        this.config = config;

        this.influxDB = InfluxDBFactory.connect(this.config.influxDB.getUrl(), this.config.influxDB.getUser(),
                this.config.influxDB.getPassword());
    }

    @Override
    public Collection<Measurements> find(String tenantId, String name, Map<String, String> dimensions, DateTime startTime, @Nullable DateTime endTime) {

        String dimWhereClause = "";
        if (dimensions != null) {
            for (String colName : dimensions.keySet()) {
                dimWhereClause += String.format(" and %1$s = '%2$s'", colName, dimensions.get(colName));

            }
        }

        String timePart = buildTimePart(startTime, endTime);
        String query = String.format("select value " +
                        "from %1$s " +
                        "where tenant_id = '%2$s' %3$s %4$s",
                name, tenantId, timePart, dimWhereClause);

        logger.debug("Query string: {}", query);

        List<Serie> result = this.influxDB.Query(this.config.influxDB.getName(), query, TimeUnit.MILLISECONDS);

        Measurements measurements = new Measurements();
        measurements.setName(name);
        measurements.setDimensions(dimensions);
        List<Object[]> valObjArryList = new LinkedList<>();
        for (Serie serie : result) {
            Object[][] valObjArry = serie.getPoints();
            for (int i = 0; i < valObjArry.length; i++) {

                Object[] objArry = new Object[3];

                // sequence_number
                objArry[0] = valObjArry[i][1];
                // time
                objArry[1] = DATETIME_FORMATTER.print((long) valObjArry[i][0]);
                ;
                // value
                objArry[2] = valObjArry[i][2];

                valObjArryList.add(objArry);
            }
        }

        measurements.setMeasurements(valObjArryList);

        return Arrays.asList(measurements);
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
}
