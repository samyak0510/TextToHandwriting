package com.example.texttohandwriting.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Client for handling communication with the OpenAI API.
 * Implements the Singleton pattern to ensure only one instance is used throughout the app.
 * Configures and manages the Retrofit and OkHttp clients for API communication.
 */
public class OpenAIClient {
    private static final String BASE_URL = "https://api.openai.com/v1/";
    private static final String MODEL_GPT4O = "gpt-4o";

    private static OpenAIClient instance;
    private final OpenAIService service;

    /**
     * Private constructor for the Singleton pattern.
     * Initializes the Retrofit client with appropriate timeouts and logging.
     */
    private OpenAIClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BODY))
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(OpenAIService.class);
    }

    /**
     * Returns the singleton instance of the OpenAIClient.
     * Creates a new instance if one doesn't exist yet.
     * 
     * @return The singleton instance of OpenAIClient
     */
    public static synchronized OpenAIClient getInstance() {
        if (instance == null) {
            instance = new OpenAIClient();
        }
        return instance;
    }

    /**
     * Gets the OpenAI service interface for making API calls.
     * 
     * @return The configured OpenAIService interface
     */
    public OpenAIService getService() {
        return service;
    }

    /**
     * Creates a request object for formatting text with the GPT-4o model.
     * 
     * @param text The text to be formatted
     * @return A configured OpenAIRequest object ready to send to the API
     */
    public OpenAIRequest createFormatRequest(String text) {
        return new OpenAIRequest(MODEL_GPT4O, text);
    }
} 