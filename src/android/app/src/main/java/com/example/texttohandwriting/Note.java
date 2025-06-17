package com.example.texttohandwriting;

/**
 * Model class representing a note in the application.
 * Contains properties for the note's identifier, title, content, and creation date.
 * Provides getters and setters for all properties.
 */
public class Note {
    private String id;
    private String title;
    private String content;
    private String date;

    /**
     * Default constructor required for data persistence.
     */
    public Note() {
        // Empty constructor needed for Firebase
    }

    /**
     * Creates a new note with the specified properties.
     * 
     * @param id Unique identifier for the note
     * @param title The title of the note
     * @param content The text content of the note
     * @param date The creation or last modified date of the note
     */
    public Note(String id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
    }

    /**
     * Gets the note's unique identifier.
     * 
     * @return The note ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the note's unique identifier.
     * 
     * @param id The note ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the note's title.
     * 
     * @return The note title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the note's title.
     * 
     * @param title The title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the note's content text.
     * 
     * @return The content of the note
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the note's content text.
     * 
     * @param content The content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the note's date (creation or last modified).
     * 
     * @return The date of the note
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the note's date.
     * 
     * @param date The date to set
     */
    public void setDate(String date) {
        this.date = date;
    }
} 