package com.scribl.player;

/** Public view of a player shown in room state. */
public record PlayerDto(Long id, String name, int score, boolean isHost, boolean connected) {

    public static PlayerDto from(Player p) {
        return new PlayerDto(p.getId(), p.getName(), p.getScore(), p.isHost(), p.isConnected());
    }
}