package com.scribl.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scribl.common.RequestHeaders;
import com.scribl.game.Round;
import com.scribl.game.RoundRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the REAL round scheduler: a room with 1-second rounds must advance
 * through all its rounds and finish without manual intervention. Rotation is
 * asserted from the durable {@code rounds} history so the test does not depend
 * on catching the exact active-round window of a fast timer.
 *
 * The context gets its own in-memory database: the suite keeps other Spring
 * contexts alive (each with a live scheduler + a shared default DB) and two
 * independent sweeps over the same rooms would make this timing test flaky.
 */
@SpringBootTest(properties = {
        "scribl.round-duration-seconds=1",
        "spring.datasource.url=jdbc:h2:mem:scribl_sched;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoundSchedulerIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    RoomRepository rooms;
    @Autowired
    RoundRepository rounds;

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
    void roomAutonomouslyAdvancesRoundsAndFinishes() throws Exception {
        RoomSessionDto host = createRoom("alice", 2);
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");
        RoomSessionDto carol = joinRoom(host.roomCode(), "carol");
        long hostId = host.playerId();

        mvc.perform(post("/api/rooms/{code}/start", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken()))
                .andExpect(status().isOk());

        RoomStateDto finalState = null;
        for (int i = 0; i < 48; i++) {
            RoomStateDto dto = state(host.roomCode(), host.playerToken());
            if (dto.status() == RoomStatus.FINISHED) {
                finalState = dto;
                break;
            }
            Thread.sleep(250);
        }

        assertThat(finalState).as("room should finish via the scheduler").isNotNull();
        assertThat(finalState.currentWord()).isNull();
        assertThat(finalState.currentDrawerId()).isNull();
        assertThat(finalState.roundEndsAt()).isNull();
        assertThat(finalState.secondsRemaining()).isNull();

        Room room = rooms.findByCode(host.roomCode()).orElseThrow();
        List<Round> history = rounds.findByRoomIdOrderByRoundNumberAsc(room.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getDrawerPlayerId()).isEqualTo(hostId);
        long roundTwoDrawer = history.get(1).getDrawerPlayerId();
        assertThat(roundTwoDrawer).isNotEqualTo(hostId);
        assertThat(bob.playerId().equals(roundTwoDrawer) || carol.playerId().equals(roundTwoDrawer)).isTrue();
        assertThat(history.get(1).getWord()).isNotEqualTo(history.get(0).getWord());
        assertThat(history).allMatch(r -> r.getStatus() == com.scribl.game.RoundStatus.ENDED);
    }
}