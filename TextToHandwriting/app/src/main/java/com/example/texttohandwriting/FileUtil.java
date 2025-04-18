package com.example.texttohandwriting;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.ResponseBody;

/**
 * Utility class for file operations in the application.
 * Provides static methods for file manipulation, including saving images,
 * reading text, zipping folders, and handling downloads.
 */
public class FileUtil {

    /**
     * Saves a bitmap image to a file on disk.
     * 
     * @param bitmap The bitmap to save
     * @param file The destination file
     * @throws IOException If there's an error writing the file
     */
    public static void saveBitmapToFile(Bitmap bitmap, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
        }
    }

    /**
     * Zips all files in a folder into a zip file.
     * 
     * @param sourceFolder The folder containing files to zip
     * @param zipFile The destination zip file
     * @throws IOException If there's an error during zipping
     */
    public static void zipFolder(File sourceFolder, File zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zipFolderRecursive(sourceFolder, sourceFolder, zos);
        }
    }

    /**
     * Recursively zips files and subfolders within a folder.
     * Used internally by zipFolder to process nested directories.
     * 
     * @param baseFolder The root folder being zipped (for path relativity)
     * @param currentFolder The current folder being processed
     * @param zos The zip output stream
     * @throws IOException If there's an error during zipping
     */
    private static void zipFolderRecursive(File baseFolder, File currentFolder, ZipOutputStream zos) throws IOException {
        File[] files = currentFolder.listFiles();
        if (files == null) return;
        for (File file : files) {
            String entryName = baseFolder.toURI().relativize(file.toURI()).getPath();
            if (file.isDirectory()) {
                zos.putNextEntry(new ZipEntry(entryName));
                zos.closeEntry();
                zipFolderRecursive(baseFolder, file, zos);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        }
    }

    /**
     * Writes a downloaded response body to a file on disk.
     * Used for downloading and saving font files.
     * 
     * @param context The application context
     * @param body The response body containing the file data
     * @return The file that was written
     * @throws IOException If there's an error writing the file
     */
    public static File writeResponseBodyToDisk(Context context, ResponseBody body) throws IOException {
        File outputFile = new File(context.getFilesDir(), "downloaded_font.ttf");
        try (InputStream inputStream = body.byteStream();
             OutputStream outputStream = new FileOutputStream(outputFile)) {
            byte[] fileReader = new byte[4096];
            int read;
            while ((read = inputStream.read(fileReader)) != -1) {
                outputStream.write(fileReader, 0, read);
            }
            outputStream.flush();
        }
        return outputFile;
    }

    /**
     * Copies a file from one location to another.
     * 
     * @param source The source file to copy
     * @param dest The destination file
     * @throws IOException If there's an error during copying
     */
    public static void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    /**
     * Reads text content from a file and returns it as a string.
     * 
     * @param file The file to read
     * @return The text content of the file
     */
    public static String readTextFromFile(File file) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
