package com.scribl.drawing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One append-only drawing event. POINT events carry geometry and style; CLEAR
 * events (geometry NULL) wipe the round canvas. Clients reproduce the image by
 * replaying a round's events in seq order, fetching only what is newer than
 * their high-water mark. This is the deliberately naive V1 baseline: one row
 * per event, re-fetched wholesale per poll.
 */
@Entity
@Table(name = "drawing_points")
public class DrawingPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false)
    private long seq;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private DrawingAction action;

    @Column(name = "x")
    private Double x;

    @Column(name = "y")
    private Double y;

    @Column(name = "color", length = 9)
    private String color;

    @Column(name = "size")
    private Integer size;

    @Column(name = "tool", length = 8)
    private String tool;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DrawingPoint() {
    }

    public DrawingPoint(Long roomId, Long roundId, Long playerId, long seq, DrawingAction action,
                        Double x, Double y, String color, Integer size, String tool) {
        this.roomId = roomId;
        this.roundId = roundId;
        this.playerId = playerId;
        this.seq = seq;
        this.action = action;
        this.x = x;
        this.y = y;
        this.color = color;
        this.size = size;
        this.tool = tool;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getRoundId() {
        return roundId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public long getSeq() {
        return seq;
    }

    public DrawingAction getAction() {
        return action;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public String getColor() {
        return color;
    }

    public Integer getSize() {
        return size;
    }

    public String getTool() {
        return tool;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}