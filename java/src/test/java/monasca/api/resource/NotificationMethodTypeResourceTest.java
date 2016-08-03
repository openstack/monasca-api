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

package monasca.api.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Test;

import monasca.api.ApiConfig;
import monasca.api.domain.model.common.Paged;
import monasca.api.domain.model.notificationmethod.NotificationMethodType;
import monasca.api.domain.model.notificationmethod.NotificationMethodTypesRepo;
import monasca.api.infrastructure.persistence.PersistUtils;

@Test
public class NotificationMethodTypeResourceTest extends AbstractMonApiResourceTest {

  private ApiConfig config;
  NotificationMethodTypesResource  resource;

  @Override
  protected void setupResources() throws Exception {
    super.setupResources();
    config = mock(ApiConfig.class);
    config.validNotificationPeriods = Arrays.asList(0, 60);

    List<String> NOTIFICATION_METHODS = Arrays.asList("Email", "PagerDuty", "WebHook");

    NotificationMethodTypesRepo repo = mock(NotificationMethodTypesRepo.class);
    when(repo.listNotificationMethodTypes())
        .thenReturn(NOTIFICATION_METHODS);

    resource =  new NotificationMethodTypesResource(config, repo,new PersistUtils());
    addResources(resource);
  }




  private Set<String> getNotificationMethods(List<?> elements)
  {
     Set<String>  returnNotificationMethods = new TreeSet<String>();

     for ( Object p : elements){
         Map mp = (Map)p;
         NotificationMethodType m = new NotificationMethodType((String)mp.get("type"));
         returnNotificationMethods.add(m.getType());
     }
      return returnNotificationMethods;

  }

  public void shouldListCorrectNotifcationTypes() throws Exception
  {
      List<Paged> pages  =  (List<Paged>) client().resource("/v2.0/notification-methods/types").get(Paged.class).elements;

      Set<String> responseGot = getNotificationMethods(pages);
      Set<String>  expectedNotificationMethodTypes = new TreeSet<String>(Arrays.asList("EMAIL", "WEBHOOK", "PAGERDUTY"));
      assertEquals(responseGot, expectedNotificationMethodTypes);

      // Change the config to have one notification type

      NotificationMethodTypesRepo repo = mock(NotificationMethodTypesRepo.class);
      when(repo.listNotificationMethodTypes())
          .thenReturn(Arrays.asList("Email"));
      resource.repo = repo;
      pages  =  (List<Paged>) client().resource("/v2.0/notification-methods/types").get(Paged.class).elements;
      responseGot = getNotificationMethods(pages);

      expectedNotificationMethodTypes = new TreeSet<String>(Arrays.asList("EMAIL"));
      assertEquals(responseGot, expectedNotificationMethodTypes);


      // Change the config to have more than one notification type
      repo = mock(NotificationMethodTypesRepo.class);
      when(repo.listNotificationMethodTypes())
          .thenReturn(Arrays.asList("Email", "Type1", "Type2", "Type3"));
      resource.repo = repo;
      pages  =  (List<Paged>) client().resource("/v2.0/notification-methods/types").get(Paged.class).elements;

      responseGot = getNotificationMethods(pages);
      expectedNotificationMethodTypes = new TreeSet<String>(Arrays.asList("EMAIL", "TYPE1", "TYPE2", "TYPE3"));
      assertEquals(responseGot, expectedNotificationMethodTypes);


  }

}
