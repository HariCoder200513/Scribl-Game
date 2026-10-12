package com.scribl.room;

import com.scribl.common.CurrentPlayer;
import com.scribl.game.RoundService;
import com.scribl.player.Player;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoundService roundService;

    public RoomController(RoomService roomService, RoundService roundService) {
        this.roomService = roomService;
        this.roundService = roundService;
    }

    @PostMapping
    public ResponseEntity<RoomSessionDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomSessionDto session = roomService.createRoom(request.playerName(), request.totalRounds());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PostMapping("/{code}/join")
    public RoomSessionDto joinRoom(@PathVariable String code, @Valid @RequestBody JoinRoomRequest request) {
        return roomService.joinRoom(code, request.playerName());
    }

    @GetMapping("/{code}/state")
    public RoomStateDto getState(@PathVariable String code, @CurrentPlayer Player currentPlayer) {
        return roomService.getState(code, currentPlayer);
    }

    @PostMapping("/{code}/start")
    public void start(@PathVariable String code, @CurrentPlayer Player currentPlayer) {
        roundService.start(code, currentPlayer);
    }
}