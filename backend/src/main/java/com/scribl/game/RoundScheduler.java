package com.scribl.game;

import com.scribl.room.RoomRepository;
import com.scribl.room.RoomStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Single-threaded sweep that advances rooms whose round timer expired.
 *
 * Each room is transitioned by {@link RoundService#advanceIfExpired(Long)} in
 * its own transaction under a row lock, so a room either advances exactly once
 * or is a no-op when a concurrent sweep already handled it. The optimistic-lock
 * catch below is defensive only: it makes a lost race harmless instead of
 * tripping the error handler.
 *
 * Deliberately not multi-instance safe (see ADR-0001); cross-instance round
 * coordination is a V5/V6 concern.
 */
@Component
public class RoundScheduler {

    private static final Logger log = LoggerFactory.getLogger(RoundScheduler.class);

    private final RoomRepository rooms;
    private final RoundService roundService;

    public RoundScheduler(RoomRepository rooms, RoundService roundService) {
        this.rooms = rooms;
        this.roundService = roundService;
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    public void sweepExpiredRounds() {
        List<Long> expiredIds = rooms.findExpiredIds(RoomStatus.IN_PROGRESS, Instant.now());
        for (Long roomId : expiredIds) {
            try {
                roundService.advanceIfExpired(roomId);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.debug("room {} transitioned concurrently; skipping", roomId);
            }
        }
    }
}