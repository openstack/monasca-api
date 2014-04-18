package com.hpcloud.mon.domain.model.statistic;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulate a statistics
 */
public class Statistics {

  private Double average;
  private Long count;
  private Double min;
  private Double max;

  public Double getSum() {
    return sum;
  }

  public void setSum(Double sum) {
    this.sum = sum;
  }

  private Double sum;
  private Long timestamp;

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public Double getAverage() {
    return average;
  }

  public void setAverage(Double average) {
    this.average = average;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public Double getMin() {
    return min;
  }


  public void setMin(Double min) {
    this.min = min;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public Double getMax() {
    return max;
  }

  public void setMax(Double max) {
    this.max = max;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp/1000;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }





  /*public Statistics() {
    rows = new ArrayList<Statistics>();
  } */

 /*
  @JsonProperty("timestamp")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public List<Long> getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(List<Long> timestamp) {
    this.timestamp = timestamp;
  }

  private List<Double> average;

  @JsonProperty("avg")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public List<Double> getAverage() {
    return average;
  }

  public void setAverage(List<Double> average) {
    this.average = average;
  }

  private List<Double> min;

  @JsonProperty("min")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public List<Double> getMin() {
    return min;
  }

  public void setMin(List<Double> min) {
    this.min = min;
  }

  private List<Double> max;

  @JsonProperty("max")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public List<Double> getMax() {
    return max;
  }

  public void setMax(List<Double> mas) {
    this.max = mas;
  }

  private List<Long> count;

  @JsonProperty("count")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public List<Long> getCount() {
    return count;
  }

  public void setCount(List<Long> count) {
    this.count = count;
  }

  private List<Double> sum;

  @JsonProperty("sum")
  @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
  public List<Double> getSum() {
    return sum;
  }

  public void addSum(Double value) {
    sum = lazyInstantiation(sum, value);
  }

  public void addAverage(Double value) {
    average = lazyInstantiation(average, value);
  }

  public void addMin(Double value) {
    min = lazyInstantiation(min, value);
  }

  public void addMax(Double value) {
    max = lazyInstantiation(max, value);
  }

  public void addCount(Long value) {
    count = lazyInstantiation(count, value);
  }

  public void addTimeStamp(Long value) {
    //need to convert from Java Timestamp to Unix Timestamp
    timestamp = lazyInstantiation(timestamp, value/1000);
  }

  public void setSum(List<Double> sum) {
    this.sum = sum;
  }
  //lazy instantiation
  private <T> List<T> lazyInstantiation(List<T> list, T value) {
    if (list == null)
      list = new ArrayList<T>();

    list.add(value);
    return list;

  }  */

}
