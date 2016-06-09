/*
 * (C) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package monasca.api.app.validation;

import monasca.api.domain.model.notificationmethod.NotificationMethodType;
import monasca.api.resource.exception.Exceptions;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.List;

public class NotificationMethodValidation {

    public static void validate(NotificationMethodType type, String address, String period,
                                List<Integer> validPeriods) {
        int convertedPeriod = Validation.parseAndValidateNumber(period, "period");
        switch (type) {
            case EMAIL : {
                if (!EmailValidator.getInstance(true).isValid(address))
                    throw Exceptions.unprocessableEntity("Address %s is not of correct format", address);
                if (convertedPeriod != 0)
                    throw Exceptions.unprocessableEntity("Period can not be non zero for Email");
            } break;
            case WEBHOOK : {
                String[] schemes = {"http","https"};
                UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS | UrlValidator.ALLOW_2_SLASHES);
                if (!urlValidator.isValid(address))
                    throw Exceptions.unprocessableEntity("Address %s is not of correct format", address);
            } break;
            case PAGERDUTY : {
                if (convertedPeriod != 0)
                    throw Exceptions.unprocessableEntity("Period can not be non zero for Pagerduty");
            } break;
        }
        if (convertedPeriod != 0 && !validPeriods.contains(convertedPeriod)){
            throw Exceptions.unprocessableEntity("%d is not a valid period", convertedPeriod);
        }
    }
}
