package com.scribl.room;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    /** Ids of rooms still in progress whose round timer has expired. Only ids are
     * returned so each transition runs in its own transaction on a fresh load. */
    @Query("select r.id from Room r where r.status = :status and r.roundEndsAt <= :now")
    List<Long> findExpiredIds(@Param("status") RoomStatus status, @Param("now") Instant now);

    /** Re-read a room with a row lock so a round transition happens exactly once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    /** Lock a room by code; guards seq assignment when the current drawer draws. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.code = :code")
    Optional<Room> findByCodeForUpdate(@Param("code") String code);
}