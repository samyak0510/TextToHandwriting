#!/usr/bin/env python3
"""
Integration tests for the TextToHandwriting font processing pipeline.

This test suite validates the complete end-to-end font generation workflow,
including ZIP upload, processing, and TTF file generation.

Requirements:
- Backend server running on localhost:8080
- Test glyph images in tests/fixtures/
- Python requests library

Run with: python -m pytest tests/integration/test_font_processing.py -v
"""

import os
import sys
import zipfile
import tempfile
import requests
import pytest
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import io

# Add project root to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent.parent))

class TestFontProcessing:
    """Test suite for font processing functionality."""
    
    BASE_URL = "http://localhost:8080/api/v1"
    UPLOAD_ENDPOINT = f"{BASE_URL}/fonts/upload"
    HEALTH_ENDPOINT = f"{BASE_URL}/health"
    
    @pytest.fixture(autouse=True)
    def setup_test_environment(self):
        """Setup test environment and verify server availability."""
        try:
            response = requests.get(self.HEALTH_ENDPOINT, timeout=5)
            if response.status_code != 200:
                pytest.skip("Backend server not available")
        except requests.RequestException:
            pytest.skip("Backend server not reachable")

    @pytest.fixture
    def sample_glyph_zip(self):
        """Creates a ZIP file with sample glyph images for testing."""
        # Create temporary directory for test images
        with tempfile.TemporaryDirectory() as temp_dir:
            zip_path = os.path.join(temp_dir, "test_glyphs.zip")
            
            # Characters to generate (subset for faster testing)
            test_chars = ['A', 'B', 'C', 'a', 'b', 'c', '1', '2', '3']
            
            with zipfile.ZipFile(zip_path, 'w') as zip_file:
                for char in test_chars:
                    # Generate a simple glyph image
                    img = self._create_test_glyph(char)
                    
                    # Save to bytes
                    img_bytes = io.BytesIO()
                    img.save(img_bytes, format='PNG')
                    img_bytes.seek(0)
                    
                    # Add to ZIP with correct naming convention
                    ascii_code = ord(char)
                    filename = f"glyph_{ascii_code}.png"
                    zip_file.writestr(filename, img_bytes.getvalue())
            
            yield zip_path

    def _create_test_glyph(self, character, size=(200, 200)):
        """
        Creates a test glyph image for the given character.
        
        Args:
            character: The character to render
            size: Tuple of (width, height) for the image
            
        Returns:
            PIL Image object
        """
        # Create white background image
        img = Image.new('RGB', size, color='white')
        draw = ImageDraw.Draw(img)
        
        # Try to use a system font, fallback to default
        try:
            # Common font paths for different systems
            font_paths = [
                "/System/Library/Fonts/Arial.ttf",  # macOS
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",  # Linux
                "C:/Windows/Fonts/arial.ttf"  # Windows
            ]
            
            font = None
            for font_path in font_paths:
                if os.path.exists(font_path):
                    font = ImageFont.truetype(font_path, 80)
                    break
            
            if font is None:
                font = ImageFont.load_default()
                
        except Exception:
            font = ImageFont.load_default()
        
        # Get text bounding box for centering
        bbox = draw.textbbox((0, 0), character, font=font)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
        
        # Calculate position to center text
        x = (size[0] - text_width) // 2
        y = (size[1] - text_height) // 2
        
        # Draw the character in black
        draw.text((x, y), character, fill='black', font=font)
        
        return img

    def test_server_health_check(self):
        """Test that the server health endpoint is accessible."""
        response = requests.get(self.HEALTH_ENDPOINT)
        
        assert response.status_code == 200
        health_data = response.json()
        assert health_data.get('status') == 'UP'

    def test_font_upload_and_processing(self, sample_glyph_zip):
        """
        Test the complete font processing pipeline.
        
        This test validates:
        1. ZIP file upload
        2. Image processing
        3. TTF file generation
        4. Response format and content
        """
        # Prepare the multipart upload
        with open(sample_glyph_zip, 'rb') as zip_file:
            files = {'fontZip': ('test_glyphs.zip', zip_file, 'application/zip')}
            
            # Make the upload request with extended timeout
            response = requests.post(
                self.UPLOAD_ENDPOINT,
                files=files,
                timeout=120  # Font processing can take time
            )
        
        # Verify successful response
        assert response.status_code == 200, f"Upload failed with status {response.status_code}"
        
        # Verify response is a binary TTF file
        assert response.headers.get('content-type') == 'application/octet-stream'
        assert 'attachment' in response.headers.get('content-disposition', '')
        
        # Verify TTF file structure (basic validation)
        ttf_data = response.content
        assert len(ttf_data) > 1000, "TTF file seems too small"
        
        # TTF files start with specific magic bytes
        # OpenType/TrueType fonts start with 0x00010000 or "OTTO"
        magic_bytes = ttf_data[:4]
        assert magic_bytes in [b'\x00\x01\x00\x00', b'OTTO'], "Invalid TTF file signature"
        
        print(f"✓ Generated TTF file: {len(ttf_data)} bytes")

    def test_empty_zip_handling(self):
        """Test server response to an empty ZIP file."""
        # Create empty ZIP
        with tempfile.NamedTemporaryFile(suffix='.zip', delete=False) as temp_zip:
            with zipfile.ZipFile(temp_zip.name, 'w') as zip_file:
                pass  # Create empty ZIP
            
            try:
                with open(temp_zip.name, 'rb') as zip_file:
                    files = {'fontZip': ('empty.zip', zip_file, 'application/zip')}
                    response = requests.post(self.UPLOAD_ENDPOINT, files=files, timeout=30)
                
                # Should handle gracefully (specific behavior depends on implementation)
                # At minimum, should not crash the server
                assert response.status_code in [200, 400, 422]
                
            finally:
                os.unlink(temp_zip.name)

    def test_invalid_file_upload(self):
        """Test server response to non-ZIP file upload."""
        # Create a simple text file
        with tempfile.NamedTemporaryFile(mode='w', suffix='.txt', delete=False) as temp_file:
            temp_file.write("This is not a ZIP file")
            temp_file.flush()
            
            try:
                with open(temp_file.name, 'rb') as text_file:
                    files = {'fontZip': ('notazip.txt', text_file, 'text/plain')}
                    response = requests.post(self.UPLOAD_ENDPOINT, files=files, timeout=30)
                
                # Should reject invalid file types
                assert response.status_code in [400, 415, 422]
                
            finally:
                os.unlink(temp_file.name)

    def test_large_file_handling(self):
        """Test server handling of files near the size limit."""
        # This test ensures the server properly handles size limits
        # We'll create a ZIP with many glyphs to approach size limits
        
        with tempfile.TemporaryDirectory() as temp_dir:
            zip_path = os.path.join(temp_dir, "large_test.zip")
            
            # Create ZIP with full character set
            full_charset = []
            # Add ASCII letters and numbers
            for i in range(ord('A'), ord('Z') + 1):
                full_charset.append(chr(i))
            for i in range(ord('a'), ord('z') + 1):
                full_charset.append(chr(i))
            for i in range(ord('0'), ord('9') + 1):
                full_charset.append(chr(i))
            
            with zipfile.ZipFile(zip_path, 'w') as zip_file:
                for char in full_charset:
                    img = self._create_test_glyph(char, size=(300, 300))  # Larger images
                    img_bytes = io.BytesIO()
                    img.save(img_bytes, format='PNG')
                    img_bytes.seek(0)
                    
                    ascii_code = ord(char)
                    filename = f"glyph_{ascii_code}.png"
                    zip_file.writestr(filename, img_bytes.getvalue())
            
            # Check file size
            file_size = os.path.getsize(zip_path)
            print(f"Large test file size: {file_size / (1024*1024):.2f} MB")
            
            # Only test if file is reasonable size (under 40MB)
            if file_size < 40 * 1024 * 1024:
                with open(zip_path, 'rb') as zip_file:
                    files = {'fontZip': ('large_test.zip', zip_file, 'application/zip')}
                    response = requests.post(
                        self.UPLOAD_ENDPOINT,
                        files=files,
                        timeout=300  # Extended timeout for large files
                    )
                
                # Should either succeed or fail gracefully
                assert response.status_code in [200, 413, 422]
            else:
                pytest.skip("Generated file too large for testing")

    def test_concurrent_processing(self, sample_glyph_zip):
        """Test that the server can handle multiple concurrent requests."""
        import threading
        import time
        
        results = []
        
        def upload_font():
            try:
                with open(sample_glyph_zip, 'rb') as zip_file:
                    files = {'fontZip': ('concurrent_test.zip', zip_file, 'application/zip')}
                    response = requests.post(self.UPLOAD_ENDPOINT, files=files, timeout=120)
                    results.append(response.status_code)
            except Exception as e:
                results.append(str(e))
        
        # Start multiple threads
        threads = []
        for i in range(3):  # Test with 3 concurrent requests
            thread = threading.Thread(target=upload_font)
            threads.append(thread)
            thread.start()
            time.sleep(1)  # Stagger requests slightly
        
        # Wait for all threads to complete
        for thread in threads:
            thread.join(timeout=150)
        
        # Verify results
        assert len(results) == 3
        success_count = sum(1 for result in results if result == 200)
        
        # At least one should succeed, others may be rate limited or queued
        assert success_count >= 1, f"No successful concurrent requests: {results}"
        print(f"✓ Concurrent processing: {success_count}/3 requests succeeded")


if __name__ == "__main__":
    # Run tests directly
    import subprocess
    import sys
    
    # Try to install required packages if not available
    try:
        import pytest
        import requests
        from PIL import Image
    except ImportError as e:
        print(f"Missing dependency: {e}")
        print("Install with: pip install pytest requests pillow")
        sys.exit(1)
    
    # Run the tests
    subprocess.run([sys.executable, "-m", "pytest", __file__, "-v"]) 