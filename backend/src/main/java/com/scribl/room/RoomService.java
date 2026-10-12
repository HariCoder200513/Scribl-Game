package com.scribl.room;

import com.scribl.common.ConflictException;
import com.scribl.common.NotFoundException;
import com.scribl.common.UnauthorizedException;
import com.scribl.player.Player;
import com.scribl.player.PlayerDto;
import com.scribl.player.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    public static final int MAX_PLAYERS = 8;
    public static final int DEFAULT_TOTAL_ROUNDS = 3;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final RoomRepository rooms;
    private final PlayerRepository players;
    private final RoomCodeGenerator codeGenerator;

    public RoomService(RoomRepository rooms, PlayerRepository players, RoomCodeGenerator codeGenerator) {
        this.rooms = rooms;
        this.players = players;
        this.codeGenerator = codeGenerator;
    }

    @Transactional
    public RoomSessionDto createRoom(String playerName, Integer totalRounds) {
        Room room = new Room();
        room.setCode(uniqueCode());
        room.setStatus(RoomStatus.LOBBY);
        room.setTotalRounds(totalRounds == null ? DEFAULT_TOTAL_ROUNDS : totalRounds);
        room = rooms.save(room);

        Player host = newPlayer(room.getId(), playerName, true);
        players.save(host);

        room.setHostPlayerId(host.getId());
        rooms.save(room);

        return new RoomSessionDto(room.getCode(), host.getId(), host.getToken());
    }

    @Transactional
    public RoomSessionDto joinRoom(String code, String playerName) {
        Room room = getRoomByCode(code);

        if (room.getStatus() != RoomStatus.LOBBY) {
            throw new ConflictException("room is not accepting new players");
        }
        if (players.countByRoomId(room.getId()) >= MAX_PLAYERS) {
            throw new ConflictException("room is full");
        }
        if (players.existsByRoomIdAndName(room.getId(), playerName)) {
            throw new ConflictException("that name is already taken in this room");
        }

        Player player = newPlayer(room.getId(), playerName, false);
        players.save(player);

        return new RoomSessionDto(room.getCode(), player.getId(), player.getToken());
    }

    @Transactional(readOnly = true)
    public RoomStateDto getState(String code, Player currentPlayer) {
        Room room = getRoomByCode(code);
        if (!room.getId().equals(currentPlayer.getRoomId())) {
            throw new UnauthorizedException("you are not a member of this room");
        }

        List<PlayerDto> memberDtos = players.findByRoomIdOrderByJoinedAtAsc(room.getId())
                .stream()
                .map(PlayerDto::from)
                .toList();

        boolean isDrawer = room.getCurrentDrawerId() != null
                && room.getCurrentDrawerId().equals(currentPlayer.getId());
        String word = isDrawer ? room.getCurrentWord() : null;
        Long secondsRemaining = secondsRemaining(room);

        return new RoomStateDto(
                room.getCode(),
                room.getStatus(),
                room.getTotalRounds(),
                room.getCurrentRoundNumber(),
                room.getHostPlayerId(),
                room.getCurrentDrawerId(),
                room.getRoundEndsAt(),
                secondsRemaining,
                word,
                memberDtos);
    }

    private Long secondsRemaining(Room room) {
        if (room.getRoundEndsAt() == null) {
            return null;
        }
        return Math.max(0, Duration.between(Instant.now(), room.getRoundEndsAt()).getSeconds());
    }

    @Transactional(readOnly = true)
    public LeaderboardDto leaderboard(String code, Player currentPlayer) {
        Room room = getRoomByCode(code);
        if (!room.getId().equals(currentPlayer.getRoomId())) {
            throw new UnauthorizedException("you are not a member of this room");
        }

        List<PlayerDto> standings = players.findByRoomIdOrderByJoinedAtAsc(room.getId())
                .stream()
                .sorted(Comparator.comparingInt(Player::getScore).reversed())
                .map(PlayerDto::from)
                .toList();

        return new LeaderboardDto(room.getCode(), room.getStatus(), standings);
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = codeGenerator.generate();
            if (!rooms.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("could not allocate a unique room code");
    }

    private Player newPlayer(Long roomId, String playerName, boolean host) {
        Player player = new Player();
        player.setRoomId(roomId);
        player.setName(playerName.trim());
        player.setHost(host);
        player.setConnected(true);
        player.setToken(UUID.randomUUID().toString());
        return player;
    }

    private Room getRoomByCode(String code) {
        return rooms.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new NotFoundException("room not found"));
    }
}