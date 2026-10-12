package com.scribl.guessing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scribl.common.RequestHeaders;
import com.scribl.player.PlayerDto;
import com.scribl.room.CreateRoomRequest;
import com.scribl.room.JoinRoomRequest;
import com.scribl.room.LeaderboardDto;
import com.scribl.room.RoomSessionDto;
import com.scribl.room.RoomStateDto;
import com.scribl.room.RoomStatus;
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
class GuessIntegrationTest {

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

    private void start(String code, String hostToken) throws Exception {
        mvc.perform(post("/api/rooms/{code}/start", code)
                        .header(RequestHeaders.PLAYER_TOKEN, hostToken))
                .andExpect(status().isOk());
    }

    private RoomStateDto state(String code, String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/rooms/{code}/state", code)
                        .header(RequestHeaders.PLAYER_TOKEN, token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), RoomStateDto.class);
    }

    private GuessResultDto guess(String code, String token, String text) throws Exception {
        MvcResult result = mvc.perform(post("/api/rooms/{code}/guesses", code)
                        .header(RequestHeaders.PLAYER_TOKEN, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GuessRequest(text))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), GuessResultDto.class);
    }

    private LeaderboardDto leaderboard(String code, String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/rooms/{code}/leaderboard", code)
                        .header(RequestHeaders.PLAYER_TOKEN, token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), LeaderboardDto.class);
    }

    @Test
    void correctGuess_scoresEveryone_endsTheRound_andAdvances() throws Exception {
        RoomSessionDto host = createRoom("alice", 3);
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");
        start(host.roomCode(), host.playerToken());

        String word = state(host.roomCode(), host.playerToken()).currentWord();
        assertThat(word).isNotBlank();

        GuessResultDto wrong = guess(host.roomCode(), bob.playerToken(), "definitely-not-it");
        assertThat(wrong.correct()).isFalse();
        assertThat(wrong.awardedPoints()).isZero();

        GuessResultDto right = guess(host.roomCode(), bob.playerToken(), word.toUpperCase());
        assertThat(right.correct()).isTrue();
        assertThat(right.awardedPoints()).isGreaterThanOrEqualTo(100);

        RoomStateDto after = state(host.roomCode(), host.playerToken());
        assertThat(after.status()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(after.currentRoundNumber()).isEqualTo(2);
        assertThat(after.currentDrawerId()).isEqualTo(bob.playerId()); // drawer rotates

        LeaderboardDto board = leaderboard(host.roomCode(), bob.playerToken());
        assertThat(board.standings()).hasSize(2);
        PlayerDto top = board.standings().get(0);
        PlayerDto second = board.standings().get(1);
        assertThat(top.id()).isEqualTo(bob.playerId());
        assertThat(top.score()).isEqualTo((int) right.awardedPoints());
        assertThat(second.id()).isEqualTo(host.playerId());
        assertThat(second.score()).isEqualTo(GuessService.DRAWER_BONUS_POINTS);
    }

    @Test
    void correctGuess_onTheFinalRound_finishesTheRoom() throws Exception {
        RoomSessionDto host = createRoom("alice", 1);
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");
        start(host.roomCode(), host.playerToken());

        String word = state(host.roomCode(), host.playerToken()).currentWord();
        GuessResultDto right = guess(host.roomCode(), bob.playerToken(), word);
        assertThat(right.correct()).isTrue();

        RoomStateDto after = state(host.roomCode(), bob.playerToken());
        assertThat(after.status()).isEqualTo(RoomStatus.FINISHED);
        assertThat(after.currentDrawerId()).isNull();

        LeaderboardDto board = leaderboard(host.roomCode(), bob.playerToken());
        assertThat(board.status()).isEqualTo(RoomStatus.FINISHED);
        assertThat(board.standings().get(0).id()).isEqualTo(bob.playerId());
    }

    @Test
    void rejects_drawerGuesses_beforeStart_unknownRooms_andOutsiders() throws Exception {
        RoomSessionDto host = createRoom("alice", 3);
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");

        // Guessing before the game starts.
        mvc.perform(post("/api/rooms/{code}/guesses", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"cat\"}"))
                .andExpect(status().isForbidden());

        start(host.roomCode(), host.playerToken());

        // The drawer cannot guess.
        mvc.perform(post("/api/rooms/{code}/guesses", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"cat\"}"))
                .andExpect(status().isForbidden());

        // A member of a different room cannot guess here.
        RoomSessionDto outsider = createRoom("zoe", 3);
        mvc.perform(post("/api/rooms/{code}/guesses", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, outsider.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"cat\"}"))
                .andExpect(status().isUnauthorized());

        // Unknown room.
        mvc.perform(post("/api/rooms/{code}/guesses", "NOPE99")
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"cat\"}"))
                .andExpect(status().isNotFound());

        // Blank guess is rejected by validation.
        mvc.perform(post("/api/rooms/{code}/guesses", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leaderboard_requiresMembership() throws Exception {
        RoomSessionDto host = createRoom("alice", 3);
        RoomSessionDto outsider = createRoom("zoe", 3);

        mvc.perform(get("/api/rooms/{code}/leaderboard", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, outsider.playerToken()))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/rooms/{code}/leaderboard", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standings.length()").value(1));
    }
}