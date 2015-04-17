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

package monasca.api.infrastructure.persistence.influxdb;

import com.google.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;

import monasca.api.ApiConfig;

public class InfluxV9RepoReader {

  private static final Logger logger = LoggerFactory.getLogger(InfluxV9RepoReader.class);

  private final String influxName;
  private final String influxUrl;
  private final String influxCreds;
  private final String influxUser;
  private final String influxPass;
  private final String baseAuthHeader;
  private final boolean gzip;

  private final CloseableHttpClient httpClient;

  @Inject
  public InfluxV9RepoReader(final ApiConfig config) {

    this.influxName = config.influxDB.getName();
    logger.debug("Influxdb database name: {}", this.influxName);

    this.influxUrl = config.influxDB.getUrl() + "/query";
    logger.debug("Influxdb URL: {}", this.influxUrl);

    this.influxUser = config.influxDB.getUser();
    this.influxPass = config.influxDB.getPassword();
    this.influxCreds = this.influxUser + ":" + this.influxPass;

    this.gzip = config.influxDB.getGzip();
    logger.debug("Influxdb gzip responses: {}", this.gzip);

    logger.debug("Setting up basic Base64 authentication");
    this.baseAuthHeader = "Basic " + new String(Base64.encodeBase64(this.influxCreds.getBytes()));

    // We inject InfluxV9RepoReader as a singleton. So, we must share connections safely.
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(config.influxDB.getMaxHttpConnections());

    if (this.gzip) {

      logger.debug("Setting up gzip responses from Influxdb");

      this.httpClient =
          HttpClients.custom().setConnectionManager(cm)
              .addInterceptorFirst(new HttpRequestInterceptor() {

                public void process(final HttpRequest request, final HttpContext context)
                    throws HttpException, IOException {
                  if (!request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                  }
                }
              }).addInterceptorFirst(new HttpResponseInterceptor() {

            public void process(final HttpResponse response, final HttpContext context)
                throws HttpException, IOException {
              HttpEntity entity = response.getEntity();
              if (entity != null) {
                Header ceheader = entity.getContentEncoding();
                if (ceheader != null) {
                  HeaderElement[] codecs = ceheader.getElements();
                  for (int i = 0; i < codecs.length; i++) {
                    if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                      response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                      return;
                    }
                  }
                }
              }
            }
          }).build();

    } else {

      logger.debug("Setting up non-gzip responses from Influxdb");

      this.httpClient = HttpClients.custom().setConnectionManager(cm).build();

    }
  }

  protected String read(final String query) throws Exception {

    HttpGet request = new HttpGet(this.influxUrl + "?q=" + URLEncoder.encode(query, "UTF-8")
                                  + "&db=" + URLEncoder.encode(this.influxName, "UTF-8"));

    request.addHeader("content-type", "application/json");
    request.addHeader("Authorization", this.baseAuthHeader);

    try {

      logger.debug("Sending query {} to influx database {} at {}", query, this.influxName,
                   this.influxUrl);

      HttpResponse response = this.httpClient.execute(request);

      int rc = response.getStatusLine().getStatusCode();

      logger.debug("Received {} status code from influx database {} at {}", rc, this.influxName,
                   this.influxUrl);

      if (rc != HttpStatus.SC_OK) {

        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity, "UTF-8");
        logger
            .error("Failed to query influx database {} at {}: {}", this.influxName, this.influxUrl,
                   String.valueOf(rc));
        logger.error("Http response: {}", responseString);

        throw new Exception(rc + ":" + responseString);
      }

      logger
          .debug("Successfully queried influx database {} at {}", this.influxName, this.influxUrl);

      HttpEntity entity = response.getEntity();
      return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;

    } finally {

      request.releaseConnection();

    }
  }
}
