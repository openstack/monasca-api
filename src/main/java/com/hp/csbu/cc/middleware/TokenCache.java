package com.hp.csbu.cc.middleware;

import com.google.common.cache.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class TokenCache<K,V> {

  private final LoadingCache<K,V> cache;
  private final Config appConfig = Config.getInstance();
  private AuthClientFactory factory;
  private AuthClient client;
  private static final Logger logger = LoggerFactory
    .getLogger(TokenCache.class);


  public TokenCache(final long timeToExpire, final Map<String,String> map) {
    factory = appConfig.getFactory();


    cache = CacheBuilder.newBuilder().maximumSize(10000)
      .expireAfterWrite(timeToExpire, TimeUnit.SECONDS)
      .build(new CacheLoader<K, V>() {
        public V load(K key) throws TException,ClientProtocolException {
          //return       (V) client.validateTokenForServiceEndpointV3((String)key,map);
          V value = null;
          //AuthClient client = null;
          try {
            client = factory.getClient();
            if (appConfig.getAuthVersion().equals("v2.0")) {

              value = (V) client.validateTokenForServiceEndpointV2((String) key, appConfig.getServiceIds(),
                appConfig.getEndpointIds(), appConfig.isIncludeCatalog());
            } else {
              value = (V) client.validateTokenForServiceEndpointV3((String) key, map);
            }
          }catch(Exception e) {
            //factory.discard(client);
            factory.recycle(client);
            throw e;
          }
          return value;
        }
      });
  }

  public V getToken(K key)   {
    V value = null;

    try {
     value = cache.get(key);
    } catch (ExecutionException e) {
      logger.debug("Problem retrieving key from cache: " + e.getStackTrace());
      factory.recycle(client);
    }
    return value;
  }

  public void put(K key, V value) {
    cache.put(key,value);
  }

}
