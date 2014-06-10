package com.hp.csbu.cc.middleware;

import org.apache.commons.collections4.map.PassiveExpiringMap;
/**
 * Created by johnderr on 6/9/14.
 */
public class TokenCache<K,V> {
  private PassiveExpiringMap<K,V> map;

  public TokenCache(Long timeToExpire) {
    map = new PassiveExpiringMap<>(timeToExpire);
  }

  public V getToken(K key) {

    return map.get(key);
  }

  public void put(K key, V value) {
    map.put(key,value);
  }
}
