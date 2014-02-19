package com.hpcloud.mon.app.command;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * Request for a new notification method.
 * 
 * @author Jonathan Halterman
 */
public class CreateNotificationMethodCommand {
  @Valid @NotNull public CreateNotificationMethodInner notificationMethod;

  /** Actual resource values are wrapped since that's how OpenStack does it. */
  public static class CreateNotificationMethodInner {
    @NotEmpty public String name;
    @NotNull public NotificationMethodType type;
    @NotEmpty public String address;

    public CreateNotificationMethodInner() {
    }

    public CreateNotificationMethodInner(String name, NotificationMethodType type, String address) {
      this.name = name;
      this.type = type;
      this.address = address;
    }

    public void validate() {
      if (name.length() > 250)
        throw Exceptions.unprocessableEntity("Name %s must be 250 characters or less", name);
      if (address.length() > 100)
        throw Exceptions.unprocessableEntity("Address %s must be 100 characters or less", address);
      int atPos = address.indexOf("@");
      int commaPos = address.indexOf(",");
      if (type == NotificationMethodType.EMAIL
          && (atPos <= 0 || atPos == address.length() - 1 || commaPos >= 0))
        throw Exceptions.unprocessableEntity("Address %s is not of correct format", address);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      CreateNotificationMethodInner other = (CreateNotificationMethodInner) obj;
      if (address == null) {
        if (other.address != null)
          return false;
      } else if (!address.equals(other.address))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (type != other.type)
        return false;
      return true;
    }
  }

  public CreateNotificationMethodCommand() {
  }

  public CreateNotificationMethodCommand(String name, NotificationMethodType type, String address) {
    notificationMethod = new CreateNotificationMethodInner(name, type, address);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CreateNotificationMethodCommand other = (CreateNotificationMethodCommand) obj;
    if (notificationMethod == null) {
      if (other.notificationMethod != null)
        return false;
    } else if (!notificationMethod.equals(other.notificationMethod))
      return false;
    return true;
  }
}
