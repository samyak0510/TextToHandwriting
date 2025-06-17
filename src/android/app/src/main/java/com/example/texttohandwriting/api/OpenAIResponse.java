package com.example.texttohandwriting.api;

import java.util.List;

/**
 * Response model for OpenAI API.
 * Parses the response received from OpenAI's chat completion endpoint.
 * Provides methods to extract the formatted text from the complex response structure.
 */
public class OpenAIResponse {
    private List<Choice> choices;

    /**
     * Extracts the formatted text from the OpenAI API response.
     * 
     * @return The formatted text from the first choice in the response, or an empty string if no valid response is available
     */
    public String getFormattedText() {
        if (choices != null && !choices.isEmpty() && choices.get(0).message != null) {
            return choices.get(0).message.content;
        }
        return "";
    }

    /**
     * Inner class representing a single choice/response from the OpenAI API.
     * Each choice contains a message with the AI-generated content.
     */
    public static class Choice {
        private Message message;
    }

    /**
     * Inner class representing a message in the OpenAI response.
     * Contains the actual formatted text content.
     */
    public static class Message {
        private String content;
    }
} 