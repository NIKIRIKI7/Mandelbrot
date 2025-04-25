// File: core/src/main/java/model/ColorScheme.java
package model;

import java.awt.Color;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Функциональный интерфейс, определяющий стратегию преобразования
 * количества итераций, полученных при расчете точки фрактала, в цвет.
 * Реализации этого интерфейса позволяют легко менять внешний вид
 * визуализации фрактала.
 * <p>
 * Реализации должны быть {@link Serializable}, чтобы {@link FractalState},
 * содержащий схему, можно было сохранять и загружать.
 * </p>
 * <p>
 * Помечен как {@link FunctionalInterface}, так как содержит единственный
 * абстрактный метод {@link #getColor(int, int)}, хотя наличие
 * метода по умолчанию {@link #getName()} этому не мешает.
 * </p>
 */
@FunctionalInterface
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NonlinearRGBScheme.class, name = "NonlinearRGB"),
    @JsonSubTypes.Type(value = GrayscaleScheme.class, name = "Grayscale")
})
public interface ColorScheme extends Serializable {
    /**
     * Версия для сериализации. Необходимо явно указывать для всех
     * {@link Serializable} классов и интерфейсов для контроля совместимости версий.
     */
    long serialVersionUID = 1L;

    /**
     * Вычисляет и возвращает цвет для точки фрактала на основе
     * количества выполненных итераций и максимального заданного
     * количества итераций для рендеринга.
     * <p>
     * По соглашению, если {@code iterations == maxIterations}, это означает,
     * что точка считается принадлежащей множеству (не "убежала" за предел),
     * и для таких точек обычно возвращается {@link Color#BLACK}.
     * </p>
     *
     * @param iterations    Количество итераций, выполненных до выхода точки за предел
     *                      (или {@code maxIterations}, если точка принадлежит множеству).
     * @param maxIterations Максимальное количество итераций, заданное в настройках
     *                      рендеринга (используется как верхний предел и для нормализации).
     * @return Рассчитанный {@link Color} для данной точки.
     */
    Color getColor(int iterations, int maxIterations);

    /**
     * Возвращает имя цветовой схемы, понятное пользователю.
     * Это имя может использоваться, например, в элементах управления
     * пользовательского интерфейса (меню, списки выбора).
     * <p>
     * Реализация по умолчанию возвращает простое имя класса реализации схемы.
     * Реализации могут переопределить этот метод для предоставления более
     * осмысленного или локализованного имени.
     * </p>
     *
     * @return Строка с именем цветовой схемы.
     */
    default String getName() {
        // Имя по умолчанию - простое имя класса реализации.
        return this.getClass().getSimpleName();
    }

    // Метод equals() и hashCode() не объявляются в интерфейсе,
    // но реализации ДОЛЖНЫ их переопределять, если они имеют состояние,
    // или для корректного сравнения в FractalState.
    // Стандартное поведение equals/hashCode от Object не подходит для Serializable объектов
    // и для логики сравнения состояний.
}