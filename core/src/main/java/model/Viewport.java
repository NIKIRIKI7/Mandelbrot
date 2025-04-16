// File: core/src/main/java/model/Viewport.java
package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет прямоугольную область комплексной плоскости, отображаемую в данный момент.
 * Является неизменяемым (immutable) объектом, определяемым минимальными и максимальными
 * вещественными (X) и мнимыми (Y) координатами.
 * Используется для определения, какая часть фрактала видна, и для преобразования
 * координат между экранными пикселями и комплексными числами.
 * Реализует {@link Serializable} для сохранения/загрузки состояния.
 */
public final class Viewport implements Serializable {
    /**
     * Версия для сериализации. Важна для совместимости.
     */
    private static final long serialVersionUID = 1L;

    /** Минимальная вещественная координата (левая граница). */
    private final double minX;
    /** Максимальная вещественная координата (правая граница). */
    private final double maxX;
    /** Минимальная мнимая координата (нижняя граница). */
    private final double minY;
    /** Максимальная мнимая координата (верхняя граница). */
    private final double maxY;

    /**
     * Область просмотра по умолчанию, охватывающая стандартный вид множества Мандельброта
     * (от -2.0 до 1.0 по вещественной оси, от -1.5 до 1.5 по мнимой).
     */
    public static final Viewport DEFAULT_VIEWPORT = new Viewport(-2.0, 1.0, -1.5, 1.5);

