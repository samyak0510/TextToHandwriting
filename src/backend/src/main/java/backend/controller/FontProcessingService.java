package backend.controller;

import backend.config.ProcessingConfiguration;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Professional service for processing handwritten character images and generating TTF font files.
 * 
 * This service manages the complete font generation pipeline with enterprise-grade features:
 * - Automatic tool path detection across platforms
 * - Comprehensive error handling and logging
 * - Configurable processing parameters
 * - Resource cleanup and lifecycle management
 * - Asynchronous processing for scalability
 * 
 * Processing Pipeline:
 * 1. Extract PNG images from uploaded ZIP file
 * 2. Apply intelligent image preprocessing (crop whitespace, normalize)
 * 3. Convert images to BMP format for vector processing
 * 4. Use Potrace to convert BMP to SVG vector format
 * 5. Use FontForge to compile SVG files into production TTF font
 * 6. Return generated font with proper cleanup
 * 
 * @author TextToHandwriting Team
 * @version 2.0
 * @since 2024-01-15
 */
@Service
public class FontProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FontProcessingService.class);
  private static final String FONTFORGE_SCRIPT = "src/main/resources/generate_font.py";
    
    @Autowired
    private ProcessingConfiguration config;
  
  /**
   * Processes a ZIP file containing handwritten character images to generate a TTF font.
   * This method runs asynchronously to prevent blocking the caller and includes comprehensive
   * error handling, logging, and resource management.
   * 
   * @param zipFile The ZIP file containing PNG images of handwritten characters
   * @return A CompletableFuture that will contain the generated TTF font file when processing completes
   * @throws RuntimeException if processing fails due to configuration or external tool issues
   */
  @Async
  public CompletableFuture<File> processGlyphZip(File zipFile) {
    logger.info("Starting font processing for ZIP file: {}", zipFile.getName());
    
    try {
      // 1. Create a temporary working directory with proper cleanup
      Path tempDir = Files.createTempDirectory("font_work_");
      File workDir = tempDir.toFile();
      workDir.deleteOnExit();
      logger.debug("Created working directory: {}", workDir.getAbsolutePath());

      // 2. Use configured storage directory instead of hardcoded path
      File finalStorageDir = new File(config.getStorageDirectory());
      if (!finalStorageDir.exists()) {
        boolean created = finalStorageDir.mkdirs();
        if (!created) {
          logger.warn("Failed to create storage directory: {}", finalStorageDir.getAbsolutePath());
        }
      }
      
      // Archive original ZIP file for reference
      Files.copy(zipFile.toPath(),
          new File(finalStorageDir, zipFile.getName()).toPath(),
          StandardCopyOption.REPLACE_EXISTING);
      logger.debug("Archived ZIP file to storage directory");

      // 2. Extract the ZIP file into workDir.
      extractZip(zipFile, workDir);

      // 3. For each glyph image in workDir, convert it to BMP (if needed) and run Potrace.
      File[] files = workDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
      if (files != null) {
        for (File pngFile : files) {
          // Convert PNG to BMP using ImageIO (if Potrace requires BMP)
          BufferedImage pngImage = ImageIO.read(pngFile);

          // ────── Trim off all‑white border ──────
          BufferedImage cropped = trimWhitespace(pngImage);

          // Now convert the cropped image to BMP
          BufferedImage bmpImage = new BufferedImage(
              cropped.getWidth(), cropped.getHeight(),
              BufferedImage.TYPE_INT_RGB
          );
          Graphics2D g = bmpImage.createGraphics();
          g.drawImage(cropped, 0, 0, null);
          g.dispose();

          // Define the BMP file name and write the BMP image to disk
          File bmpFile = new File(workDir, pngFile.getName().replace(".png", ".bmp"));
          boolean writeSuccess = ImageIO.write(bmpImage, "bmp", bmpFile);
          if (!writeSuccess) {
            System.err.println("Failed to write BMP file for " + pngFile.getName());
          }
          // Delete PNG (optional)
          pngFile.delete();

          // Generate corresponding SVG file via Potrace with improved error handling
          String baseName = bmpFile.getName().substring(0, bmpFile.getName().lastIndexOf('.'));
          File svgFile = new File(workDir, baseName + ".svg");

          ProcessBuilder pb = new ProcessBuilder(
              config.getResolvedPotracePath(),
              "-s", bmpFile.getAbsolutePath(),  // output as SVG
              "-o", svgFile.getAbsolutePath()
          );
          pb.redirectErrorStream(true);
          logger.debug("Running Potrace for file: {}", bmpFile.getName());
          
          Process proc = pb.start();
          int exitCode = proc.waitFor();
          
          if (exitCode != 0) {
            logger.warn("Potrace process failed with exit code {} for file: {}", exitCode, bmpFile.getName());
          }
          
          // Cleanup temporary BMP file
          boolean deleted = bmpFile.delete();
          if (!deleted) {
            logger.debug("Failed to delete temporary BMP file: {}", bmpFile.getName());
          }
        }
      }

      // 4. Call FontForge to assemble a TTF font from the SVGs with comprehensive logging
      File outputTtf = new File(workDir, "output_font.ttf");
      ProcessBuilder ffBuilder = new ProcessBuilder(
          config.getResolvedFontforgePath(),
          "-lang=py",
          "-script",
          FONTFORGE_SCRIPT,
          workDir.getAbsolutePath(),      // directory with SVG files
          outputTtf.getAbsolutePath()     // output TTF file path
      );
      ffBuilder.redirectErrorStream(true);
      File ffLog = new File(workDir, "fontforge.log");
      ffBuilder.redirectOutput(ffLog);
      
      logger.info("Starting FontForge processing...");
      Process ffProcess = ffBuilder.start();
      int ffExit = ffProcess.waitFor();
      
      if (ffExit != 0 || !outputTtf.exists()) {
        logger.error("FontForge script failed with exit code: {}", ffExit);
        
        // Log FontForge output for debugging
        try (BufferedReader r = new BufferedReader(new FileReader(ffLog))) {
          String line;
          while ((line = r.readLine()) != null) {
            logger.error("FontForge LOG: {}", line);
          }
        } catch (IOException e) {
          logger.error("Failed to read FontForge log file", e);
        }
        
        return CompletableFuture.completedFuture(null);
      }
      
      logger.info("FontForge processing completed successfully");

      // Copy generated SVG files to final storage directory for reference
      File[] svgFiles = workDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
      if (svgFiles != null) {
        for (File svgFile : svgFiles) {
          Files.copy(svgFile.toPath(),
              new File(finalStorageDir, svgFile.getName()).toPath(),
              StandardCopyOption.REPLACE_EXISTING);
        }
      }

      // Copy generated TTF font to final storage directory
      Files.copy(outputTtf.toPath(),
          new File(finalStorageDir, outputTtf.getName()).toPath(),
          StandardCopyOption.REPLACE_EXISTING);

      logger.info("Font processing completed successfully. Output: {}", outputTtf.getName());
      return CompletableFuture.completedFuture(outputTtf);
      
    } catch (Exception e) {
      logger.error("Font processing failed for ZIP file: " + zipFile.getName(), e);
      return CompletableFuture.completedFuture(null);
    }
  }

  /**
   * Helper method: Extracts a ZIP file to the given directory.
   * 
   * @param zipFile The ZIP file to extract
   * @param targetDir The directory to extract files to
   * @throws IOException If there's an error reading or extracting the ZIP file
   */
  private void extractZip(File zipFile, File targetDir) throws IOException {
    try (ZipFile zip = new ZipFile(zipFile)) {
      Enumeration<?> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();
        File outFile = new File(targetDir, entry.getName());
        if (entry.isDirectory()) {
          outFile.mkdirs();
        } else {
          outFile.getParentFile().mkdirs();
          try (InputStream in = zip.getInputStream(entry);
              OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
              out.write(buffer, 0, len);
            }
          }
        }
      }
    }
  }

  /**
   * Crops away any white (or near‑white) border around a BufferedImage.
   * This improves the quality of the generated glyphs by removing unnecessary whitespace.
   * 
   * @param src The source image to crop
   * @return A new BufferedImage with whitespace removed from all sides
   */
  private BufferedImage trimWhitespace(BufferedImage src) {
    int width = src.getWidth(), height = src.getHeight();
    int minX = width, minY = height, maxX = 0, maxY = 0;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (!isWhite(src.getRGB(x, y))) {
          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }
    }
    if (maxX < minX || maxY < minY) {
      return src;
    }
    return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
  }

  /**
   * Determines if a pixel is white or near-white.
   * Used by the trimWhitespace method to identify areas to crop.
   * 
   * @param rgb The RGB value of the pixel
   * @return true if the pixel is white or near-white, false otherwise
   */
  private boolean isWhite(int rgb) {
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;
    return r > 245 && g > 245 && b > 245;
  }
}