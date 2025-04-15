// File: core/src/main/java/model/Viewport.java
package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет прямоугольную область комплексной плоскости, отображаемую в данный момент.
 * Определяется минимальными и максимальными вещественными (x) и мнимыми (y) координатами.
 * Экземпляры этого класса являются неизменяемыми (immutable).
 */
public final class Viewport implements Serializable {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /** Минимальная вещественная координата (ось X). */
    private final double minX;
    /** Максимальная вещественная координата (ось X). */
    private final double maxX;
    /** Минимальная мнимая координата (ось Y). */
    private final double minY;
    /** Максимальная мнимая координата (ось Y). */
    private final double maxY;

    /**
     * Область просмотра по умолчанию, охватывающая стандартный вид множества Мандельброта.
     */
    public static final Viewport DEFAULT_VIEWPORT = new Viewport(-2.0, 1.0, -1.5, 1.5);

    /**
     * Создает новый экземпляр Viewport. Гарантирует, что minX <= maxX и minY <= maxY.
     *
     * @param minX Минимальная вещественная координата.
     * @param maxX Максимальная вещественная координата.
     * @param minY Минимальная мнимая координата.
     * @param maxY Максимальная мнимая координата.
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
     * Возвращает минимальную вещественную координату (minX).
     * @return minX.
     */
    public double getMinX() {
        return minX;
    }

    /**
     * Возвращает максимальную вещественную координату (maxX).
     * @return maxX.
     */
    public double getMaxX() {
        return maxX;
    }

    /**
     * Возвращает минимальную мнимую координату (minY).
     * @return minY.
     */
    public double getMinY() {
        return minY;
    }

    /**
     * Возвращает максимальную мнимую координату (maxY).
     * @return maxY.
     */
    public double getMaxY() {
        return maxY;
    }

    /**
     * Вычисляет ширину области просмотра (maxX - minX).
     * @return Ширина области.
     */
    public double getWidth() {
        return maxX - minX;
    }

    /**
     * Вычисляет высоту области просмотра (maxY - minY).
     * @return Высота области.
     */
    public double getHeight() {
        return maxY - minY;
    }

    /**
     * Вычисляет соотношение сторон области просмотра (ширина / высота).
     * @return Соотношение сторон. Возвращает {@link Double#NaN}, если высота равна нулю.
     */
    public double getAspectRatio() {
         double h = getHeight();
         // Проверка деления на ноль
         return (h == 0) ? Double.NaN : getWidth() / h;
    }

    // --- Методы для изменения Viewport (возвращают новый экземпляр) ---

    /**
     * Создает новый Viewport путем масштабирования (приближения) к указанной
     * прямоугольной области комплексной плоскости. Корректирует эту область
     * для сохранения заданного соотношения сторон.
     *
     * @param newMinX      Желаемая минимальная вещественная координата новой области.
     * @param newMaxX      Желаемая максимальная вещественная координата новой области.
     * @param newMinY      Желаемая минимальная мнимая координата новой области.
     * @param newMaxY      Желаемая максимальная мнимая координата новой области.
     * @param targetAspectRatio Желаемое соотношение сторон (например, соотношение сторон панели отрисовки).
     * @return Новый экземпляр {@link Viewport}, масштабированный и скорректированный
     *         по соотношению сторон. Если входные данные некорректны, может вернуть текущий Viewport.
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
        if (requestedWidth <= 0 || requestedHeight <= 0 || Double.isNaN(targetAspectRatio) || targetAspectRatio <= 0) {
            // Невалидный зум или соотношение сторон, возвращаем текущий viewport без изменений
            System.err.println("Invalid zoom parameters or aspect ratio. Returning current viewport.");
            return this;
        }

        // Конечные координаты, которые будут скорректированы
        double finalMinX = requestedMinX;
        double finalMaxX = requestedMaxX;
        double finalMinY = requestedMinY;
        double finalMaxY = requestedMaxY;

        // Вычисляем текущее соотношение сторон запрошенной области
        double currentAspectRatio = requestedWidth / requestedHeight;

        // Сравниваем с целевым соотношением сторон (с небольшим допуском для double)
        final double EPSILON = 1e-9;
        if (Math.abs(currentAspectRatio - targetAspectRatio) > EPSILON) {
            // Соотношения сторон не совпадают, нужно скорректировать
            if (currentAspectRatio < targetAspectRatio) {
                // Запрошенный прямоугольник "уже" (меньше ширина), чем целевой.
                // Нужно увеличить ширину, сохраняя центр по X.
                double requiredWidth = requestedHeight * targetAspectRatio;
                double widthDiff = requiredWidth - requestedWidth;
                // Распределяем разницу поровну слева и справа
                finalMinX -= widthDiff / 2.0;
                finalMaxX += widthDiff / 2.0;
            } else {
                // Запрошенный прямоугольник "шире" (меньше высота), чем целевой.
                // Нужно увеличить высоту, сохраняя центр по Y.
                double requiredHeight = requestedWidth / targetAspectRatio;
                double heightDiff = requiredHeight - requestedHeight;
                // Распределяем разницу поровну сверху и снизу
                finalMinY -= heightDiff / 2.0;
                finalMaxY += heightDiff / 2.0;
            }
        }

        // Опционально: можно ограничить новый viewport границами старого,
        // но для свободного зума это обычно не требуется.
        // finalMinX = Math.max(finalMinX, this.minX);
        // finalMaxX = Math.min(finalMaxX, this.maxX);
        // finalMinY = Math.max(finalMinY, this.minY);
        // finalMaxY = Math.min(finalMaxY, this.maxY);

        // Создаем новый экземпляр с вычисленными границами
        return new Viewport(finalMinX, finalMaxX, finalMinY, finalMaxY);
    }


     /**
      * Создает новый Viewport путем панорамирования (сдвига) текущей области просмотра.
      *
      * @param deltaX Изменение вещественной координаты (сдвиг вдоль оси X).
      * @param deltaY Изменение мнимой координаты (сдвиг вдоль оси Y).
      * @return Новый экземпляр {@link Viewport}, сдвинутый на указанные значения.
      */
     public Viewport pan(double deltaX, double deltaY) {
         // Просто прибавляем смещения к текущим границам
         return new Viewport(minX + deltaX, maxX + deltaX, minY + deltaY, maxY + deltaY);
     }

    // --- Стандартные методы Object ---

    /**
     * Сравнивает этот Viewport с другим объектом на равенство.
     * Два Viewport равны, если их соответствующие координаты (minX, maxX, minY, maxY) равны.
     *
     * @param o Объект для сравнения.
     * @return true, если объекты равны, иначе false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Viewport viewport = (Viewport) o;
        // Используем Double.compare для корректного сравнения double, включая NaN и +/- Infinity
        return Double.compare(viewport.minX, minX) == 0 &&
               Double.compare(viewport.maxX, maxX) == 0 &&
               Double.compare(viewport.minY, minY) == 0 &&
               Double.compare(viewport.maxY, maxY) == 0;
    }

    /**
     * Вычисляет хэш-код для этого Viewport.
     * Основан на хэш-кодах координат.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        return Objects.hash(minX, maxX, minY, maxY);
    }

    /**
     * Возвращает строковое представление Viewport.
     * Включает координаты, ширину и высоту.
     *
     * @return Строковое представление.
     */
    @Override
    public String toString() {
        // Используем %g для компактного представления double
        return String.format("Viewport[x=(%g, %g), y=(%g, %g), w=%g, h=%g]",
                             minX, maxX, minY, maxY, getWidth(), getHeight());
    }
}