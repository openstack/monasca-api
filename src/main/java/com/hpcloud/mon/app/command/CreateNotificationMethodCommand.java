package com.hpcloud.mon.app.command;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotEmpty;

import com.hpcloud.mon.domain.model.notificationmethod.NotificationMethod.NotificationMethodType;
import com.hpcloud.mon.resource.exception.Exceptions;

/**
 * @author Jonathan Halterman
 */
public class CreateNotificationMethodCommand {
  @Valid @NotNull public CreateNotificationMethodInner notificationMethod;

  public static class CreateNotificationMethodInner {
    @NotEmpty @Size(min = 1, max = 250) public String name;
    @NotNull public NotificationMethodType type;
    @NotEmpty @Size(min = 1, max = 100) public String address;

    public CreateNotificationMethodInner() {
    }

    public CreateNotificationMethodInner(String name, NotificationMethodType type, String address) {
      this.name = name;
      this.type = type;
      this.address = address;
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

    public void validate() {
      int atPos = address.indexOf("@");
      int commaPos = address.indexOf(",");
      if (type == NotificationMethodType.EMAIL
          && (atPos <= 0 || atPos == address.length() - 1 || commaPos >= 0))
        throw Exceptions.unprocessableEntity("Address %s is not of correct format", address);
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
