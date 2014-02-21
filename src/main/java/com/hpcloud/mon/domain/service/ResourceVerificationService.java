package com.hpcloud.mon.domain.service;

import javax.annotation.Nullable;

/**
 * Performs verification of a resource;
 * 
 * @author Jonathan Halterman
 */
public interface ResourceVerificationService {
  /**
   * Returns whether the {@code tenantId} is the verified owner of the {@code resourceId} in the
   * {@code az}.
   * 
   * @param tenantId to verify ownership for
   * @param resourceId to verify ownership of
   * @param az of resource to verify ownership in
   * @param authToken an auth token to pass through
   * @throws NullPointerException if {@code tenantId} or {@code resourceId} are null
   * @throws IllegalArgumentException if the {@code resourceId} is not tied to the
   *           {@code secondaryResourceId} for the {@code tenantId}
   */
  boolean isVerifiedOwner(String tenantId, String resourceId, @Nullable String az,
      @Nullable String authToken);
}
