package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.DirectEditRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/edit")
public class EditController {
@PostMapping("/sentence")
 public ResponseEntity<String> editSentence(@RequestBody DirectEditRequestDTO request) {
        String result = geminiService.editSentence(
                request.getFullText(),
                request.getSelectedText(),
                request.getInstruction()
        );
        return ResponseEntity.ok(result);
    }
}

