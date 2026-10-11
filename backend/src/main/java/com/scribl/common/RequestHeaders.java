package com.scribl.common;

/** HTTP header names used by the API. */
public final class RequestHeaders {

    /** Opaque UUID credential issued at create/join; identifies the acting player. */
    public static final String PLAYER_TOKEN = "X-Scribl-Player-Token";

    private RequestHeaders() {
    }
}