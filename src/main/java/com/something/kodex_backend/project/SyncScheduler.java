package com.something.kodex_backend.project;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncScheduler {

  private static final int syncIntervalSeconds = 20;

  private final FileSyncEngine fileSyncEngine;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final ConcurrentHashMap<Integer, ScheduledFuture<?>> activeSessions = new ConcurrentHashMap<> ();

  public void startScheduling(String accessToken, Integer projectId, String projectDriveId) {
    if(activeSessions.containsKey(projectId)) {
      log.warn("Scheduling for project {} is in progress, skipping", projectId);

      return;
    }

    ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
      () -> runPush(accessToken, projectId, projectDriveId),
      syncIntervalSeconds, // initial delay, no need to push right after a pull
      syncIntervalSeconds,
      TimeUnit.SECONDS
    );

    activeSessions.put(projectId, future);
    log.info("Scheduled sync for project {}", projectId);
  }

  public void stopScheduling(Integer projectId) {
    ScheduledFuture<?> future = activeSessions.remove(projectId);

    if(future != null) {
      future.cancel(false); // don't interrupt if a push is currently running
      log.info("Scheduled sync stopped for project {}", projectId);
    } else {
      log.warn("No active schedule found for project {}", projectId);
    }
  }

  private void runPush(String accessToken,  Integer projectId, String projectDriveId) {
    try {
      fileSyncEngine.push(accessToken, projectId, projectDriveId);
    } catch(Exception ex) {
      log.error("Scheduled push failed for project {}: {}", projectId, ex.getMessage(), ex);
    }
  }

}