package com.scribl.guessing;

import com.scribl.common.CurrentPlayer;
import com.scribl.player.Player;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class GuessController {

    private final GuessService guessService;

    public GuessController(GuessService guessService) {
        this.guessService = guessService;
    }

    @PostMapping("/{code}/guesses")
    public GuessResultDto guess(@PathVariable String code,
                                @Valid @RequestBody GuessRequest request,
                                @CurrentPlayer Player guesser) {
        return guessService.guess(code, request, guesser);
    }
}