package com.example.texttohandwriting;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying notes in a RecyclerView.
 * Handles the creation of view holders and binding of note data to views.
 * Supports click and long-click events for note items.
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> notes;
    private OnNoteClickListener listener;

    /**
     * Interface for handling note item click events.
     * Implemented by the activity or fragment containing the RecyclerView.
     */
    public interface OnNoteClickListener {
        void onNoteClick(int position);
        void onNoteLongClick(int position);
    }

    /**
     * Creates a new adapter with the specified notes and click listener.
     * 
     * @param notes List of notes to display
     * @param listener Listener for note click events
     */
    public NoteAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    /**
     * Creates a new ViewHolder by inflating the note item layout.
     * 
     * @param parent The parent ViewGroup
     * @param viewType The view type (not used in this implementation)
     * @return A new NoteViewHolder instance
     */
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    /**
     * Binds note data to the views in the ViewHolder.
     * 
     * @param holder The ViewHolder to bind data to
     * @param position The position of the note in the list
     */
    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvNoteTitle.setText(note.getTitle());
        holder.tvNoteContent.setText(note.getContent());
        holder.tvNoteDate.setText(note.getDate());
    }

    /**
     * Returns the total number of notes in the list.
     * 
     * @return The size of the notes list
     */
    @Override
    public int getItemCount() {
        return notes.size();
    }

    /**
     * Updates the adapter with a new list of notes and refreshes the view.
     * 
     * @param newNotes The new list of notes to display
     */
    public void updateNotes(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for note items.
     * Contains references to the views within each note item and handles click events.
     */
    public class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoteTitle, tvNoteContent, tvNoteDate;

        /**
         * Creates a new ViewHolder and sets up click listeners.
         * 
         * @param itemView The view for this ViewHolder
         */
        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteTitle = itemView.findViewById(R.id.tvNoteTitle);
            tvNoteContent = itemView.findViewById(R.id.tvNoteContent);
            tvNoteDate = itemView.findViewById(R.id.tvNoteDate);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNoteClick(position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNoteLongClick(position);
                    return true;
                }
                return false;
            });
        }
    }
} 