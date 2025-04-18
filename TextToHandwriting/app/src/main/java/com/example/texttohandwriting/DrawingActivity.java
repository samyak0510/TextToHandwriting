package com.example.texttohandwriting;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.texttohandwriting.model.GlyphData;
import com.example.texttohandwriting.model.GlyphMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Activity for drawing handwritten glyphs to create a custom font.
 * Guides the user through drawing each character in a character set,
 * processes the drawn glyphs, and sends them to a server for conversion into a TTF font file.
 */
public class DrawingActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private TextView tvCurrentChar;
    private Button btnClear, btnNext, btnFinish, btnBack;
    private GlyphMap glyphMap = new GlyphMap();

    // Comprehensive character set
    private static final String CHAR_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,?!'\"()\\-_:;@#&$%*+=\\[\\]{}<>^~/\\\\|";
    private int currentIndex = 0;

    // Retrofit API service
    private FontApiService fontApiService;
    ProgressBar progressBar;

    private SeekBar seekBarMarkerSize;

    /**
     * Initializes the activity, sets up UI components, and configures API service.
     * Sets up drawing controls, character navigation, and font submission flow.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drawing);

        drawingView = findViewById(R.id.drawingView);
        tvCurrentChar = findViewById(R.id.tvCurrentChar);
        btnClear = findViewById(R.id.btnClear);
        btnNext = findViewById(R.id.btnNext);
        btnFinish = findViewById(R.id.btnFinish);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        seekBarMarkerSize = findViewById(R.id.seekBarMarkerSize);

        // Create an OkHttpClient with extended timeouts
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();

        // Initialize Retrofit with extended timeout, change BASE_URL as needed
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://YOURIPADDRESS:8080/")  // Replace with your server URL
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        fontApiService = retrofit.create(FontApiService.class);

        updateCurrentChar();

        btnClear.setOnClickListener(v -> drawingView.clear());

        btnNext.setOnClickListener(v -> {
            // Get the drawn bitmap from the canvas
            Bitmap drawnBitmap = drawingView.getBitmap();
            char currentChar = CHAR_SET.charAt(currentIndex);
            // Save the drawing along with its character in GlyphData
            GlyphData glyphData = new GlyphData(currentChar, drawnBitmap.copy(drawnBitmap.getConfig(), false));
            glyphMap.addGlyph(glyphData);

            currentIndex++;
            if (currentIndex < CHAR_SET.length()) {
                updateCurrentChar();
                drawingView.clear();
            } else {
                tvCurrentChar.setText("All glyphs drawn");
                btnNext.setEnabled(false);
            }
        });

        // Back button: go back one character if possible
        btnBack.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                // Optionally remove the glyph for the current character, if already saved.
                // Here we remove it from the glyphMap:
                glyphMap.getGlyphs().remove(CHAR_SET.charAt(currentIndex));
                updateCurrentChar();
            }else{
                //Go back to MainActivity
                startActivity(new Intent(DrawingActivity.this, MainActivity.class));
            }
        });

        seekBarMarkerSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /**
             * Updates the marker size when the seek bar position changes.
             * 
             * @param seekBar The seek bar whose progress has changed
             * @param progress The current progress level
             * @param fromUser True if the progress change was initiated by the user
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Set a minimum marker size of 1 pixel.
                float markerSize = progress < 1 ? 1 : progress;
                drawingView.setMarkerSize(markerSize + 65);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        btnFinish.setOnClickListener(v -> {
            // Show a dialog asking the user for the desired font name.
            AlertDialog.Builder builder = new AlertDialog.Builder(DrawingActivity.this);
            builder.setTitle("Enter Font Name");
            final EditText input = new EditText(DrawingActivity.this);
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            input.setHint("Font name (e.g., MyFont.ttf)");
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                /**
                 * Handles the confirmation of font creation.
                 * Processes the drawn glyphs and sends them to the server for TTF generation.
                 * 
                 * @param dialog The dialog that received the click
                 * @param which The button that was clicked
                 */
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String desiredFontName = input.getText().toString().trim();
                    if (desiredFontName.isEmpty()) {
                        Toast.makeText(DrawingActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Append ".ttf" if not provided.
                    if (!desiredFontName.toLowerCase().endsWith(".ttf")) {
                        desiredFontName += ".ttf";
                    }

                    // Show the progress indicator.
                    progressBar.setVisibility(View.VISIBLE);

                    // Start background processing using ExecutorService.
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    String finalDesiredFontName = desiredFontName;
                    executor.execute(() -> {
                        File zipFile = null;
                        try {
                            // Create a temporary directory to save glyph images.
                            File glyphDir = new File(getCacheDir(), "glyphs");
                            if (!glyphDir.exists()) {
                                glyphDir.mkdirs();
                            }
                            // Save each glyph as PNG (using filename: glyph_<ascii>.png).
                            for (Map.Entry<Character, GlyphData> entry : glyphMap.getGlyphs().entrySet()) {
                                char ch = entry.getKey();
                                File outFile = new File(glyphDir, "glyph_" + ((int) ch) + ".png");
                                FileUtil.saveBitmapToFile(entry.getValue().getBitmap(), outFile);
                            }
                            // Zip the directory containing all glyph images.
                            zipFile = new File(getCacheDir(), "glyphs.zip");
                            FileUtil.zipFolder(glyphDir, zipFile);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        final File finalZipFile = zipFile;

                        runOnUiThread(() -> {
                            if (finalZipFile != null) {
                                uploadFontZip(finalZipFile, finalDesiredFontName);
                            } else {
                                Toast.makeText(DrawingActivity.this, "Error creating ZIP file", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                        executor.shutdown();
                    });
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

    }

    /**
     * Uploads the zip file containing glyph images to the server for TTF generation.
     * Handles the API response, saves the TTF file, and navigates to the next activity.
     * 
     * @param zipFile The zip file containing glyph images
     * @param desiredFontName The name to give the generated font file
     */
    private void uploadFontZip(File zipFile, String desiredFontName) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("application/zip"), zipFile);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("fontZip", zipFile.getName(), requestFile);

        fontApiService.uploadFontZip(multipartBody).enqueue(new Callback<ResponseBody>() {
            /**
             * Handles the API response containing the generated TTF file.
             * Saves the file and navigates to the AfterGenerationActivity.
             */
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    try {
                        // Save the received TTF file locally.
                        File ttfFile = FileUtil.writeResponseBodyToDisk(DrawingActivity.this, response.body());

                        // Create permanent fonts directory in internal storage.
                        File fontsDir = new File(getFilesDir(), "fonts");
                        if (!fontsDir.exists()) {
                            fontsDir.mkdirs();
                        }

                        // New file with the desired font name.
                        File newFontFile = new File(fontsDir, desiredFontName);

                        // Rename (or copy) the TTF file.
                        boolean renamed = ttfFile.renameTo(newFontFile);
                        if (!renamed) {
                            // If rename fails, copy the file.
                            FileUtil.copyFile(ttfFile, newFontFile);
                            ttfFile.delete();
                        }

                        // Launch AfterGenerationActivity with the TTF path.
                        Intent intent = new Intent(DrawingActivity.this, AfterGenerationActivity.class);
                        intent.putExtra("ttfPath", newFontFile.getAbsolutePath());
                        startActivity(intent);
                    } catch (IOException e) {
                        e.printStackTrace();
                        tvCurrentChar.setText("Error saving font: " + e.getMessage());
                    }
                } else {
                    tvCurrentChar.setText("Upload failed: " + response.code());
                }
            }

            /**
             * Handles API call failures.
             * Displays error messages to the user.
             */
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvCurrentChar.setText("Upload error: " + t.getMessage());
            }
        });
    }

    /**
     * Updates the current character display.
     * Shows the next character to be drawn by the user.
     */
    private void updateCurrentChar() {
        char currentChar = CHAR_SET.charAt(currentIndex);
        tvCurrentChar.setText("Draw: " + (currentChar == ' ' ? "Space" : currentChar));
    }
}