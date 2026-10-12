package com.scribl.guessing;

/** Outcome of a guess: whether it matched and how many points it scored. */
public record GuessResultDto(boolean correct, long awardedPoints) {
}