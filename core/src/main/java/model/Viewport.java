package model;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Представляет прямоугольную область комплексной плоскости, отображаемую в данный момент.
 * Immutable-класс, определяемый минимальными и максимальными вещественными (X) и мнимыми (Y)
 * координатами. Используется для сохранения/загрузки состояния и преобразования координат.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Viewport implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Левая граница (минимальная вещественная координата). */
    private double minX;
    /** Правая граница (максимальная вещественная координата). */
    private double maxX;
    /** Нижняя граница (минимальная мнимая координата). */
    private double minY;
    /** Верхняя граница (максимальная мнимая координата). */
    private double maxY;
    /** Соотношение сторон, при необходимости сохраняется отдельно. */
    private double aspectRatio;

    /**
     * Область просмотра по умолчанию: X=[-2.0, 1.0], Y=[-1.5, 1.5].
     */
    public static final Viewport DEFAULT_VIEWPORT = new Viewport(-2.0, 1.0, -1.5, 1.5);

    /**
     * Конструктор, создаёт экземпляр с гарантией min≤max для каждой оси.
     */
    @JsonCreator
    public Viewport(
        @JsonProperty("minX") double minX,
        @JsonProperty("maxX") double maxX,
        @JsonProperty("minY") double minY,
        @JsonProperty("maxY") double maxY
    ) {
        this.minX = Math.min(minX, maxX);
        this.maxX = Math.max(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
    }

    public double getMinX() { return minX; }
    public void setMinX(double minX) { this.minX = minX; }

    public double getMaxX() { return maxX; }
    public void setMaxX(double maxX) { this.maxX = maxX; }

    public double getMinY() { return minY; }
    public void setMinY(double minY) { this.minY = minY; }

    public double getMaxY() { return maxY; }
    public void setMaxY(double maxY) { this.maxY = maxY; }

    public void setAspectRatio(double aspectRatio) { this.aspectRatio = aspectRatio; }

    /**
     * Ширина области (maxX - minX).
     * Исключаем из сериализации, чтобы в JSON не попадало поле "width".
     */
    @JsonIgnore
    public double getWidth() {
        return maxX - minX;
    }

    /**
     * Высота области (maxY - minY).
     * Исключаем из сериализации, чтобы в JSON не попадало поле "height".
     */
    @JsonIgnore
    public double getHeight() {
        return maxY - minY;
    }

    /**
     * Соотношение сторон (width/height).
     */
    public double getAspectRatio() {
        double h = getHeight();
        return (h == 0.0) ? Double.NaN : getWidth() / h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Viewport)) return false;
        Viewport that = (Viewport) o;
        return Double.compare(that.minX, minX) == 0 &&
               Double.compare(that.maxX, maxX) == 0 &&
               Double.compare(that.minY, minY) == 0 &&
               Double.compare(that.maxY, maxY) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, maxX, minY, maxY);
    }

    @Override
    public String toString() {
        return String.format("Viewport[X=[%g, %g], Y=[%g, %g]]", minX, maxX, minY, maxY);
    }

    /**
     * Сдвиг области просмотра на указанные смещения.
     * @param deltaX сдвиг по X (вещественная ось)
     * @param deltaY сдвиг по Y (мнимая ось)
     * @return новый Viewport с сдвинутыми координатами
     */
    public Viewport pan(double deltaX, double deltaY) {
        return new Viewport(
            this.minX + deltaX,
            this.maxX + deltaX,
            this.minY + deltaY,
            this.maxY + deltaY
        );
    }

    /**
     * Изменяет область просмотра на заданный прямоугольник с учётом соотношения сторон.
     * Коррекция выполняется путём расширения более короткой стороны.
     * @param targetMinX желаемая минимальная X
     * @param targetMaxX желаемая максимальная X
     * @param targetMinY желаемая минимальная Y
     * @param targetMaxY желаемая максимальная Y
     * @param targetAspectRatio требуемое соотношение сторон (width/height)
     * @return новый Viewport, отмасштабированный с учётом aspect ratio
     */
    public Viewport zoom(
        double targetMinX,
        double targetMaxX,
        double targetMinY,
        double targetMaxY,
        double targetAspectRatio
    ) {
        double requestedWidth = targetMaxX - targetMinX;
        double requestedHeight = targetMaxY - targetMinY;
        double finalMinX = targetMinX;
        double finalMaxX = targetMaxX;
        double finalMinY = targetMinY;
        double finalMaxY = targetMaxY;

        if (requestedWidth / requestedHeight > targetAspectRatio) {
            // область слишком широкая: увеличиваем высоту
            double centerY = (targetMinY + targetMaxY) / 2.0;
            double requiredHeight = requestedWidth / targetAspectRatio;
            finalMinY = centerY - requiredHeight / 2.0;
            finalMaxY = centerY + requiredHeight / 2.0;
        } else {
            // область слишком высокая: увеличиваем ширину
            double centerX = (targetMinX + targetMaxX) / 2.0;
            double requiredWidth = requestedHeight * targetAspectRatio;
            finalMinX = centerX - requiredWidth / 2.0;
            finalMaxX = centerX + requiredWidth / 2.0;
        }

        return new Viewport(finalMinX, finalMaxX, finalMinY, finalMaxY);
    }
}
