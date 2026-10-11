package com.scribl.player;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByToken(String token);

    List<Player> findByRoomIdOrderByJoinedAtAsc(Long roomId);

    boolean existsByRoomIdAndName(Long roomId, String name);

    long countByRoomId(Long roomId);
}