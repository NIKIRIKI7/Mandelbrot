// File: core/src/main/java/model/ColorScheme.java
package model;

import java.awt.Color;
import java.io.Serializable;

/**
 * Интерфейс для цветовых схем, используемых при рендеринге фрактала.
 * Реализации должны быть Serializable для сохранения в файлы .frac.
 */
@FunctionalInterface // Позволяет использовать лямбды там, где ожидается этот интерфейс (если не используется getName)
public interface ColorScheme extends Serializable {
    /**
     * Версия для сериализации. Необходимо для корректной десериализации сохраненных состояний.
     */
    long serialVersionUID = 1L; // Явно указываем serialVersionUID

    /**
     * Вычисляет цвет точки на основе количества выполненных итераций
     * и максимального заданного количества итераций.
     *
     * @param iterations    Количество итераций, выполненных до выхода точки за пределы
     *                      (или maxIterations, если точка принадлежит множеству).
     * @param maxIterations Максимальное количество итераций, заданное в настройках.
     * @return Рассчитанный {@link Color}. Должен возвращать {@link Color#BLACK}, если iterations == maxIterations.
     */
    Color getColor(int iterations, int maxIterations);

    /**
     * Возвращает имя цветовой схемы, понятное пользователю.
     * Используется, например, в меню выбора схемы.
     *
     * @return Имя схемы.
     */
    default String getName() {
        // Имя по умолчанию - простое имя класса реализации.
        return this.getClass().getSimpleName();
    }
}