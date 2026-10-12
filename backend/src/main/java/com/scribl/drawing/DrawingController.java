package com.scribl.drawing;

import com.scribl.common.CurrentPlayer;
import com.scribl.player.Player;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class DrawingController {

    private final DrawingService drawingService;

    public DrawingController(DrawingService drawingService) {
        this.drawingService = drawingService;
    }

    /** Current drawer appends a stroke or canvas clear to the active round. */
    @PostMapping("/{code}/drawing")
    public ResponseEntity<DrawingAppendedDto> append(@PathVariable String code,
                                                     @Valid @RequestBody AppendDrawingRequest request,
                                                     @CurrentPlayer Player drawer) {
        DrawingAppendedDto appended = drawingService.append(code, request, drawer);
        return ResponseEntity.status(HttpStatus.CREATED).body(appended);
    }

    /** Any room member fetches events newer than {@code sinceSeq}, in seq order. */
    @GetMapping("/{code}/drawing")
    public RoundDrawingDto fetch(@PathVariable String code,
                                 @RequestParam(required = false) Integer roundNumber,
                                 @RequestParam(required = false) Long sinceSeq,
                                 @CurrentPlayer Player member) {
        return drawingService.fetch(code, member, roundNumber, sinceSeq);
    }
}