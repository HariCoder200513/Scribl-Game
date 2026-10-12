package com.scribl.game;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Draws the secret word for a round from a static, in-code list (V1 choice:
 * no words table, no extra state). Words already used by a room are excluded
 * so a game does not repeat a word.
 */
@Component
public class WordProvider {

    static final List<String> WORDS = List.of(
            "apple", "banana", "cherry", "lemon", "melon", "grape",
            "cat", "dog", "bird", "fish", "horse", "rabbit",
            "house", "castle", "bridge", "tower", "ladder", "fence",
            "car", "bicycle", "train", "airplane", "sailboat", "rocket",
            "sun", "moon", "star", "rainbow", "cloud", "lightning",
            "tree", "flower", "mushroom", "cactus", "leaf", "pineapple",
            "pizza", "cake", "cookie", "burger", "icecream", "donut",
            "book", "pencil", "scissors", "keyboard", "clock", "lamp",
            "hat", "sock", "shoe", "scarf", "glasses", "umbrella",
            "balloon", "gift", "candle", "kite", "drum", "guitar",
            "mountain", "volcano", "island", "waterfall", "desert", "beach",
            "spider", "butterfly", "snail", "bee", "owl", "penguin"
    );

    private final SecureRandom random = new SecureRandom();

    public String pick(Set<String> excluded) {
        List<String> usable = WORDS.stream()
                .filter(w -> !excluded.contains(w))
                .collect(Collectors.toList());
        List<String> pool = usable.isEmpty() ? WORDS : usable;
        return pool.get(random.nextInt(pool.size()));
    }
}