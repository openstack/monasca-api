/*
 * (C) Copyright 2016 Hewlett Packard Enterprise Development LP
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

import monasca.api.resource.exception.Exceptions;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.RegexValidator;
import org.apache.commons.validator.routines.UrlValidator;

import java.util.List;

public class NotificationMethodValidation {
    private static final String[] SCHEMES = {"http","https"};
    // Allow QA to use the TLD .test. This is valid according to RFC-2606
    // The UrlValidator does not take the port off of the authority so have to handle that
    private static final RegexValidator TEST_TLD_VALIDATOR = new RegexValidator(".+\\.test(:[0-9]+)?$");
    private static final UrlValidator URL_VALIDATOR =
              new UrlValidator(SCHEMES,
                               TEST_TLD_VALIDATOR,
                               UrlValidator.ALLOW_LOCAL_URLS | UrlValidator.ALLOW_2_SLASHES);

    public static void validate(String type, String address, int period,
                                List<Integer> validPeriods) {

        if (type.equals("EMAIL")) {
                if (!EmailValidator.getInstance(true).isValid(address))
                    throw Exceptions.unprocessableEntity("Address %s is not of correct format", address);
            }
        if (type.equals("WEBHOOK")) {
            if (!URL_VALIDATOR.isValid(address))
                 throw Exceptions.unprocessableEntity("Address %s is not of correct format", address);
            if (period != 0 && !validPeriods.contains(period)){
                 throw Exceptions.unprocessableEntity("%d is not a valid period", period);
            }
        }
        if (period != 0 && !type.equals("WEBHOOK")){
               throw Exceptions.unprocessableEntity("Period can not be non zero for %s", type);
        }

    }


}
