/*
 * Copyright (c) 2014 Hewlett-Packard Development Company, L.P.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package monasca.api.domain.service.impl;

import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import monasca.api.domain.exception.EntityNotFoundException;
import monasca.api.domain.model.version.Version;
import monasca.api.domain.model.version.Version.VersionStatus;
import monasca.api.domain.model.version.VersionRepo;

/**
 * Version repository implementation.
 */
public class VersionRepoImpl implements VersionRepo {
  private static final Version v2_0 = new Version("v2.0", VersionStatus.CURRENT, new DateTime(
      DateTimeZone.UTC));

  @Override
  public List<Version> find() {
    return Arrays.asList(v2_0);
  }

  @Override
  public Version findById(String versionId) {
    if ("v2.0".equals(versionId))
      return v2_0;
    throw new EntityNotFoundException("No version exists for %s", versionId);
  }
}
