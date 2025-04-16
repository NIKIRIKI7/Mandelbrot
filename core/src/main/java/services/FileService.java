// File: core/src/main/java/services/FileService.java
package services;

import model.FractalState;
import model.Viewport;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Objects;

/**
 * Сервисный класс (Фасад), предоставляющий операции для работы с файлами,
 * связанные с приложением визуализации фракталов.
 * <p>
 * Отвечает за:
 * <ul>
 *     <li>Сохранение состояния фрактала ({@link FractalState}) в бинарный файл формата {@code .frac}
 *         с использованием сериализации Java.</li>
 *     <li>Загрузку состояния фрактала из файла {@code .frac}.</li>
 *     <li>Экспорт текущего вида фрактала в виде изображений формата JPEG или PNG,
 *         с добавлением текстовой подписи с параметрами вида (координаты, итерации).</li>
 * </ul>
 * </p>
 */
public class FileService {

    /** Расширение файла для сохранения состояния фрактала. */
    private static final String FRAC_EXTENSION = ".frac";
    /** Расширение файла для сохранения JPEG изображений. */
    private static final String JPG_EXTENSION = ".jpg";
    /** Расширение файла для сохранения PNG изображений. */
    private static final String PNG_EXTENSION = ".png";


    /**
     * Сохраняет переданное состояние фрактала {@link FractalState} в указанный файл
     * с использованием механизма сериализации Java.
     * Файлу автоматически добавляется расширение {@value #FRAC_EXTENSION}, если оно отсутствует.
     *
     * @param state    Состояние {@link FractalState} для сохранения. Не может быть null.
     * @param file     Файл назначения. Не может быть null. Путь к файлу должен существовать.
     *                 Если имя файла не содержит {@value #FRAC_EXTENSION}, оно будет добавлено.
     * @throws IOException Если возникает ошибка ввода/вывода во время записи файла (например, нет прав доступа).
     * @throws NullPointerException если {@code state} или {@code file} равны null.
     */
    public void saveFractalState(FractalState state, File file) throws IOException {
        Objects.requireNonNull(state, "Состояние FractalState не может быть null для сохранения");
        Objects.requireNonNull(file, "Файл для сохранения состояния не может быть null");

        // Убедимся, что файл имеет правильное расширение
        File targetFile = ensureExtension(file, FRAC_EXTENSION);

        // Используем try-with-resources для автоматического закрытия потоков
        try (ObjectOutputStream oos = new ObjectOutputStream(
                                           new BufferedOutputStream(
                                               new FileOutputStream(targetFile)))) {
            oos.writeObject(state); // Сериализуем объект состояния
            System.out.println("Состояние фрактала успешно сохранено в: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении состояния фрактала в " + targetFile.getAbsolutePath() + ": " + e.getMessage());
            throw e; // Перебрасываем исключение дальше
        }
    }

    /**
     * Загружает состояние фрактала {@link FractalState} из указанного файла формата {@code .frac}.
     * Выполняет десериализацию объекта из файла.
     *
     * @param file Файл формата {@code .frac} для загрузки. Не может быть null, должен существовать
     *             и иметь расширение {@value #FRAC_EXTENSION}.
     * @return Загруженный объект {@link FractalState}.
     * @throws IOException            Если возникает ошибка ввода/вывода во время чтения файла.
     * @throws FileNotFoundException  Если файл не найден или не является обычным файлом.
     * @throws ClassNotFoundException Если класс {@code FractalState} (или один из его зависимых классов)
     *                                не найден во время десериализации (проблема classpath или версии).
     * @throws ClassCastException     Если объект, десериализованный из файла, не является
     *                                экземпляром {@code FractalState}.
     * @throws IllegalArgumentException Если файл не имеет расширения {@value #FRAC_EXTENSION}.
     * @throws NullPointerException   если {@code file} равен null.
     */
    public FractalState loadFractalState(File file) throws IOException, ClassNotFoundException {
        Objects.requireNonNull(file, "Файл для загрузки состояния не может быть null");

        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("Файл не найден или не является обычным файлом: " + file.getAbsolutePath());
        }
         // Проверяем расширение файла
         if (!file.getName().toLowerCase().endsWith(FRAC_EXTENSION)) {
             throw new IllegalArgumentException("Файл для загрузки состояния должен иметь расширение " + FRAC_EXTENSION);
         }

