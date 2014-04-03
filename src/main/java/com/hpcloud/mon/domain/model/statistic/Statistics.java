package com.hpcloud.mon.domain.model.statistic;


import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulate a statistics
 */
public class Statistics {

  private List<Long> timestamp;

  public List<Long> getTimestamp() { return timestamp;}

  public void setTimestamp(List<Long> timestamp) { this.timestamp = timestamp;}

  private List<Double> average;

  public List<Double> getAverage() {
    return average;
  }

  public void setAverage(List<Double> average) {
    this.average = average;
  }

  private List<Double> min;


  public List<Double> getMin() {
    return min;
  }

  public void setMin(List<Double> min) {
    this.min = min;
  }

  private List<Double> max;

  public List<Double> getMax() {
    return max;
  }

  public void setMax(List<Double> mas) {
    this.max = mas;
  }

  private List<Double> count;

  public List<Double> getCount() {
    return count;
  }

  public void setCount(List<Double> count) {
    this.count = count;
  }

  private List<Double> sum;

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

  public void addCount(Double value) {
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

  }

}
