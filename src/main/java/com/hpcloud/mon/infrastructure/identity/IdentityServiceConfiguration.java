package com.hpcloud.mon.infrastructure.identity;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Jonathan Halterman
 */
public class IdentityServiceConfiguration {
  @NotEmpty public String url;
  @NotEmpty public String username;
  @NotEmpty public String password;
  @NotEmpty public String tenantId;
}