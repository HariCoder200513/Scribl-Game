package com.scribl.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {

    Optional<Round> findByRoomIdAndRoundNumber(Long roomId, int roundNumber);

    List<Round> findByRoomIdOrderByRoundNumberAsc(Long roomId);

    @Query("select r.word from Round r where r.roomId = :roomId")
    List<String> findWordsByRoomId(@Param("roomId") Long roomId);
}