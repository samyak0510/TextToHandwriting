package com.example.texttohandwriting.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit interface for OpenAI API communication.
 * Defines endpoints and request/response structures for text formatting functionality.
 */
public interface OpenAIService {
    /**
     * Sends text to OpenAI's chat completion endpoint for formatting.
     * 
     * @param authHeader The authorization header containing the API key in format "Bearer YOUR_API_KEY"
     * @param request The request body containing model, messages, and temperature settings
     * @return A Call object containing the formatted text response
     */
    @POST("chat/completions")
    Call<OpenAIResponse> formatText(
            @Header("Authorization") String authHeader,
            @Body OpenAIRequest request
    );
} 