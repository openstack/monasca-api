package com.hpcloud.mon.infrastructure.zookeeper;

import io.dropwizard.util.Duration;

import javax.inject.Inject;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.client.ZooKeeperSaslClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hpcloud.mon.domain.service.LockService;
import com.hpcloud.util.Exceptions;

/**
 * Zookeeper based distributed lock service implementation.
 * 
 * @author Jonathan Halterman
 */
public class ZookeeperLockService implements LockService {
  private static final Logger LOG = LoggerFactory.getLogger(ZookeeperLockService.class);
  private final CuratorFramework curator;

  static {
    System.setProperty("zookeeper.authProvider.1",
        "org.apache.zookeeper.server.auth.SASLAuthenticationProvider");
    System.setProperty(ZooKeeperSaslClient.LOGIN_CONTEXT_NAME_KEY, "Client");
  }

  @Inject
  public ZookeeperLockService(ZookeeperConfiguration config) {
    RetryPolicy retryPolicy = new ExponentialBackoffRetry(10000, 3);
    curator = CuratorFrameworkFactory.newClient(config.connectString, retryPolicy);
  }

  @Override
  public DistributedLock acquire(final String resourceId, Duration waitTime) {
    final InterProcessLock lock = new InterProcessSemaphoreMutex(curator, resourceId);
    try {
      if (lock.acquire(waitTime.getQuantity(), waitTime.getUnit())) {
        LOG.debug("Distributed lock acquired for {}", resourceId);
        return new DistributedLock() {
          @Override
          public void release() {
            try {
              lock.release();
              LOG.debug("Distributed lock released for {}", resourceId);
            } catch (Exception e) {
              throw Exceptions.uncheck(e, "Error while releasing distributed lock");
            }
          }
        };
      }
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error while acquiring distributed lock");
    }

    throw new IllegalStateException("Failed to acquire lock");
  }

  @Override
  public DistributedLock acquireIfNotExists(final String resourceId) {
    try {
      if (curator.checkExists().forPath(resourceId) == null) {
        curator.create()
            .creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(resourceId);
        LOG.debug("Distributed lock acquired for {}", resourceId);
        return new DistributedLock() {
          @Override
          public void release() {
            try {
              curator.delete().forPath(resourceId);
              LOG.debug("Distributed lock released for {}", resourceId);
            } catch (Exception e) {
              throw Exceptions.uncheck(e, "Error while releasing distributed lock");
            }
          }
        };
      }
    } catch (Exception e) {
      throw Exceptions.uncheck(e, "Error while acquiring distributed lock");
    }

    throw new IllegalStateException("Lock already exists for " + resourceId);
  }

  @Override
  public void start() throws Exception {
    LOG.info("Starting ZookeeperLockService");
    curator.start();

    // Ensure that the root path exists
    if (curator.checkExists().forPath("/") == null)
      curator.create().creatingParentsIfNeeded().forPath("/");
  }

  @Override
  public void stop() throws Exception {
    LOG.info("Stopping ZookeeperLockService");
    curator.close();
  }
}
