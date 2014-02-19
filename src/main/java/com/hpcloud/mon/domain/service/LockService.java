package com.hpcloud.mon.domain.service;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.util.Duration;

/**
 * Distributed resource lock service.
 * 
 * @author Jonathan Halterman
 */
public interface LockService extends Managed {
  public interface DistributedLock {
    /**
     * Releases the distributed lock. Should be called within a finally block of any work performed
     * immediately after acquiring the lock.
     * 
     * @throws RuntimeException if an error occurs while releasing the lock
     */
    void release();
  }

  /**
   * Acquires a distributed lock for the {@code resourceId}, waiting for the {@code waitTime} before
   * giving up.
   * 
   * @throws IllegalStateException if the lock could not be acquired within the {@code waitTime}
   * @throws RuntimeException if an error occurs while acquiring the lock
   */
  DistributedLock acquire(String resourceId, Duration waitTime);

  /**
   * Acquires a distributed lock for the {@code resourceId} if it does not currently exist, throwing
   * IllegalStateException if it does already exist.
   * 
   * @throws IllegalStateException if the lock already exists for the {@code resourceId}
   * @throws RuntimeException if an error occurs while acquiring the lock
   */
  DistributedLock acquireIfNotExists(String resourceId);
}
