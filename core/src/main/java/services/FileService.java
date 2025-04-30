package services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.FractalState;
import model.Viewport;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * <p>
 * Отвечает за:
 * <ul>
 *     <li>Сохранение состояния фрактала ({@link FractalState}) в файл формата {@code .frac}
 *         с использованием JSON-сериализации через Jackson.</li>
 *     <li>Загрузку состояния фрактала из файла {@code .frac}.</li>
 *     <li>Экспорт текущего вида фрактала в виде изображений формата JPEG или PNG,
 *         с добавлением текстовой подписи с параметрами вида (координаты, итерации).</li>
 * </ul>
 * </p>
 */
public class FileService {

    /** Расширение файла для сохранения JPEG изображений. */
    private static final String JPG_EXTENSION = ".jpg";
    /** Расширение файла для сохранения PNG изображений. */
    private static final String PNG_EXTENSION = ".png";

    /**
     * Сохраняет состояние фрактала в файл .frac (JSON).
     * 
     * Реализация сохранения использует библиотеку Jackson для сериализации объектов:
     * 1. Создаётся ObjectMapper - центральный класс Jackson для сериализации/десериализации
     * 2. Конфигурируется поведение маппера для обеспечения обратной совместимости
     *    (FAIL_ON_UNKNOWN_PROPERTIES=false позволяет игнорировать новые свойства при чтении старых файлов)
     * 3. Метод writerWithDefaultPrettyPrinter() создаёт писатель с форматированием JSON для читаемости
     * 4. Метод writeValue() сериализует объект FractalState в JSON и записывает в указанный файл
     * 
     * Процесс сериализации:
     * - Jackson анализирует все поля объекта FractalState с помощью рефлексии
     * - Для каждого поля определяется соответствующее JSON-представление
     * - Вложенные объекты (как Viewport в FractalState) также рекурсивно сериализуются
     * - Итоговый JSON преобразуется в строку и записывается в файл
     * 
     * @param state Объект FractalState, содержащий параметры фрактала для сохранения
     * @param file Файл, в который будет сохранено состояние (.frac)
     * @throws IOException При ошибках ввода-вывода или проблемах сериализации
     */
    public void saveFractalState(FractalState state, File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Игнорировать неизвестные свойства при десериализации (устойчивость к добавлениям)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, state);
    }

    /**
     * Загружает состояние фрактала из файла .frac (JSON).
     * 
     * Процесс десериализации с использованием Jackson:
     * 1. Создаётся и конфигурируется ObjectMapper для чтения JSON
     * 2. Настройка FAIL_ON_UNKNOWN_PROPERTIES=false имеет критическое значение для обратной совместимости:
     *    - Позволяет загружать старые состояния в новые версии программы с дополнительными полями
     *    - Предотвращает исключения при чтении файлов, созданных в более ранних или более поздних версиях
     * 3. Метод readValue() выполняет всю работу по десериализации:
     *    - Чтение JSON из файла и парсинг в древовидную структуру
     *    - Сопоставление полей JSON с полями класса FractalState
     *    - Создание нового экземпляра FractalState и заполнение всех его полей
     *    - Рекурсивная обработка вложенных объектов (Viewport, ColorScheme и т.д.)
     * 
     * Jackson использует рефлексию и аннотации для определения соответствия 
     * между JSON-свойствами и полями Java-объекта. Класс FractalState должен иметь:
     * - Конструктор по умолчанию (без параметров)
     * - Геттеры и сеттеры для всех сериализуемых полей
     * 
     * @param file Файл .frac с сохранённым состоянием
     * @return Новый объект FractalState с загруженными параметрами
     * @throws IOException При ошибках ввода-вывода или неверном формате файла
     */
    public FractalState loadFractalState(File file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(file, FractalState.class);
    }

    /**
     * Сохраняет изображение с подписью текущего состояния фрактала.
     * @param image изображение фрактала
     * @param state состояние для подписи
     * @param file целевой файл
     * @param format "JPEG" или "PNG"
     */
    public void saveImage(BufferedImage image, FractalState state, File file, String format) throws IOException {
        Objects.requireNonNull(image, "Изображение (image) не может быть null для сохранения");
        Objects.requireNonNull(state, "Состояние (state) не может быть null для добавления подписи");
        Objects.requireNonNull(file, "Файл для сохранения изображения не может быть null");
        Objects.requireNonNull(format, "Формат изображения (format) не может быть null");

        String lowerFormat = format.toLowerCase().trim();
        String targetFormat;
        String targetExtension;

        if ("jpeg".equals(lowerFormat) || "jpg".equals(lowerFormat)) {
            targetFormat = "jpeg";
            targetExtension = JPG_EXTENSION;
        } else if ("png".equals(lowerFormat)) {
            targetFormat = "png";
            targetExtension = PNG_EXTENSION;
        } else {
            throw new IllegalArgumentException(
                    "Неподдерживаемый формат изображения: " + format + ". Используйте JPEG или PNG."
            );
        }

        File targetFile = ensureExtension(file, targetExtension);

        int imageType = targetFormat.equals("jpeg")
                ? BufferedImage.TYPE_INT_RGB
                : BufferedImage.TYPE_INT_ARGB;
        BufferedImage imageWithCaption = new BufferedImage(
                image.getWidth(), image.getHeight(), imageType
        );

        Graphics2D g = imageWithCaption.createGraphics();
        try {
            g.drawImage(image, 0, 0, null);
            Viewport vp = state.getViewport();
            String caption = String.format(
                    "Viewport: X=[%.6g, %.6g], Y=[%.6g, %.6g], Iter: %d",
                    vp.getMinX(), vp.getMaxX(), vp.getMinY(), vp.getMaxY(), state.getMaxIterations()
            );
            g.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
            );
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );
            g.setFont(new Font("SansSerif", Font.BOLD, 14));

            // Используем FontMetrics для корректного позиционирования текста
            FontMetrics fm = g.getFontMetrics();
            int padding = 5;
            int textY = image.getHeight() - padding - fm.getDescent();
            int textX = padding;

            Color textColor = Color.WHITE;
            Color outlineColor = Color.BLACK;
            int o = 1;
            g.setColor(outlineColor);
            g.drawString(caption, textX - o, textY - o);
            g.drawString(caption, textX + o, textY - o);
            g.drawString(caption, textX - o, textY + o);
            g.drawString(caption, textX + o, textY + o);
            g.drawString(caption, textX, textY - o);
            g.drawString(caption, textX, textY + o);
            g.drawString(caption, textX - o, textY);
            g.drawString(caption, textX + o, textY);
            g.setColor(textColor);
            g.drawString(caption, textX, textY);
        } finally {
            g.dispose();
        }

        if (!ImageIO.write(imageWithCaption, targetFormat, targetFile)) {
            throw new IOException(
                    "Не удалось сохранить изображение: не найден подходящий writer для формата '"
                            + targetFormat + "'."
            );
        }
        System.out.println(
                "Изображение успешно сохранено в: "
                        + targetFile.getAbsolutePath()
                        + " (формат: " + targetFormat.toUpperCase() + ")"
        );
    }

    /**
     * Проверяет/добавляет расширение к файлу.
     */
    private File ensureExtension(File originalFile, String desiredExtension) {
        String fileName = originalFile.getName();
        String lower = fileName.toLowerCase();
        String ext = desiredExtension.toLowerCase();
        if (lower.endsWith(ext)) return originalFile;

        String nameWithoutExt;
        int idx = fileName.lastIndexOf('.');
        if (idx <= 0) nameWithoutExt = fileName;
        else nameWithoutExt = fileName.substring(0, idx);

        String newFileName = nameWithoutExt + desiredExtension;
        return new File(originalFile.getParentFile(), newFileName);
    }
}