    /**
     * Создает новый экземпляр Viewport.
     * Гарантирует, что {@code minX <= maxX} и {@code minY <= maxY},
     * автоматически упорядочивая переданные значения при необходимости.
     *
     * @param minX Минимальная или максимальная вещественная координата.
     * @param maxX Максимальная или минимальная вещественная координата.
     * @param minY Минимальная или максимальная мнимая координата.
     * @param maxY Максимальная или минимальная мнимая координата.
     */
    public Viewport(double minX, double maxX, double minY, double maxY) {
        // Гарантируем правильный порядок координат min <= max
        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    // --- Геттеры ---

    /**
     * Возвращает минимальную вещественную координату (левую границу) области просмотра.
     * @return Значение {@code minX}.
     */
    public double getMinX() {
        return minX;
    }

    /**
     * Возвращает максимальную вещественную координату (правую границу) области просмотра.
     * @return Значение {@code maxX}.
     */
    public double getMaxX() {
        return maxX;
    }

    /**
     * Возвращает минимальную мнимую координату (нижнюю границу) области просмотра.
     * @return Значение {@code minY}.
     */
    public double getMinY() {
        return minY;
    }

    /**
     * Возвращает максимальную мнимую координату (верхнюю границу) области просмотра.
     * @return Значение {@code maxY}.
     */
    public double getMaxY() {
        return maxY;
    }

    /**
     * Вычисляет ширину области просмотра ({@code maxX - minX}).
     * @return Ширина области просмотра. Может быть 0.
     */
    public double getWidth() {
        return maxX - minX;
    }

    /**
     * Вычисляет высоту области просмотра ({@code maxY - minY}).
     * @return Высота области просмотра. Может быть 0.
     */
    public double getHeight() {
        return maxY - minY;
    }

    /**
     * Вычисляет соотношение сторон области просмотра (ширина / высота).
     * Важно для сохранения пропорций при масштабировании.
     * @return Соотношение сторон (width / height). Возвращает {@link Double#NaN}, если высота равна нулю.
     */
    public double getAspectRatio() {
         double h = getHeight();
         // Проверка деления на ноль
         return (h == 0.0) ? Double.NaN : getWidth() / h; // Используем 0.0 для сравнения с double
    }

    // --- Методы для изменения Viewport (возвращают новый immutable экземпляр) ---

    /**
     * Создает новый экземпляр {@code Viewport}, представляющий результат масштабирования (приближения)
     * к указанной прямоугольной области комплексной плоскости ({@code newMinX}, {@code newMaxX},
     * {@code newMinY}, {@code newMaxY}).
     * <p>
     * Важной особенностью является корректировка результирующей области для сохранения
     * заданного целевого соотношения сторон ({@code targetAspectRatio}), которое обычно
     * соответствует соотношению сторон панели отрисовки. Это предотвращает искажения изображения.
     * Коррекция выполняется путем расширения более короткой стороны (относительно целевого соотношения)
     * так, чтобы центр исходно запрошенной области остался центром скорректированной области.
     *
     * @param newMinX      Желаемая минимальная вещественная координата новой области (или максимальная).
     * @param newMaxX      Желаемая максимальная вещественная координата новой области (или минимальная).
     * @param newMinY      Желаемая минимальная мнимая координата новой области (или максимальная).
     * @param newMaxY      Желаемая максимальная мнимая координата новой области (или минимальная).
     * @param targetAspectRatio Желаемое соотношение сторон (ширина / высота) результирующего Viewport.
     *                          Должно быть положительным числом.
     * @return Новый экземпляр {@link Viewport}, представляющий масштабированную и скорректированную
     *         область. Если входные параметры некорректны (например, нулевая ширина/высота запрошенной области
     *         или некорректное {@code targetAspectRatio}), может вернуть текущий {@code Viewport} без изменений.
     */
    public Viewport zoom(double newMinX, double newMaxX, double newMinY, double newMaxY, double targetAspectRatio) {
        // Упорядочиваем координаты, чтобы min был меньше max
        double requestedMinX = Math.min(newMinX, newMaxX);
        double requestedMaxX = Math.max(newMinX, newMaxX);
        double requestedMinY = Math.min(newMinY, newMaxY);
        double requestedMaxY = Math.max(newMinY, newMaxY);

        double requestedWidth = requestedMaxX - requestedMinX;
        double requestedHeight = requestedMaxY - requestedMinY;

        // Проверка на вырожденный прямоугольник или некорректное соотношение сторон
        // Используем небольшой допуск EPSILON для сравнения double с нулем
        final double EPSILON = 1e-9;
        if (requestedWidth < EPSILON || requestedHeight < EPSILON || Double.isNaN(targetAspectRatio) || targetAspectRatio < EPSILON) {
            System.err.println("Invalid zoom parameters or aspect ratio. Returning current viewport. "
                             + "Requested W/H: " + requestedWidth + "/" + requestedHeight + ", Target Aspect: " + targetAspectRatio);
            return this; // Невалидный зум или соотношение сторон, возвращаем текущий viewport без изменений
        }

        // Конечные координаты, которые будут скорректированы
        double finalMinX = requestedMinX;
        double finalMaxX = requestedMaxX;
        double finalMinY = requestedMinY;
        double finalMaxY = requestedMaxY;

        // Вычисляем текущее соотношение сторон запрошенной области
        double currentAspectRatio = requestedWidth / requestedHeight;

        // Сравниваем с целевым соотношением сторон (с небольшим допуском для double)
        if (Math.abs(currentAspectRatio - targetAspectRatio) > EPSILON) {
            // Соотношения сторон не совпадают, нужно скорректировать
            if (currentAspectRatio < targetAspectRatio) {
                // Запрошенный прямоугольник "уже" (меньше ширина), чем целевой.
                // Нужно увеличить ширину, сохраняя центр по X.
                double centerX = (requestedMinX + requestedMaxX) / 2.0;
                double requiredWidth = requestedHeight * targetAspectRatio;
                // double widthDiff = requiredWidth - requestedWidth;
                // finalMinX -= widthDiff / 2.0;
                // finalMaxX += widthDiff / 2.0;
                // Альтернативный расчет через центр:
                finalMinX = centerX - requiredWidth / 2.0;
                finalMaxX = centerX + requiredWidth / 2.0;

            } else {
                // Запрошенный прямоугольник "шире" (меньше высота), чем целевой.
                // Нужно увеличить высоту, сохраняя центр по Y.
                double centerY = (requestedMinY + requestedMaxY) / 2.0;
                double requiredHeight = requestedWidth / targetAspectRatio;
                // double heightDiff = requiredHeight - requestedHeight;
                // finalMinY -= heightDiff / 2.0;
                // finalMaxY += heightDiff / 2.0;
                // Альтернативный расчет через центр:
                finalMinY = centerY - requiredHeight / 2.0;
                finalMaxY = centerY + requiredHeight / 2.0;
            }
        }

        // Опционально: можно было бы ограничить новый viewport границами старого,
        // но для свободного зума это обычно не требуется.
        // finalMinX = Math.max(finalMinX, this.minX);
        // finalMaxX = Math.min(finalMaxX, this.maxX);
        // finalMinY = Math.max(finalMinY, this.minY);
        // finalMaxY = Math.min(finalMaxY, this.maxY);

        // Создаем новый неизменяемый экземпляр с вычисленными границами
        return new Viewport(finalMinX, finalMaxX, finalMinY, finalMaxY);
    }


     /**
      * Создает новый экземпляр {@code Viewport} путем панорамирования (сдвига)
      * текущей области просмотра на указанные смещения по осям X и Y
      * в координатах комплексной плоскости.
      *
      * @param deltaX Изменение вещественной координаты (сдвиг вдоль оси X).
      *               Положительное значение сдвигает вправо, отрицательное - влево.
      * @param deltaY Изменение мнимой координаты (сдвиг вдоль оси Y).
      *               Положительное значение сдвигает вверх, отрицательное - вниз.
      * @return Новый экземпляр {@link Viewport}, представляющий сдвинутую область просмотра.
      */
     public Viewport pan(double deltaX, double deltaY) {
         // Просто прибавляем смещения к текущим границам для создания нового Viewport
         return new Viewport(minX + deltaX, maxX + deltaX, minY + deltaY, maxY + deltaY);
     }

    // --- Стандартные методы Object ---

    /**
     * Сравнивает этот {@code Viewport} с другим объектом на равенство.
     * Два {@code Viewport} считаются равными, если их соответствующие координаты
     * ({@code minX}, {@code maxX}, {@code minY}, {@code maxY}) равны.
     * Используется {@link Double#compare(double, double)} для корректного сравнения
     * значений типа double, включая {@code NaN} и {@code +/-Infinity}.
     *
     * @param o Объект для сравнения.
     * @return {@code true}, если объекты равны, иначе {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Viewport viewport = (Viewport) o;
        // Сравнение всех четырех координат
        return Double.compare(viewport.minX, minX) == 0 &&
               Double.compare(viewport.maxX, maxX) == 0 &&
               Double.compare(viewport.minY, minY) == 0 &&
               Double.compare(viewport.maxY, maxY) == 0;
    }

    /**
     * Вычисляет хэш-код для этого {@code Viewport}.
     * Хэш-код зависит от всех четырех координат ({@code minX}, {@code maxX}, {@code minY}, {@code maxY}).
     * Реализация согласована с {@link #equals(Object)}.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        return Objects.hash(minX, maxX, minY, maxY);
    }

    /**
     * Возвращает строковое представление {@code Viewport}.
     * Удобно для логирования и отладки. Включает координаты границ, ширину и высоту.
     * Использует формат {@code %g} для компактного представления чисел double.
     *
     * @return Строковое представление объекта {@code Viewport}.
     */
    @Override
    public String toString() {
        return String.format("Viewport[x=(%g, %g), y=(%g, %g), w=%g, h=%g]",
                             minX, maxX, minY, maxY, getWidth(), getHeight());
    }
}