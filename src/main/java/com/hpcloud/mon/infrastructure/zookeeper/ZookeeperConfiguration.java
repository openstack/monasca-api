package com.hpcloud.mon.infrastructure.zookeeper;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Zookeeper configuration.
 * 
 * @author Jonathan Halterman
 */
public class ZookeeperConfiguration {
  @NotEmpty public String connectString;
}
