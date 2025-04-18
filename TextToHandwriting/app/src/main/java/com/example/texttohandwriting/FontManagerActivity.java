package com.example.texttohandwriting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing custom handwriting fonts.
 * Provides functionality to view, rename, delete, and export TTF font files.
 * Also allows navigation back to font creation.
 */
public class FontManagerActivity extends AppCompatActivity {

    private ListView listView;
    private Button btnRefresh;
    private Button postCreationCreateFontButton, btnBack;
    private File fontsDir;

    /**
     * Initializes the activity, sets up UI components, and configures event listeners.
     * Loads the list of available fonts and sets up interactions for font management.
     * 
     * @param savedInstanceState If the activity is being re-initialized after previously
     *                           being shut down, this contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_font_manager);
        listView = findViewById(R.id.listViewFonts);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnBack = findViewById(R.id.btnBack);
        postCreationCreateFontButton = findViewById(R.id.postCreationCreateFontButton);
        fontsDir = new File(getFilesDir(), "fonts");
        if(!fontsDir.exists()){
            fontsDir.mkdirs();
        }
        loadFontList();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(FontManagerActivity.this, AfterGenerationActivity.class));
                finish();
            }
        });

        // When an item is tapped, show popup menu with options
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String selectedFont = (String) parent.getItemAtPosition(position);
            showPopupMenu(view, selectedFont);
        });

        btnRefresh.setOnClickListener(v -> loadFontList());

        postCreationCreateFontButton.setOnClickListener(v -> {
            // Launch AfterGenerationActivity.
            Intent intent = new Intent(FontManagerActivity.this, MainActivity.class);
            intent.putExtra("returnedFromAnotherClass", true);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Loads and displays the list of font files from the fonts directory.
     * Filters for TTF files and creates a custom adapter to display them.
     */
    private void loadFontList() {
        File[] fonts = fontsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf"));
        List<String> fontNames = new ArrayList<>();
        if (fonts != null) {
            for (File font : fonts) {
                fontNames.add(font.getName());
            }
        }
        
        // Use custom layout with larger font size instead of default
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, 
            R.layout.item_font,  // Custom layout with larger text
            R.id.tvFontName,     // ID of the TextView in the custom layout
            fontNames
        );
        
        listView.setAdapter(adapter);
    }
    
    /**
     * Displays a popup menu with font management options.
     * Shows rename, delete, and export options for the selected font.
     * 
     * @param view The view to anchor the popup menu to
     * @param fontName The name of the font to manage
     */
    private void showPopupMenu(View view, String fontName) {
        // Create a ContextThemeWrapper with our custom style
        Context wrapper = new ContextThemeWrapper(this, R.style.RoundedPopupMenu);
        
        // Create the popup menu with the themed context
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.font_options_menu, popupMenu.getMenu());
        
        // Set up click listener for menu items
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            File fontFile = new File(fontsDir, fontName);
            int itemId = menuItem.getItemId();
            
            if (itemId == R.id.action_rename) {
                showRenameDialog(fontFile);
                return true;
            } else if (itemId == R.id.action_delete) {
                showDeleteConfirmationDialog(fontFile);
                return true;
            } else if (itemId == R.id.action_export) {
                exportFont(fontFile);
                return true;
            }
            return false;
        });
        
        // Apply animation to the clicked view
        view.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .start();

        // Set a dismiss listener to reset the animation
        popupMenu.setOnDismissListener(menu -> {
            view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start();
        });
        
        // Show the popup menu
        popupMenu.show();
    }

    /**
     * Shows a confirmation dialog for deleting a font file.
     * Deletes the file if confirmed and refreshes the font list.
     * 
     * @param fontFile The font file to delete
     */
    private void showDeleteConfirmationDialog(final File fontFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Font");
        builder.setMessage("Are you sure you want to delete \"" + fontFile.getName() + "\"?");
        
        // Add the buttons
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (fontFile.delete()) {
                    Toast.makeText(FontManagerActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadFontList();
                } else {
                    Toast.makeText(FontManagerActivity.this, "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        
        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Shows a dialog for renaming a font file.
     * Renames the file if confirmed and refreshes the font list.
     * 
     * @param fontFile The font file to rename
     */
    private void showRenameDialog(final File fontFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename " + fontFile.getName());
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.toLowerCase().endsWith(".ttf")) {
                newName += ".ttf";
            }
            File newFile = new File(fontsDir, newName);
            if (fontFile.renameTo(newFile)) {
                Toast.makeText(FontManagerActivity.this, "Renamed", Toast.LENGTH_SHORT).show();
                loadFontList();
            } else {
                Toast.makeText(FontManagerActivity.this, "Rename failed", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Exports a font file to other applications using a share intent.
     * Allows users to send the TTF file via email, messaging, etc.
     * 
     * @param fontFile The font file to export
     */
    private void exportFont(File fontFile) {
        // For exporting, we can create a share Intent.
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/ttf");
        intent.putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".provider", fontFile));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Export Font"));
    }
}