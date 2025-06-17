package backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application class for the Text-to-Handwriting conversion service.
 * This application provides a REST API that converts handwritten character images
 * (submitted as PNG files in a ZIP archive) into a TTF font file.
 * 
 * The @EnableAsync annotation allows asynchronous processing of font generation tasks,
 * which can be resource-intensive.
 */
@EnableAsync
@SpringBootApplication
public class FontGeneratorApplication {
  public static void main(String[] args) {
    SpringApplication.run(FontGeneratorApplication.class, args);
  }
}
