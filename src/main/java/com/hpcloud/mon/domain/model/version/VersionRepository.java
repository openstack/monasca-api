package com.hpcloud.mon.domain.model.version;

import java.util.List;

import com.hpcloud.mon.domain.exception.EntityNotFoundException;

/**
 * Repository for versions.
 * 
 * @author Jonathan Halterman
 */
public interface VersionRepository {
  List<Version> find();

  /**
   * @throws EntityNotFoundException a version cannot be found for the {@code versionId}
   */
  Version findById(String versionId);
}
