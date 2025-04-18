package com.example.texttohandwriting;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.texttohandwriting.api.OpenAIClient;
import com.example.texttohandwriting.api.OpenAIRequest;
import com.example.texttohandwriting.api.OpenAIResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity for creating and editing text notes with rich formatting options.
 * Provides text styling features, font selection, spacing controls, and AI-powered text formatting.
 */
public class NoteEditorActivity extends AppCompatActivity {

    private EditText etNoteTitle, etNote;
    private Spinner spinnerFonts;
    private File fontsDir, notesDir;

    private ImageButton formatBoldButton, formatItalicButton, formatUnderlineButton, formatBulletsButton;
    private EditText etLetterSpacing, etWordSpacing, etFontSize, etLineSpacing;
    private Typeface currentTypeface;
    private float currentFontSize = 16f;
    private int currentTextColor = android.graphics.Color.BLACK;
    private float currentLetterSpacing = 0f;
    private float currentWordSpacing = 0f;
    private float currentLineSpacing = 1.0f;
    private Button btnBack, btnSaveNote;

    private ImageButton btnFontColor;

    // Formatting state variables
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;

    // Text watcher for monitoring changes
    private TextWatcher formattingTextWatcher;

    // Flags to prevent infinite loops in text watchers
    private boolean isUpdatingWordSpacing = false;
    private boolean isUpdatingFormatting = false;

    private ImageButton btnGenAI;
    private static final String OPENAI_API_KEY = "YOUROPENAIKEY"; // Placeholder for OpenAI API key

    /**
     * Initializes the activity, sets up UI components, and configures event listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note_editor);

        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNote = findViewById(R.id.etNote);
        etFontSize = findViewById(R.id.etFontSize);
        spinnerFonts = findViewById(R.id.spinnerFonts);
        btnFontColor = findViewById(R.id.btnFontColor);
        etLetterSpacing = findViewById(R.id.etLetterSpacing);
        etWordSpacing = findViewById(R.id.etWordSpacing);
        etLineSpacing = findViewById(R.id.etLineSpacing);

        // Initialize formatting buttons
        formatBoldButton = findViewById(R.id.btnBold);
        formatItalicButton = findViewById(R.id.btnItalic);
        formatUnderlineButton = findViewById(R.id.btnUnderline);
        formatBulletsButton = findViewById(R.id.btnBullets);

        btnBack = findViewById(R.id.btnBack);
        btnSaveNote = findViewById(R.id.btnSaveNote);
        btnGenAI = findViewById(R.id.btnGenAI);

        // Directories for fonts and notes
        fontsDir = new File(getFilesDir(), "fonts");
        if (!fontsDir.exists()) fontsDir.mkdirs();
        notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) notesDir.mkdirs();

        loadAvailableFonts();

        // Set default font size
        etFontSize.setText("16");

        // Font size EditText listener
        etFontSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    try {
                        currentFontSize = Float.parseFloat(s.toString());
                        etNote.setTextSize(currentFontSize);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        spinnerFonts.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedFont = (String) parent.getItemAtPosition(position);
                File fontFile = new File(fontsDir, selectedFont);
                if (fontFile.exists()) {
                    currentTypeface = Typeface.createFromFile(fontFile);
                    etNote.setTypeface(currentTypeface);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnFontColor.setOnClickListener(v -> showColorPickerDialog());

        // Set up formatting button listeners - now they toggle formatting modes
        setupFormatButtons();

        // Update button backgrounds to show active state
        updateFormattingButtonsAppearance();

        etLetterSpacing.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    currentLetterSpacing = Float.parseFloat(s.toString());
                    etNote.setLetterSpacing(currentLetterSpacing);
                } catch (NumberFormatException e) {
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // Line spacing (previously called word spacing)
        etLineSpacing.setText("1.0");
        etLineSpacing.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    currentLineSpacing = Float.parseFloat(s.toString());
                    // This is actually line spacing functionality
                    etNote.setLineSpacing(0, currentLineSpacing);
                } catch (NumberFormatException e) {
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // New proper word spacing implementation
        etWordSpacing.setText("0.0");
        etWordSpacing.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    currentWordSpacing = Float.parseFloat(s.toString());
                    applyWordSpacing();
                } catch (NumberFormatException e) {
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        // Setup text watcher for handling real-time formatting of newly entered text
        setupFormattingTextWatcher();

        // If editing an existing note:
        String notePath = getIntent().getStringExtra("notePath");
        if (notePath != null) {
            File noteFile = new File(notePath);
            String content = FileUtil.readTextFromFile(noteFile);
            etNoteTitle.setText(noteFile.getName());
            etNote.setText(content);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(NoteEditorActivity.this, NotesActivity.class));
                finish();
            }
        });

        btnSaveNote.setOnClickListener(v -> {
            String noteTitle = etNoteTitle.getText().toString().trim();
            if (noteTitle.isEmpty()) {
                noteTitle = "untitled";
            }
            if (!noteTitle.toLowerCase().endsWith(".txt")) {
                noteTitle += ".txt";
            }
            File noteFile = new File(notesDir, noteTitle);
            try (FileOutputStream fos = new FileOutputStream(noteFile)) {
                fos.write(etNote.getText().toString().getBytes());
                fos.flush();
                Toast.makeText(NoteEditorActivity.this, "Note saved", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NoteEditorActivity.this, NotesActivity.class);
                startActivity(intent);
                finish();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(NoteEditorActivity.this, "Error saving note", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up AI button click listener
        btnGenAI.setOnClickListener(v -> formatTextWithAI());
    }

    /**
     * Sets up click listeners for text formatting buttons (bold, italic, underline, bullets).
     * Each button toggles its respective formatting state.
     */
    private void setupFormatButtons() {
        // Set click listeners for formatting buttons
        formatBoldButton.setOnClickListener(v -> {
            isBoldActive = !isBoldActive;
            updateFormattingButtonsAppearance();
        });

        formatItalicButton.setOnClickListener(v -> {
            isItalicActive = !isItalicActive;
            updateFormattingButtonsAppearance();
        });

        formatUnderlineButton.setOnClickListener(v -> {
            isUnderlineActive = !isUnderlineActive;
            updateFormattingButtonsAppearance();
        });

        formatBulletsButton.setOnClickListener(v -> {
            applyBulletFormatting();
        });
    }

