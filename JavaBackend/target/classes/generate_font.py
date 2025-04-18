import fontforge
import sys
import os

def create_font(svg_dir, output_path):
    """
    Creates a TTF font file from a directory of SVG files.
    
    Each SVG file should be named in the format "glyph_X.svg" where X is the 
    Unicode code point of the character. For example:
    - glyph_65.svg would map to the character 'A' (Unicode point 65)
    - glyph_97.svg would map to the character 'a' (Unicode point 97)
    
    Parameters:
    -----------
    svg_dir : str
        Directory containing SVG files representing glyphs
    output_path : str
        Path where the TTF font file should be saved
    """
    font = fontforge.font()
    font.encoding = 'UnicodeFull'
    for file in os.listdir(svg_dir):
        if file.lower().endswith('.svg'):
            base_name = os.path.splitext(file)[0]  # e.g., glyph_65
            parts = base_name.split('_')
            if len(parts) == 2 and parts[0] == 'glyph':
                char_code_str = parts[1]
                if char_code_str.isdigit():
                    char_code = int(char_code_str)
                    glyph = font.createChar(char_code)
                    glyph.importOutlines(os.path.join(svg_dir, file))
    font.generate(output_path)
    print("Font generated at:", output_path)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: generate_font.py <svg_dir> <output_font>")
        sys.exit(1)
    svg_directory = sys.argv[1]
    ttf_output = sys.argv[2]
    create_font(svg_directory, ttf_output)
