package services;

import model.FractalState;
import model.Viewport;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Objects;

/**
 * Service class for handling file operations like saving and loading fractal states
 * and exporting images.
 */
public class FileService {

    /**
     * Saves the given FractalState to a file in the custom .frac format.
     *
     * @param state    The FractalState to save.
     * @param file     The File object representing the destination file.
     * @throws IOException If an I/O error occurs during saving.
     */
    public void saveFractalState(FractalState state, File file) throws IOException {
        Objects.requireNonNull(state, "FractalState cannot be null");
        Objects.requireNonNull(file, "File cannot be null");

        // Убедимся, что файл имеет расширение .frac
        if (!file.getName().toLowerCase().endsWith(".frac")) {
             file = new File(file.getParentFile(), file.getName() + ".frac");
        }


        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(state);
            System.out.println("Fractal state saved to: " + file.getAbsolutePath());
        }
    }

    /**
     * Loads a FractalState from a .frac file.
     *
     * @param file The File object representing the .frac file to load.
     * @return The loaded FractalState.
     * @throws IOException            If an I/O error occurs during loading.
     * @throws ClassNotFoundException If the file does not contain a valid FractalState object.
     * @throws ClassCastException     If the object in the file is not a FractalState.
     */
    public FractalState loadFractalState(File file) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(file, "File cannot be null");
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found or is not a regular file: " + file.getAbsolutePath());
        }
         if (!file.getName().toLowerCase().endsWith(".frac")) {
             throw new IllegalArgumentException("File must have a .frac extension");
         }


        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            Object loadedObject = ois.readObject();
            if (loadedObject instanceof FractalState) {
                System.out.println("Fractal state loaded from: " + file.getAbsolutePath());
                return (FractalState) loadedObject;
            } else {
                throw new ClassCastException("File does not contain a valid FractalState object. Found: "
                                             + loadedObject.getClass().getName());
            }
        }
    }

     /**
     * Saves the given BufferedImage to a file in either JPEG or PNG format.
     * Adds a caption with viewport coordinates to the image.
     *
     * @param image     The BufferedImage to save.
     * @param state     The FractalState used to get viewport coordinates for the caption.
     * @param file      The File object representing the destination file.
     * @param format    The desired format ("JPEG" or "PNG", case-insensitive).
     * @throws IOException              If an I/O error occurs during saving.
     * @throws IllegalArgumentException If the format is unsupported or image/state is null.
     */
     public void saveImage(BufferedImage image, FractalState state, File file, String format) throws IOException {
         Objects.requireNonNull(image, "Image cannot be null");
         Objects.requireNonNull(state, "State cannot be null for adding caption");
         Objects.requireNonNull(file, "File cannot be null");
         Objects.requireNonNull(format, "Format cannot be null");

         String lowerFormat = format.toLowerCase();
         if (!"jpeg".equals(lowerFormat) && !"jpg".equals(lowerFormat) && !"png".equals(lowerFormat)) {
             throw new IllegalArgumentException("Unsupported image format: " + format + ". Use JPEG or PNG.");
         }

         String targetFormat = "png"; // По умолчанию PNG
         String expectedExtension = ".png";
         if ("jpeg".equals(lowerFormat) || "jpg".equals(lowerFormat)) {
              targetFormat = "jpeg";
              expectedExtension = ".jpg";
         }

         // Проверяем и исправляем расширение файла
         String fileName = file.getName();
         if (!fileName.toLowerCase().endsWith(expectedExtension)) {
              // Удаляем любое существующее расширение и добавляем правильное
              int dotIndex = fileName.lastIndexOf('.');
              String nameWithoutExt = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
              file = new File(file.getParentFile(), nameWithoutExt + expectedExtension);
         }


         // Создаем копию изображения для рисования текста (чтобы не изменять оригинал)
         BufferedImage imageWithCaption = new BufferedImage(image.getWidth(), image.getHeight(),
                                                            targetFormat.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
         Graphics2D g = imageWithCaption.createGraphics();

         try {
             // Рисуем исходное изображение
             g.drawImage(image, 0, 0, null);

             // Добавляем текст с координатами
             Viewport vp = state.getViewport();
             String caption = String.format("Viewport: X=[%.6g, %.6g], Y=[%.6g, %.6g], Iter: %d",
                                            vp.getMinX(), vp.getMaxX(), vp.getMinY(), vp.getMaxY(), state.getMaxIterations());

             // Настройки шрифта и цвета
             g.setFont(new Font("SansSerif", Font.BOLD, 14));
             // Выбираем цвет, контрастный фону (простой вариант - белый с черной обводкой)
             int textY = image.getHeight() - 10; // Позиция Y текста (снизу)
             int textX = 10;                   // Позиция X текста (слева)

             // Рисуем обводку (черную)
             g.setColor(Color.BLACK);
             g.drawString(caption, textX - 1, textY - 1);
             g.drawString(caption, textX + 1, textY - 1);
             g.drawString(caption, textX - 1, textY + 1);
             g.drawString(caption, textX + 1, textY + 1);
             g.drawString(caption, textX, textY - 1);
             g.drawString(caption, textX, textY + 1);
             g.drawString(caption, textX - 1, textY);
             g.drawString(caption, textX + 1, textY);


             // Рисуем основной текст (белый)
             g.setColor(Color.WHITE);
             g.drawString(caption, textX, textY);

         } finally {
             g.dispose(); // Освобождаем ресурсы Graphics2D
         }

         // Сохраняем изображение с подписью
         boolean success = ImageIO.write(imageWithCaption, targetFormat, file);
         if (!success) {
             throw new IOException("Failed to write image using ImageIO for format: " + targetFormat +
                                   ". No appropriate writer found?");
         }
         System.out.println("Image saved to: " + file.getAbsolutePath() + " as " + targetFormat.toUpperCase());
     }
}