    /**
     * Sets up text watchers to apply real-time formatting as text is entered.
     * Handles bold, italic, and underline formatting based on active formatting buttons.
     */
    private void setupFormattingTextWatcher() {
        etNote.addTextChangedListener(new TextWatcher() {
            private int startPosition;
            private int endPosition;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Check both flags to prevent updates during formatting OR word spacing changes
                if (!isUpdatingFormatting && !isUpdatingWordSpacing) {
                    // Only track positions when text is being added (count > 0), not when deleted (before > 0)
                    if (count > 0) {
                        startPosition = start;
                        endPosition = start + count;
                    } else {
                        // Reset positions when text is deleted to prevent applying formatting
                        startPosition = 0;
                        endPosition = 0;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Skip formatting if either formatting or word spacing is updating
                // Also skip if there's nothing to format or if positions indicate a deletion operation
                if (isUpdatingFormatting || isUpdatingWordSpacing || startPosition >= endPosition || editable.length() == 0) {
                    return;
                }

                // Safety check to make sure we're not going past the text boundaries
                if (endPosition > editable.length()) {
                    endPosition = editable.length();
                }

                // Skip if there's nothing to format after boundary checks
                if (startPosition >= endPosition) {
                    return;
                }

                isUpdatingFormatting = true;

                try {
                    // Apply active formatting to newly entered text
                    if (isBoldActive) {
                        editable.setSpan(new StyleSpan(Typeface.BOLD),
                                startPosition, endPosition, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    if (isItalicActive) {
                        editable.setSpan(new StyleSpan(Typeface.ITALIC),
                                startPosition, endPosition, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    if (isUnderlineActive) {
                        editable.setSpan(new UnderlineSpan(),
                                startPosition, endPosition, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } catch (Exception e) {
                    // Catch any potential exceptions to prevent app crashes
                    e.printStackTrace();
                } finally {
                    isUpdatingFormatting = false;
                }
            }
        });
    }

    /**
     * Updates the visual appearance of formatting buttons to reflect active state.
     * Changes button backgrounds to indicate which formatting options are enabled.
     */
    private void updateFormattingButtonsAppearance() {
        formatBoldButton.setActivated(isBoldActive);
        formatItalicButton.setActivated(isItalicActive);
        formatUnderlineButton.setActivated(isUnderlineActive);
        // Bullet button doesn't show toggled state as it only works on selections
    }

    /**
     * Applies bullet point formatting to the selected text.
     * If the selection already has bullet points, they are removed (toggle behavior).
     */
    private void applyBulletFormatting() {
        int selStart = etNote.getSelectionStart();
        int selEnd = etNote.getSelectionEnd();

        if (selStart < selEnd) {
            // Find the start of the line
            String text = etNote.getText().toString();
            while (selStart > 0 && text.charAt(selStart - 1) != '\n') {
                selStart--;
            }

            // Find the end of all selected lines
            while (selEnd < text.length() && text.charAt(selEnd - 1) != '\n') {
                selEnd++;
            }

            Spannable str = etNote.getText();
            BulletSpan[] spans = str.getSpans(selStart, selEnd, BulletSpan.class);

            // Check if there's already a bullet span in this selection
            boolean hasBulletSpan = spans.length > 0;
            if (hasBulletSpan) {
                for (BulletSpan span : spans) {
                    str.removeSpan(span);
                }
                Toast.makeText(this, "Bullet points removed", Toast.LENGTH_SHORT).show();
            } else {
                // Apply bullet span to each line in the selection
                String[] lines = text.substring(selStart, selEnd).split("\n");
                int lineStart = selStart;

                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        int lineEnd = lineStart + line.length();
                        str.setSpan(new BulletSpan(20, currentTextColor),
                                lineStart, lineEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    lineStart += line.length() + 1; // +1 for the newline character
                }
                Toast.makeText(this, "Bullet points applied", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please select text to format with bullets", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads available font files from the app's fonts directory.
     * Populates the font spinner with the available font options.
     */
    private void loadAvailableFonts() {
        File[] fontFiles = fontsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf"));
        List<String> fontList = new ArrayList<>();
        if (fontFiles != null) {
            for (File font : fontFiles) {
                fontList.add(font.getName());
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, fontList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFonts.setAdapter(adapter);
    }

    /**
     * Displays a color picker dialog for selecting text color.
     * Provides a grid of color options and applies the selected color to the text.
     */
    private void showColorPickerDialog() {
        // Create an array of color options (expanded from the original 7 colors)
        final int[] colorOptions = {
                android.graphics.Color.BLACK,
                android.graphics.Color.DKGRAY,
                android.graphics.Color.GRAY,
                android.graphics.Color.LTGRAY,
                android.graphics.Color.WHITE,
                android.graphics.Color.RED,
                android.graphics.Color.rgb(255, 102, 102), // Light red
                android.graphics.Color.rgb(153, 0, 0),     // Dark red
                android.graphics.Color.GREEN,
                android.graphics.Color.rgb(102, 255, 102), // Light green
                android.graphics.Color.rgb(0, 102, 0),     // Dark green
                android.graphics.Color.BLUE,
                android.graphics.Color.rgb(102, 102, 255), // Light blue
                android.graphics.Color.rgb(0, 0, 153),     // Dark blue
                android.graphics.Color.YELLOW,
                android.graphics.Color.rgb(255, 255, 153), // Light yellow
                android.graphics.Color.rgb(153, 153, 0),   // Dark yellow
                android.graphics.Color.CYAN,
                android.graphics.Color.rgb(153, 255, 255), // Light cyan
                android.graphics.Color.rgb(0, 153, 153),   // Dark cyan
                android.graphics.Color.MAGENTA,
                android.graphics.Color.rgb(255, 153, 255), // Light magenta
                android.graphics.Color.rgb(153, 0, 153),   // Dark magenta
                android.graphics.Color.rgb(255, 165, 0),   // Orange
                android.graphics.Color.rgb(255, 192, 203), // Pink
                android.graphics.Color.rgb(128, 0, 128),   // Purple
                android.graphics.Color.rgb(165, 42, 42),   // Brown
                android.graphics.Color.rgb(0, 128, 0)      // Dark green
        };

        // Inflate the color picker dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_color_picker, null);
        GridView gridColors = dialogView.findViewById(R.id.gridColors);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnApply = dialogView.findViewById(R.id.btnApply);

        // Create and set up the adapter
        ColorAdapter colorAdapter = new ColorAdapter(this, colorOptions);
        gridColors.setAdapter(colorAdapter);

        // Find the index of the current color (or closest match)
        int currentColorIndex = 0;
        int minColorDistance = Integer.MAX_VALUE;
        for (int i = 0; i < colorOptions.length; i++) {
            int distance = colorDistance(currentTextColor, colorOptions[i]);
            if (distance < minColorDistance) {
                minColorDistance = distance;
                currentColorIndex = i;
            }
        }
        colorAdapter.setSelectedPosition(currentColorIndex);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setView(dialogView).create();

        // Set up grid item click listener
        gridColors.setOnItemClickListener((parent, view, position, id) -> {
            colorAdapter.setSelectedPosition(position);
        });

        // Set up button click listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            currentTextColor = colorAdapter.getSelectedColor();
            etNote.setTextColor(currentTextColor);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Calculates the "distance" between two colors to find the closest match.
     * Used for determining the initially selected color in the color picker.
     * 
     * @param color1 First color
     * @param color2 Second color
     * @return Integer representing the color distance
     */
    private int colorDistance(int color1, int color2) {
        int r1 = android.graphics.Color.red(color1);
        int g1 = android.graphics.Color.green(color1);
        int b1 = android.graphics.Color.blue(color1);
        int r2 = android.graphics.Color.red(color2);
        int g2 = android.graphics.Color.green(color2);
        int b2 = android.graphics.Color.blue(color2);

        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }

    /**
     * Applies word spacing to the text by adjusting spaces between words.
     * Normalizes existing multi-spaces before applying the new spacing.
     * Uses flags to prevent recursive calls when modifying text.
     */
    private void applyWordSpacing() {
        Editable editable = etNote.getText();
        if (editable == null || editable.length() == 0) return;
        if (isUpdatingWordSpacing) return; // Prevent recursion

        int cursorPosition = etNote.getSelectionStart();
        String content = editable.toString();

        isUpdatingWordSpacing = true; // Set flag

        try {
            // 1. Normalize: Replace multiple spaces between non-space characters back to single spaces
            String normalizedText = content.replaceAll("(?<=[^\\s]) {2,}(?=[^\\s])", " ");

            // 2. Apply new spacing based on normalized text
            int extraSpaces = (int) Math.max(0, Math.floor(currentWordSpacing * 3)); // Adjust multiplier as needed
            String modifiedText = normalizedText; // Default to normalized if no extra spaces needed

            if (extraSpaces > 0) {
                StringBuilder spaceBuilder = new StringBuilder();
                for (int i = 0; i < extraSpaces; i++) {
                    spaceBuilder.append(" ");
                }
                String spaceString = spaceBuilder.toString();
                // Apply new spacing by replacing single spaces in the normalized text
                modifiedText = normalizedText.replaceAll("(?<=[^\\s]) (?=[^\\s])", " " + spaceString);
            }

            // Only update if the final text is different from the original editable content
            if (!editable.toString().equals(modifiedText)) {
                editable.replace(0, editable.length(), modifiedText);
                // Restore cursor position (basic adjustment, might need refinement for complex cases)
                // Calculate the *change* in length near the cursor to adjust more accurately if possible
                // For now, simple min adjustment
                int newPosition = Math.min(cursorPosition, editable.length());
                etNote.setSelection(newPosition);
            }

        } finally {
            isUpdatingWordSpacing = false; // Reset flag
        }
    }

    /**
     * Formats the note text using OpenAI's GPT-4o model.
     * Shows a progress dialog during the API call and handles success/failure responses.
     */
    private void formatTextWithAI() {
        String noteText = etNote.getText().toString().trim();

        // Check if there's text to format
        if (noteText.isEmpty()) {
            Toast.makeText(this, "Please add some text to format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Formatting text with AI...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create API request
        OpenAIClient client = OpenAIClient.getInstance();
        OpenAIRequest request = client.createFormatRequest(noteText);

        // Make API call
        client.getService().formatText("Bearer " + OPENAI_API_KEY, request)
                .enqueue(new Callback<OpenAIResponse>() {
                    @Override
                    public void onResponse(Call<OpenAIResponse> call, Response<OpenAIResponse> response) {
                        progressDialog.dismiss();

                        if (response.isSuccessful() && response.body() != null) {
                            String formattedText = response.body().getFormattedText();
                            if (!formattedText.isEmpty()) {
                                // Update note text with formatted text
                                etNote.setText(formattedText);
                                Toast.makeText(NoteEditorActivity.this, "Text formatted successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(NoteEditorActivity.this, "Couldn't format text", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            handleApiError(response);
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenAIResponse> call, Throwable t) {
                        progressDialog.dismiss();
                        Toast.makeText(NoteEditorActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Handles API error responses from the OpenAI API.
     * Parses error messages from the response body and displays appropriate alerts.
     * 
     * @param response The error response from the API call
     */
    private void handleApiError(Response<OpenAIResponse> response) {
        String errorMessage = "Error formatting text";

        try {
            if (response.errorBody() != null) {
                errorMessage = response.errorBody().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Show error to user
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();

        // If API key is empty, show specific message
        if (OPENAI_API_KEY.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("API Key Missing")
                    .setMessage("Please add your OpenAI API key to use this feature.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}