        // Используем try-with-resources для автоматического закрытия потоков
        try (ObjectInputStream ois = new ObjectInputStream(
                                           new BufferedInputStream(
                                               new FileInputStream(file)))) {
            Object loadedObject = ois.readObject(); // Десериализуем объект
            if (loadedObject instanceof FractalState) {
                System.out.println("Состояние фрактала успешно загружено из: " + file.getAbsolutePath());
                return (FractalState) loadedObject;
            } else {
                // Если файл содержит объект другого типа
                throw new ClassCastException("Файл не содержит корректный объект FractalState. Обнаружен тип: "
                                             + (loadedObject != null ? loadedObject.getClass().getName() : "null"));
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.err.println("Ошибка при загрузке состояния фрактала из " + file.getAbsolutePath() + ": " + e.getMessage());
            throw e; // Перебрасываем исключение дальше
        }
    }

     /**
     * Сохраняет переданное изображение {@link BufferedImage} в файл указанного формата (JPEG или PNG).
     * Перед сохранением на изображение добавляется текстовая подпись (caption), содержащая
     * информацию о текущем виде фрактала (координаты {@link Viewport} и максимальное
     * количество итераций) из объекта {@code state}.
     * Файлу автоматически присваивается соответствующее расширение ({@value #JPG_EXTENSION} или {@value #PNG_EXTENSION}),
     * если оно отсутствует или некорректно.
     *
     * @param image     Изображение {@link BufferedImage} для сохранения. Не может быть null.
     * @param state     Текущее состояние {@link FractalState}, используемое для получения
     *                  информации (координаты, итерации) для подписи на изображении. Не может быть null.
     * @param file      Файл назначения. Не может быть null. Путь к файлу должен существовать.
     *                  Расширение будет скорректировано автоматически.
     * @param format    Строка, определяющая желаемый формат сохранения: "JPEG" (или "JPG") или "PNG"
     *                  (регистр не важен). Не может быть null.
     * @throws IOException              Если возникает ошибка ввода/вывода во время записи файла
     *                                  или если не найден подходящий {@code ImageWriter} для формата.
     * @throws IllegalArgumentException Если указан неподдерживаемый {@code format}.
     * @throws NullPointerException     если {@code image}, {@code state}, {@code file} или {@code format} равны null.
     */
     public void saveImage(BufferedImage image, FractalState state, File file, String format) throws IOException {
         Objects.requireNonNull(image, "Изображение (image) не может быть null для сохранения");
         Objects.requireNonNull(state, "Состояние (state) не может быть null для добавления подписи");
         Objects.requireNonNull(file, "Файл для сохранения изображения не может быть null");
         Objects.requireNonNull(format, "Формат изображения (format) не может быть null");

         String lowerFormat = format.toLowerCase().trim();
         String targetFormat; // Финальный формат для ImageIO ("jpeg" или "png")
         String targetExtension; // Ожидаемое расширение файла (".jpg" или ".png")

         if ("jpeg".equals(lowerFormat) || "jpg".equals(lowerFormat)) {
              targetFormat = "jpeg";
              targetExtension = JPG_EXTENSION;
         } else if ("png".equals(lowerFormat)) {
              targetFormat = "png";
              targetExtension = PNG_EXTENSION;
         } else {
             // Если формат не JPEG и не PNG
             throw new IllegalArgumentException("Неподдерживаемый формат изображения: " + format + ". Используйте JPEG или PNG.");
         }

         // Убедимся, что файл имеет правильное расширение
         File targetFile = ensureExtension(file, targetExtension);

         // Создаем новое изображение для рисования подписи, чтобы не изменять оригинал
         // Для JPEG используем TYPE_INT_RGB (без альфа-канала), для PNG - TYPE_INT_ARGB (с альфа-каналом)
         int imageType = targetFormat.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
         BufferedImage imageWithCaption = new BufferedImage(image.getWidth(), image.getHeight(), imageType);

         // Получаем Graphics2D для рисования на новом изображении
         Graphics2D g = imageWithCaption.createGraphics();
         try {
             // 1. Рисуем исходное изображение на новом холсте
             g.drawImage(image, 0, 0, null);

             // 2. Добавляем текстовую подпись с параметрами вида
             Viewport vp = state.getViewport();
             // Используем %g для компактного вывода double, %.6g для большей точности при необходимости
             String caption = String.format("Viewport: X=[%.6g, %.6g], Y=[%.6g, %.6g], Iter: %d",
                                            vp.getMinX(), vp.getMaxX(), vp.getMinY(), vp.getMaxY(), state.getMaxIterations());

             // Настройки для рендеринга текста (шрифт, сглаживание)
             g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
             g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
             g.setFont(new Font("SansSerif", Font.BOLD, 14)); // Можно сделать настраиваемым

             // Выбираем позицию и цвет для текста
             FontMetrics fm = g.getFontMetrics();
             int textHeight = fm.getAscent(); // Высота текста над базовой линией
             int padding = 5; // Отступ от краев
             int textY = image.getHeight() - padding; // Позиция Y базовой линии текста (внизу)
             int textX = padding;                   // Позиция X текста (слева)

             // Рисуем текст с контрастной обводкой для лучшей читаемости на любом фоне
             Color textColor = Color.WHITE;
             Color outlineColor = Color.BLACK;
             int outlineOffset = 1; // Смещение обводки

             // Рисуем обводку (текст со смещением в 8 направлениях)
             g.setColor(outlineColor);
             g.drawString(caption, textX - outlineOffset, textY - outlineOffset);
             g.drawString(caption, textX + outlineOffset, textY - outlineOffset);
             g.drawString(caption, textX - outlineOffset, textY + outlineOffset);
             g.drawString(caption, textX + outlineOffset, textY + outlineOffset);
             g.drawString(caption, textX, textY - outlineOffset);
             g.drawString(caption, textX, textY + outlineOffset);
             g.drawString(caption, textX - outlineOffset, textY);
             g.drawString(caption, textX + outlineOffset, textY);

             // Рисуем основной текст поверх обводки
             g.setColor(textColor);
             g.drawString(caption, textX, textY);

         } finally {
             g.dispose(); // Освобождаем ресурсы Graphics2D - ВАЖНО!
         }

         // 3. Сохраняем изображение с подписью в выбранном формате
         boolean success = ImageIO.write(imageWithCaption, targetFormat, targetFile);

         if (!success) {
             // ImageIO.write возвращает false, если не найден подходящий ImageWriter
             throw new IOException("Не удалось сохранить изображение: не найден подходящий writer для формата '" + targetFormat + "'.");
         }
         System.out.println("Изображение успешно сохранено в: " + targetFile.getAbsolutePath() + " (формат: " + targetFormat.toUpperCase() + ")");
     }

