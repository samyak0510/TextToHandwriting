package com.example.texttohandwriting;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;

/**
 * Activity displayed after font generation or as the main menu when fonts exist.
 * Provides navigation to the notes editor, font manager, and back to the drawing activity.
 * Handles the recently generated font file path if coming from the font generation process.
 */
public class AfterGenerationActivity extends AppCompatActivity {

    private String ttfPath;
    private Button btnBack;

    /**
     * Initializes the activity, sets up UI components, and configures navigation buttons.
     * Receives and processes font file path if provided from previous activity.
     * Sets up click listeners for navigation to other activities.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_after_generation);

        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(AfterGenerationActivity.this, DrawingActivity.class));
        });

        ttfPath = getIntent().getStringExtra("ttfPath");
        if (ttfPath != null) {

            Toast.makeText(this, "TTF file received.", Toast.LENGTH_LONG).show();
        }

        // Show dialog to ask for a file name

        // Setup UI
        findViewById(R.id.cardNotes).setOnClickListener(v -> {
            // Launch NotesActivity when the "Notes" card is tapped.
            startActivity(new Intent(AfterGenerationActivity.this, NotesActivity.class));
        });

        Button btnFontManager = findViewById(R.id.fontManagerButton);
        btnFontManager.setOnClickListener(v -> {
            // Launch FontManagerActivity.
            startActivity(new Intent(AfterGenerationActivity.this, FontManagerActivity.class));
        });
    }

}