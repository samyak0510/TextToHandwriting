package com.example.texttohandwriting.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for OpenAI API.
 * Constructs the request body in the format expected by OpenAI's chat completion endpoint.
 * Includes the selected model, formatting instructions, and user content to be formatted.
 */
public class OpenAIRequest {
    private String model;
    private List<Message> messages;
    private double temperature;

    /**
     * Creates a new request to format text using OpenAI.
     * 
     * @param model The OpenAI model to use (e.g., "gpt-4o")
     * @param content The user's text content to be formatted
     */
    public OpenAIRequest(String model, String content) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("system", "You are a helpful assistant that formats text to make it more readable and professional. Keep the original meaning intact but improve formatting, grammar, and structure."));
        this.messages.add(new Message("user", content));
        this.temperature = 0.7;
    }

    /**
     * Inner class representing a message in the OpenAI chat completion request.
     * Each message has a role (system, user, assistant) and content.
     */
    public static class Message {
        private String role;
        private String content;

        /**
         * Creates a new message with the specified role and content.
         * 
         * @param role The role of the message sender ("system", "user", or "assistant")
         * @param content The text content of the message
         */
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
} 