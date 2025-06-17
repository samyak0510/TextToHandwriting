package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for managing external tool paths and processing settings.
 * This class automatically detects FontForge and Potrace installations across different platforms.
 * 
 * Features:
 * - Cross-platform tool detection (Windows, macOS, Linux)
 * - Fallback path resolution
 * - Validation of tool availability
 * - Configuration property binding
 * 
 * @author TextToHandwriting Team
 * @version 1.0
 * @since 2024-01-15
 */
@Configuration
@ConfigurationProperties(prefix = "")
@Component
public class ProcessingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingConfiguration.class);
    
    // Configuration properties from application.properties
    private String fontforgePath = "auto";
    private String potracePath = "auto";
    private String storageDirectory;
    private String storageTempDirectory;
    private boolean storageCleanupOnStartup = true;
    
    // Processing configuration
    private int processingMaxConcurrentJobs = 3;
    private int processingTimeoutSeconds = 300;
    private int processingImageMinSize = 50;
    private int processingImageMaxSize = 2000;
    
    // Resolved paths
    private String resolvedFontforgePath;
    private String resolvedPotracePath;

    /**
     * Initializes the configuration and performs auto-detection of external tools.
     * This method is called after dependency injection is complete.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing ProcessingConfiguration...");
        
        resolvedFontforgePath = resolveFontforgePath();
        resolvedPotracePath = resolvePotracePath();
        
        validateConfiguration();
        createDirectories();
        
        logger.info("Configuration initialized successfully");
        logger.info("FontForge path: {}", resolvedFontforgePath);
        logger.info("Potrace path: {}", resolvedPotracePath);
        logger.info("Storage directory: {}", storageDirectory);
    }

    /**
     * Resolves the FontForge executable path using multiple detection strategies.
     * 
     * @return The resolved path to FontForge executable
     * @throws RuntimeException if FontForge cannot be found
     */
    private String resolveFontforgePath() {
        if (!"auto".equals(fontforgePath)) {
            if (isValidExecutable(fontforgePath)) {
                logger.info("Using configured FontForge path: {}", fontforgePath);
                return fontforgePath;
            } else {
                logger.warn("Configured FontForge path is invalid: {}", fontforgePath);
            }
        }

        List<String> candidatePaths = getFontforgeCandidatePaths();
        
        for (String path : candidatePaths) {
            if (isValidExecutable(path)) {
                logger.info("Auto-detected FontForge at: {}", path);
                return path;
            }
        }
        
        throw new RuntimeException("FontForge executable not found. Please install FontForge or set fontforge.path property.");
    }

    /**
     * Resolves the Potrace executable path using multiple detection strategies.
     * 
     * @return The resolved path to Potrace executable
     * @throws RuntimeException if Potrace cannot be found
     */
    private String resolvePotracePath() {
        if (!"auto".equals(potracePath)) {
            if (isValidExecutable(potracePath)) {
                logger.info("Using configured Potrace path: {}", potracePath);
                return potracePath;
            } else {
                logger.warn("Configured Potrace path is invalid: {}", potracePath);
            }
        }

        List<String> candidatePaths = getPotraceCandidatePaths();
        
        for (String path : candidatePaths) {
            if (isValidExecutable(path)) {
                logger.info("Auto-detected Potrace at: {}", path);
                return path;
            }
        }
        
        throw new RuntimeException("Potrace executable not found. Please install Potrace or set potrace.path property.");
    }

    /**
     * Returns a list of candidate paths for FontForge based on the current operating system.
     * 
     * @return List of potential FontForge installation paths
     */
    private List<String> getFontforgeCandidatePaths() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("windows")) {
            return Arrays.asList(
                "C:\\Program Files (x86)\\FontForgeBuilds\\fontforge.bat",
                "C:\\Program Files\\FontForgeBuilds\\fontforge.bat",
                "C:\\fontforge\\fontforge.bat",
                "fontforge.bat",
                "fontforge"
            );
        } else if (os.contains("mac")) {
            return Arrays.asList(
                "/usr/local/bin/fontforge",
                "/opt/homebrew/bin/fontforge",
                "/Applications/FontForge.app/Contents/MacOS/FontForge",
                "fontforge"
            );
        } else {
            // Linux and other Unix-like systems
            return Arrays.asList(
                "/usr/bin/fontforge",
                "/usr/local/bin/fontforge",
                "/opt/fontforge/bin/fontforge",
                "fontforge"
            );
        }
    }

    /**
     * Returns a list of candidate paths for Potrace based on the current operating system.
     * 
     * @return List of potential Potrace installation paths
     */
    private List<String> getPotraceCandidatePaths() {
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("windows")) {
            return Arrays.asList(
                "C:\\tools\\potrace-1.16.win64\\potrace.exe",
                "C:\\Program Files\\potrace\\potrace.exe",
                "C:\\Program Files (x86)\\potrace\\potrace.exe",
                "potrace.exe",
                "potrace"
            );
        } else if (os.contains("mac")) {
            return Arrays.asList(
                "/usr/local/bin/potrace",
                "/opt/homebrew/bin/potrace",
                "potrace"
            );
        } else {
            // Linux and other Unix-like systems
            return Arrays.asList(
                "/usr/bin/potrace",
                "/usr/local/bin/potrace",
                "potrace"
            );
        }
    }

    /**
     * Validates if a given path points to a valid executable file.
     * 
     * @param path The path to validate
     * @return true if the path is a valid executable, false otherwise
     */
    private boolean isValidExecutable(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        File file = new File(path);
        return file.exists() && (file.canExecute() || path.endsWith(".bat"));
    }

    /**
     * Validates the overall configuration and logs any potential issues.
     */
    private void validateConfiguration() {
        if (resolvedFontforgePath == null) {
            logger.error("FontForge path validation failed");
        }
        
        if (resolvedPotracePath == null) {
            logger.error("Potrace path validation failed");
        }
        
        if (processingMaxConcurrentJobs <= 0) {
            logger.warn("Invalid max concurrent jobs setting: {}. Using default: 3", processingMaxConcurrentJobs);
            processingMaxConcurrentJobs = 3;
        }
        
        if (processingTimeoutSeconds <= 0) {
            logger.warn("Invalid timeout setting: {}. Using default: 300", processingTimeoutSeconds);
            processingTimeoutSeconds = 300;
        }
    }

    /**
     * Creates necessary directories if they don't exist.
     */
    private void createDirectories() {
        try {
            if (storageDirectory != null) {
                Path storagePath = Paths.get(storageDirectory);
                if (!Files.exists(storagePath)) {
                    Files.createDirectories(storagePath);
                    logger.info("Created storage directory: {}", storageDirectory);
                }
            }
            
            if (storageTempDirectory != null) {
                Path tempPath = Paths.get(storageTempDirectory);
                if (!Files.exists(tempPath)) {
                    Files.createDirectories(tempPath);
                    logger.info("Created temp directory: {}", storageTempDirectory);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create directories", e);
        }
    }

    // Getters and setters with proper documentation

    /**
     * Gets the resolved FontForge executable path.
     * @return The absolute path to FontForge executable
     */
    public String getResolvedFontforgePath() {
        return resolvedFontforgePath;
    }

    /**
     * Gets the resolved Potrace executable path.
     * @return The absolute path to Potrace executable
     */
    public String getResolvedPotracePath() {
        return resolvedPotracePath;
    }

    public String getFontforgePath() {
        return fontforgePath;
    }

    public void setFontforgePath(String fontforgePath) {
        this.fontforgePath = fontforgePath;
    }

    public String getPotracePath() {
        return potracePath;
    }

    public void setPotracePath(String potracePath) {
        this.potracePath = potracePath;
    }

    public String getStorageDirectory() {
        return storageDirectory;
    }

    public void setStorageDirectory(String storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    public String getStorageTempDirectory() {
        return storageTempDirectory;
    }

    public void setStorageTempDirectory(String storageTempDirectory) {
        this.storageTempDirectory = storageTempDirectory;
    }

    public boolean isStorageCleanupOnStartup() {
        return storageCleanupOnStartup;
    }

    public void setStorageCleanupOnStartup(boolean storageCleanupOnStartup) {
        this.storageCleanupOnStartup = storageCleanupOnStartup;
    }

    public int getProcessingMaxConcurrentJobs() {
        return processingMaxConcurrentJobs;
    }

    public void setProcessingMaxConcurrentJobs(int processingMaxConcurrentJobs) {
        this.processingMaxConcurrentJobs = processingMaxConcurrentJobs;
    }

    public int getProcessingTimeoutSeconds() {
        return processingTimeoutSeconds;
    }

    public void setProcessingTimeoutSeconds(int processingTimeoutSeconds) {
        this.processingTimeoutSeconds = processingTimeoutSeconds;
    }

    public int getProcessingImageMinSize() {
        return processingImageMinSize;
    }

    public void setProcessingImageMinSize(int processingImageMinSize) {
        this.processingImageMinSize = processingImageMinSize;
    }

    public int getProcessingImageMaxSize() {
        return processingImageMaxSize;
    }

    public void setProcessingImageMaxSize(int processingImageMaxSize) {
        this.processingImageMaxSize = processingImageMaxSize;
    }
} 