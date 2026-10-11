package com.scribl.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scribl.common.RequestHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoomControllerIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    private RoomSessionDto createRoom(String name, Integer rounds) throws Exception {
        MvcResult result = mvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(name, rounds))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), RoomSessionDto.class);
    }

    private RoomSessionDto joinRoom(String code, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/rooms/{code}/join", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinRoomRequest(name))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), RoomSessionDto.class);
    }

    @Test
    void createRoom_returnsValidSessionAndLobbyState() throws Exception {
        RoomSessionDto session = createRoom("alice", 5);

        assertThat(session.roomCode()).matches("[A-Z2-9]{6}");
        assertThat(session.playerId()).isNotNull();
        assertThat(session.playerToken()).isNotBlank();

        mvc.perform(get("/api/rooms/{code}/state", session.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, session.playerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(session.roomCode()))
                .andExpect(jsonPath("$.status").value("LOBBY"))
                .andExpect(jsonPath("$.totalRounds").value(5))
                .andExpect(jsonPath("$.currentRoundNumber").value(0))
                .andExpect(jsonPath("$.hostPlayerId").value(session.playerId()))
                .andExpect(jsonPath("$.players.length()").value(1))
                .andExpect(jsonPath("$.players[0].name").value("alice"))
                .andExpect(jsonPath("$.players[0].isHost").value(true));
    }

    @Test
    void createRoom_defaultsTotalRounds() throws Exception {
        RoomSessionDto session = createRoom("alice", null);
        mvc.perform(get("/api/rooms/{code}/state", session.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, session.playerToken()))
                .andExpect(jsonPath("$.totalRounds").value(3));
    }

    @Test
    void joinRoom_addsPlayerAndAllowsMembersToSeeState() throws Exception {
        RoomSessionDto host = createRoom("alice", null);
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");

        mvc.perform(get("/api/rooms/{code}/state", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players.length()").value(2))
                .andExpect(jsonPath("$.players[1].name").value("bob"))
                .andExpect(jsonPath("$.players[1].isHost").value(false));
    }

    @Test
    void joinRoom_isCaseInsensitiveOnRoomCode() throws Exception {
        RoomSessionDto host = createRoom("alice", null);
        RoomSessionDto bob = joinRoom(host.roomCode().toLowerCase(), "bob");
        assertThat(bob.roomCode()).isEqualTo(host.roomCode());
    }

    @Test
    void joinRoom_rejectsDuplicateName() throws Exception {
        RoomSessionDto host = createRoom("alice", null);
        joinRoom(host.roomCode(), "bob");
        mvc.perform(post("/api/rooms/{code}/join", host.roomCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"bob\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void joinRoom_rejectsWhenRoomIsFull() throws Exception {
        RoomSessionDto host = createRoom("alice", null);
        for (int i = 1; i < RoomService.MAX_PLAYERS; i++) {
            joinRoom(host.roomCode(), "player" + i);
        }
        mvc.perform(post("/api/rooms/{code}/join", host.roomCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"overflow\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void state_requiresMembership() throws Exception {
        RoomSessionDto aliceRoom = createRoom("alice", null);
        RoomSessionDto otherRoom = createRoom("carol", null);

        mvc.perform(get("/api/rooms/{code}/state", aliceRoom.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, otherRoom.playerToken()))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/rooms/{code}/state", aliceRoom.roomCode()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void state_unknownRoomReturnsNotFound() throws Exception {
        RoomSessionDto session = createRoom("alice", null);
        mvc.perform(get("/api/rooms/{code}/state", "NOPE99")
                        .header(RequestHeaders.PLAYER_TOKEN, session.playerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinRoom_unknownRoomReturnsNotFound() throws Exception {
        mvc.perform(post("/api/rooms/{code}/join", "NOPE99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"bob\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validation_rejectsBlankNamesAndInvalidRounds() throws Exception {
        mvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":\"alice\",\"totalRounds\":0}"))
                .andExpect(status().isBadRequest());

        RoomSessionDto host = createRoom("alice", null);
        mvc.perform(post("/api/rooms/{code}/join", host.roomCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerName\":null}"))
                .andExpect(status().isBadRequest());
    }
}