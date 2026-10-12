package com.scribl.guessing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/** One guess submission. Correct guesses end the round early and score points. */
@Entity
@Table(name = "guesses")
public class Guess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(nullable = false, length = 255)
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Guess() {
    }

    public Guess(Long roomId, Long roundId, Long playerId, String text, boolean correct) {
        this.roomId = roomId;
        this.roundId = roundId;
        this.playerId = playerId;
        this.text = text;
        this.correct = correct;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
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

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}