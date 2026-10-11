package com.scribl.room;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates short room codes from a 32-character alphabet that omits
 * confusable characters (0/O, 1/I). SecureRandom is used because the code also
 * gates access to a room; it is not just a display string.
 */
@Component
public class RoomCodeGenerator {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}