    /**
     * Вспомогательный метод для проверки и добавления/замены расширения файла.
     * @param originalFile Исходный файл.
     * @param desiredExtension Ожидаемое расширение (включая точку, например, ".txt").
     * @return Новый объект File с гарантированно правильным расширением.
     */
    private File ensureExtension(File originalFile, String desiredExtension) {
        String fileName = originalFile.getName();
        String lowerCaseFileName = fileName.toLowerCase();
        String lowerCaseDesiredExtension = desiredExtension.toLowerCase();

        // Если расширение уже правильное, возвращаем исходный файл
        if (lowerCaseFileName.endsWith(lowerCaseDesiredExtension)) {
            return originalFile;
        }

        // Иначе, формируем новое имя
        String nameWithoutExt;
        int lastDotIndex = fileName.lastIndexOf('.');
        // Если точки нет или она в самом начале (скрытый файл)
        if (lastDotIndex <= 0) {
            nameWithoutExt = fileName;
        } else {
            // Отбрасываем текущее расширение
            nameWithoutExt = fileName.substring(0, lastDotIndex);
        }
        // Создаем новое имя с нужным расширением
        String newFileName = nameWithoutExt + desiredExtension;
        // Возвращаем новый объект File с тем же путем, но новым именем
        return new File(originalFile.getParentFile(), newFileName);
    }
}