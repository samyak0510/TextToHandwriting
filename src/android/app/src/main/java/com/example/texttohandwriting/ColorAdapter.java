package com.example.texttohandwriting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Adapter for displaying color options in a grid view.
 * Handles the display and selection state of color items in the color picker dialog.
 * Provides functionality to get the currently selected color.
 */
public class ColorAdapter extends BaseAdapter {
    private Context context;
    private int[] colors;
    private int selectedPosition = -1;
    private LayoutInflater inflater;

    /**
     * Creates a new color adapter with the specified color options.
     * 
     * @param context The activity context
     * @param colors Array of color values to display in the grid
     */
    public ColorAdapter(Context context, int[] colors) {
        this.context = context;
        this.colors = colors;
        this.inflater = LayoutInflater.from(context);
    }

    /**
     * Returns the number of color options available.
     * 
     * @return The total count of color items
     */
    @Override
    public int getCount() {
        return colors.length;
    }

    /**
     * Gets the color at the specified position.
     * 
     * @param position The position of the color in the array
     * @return The color object at the position
     */
    @Override
    public Object getItem(int position) {
        return colors[position];
    }

    /**
     * Gets the item ID for a position.
     * 
     * @param position The position of the item
     * @return The position as the ID
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Updates the currently selected color position and refreshes the view.
     * 
     * @param position The position of the newly selected color
     */
    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    /**
     * Gets the currently selected color value.
     * 
     * @return The color integer value of the selected color, or black as default if none selected
     */
    public int getSelectedColor() {
        if (selectedPosition >= 0 && selectedPosition < colors.length) {
            return colors[selectedPosition];
        }
        return android.graphics.Color.BLACK; // Default color
    }

    /**
     * Creates or recycles a view for a color item in the grid.
     * Applies the appropriate background and selection state to the color view.
     * 
     * @param position The position of the item
     * @param convertView The recycled view to use, if available
     * @param parent The parent view group
     * @return The configured view for the color item
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_color, parent, false);
        }

        View colorView = convertView.findViewById(R.id.viewColor);
        
        // Set the border style based on selection state
        colorView.setBackgroundResource(
                selectedPosition == position ? 
                R.drawable.color_item_selected_background : 
                R.drawable.color_item_background);
        
        // Set the background color
        colorView.getBackground().setColorFilter(
                colors[position], 
                android.graphics.PorterDuff.Mode.SRC_ATOP);

        return convertView;
    }
} 