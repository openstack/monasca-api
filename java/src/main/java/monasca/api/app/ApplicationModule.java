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
package monasca.api.app;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

/**
 * Application layer bindings.
 */
public class ApplicationModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(MetricService.class).in(Singleton.class);
    bind(AlarmDefinitionService.class).in(Singleton.class);
    bind(AlarmService.class).in(Singleton.class);
  }
}
