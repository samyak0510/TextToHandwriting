package com.example.texttohandwriting.model;

import android.graphics.Bitmap;

/**
 * Holds a single glyph's data: the character and the drawn bitmap.
 */
public class GlyphData {
    private char character;
    private Bitmap bitmap;

    public GlyphData(char character, Bitmap bitmap) {
        this.character = character;
        this.bitmap = bitmap;
    }

    public char getCharacter() {
        return character;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}
