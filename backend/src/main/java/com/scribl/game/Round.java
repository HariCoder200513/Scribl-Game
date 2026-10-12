package com.scribl.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One round of a room: who drew, the word to guess, and its lifecycle window.
 * This is the durable, auditable round history. */
@Entity
@Table(name = "rounds")
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "drawer_player_id")
    private Long drawerPlayerId;

    @Column(nullable = false, length = 64)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoundStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected Round() {
    }

    public Round(Long roomId, int roundNumber, Long drawerPlayerId, String word, RoundStatus status, Instant startedAt) {
        this.roomId = roomId;
        this.roundNumber = roundNumber;
        this.drawerPlayerId = drawerPlayerId;
        this.word = word;
        this.status = status;
        this.startedAt = startedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public Long getDrawerPlayerId() {
        return drawerPlayerId;
    }

    public String getWord() {
        return word;
    }

    public RoundStatus getStatus() {
        return status;
    }

    public void setStatus(RoundStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}