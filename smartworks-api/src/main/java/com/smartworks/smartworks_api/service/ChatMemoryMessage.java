package com.smartworks.smartworks_api.service;

public class ChatMemoryMessage {
    private final String role;
    private final String content;

    public ChatMemoryMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}