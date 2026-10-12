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

import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoundLifecycleIntegrationTest {

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

    private RoomStateDto state(String code, String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/rooms/{code}/state", code)
                        .header(RequestHeaders.PLAYER_TOKEN, token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), RoomStateDto.class);
    }

    @Test
    void hostStartsGame_andWordIsExposedOnlyToTheDrawer() throws Exception {
        RoomSessionDto host = createRoom("alice", 3);
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");

        // Only the host may start.
        mvc.perform(post("/api/rooms/{code}/start", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken()))
                .andExpect(status().isForbidden());

        // Host starts.
        mvc.perform(post("/api/rooms/{code}/start", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken()))
                .andExpect(status().isOk());

        RoomStateDto hostView = state(host.roomCode(), host.playerToken());
        assertThat(hostView.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(hostView.currentRoundNumber()).isEqualTo(1);
        assertThat(hostView.currentDrawerId()).isEqualTo(host.playerId());
        assertThat(hostView.secondsRemaining()).isBetween(0L, 60L);
        assertThat(hostView.roundEndsAt()).isNotNull();
        assertThat(hostView.currentWord()).isNotBlank();

        RoomStateDto bobView = state(host.roomCode(), bob.playerToken());
        assertThat(bobView.currentDrawerId()).isEqualTo(host.playerId());
        assertThat(bobView.currentWord()).isNull();
        assertThat(bobView.secondsRemaining()).isBetween(0L, 60L);
    }

    @Test
    void startRejects_aSecondStart_andSoloRoom() throws Exception {
        RoomSessionDto host = createRoom("alice", 3);
        joinRoom(host.roomCode(), "bob");
        mvc.perform(post("/api/rooms/{code}/start", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/rooms/{code}/start", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken()))
                .andExpect(status().isConflict());

        RoomSessionDto solo = createRoom("carol", 3);
        mvc.perform(post("/api/rooms/{code}/start", solo.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, solo.playerToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void startRequiresValidRoom() throws Exception {
        RoomSessionDto host = createRoom("alice", 3);
        mvc.perform(post("/api/rooms/{code}/start", "NOPE99")
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken()))
                .andExpect(status().isNotFound());
    }
}