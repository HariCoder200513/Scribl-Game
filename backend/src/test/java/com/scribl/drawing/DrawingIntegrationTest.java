package com.scribl.drawing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scribl.common.RequestHeaders;
import com.scribl.room.CreateRoomRequest;
import com.scribl.room.JoinRoomRequest;
import com.scribl.room.RoomSessionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DrawingIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    private RoomSessionDto createRoom(String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRoomRequest(name, 3))))
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

    private void append(String code, String token, AppendDrawingRequest request) throws Exception {
        mvc.perform(post("/api/rooms/{code}/drawing", code)
                        .header(RequestHeaders.PLAYER_TOKEN, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void drawerAppendsStrokes_clear_writesIncrementalReads() throws Exception {
        RoomSessionDto host = createRoom("alice");
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");
        start(host.roomCode(), host.playerToken());

        // LOBBY-time drawing was rejected before start.
        append(host.roomCode(), host.playerToken(), point(1f, 1f));
        append(host.roomCode(), host.playerToken(), point(2f, 2f));
        append(host.roomCode(), host.playerToken(), new AppendDrawingRequest("CLEAR", null, null, null, null, null));
        long lastSeq = appendAndReadSeq(host, point(3f, 3f));
        assertThat(lastSeq).isEqualTo(4L);

        // Full replay.
        mvc.perform(get("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sinceSeq").value(0))
                .andExpect(jsonPath("$.points", hasSize(4)))
                .andExpect(jsonPath("$.points[0].seq").value(1))
                .andExpect(jsonPath("$.points[0].action").value("POINT"))
                .andExpect(jsonPath("$.points[0].x").value(1.0))
                .andExpect(jsonPath("$.points[2].seq").value(3))
                .andExpect(jsonPath("$.points[2].action").value("CLEAR"))
                .andExpect(jsonPath("$.points[2].x").doesNotExist())
                .andExpect(jsonPath("$.points[3].seq").value(4));

        // Incremental reads from high-water marks.
        mvc.perform(get("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken())
                        .param("sinceSeq", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sinceSeq").value(2))
                .andExpect(jsonPath("$.points", hasSize(2)))
                .andExpect(jsonPath("$.points[0].seq").value(3))
                .andExpect(jsonPath("$.points[1].seq").value(4));

        mvc.perform(get("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken())
                        .param("sinceSeq", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(0)));
    }

    private long appendAndReadSeq(RoomSessionDto session, AppendDrawingRequest request) throws Exception {
        MvcResult result = mvc.perform(post("/api/rooms/{code}/drawing", session.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, session.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), DrawingAppendedDto.class).seq();
    }

    private AppendDrawingRequest point(float x, float y) {
        return new AppendDrawingRequest("POINT", (double) x, (double) y, "#FF0000", 4, "BRUSH");
    }

    @Test
    void onlyTheCurrentDrawerMayDraw() throws Exception {
        RoomSessionDto host = createRoom("alice");
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");

        // Not started yet.
        mvc.perform(post("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(point(1f, 1f))))
                .andExpect(status().isForbidden());

        start(host.roomCode(), host.playerToken());

        mvc.perform(post("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(point(1f, 1f))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidRequests_unknownRooms_andForeignMembers() throws Exception {
        RoomSessionDto host = createRoom("alice");
        RoomSessionDto bob = joinRoom(host.roomCode(), "bob");
        RoomSessionDto outsider = createRoom("zoe");

        // Unknown room drawing.
        mvc.perform(post("/api/rooms/{code}/drawing", "NOPE99")
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(point(1f, 1f))))
                .andExpect(status().isNotFound());

        // Bad bodies are rejected only while drawing is otherwise allowed.
        start(host.roomCode(), host.playerToken());

        // Unknown action / missing geometry.
        mvc.perform(post("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"PAN\",\"x\":1,\"y\":1}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"POINT\",\"color\":\"#000\"}"))
                .andExpect(status().isBadRequest());

        // Malformed JSON.
        mvc.perform(post("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, host.playerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());

        // Member of another room cannot read the drawing.
        mvc.perform(get("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, outsider.playerToken()))
                .andExpect(status().isUnauthorized());

        // Missing token entirely is not authenticated.
        mvc.perform(get("/api/rooms/{code}/drawing", host.roomCode()))
                .andExpect(status().isUnauthorized());

        // A round number that does not exist.
        mvc.perform(get("/api/rooms/{code}/drawing", host.roomCode())
                        .header(RequestHeaders.PLAYER_TOKEN, bob.playerToken())
                        .param("roundNumber", "7"))
                .andExpect(status().isNotFound());
    }
}