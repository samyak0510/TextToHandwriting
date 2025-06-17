package com.example.texttohandwriting;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

/**
 * Main entry point activity for the Text to Handwriting application.
 * Provides navigation to either start creating handwriting fonts or using existing fonts.
 * Implements a two-touch selection pattern for launching other activities.
 */
public class MainActivity extends AppCompatActivity {

    private ImageButton btnStartDrawing;
    private ImageButton btnStartScanning;
    private View lastSelectedButton = null;
    
    // Constants for animation
    private static final float SCALE_SELECTED = 1.1f;
    private static final float SCALE_NORMAL = 1.0f;
    private static final int ANIMATION_DURATION = 200; // ms

    /**
     * Initializes the activity, sets up UI components, and configures button listeners.
     * Checks if fonts exist and navigates directly to AfterGenerationActivity if they do.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        boolean returnedFromAnotherClass = getIntent().getBooleanExtra("returnedFromAnotherClass", false);

        if (!returnedFromAnotherClass) {
            File fontsDir = new File(getFilesDir(), "fonts");
            if (fontsDir.exists() && fontsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf")).length > 0) {
                // Font exists, launch AfterGenerationActivity directly.
                Intent intent = new Intent(MainActivity.this, AfterGenerationActivity.class);
                startActivity(intent);
                finish();
            }
        }

        btnStartDrawing = findViewById(R.id.btnStartDrawing);
        btnStartScanning = findViewById(R.id.btnStartScanning);

        setupTwoTouchListener(btnStartDrawing, DrawingActivity.class);
        setupTwoTouchListener(btnStartScanning, AfterGenerationActivity.class); // Assume this is for scanning
    }

    /**
     * Sets up a custom two-touch listener for navigation buttons.
     * The first touch selects the button with visual feedback.
     * The second touch launches the target activity.
     * 
     * @param button The button to configure with the touch listener
     * @param targetActivity The activity class to launch on second touch
     */
    private void setupTwoTouchListener(ImageButton button, final Class<?> targetActivity) {

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // If another button was previously selected, deselect it
                    if (lastSelectedButton != null && lastSelectedButton != v) {
                        animateButton(lastSelectedButton, false);
                        lastSelectedButton.setSelected(false);
                    }
                    
                    // Toggle selection state
                    if (v.isSelected()) {
                        // Second touch on the already selected button - launch activity
                        Intent intent = new Intent(MainActivity.this, targetActivity);
                        startActivity(intent);
                        animateButton(v, false);
                        v.setSelected(false); // Deselect after action
                        lastSelectedButton = null;
                    } else {
                        // First touch - select the button
                        v.setSelected(true);
                        animateButton(v, true);
                        lastSelectedButton = v;
                    }
                    return true;
            }
            return false;
        });
    }
    
    /**
     * Animates the button's scale to provide visual feedback of selection.
     * Scales the button up when selected and back to normal size when deselected.
     * 
     * @param view The view to animate
     * @param selected Whether the view is being selected (true) or deselected (false)
     */
    private void animateButton(View view, boolean selected) {
        // Create scale animations
        float targetScale = selected ? SCALE_SELECTED : SCALE_NORMAL;
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", targetScale);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", targetScale);
        
        // Combine animations and play them together
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.setDuration(ANIMATION_DURATION);
        animSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animSet.start();
    }
}