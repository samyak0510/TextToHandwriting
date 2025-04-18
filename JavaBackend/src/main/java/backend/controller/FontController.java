package backend.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller that handles font generation requests.
 * Provides an API endpoint for converting uploaded ZIP files containing character images
 * into TTF font files.
 */
@RestController
@RequestMapping("/api")
public class FontController {

  @Autowired
  private FontProcessingService fontProcessingService;

  /**
   * Generates a TTF font file from a ZIP file containing PNG images of handwritten characters.
   * The ZIP file should contain PNG images named in the format "glyph_X.png" where X is the 
   * Unicode code point of the character.
   * 
   * @param fontZip A ZIP file containing PNG images of handwritten characters
   * @return A ResponseEntity containing either the generated TTF font file or an error message
   */
  @PostMapping("/generateFont")
  public CompletableFuture<ResponseEntity<?>> generateFont(@RequestParam("fontZip") MultipartFile fontZip) {
    if (fontZip.isEmpty() || !fontZip.getContentType().equalsIgnoreCase("application/zip")) {
      return CompletableFuture.completedFuture(
          ResponseEntity.badRequest().body("Invalid or missing ZIP file.")
      );
    }

    try {
      // Save ZIP file to a temporary location
      File uploadedZip = File.createTempFile("fontZip_", ".zip");
      uploadedZip.deleteOnExit();
      fontZip.transferTo(uploadedZip);

      // Process the ZIP file to generate a TTF font
      return fontProcessingService.processGlyphZip(uploadedZip)
          .thenApply(ttfFile -> {
            if (ttfFile == null || !ttfFile.exists()) {
              return ResponseEntity.status(500).body("Font generation failed.");
            }
            try {
              InputStreamResource resource = new InputStreamResource(new FileInputStream(ttfFile));
              HttpHeaders headers = new HttpHeaders();
              headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generated_font.ttf");
              return ResponseEntity.ok()
                  .headers(headers)
                  .contentLength(ttfFile.length())
                  .contentType(MediaType.APPLICATION_OCTET_STREAM)
                  .body(resource);
            } catch (FileNotFoundException e) {
              e.printStackTrace();
              return ResponseEntity.status(500).body("Unable to read generated font file.");
            }
          });
    } catch (Exception e) {
      e.printStackTrace();
      return CompletableFuture.completedFuture(
          ResponseEntity.status(500).body("Error processing uploaded ZIP file.")
      );
    }
  }
}