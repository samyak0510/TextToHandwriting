package com.example.texttohandwriting;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesActivity extends AppCompatActivity implements NoteAdapter.OnNoteClickListener {

    private RecyclerView recyclerViewNotes;
    private FloatingActionButton fabAddNote;
    private File notesDir;
    private NoteAdapter noteAdapter;
    private List<Note> notesList;
    private ActivityResultLauncher<String> selectFileForImport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes);
        
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        fabAddNote = findViewById(R.id.fabAddNote);
        Button btnBack = findViewById(R.id.btnBack);

        // Back button listener
        btnBack.setOnClickListener(v -> finish());

        // Setup notes directory
        notesDir = new File(getFilesDir(), "notes");
        if (!notesDir.exists()) {
            notesDir.mkdirs();
        }

        // Setup RecyclerView
        notesList = new ArrayList<>();
        noteAdapter = new NoteAdapter(notesList, this);
        recyclerViewNotes.setAdapter(noteAdapter);

        // The layout manager is already set in XML, but here we can customize it further if needed
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerViewNotes.setLayoutManager(layoutManager);

        // Load notes
        loadNotes();

        // FloatingActionButton click listener to show popup menu
        fabAddNote.setOnClickListener(v -> {
            // Show menu without animation
            showFabMenu(fabAddNote);
        });
        
        // Register file picker for import functionality
        selectFileForImport = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importNote(uri);
                }
            }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload notes when returning to this activity
        loadNotes();
    }

    private void loadNotes() {
        File[] files = notesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        notesList.clear();
        
        if (files != null) {
            for (File file : files) {
                try {
                    // Get basic file info
                    String id = file.getName();
                    String title = file.getName().replace(".txt", "");
                    Date lastModDate = new Date(file.lastModified());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    String formattedDate = dateFormat.format(lastModDate);
                    
                    // Read first few lines for preview
                    StringBuilder preview = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        int lineCount = 0;
                        while ((line = reader.readLine()) != null && lineCount < 3) {
                            preview.append(line).append("\n");
                            lineCount++;
                        }
                    }
                    
                    Note note = new Note(id, title, preview.toString().trim(), formattedDate);
                    notesList.add(note);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        noteAdapter.updateNotes(notesList);
    }

    @Override
    public void onNoteClick(int position) {
        // Open the selected note in the editor
        if (position >= 0 && position < notesList.size()) {
            Note note = notesList.get(position);
            File noteFile = new File(notesDir, note.getId());
            
            Intent intent = new Intent(NotesActivity.this, NoteEditorActivity.class);
            intent.putExtra("notePath", noteFile.getAbsolutePath());
            startActivity(intent);
        }
    }

    @Override
    public void onNoteLongClick(int position) {
        // Show popup menu for the note
        if (position >= 0 && position < notesList.size()) {
            View itemView = recyclerViewNotes.findViewHolderForAdapterPosition(position).itemView;
            showPopupMenu(itemView, position);
        }
    }
    
    private void showPopupMenu(View view, final int position) {
        // Create a ContextThemeWrapper with our custom style, matching the font options menu
        android.content.Context wrapper = new android.view.ContextThemeWrapper(this, R.style.RoundedPopupMenu);
        
        // Create the popup menu with the themed context
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.note_menu, popupMenu.getMenu());
        
        // Apply animation to the view
        view.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .start();
        
        // Set up click listener for menu items
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.action_delete) {
                    showDeleteConfirmationDialog(position);
                    return true;
                } else if (id == R.id.action_export) {
                    exportNote(position);
                    return true;
                }
                return false;
            }
        });
        
        // Reset animation when popup is dismissed
        popupMenu.setOnDismissListener(menu -> {
            view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(200)
                .start();
        });
        
        // Show the popup
        popupMenu.show();
    }
    
    private void showDeleteConfirmationDialog(final int position) {
        if (position >= 0 && position < notesList.size()) {
            final Note noteToDelete = notesList.get(position);
            
            // Create confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete Note");
            builder.setMessage("Are you sure you want to delete \"" + noteToDelete.getTitle() + "\"?");
            
            // Add the buttons
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    deleteNote(position);
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
    }
    
    private void deleteNote(int position) {
        if (position >= 0 && position < notesList.size()) {
            Note noteToDelete = notesList.get(position);
            File fileToDelete = new File(notesDir, noteToDelete.getId());
            
            if (fileToDelete.exists()) {
                boolean deleted = fileToDelete.delete();
                if (deleted) {
                    // Remove from list and update RecyclerView
                    notesList.remove(position);
                    noteAdapter.notifyItemRemoved(position);
                    noteAdapter.notifyItemRangeChanged(position, notesList.size());
                    
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void exportNote(int position) {
        if (position >= 0 && position < notesList.size()) {
            try {
                Note noteToExport = notesList.get(position);
                File sourceFile = new File(notesDir, noteToExport.getId());
                
                // Create a copy in the external storage directory
                File exportDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOCUMENTS), "TextToHandwriting");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }
                
                File exportFile = new File(exportDir, noteToExport.getId());
                
                // Copy the file
                FileInputStream inStream = new FileInputStream(sourceFile);
                FileOutputStream outStream = new FileOutputStream(exportFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, length);
                }
                inStream.close();
                outStream.close();
                
                // Share the file
                Uri contentUri = FileProvider.getUriForFile(
                        this, 
                        "com.example.texttohandwriting.fileprovider", 
                        exportFile);
                
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.setType("text/plain");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                startActivity(Intent.createChooser(shareIntent, "Share Note"));
                Toast.makeText(this, "Exporting note...", Toast.LENGTH_SHORT).show();
                
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void launchImportNote() {
        selectFileForImport.launch("text/plain");
    }
    
    private void importNote(Uri uri) {
        try {
            // Generate a unique filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String filename = "note_" + timestamp + ".txt";
            
            // Create destination file
            File destinationFile = new File(notesDir, filename);
            
            // Copy content from URI to file
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            inputStream.close();
            outputStream.close();
            
            // Refresh the notes list
            loadNotes();
            
            Toast.makeText(this, "Note imported successfully", Toast.LENGTH_SHORT).show();
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to import note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showFabMenu(View view) {
        // Create a ContextThemeWrapper with our custom style
        android.content.Context wrapper = new android.view.ContextThemeWrapper(this, R.style.RoundedPopupMenu);
        
        // Create the popup menu
        PopupMenu popupMenu = new PopupMenu(wrapper, view);
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.fab_menu, popupMenu.getMenu());
        
        // Apply animation to the FAB
        fabAddNote.animate()
            .rotation(135f)
            .setDuration(200)
            .start();
        
        // Set up click listener for menu items
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_new_note) {
                // Create a new note
                Intent intent = new Intent(NotesActivity.this, NoteEditorActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_import_note) {
                // Import a note
                launchImportNote();
                return true;
            }
            return false;
        });
        
        // Set dismiss listener to reset FAB animation
        popupMenu.setOnDismissListener(menu -> {
            resetFabAnimation();
        });
        
        // Show the popup menu
        popupMenu.show();
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // If FAB is rotated and user touches outside the FAB area, reset it
        if (ev.getAction() == MotionEvent.ACTION_DOWN && fabAddNote.getRotation() != 0) {
            float x = ev.getX();
            float y = ev.getY();
            
            // Get FAB location
            int[] fabLocation = new int[2];
            fabAddNote.getLocationOnScreen(fabLocation);
            
            // Check if touch is outside FAB bounds
            if (x < fabLocation[0] || x > fabLocation[0] + fabAddNote.getWidth() ||
                y < fabLocation[1] || y > fabLocation[1] + fabAddNote.getHeight()) {
                // Touch is outside FAB, should reset animation
                resetFabAnimation();
            }
        }
        return super.dispatchTouchEvent(ev);
    }
    
    // Helper method to reset FAB animation
    private void resetFabAnimation() {
        fabAddNote.animate()
            .rotation(0f)
            .setDuration(200)
            .start();
    }
}