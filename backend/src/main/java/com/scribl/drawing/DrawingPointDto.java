package com.scribl.drawing;

/** One drawing event as exposed to room members. */
public record DrawingPointDto(
        long seq,
        String action,
        Double x,
        Double y,
        String color,
        Integer size,
        String tool,
        Long playerId
) {
    public static DrawingPointDto from(DrawingPoint p) {
        return new DrawingPointDto(p.getSeq(), p.getAction().name(), p.getX(), p.getY(),
                p.getColor(), p.getSize(), p.getTool(), p.getPlayerId());
    }
}