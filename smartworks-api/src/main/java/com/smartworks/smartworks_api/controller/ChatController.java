package com.smartworks.smartworks_api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartworks.smartworks_api.dto.ChatRequest;
import com.smartworks.smartworks_api.dto.ChatResponse;
import com.smartworks.smartworks_api.service.ChatService;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(
            @RequestBody ChatRequest req,
            Authentication authentication
    ) {
        return chatService.handle(req, authentication);
    }
}