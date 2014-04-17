package com.hpcloud.mon.domain.model.measurement;

/**
 * Encapsulates a metric measurements.
 * 
 * @author Jonathan Halterman
 */
public class Measurement {
  private long id;
  private long timestamp;
  private double value;

  public Measurement() {
  }

  public Measurement(long id, long timestamp, double value) {
    this.setId(id);
    this.timestamp = timestamp;
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Measurement other = (Measurement) obj;
    if (id != other.id)
      return false;
    if (timestamp != other.timestamp)
      return false;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
      return false;
    return true;
  }

  public long getId() {
    return id;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public double getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
    long temp;
    temp = Double.doubleToLongBits(value);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setValue(double value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.format("Measurement [id=%s, timestamp=%s, value=%s]", id, timestamp, value);
  }
}
