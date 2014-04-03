package com.hpcloud.mon.domain.model.statistic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;

/**
 * Created by johnderr on 4/2/14.
 */
public class StatisticsJoinRow {
  private Double sum;
  private Double count;
  private Double max;
  private Double min;
  private Double average;
  private Timestamp timeInterval;

  @JsonProperty("timestamp")
  public Timestamp getTime_interval() {
    return timeInterval;
  }

  public void setTime_interval(Timestamp timeInterval) {
    this.timeInterval = timeInterval;
  }

  public Double getSum() {
    return sum;
  }

  public void setSum(Double sum) {
    this.sum = sum;
  }

  public Double getCount() {
    return count;
  }

  public void setCount(Double count) {
    this.count = count;
  }

  public Double getMax() {
    return max;
  }

  public void setMax(Double max) {
    this.max = max;
  }

  public Double getMin() {
    return min;
  }

  public void setMin(Double min) {
    this.min = min;
  }

  public Double getAverage() {
    return average;
  }

  public void setAverage(Double average) {
    this.average = average;
  }
}
