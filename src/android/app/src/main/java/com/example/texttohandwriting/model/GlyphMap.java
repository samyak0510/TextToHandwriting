package com.example.texttohandwriting.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects the glyphs drawn by the user, mapping each character to its data.
 */
public class GlyphMap {
    private Map<Character, GlyphData> glyphs = new HashMap<>();

    public void addGlyph(GlyphData glyphData) {
        glyphs.put(glyphData.getCharacter(), glyphData);
    }

    public Map<Character, GlyphData> getGlyphs() {
        return glyphs;
    }

    /**
     * Checks if the glyph map contains entries for every character in the given charset.
     */
    public boolean isComplete(String charSet) {
        return glyphs.size() == charSet.length();
    }
}
