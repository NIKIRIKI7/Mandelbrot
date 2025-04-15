// File: core/src/main/java/model/GrayscaleScheme.java
package model;

import java.awt.Color;
import java.util.Objects; // Импорт для hashCode

/**
 * Простая цветовая схема в оттенках серого.
 * Точки внутри множества - черные, точки снаружи переходят от белого к черному.
 */
public class GrayscaleScheme implements ColorScheme {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L; // Явный serialVersionUID

    /**
     * {@inheritDoc}
     * Возвращает черный цвет для точек внутри множества (iterations == maxIterations)
     * и оттенки серого для точек снаружи. Чем меньше итераций, тем светлее цвет.
     *
     * @param iterations    Количество итераций.
     * @param maxIterations Максимальное количество итераций.
     * @return Цвет {@link Color}.
     */
    @Override
    public Color getColor(int iterations, int maxIterations) {
        // Точка внутри множества или некорректный maxIterations
        if (iterations >= maxIterations || maxIterations <= 1) {
            return Color.BLACK;
        }

        // Линейная интерполяция от белого (мало итераций) к почти черному (много итераций).
        // Используем (maxIterations - 1) в знаменателе, чтобы получить полный диапазон [0, 1)
        // для отношения iterations / (maxIterations - 1).
        double ratio = (double) iterations / (maxIterations - 1);

        // Инвертируем отношение (1.0 - ratio), чтобы 0 итераций давал белый цвет (255),
        // а maxIterations - 1 давал почти черный (близкий к 0).
        int gray = (int) (255 * (1.0 - ratio));

        // Ограничиваем значения диапазоном [0, 255] на случай ошибок округления.
        gray = Math.min(255, Math.max(0, gray));

        return new Color(gray, gray, gray);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Grayscale"; // Название схемы для UI
    }

    /**
     * Сравнивает эту схему с другим объектом. Две GrayscaleScheme считаются равными.
     * @param obj Объект для сравнения.
     * @return true, если obj является экземпляром GrayscaleScheme, иначе false.
     */
    @Override
    public boolean equals(Object obj) {
        // Две схемы этого типа равны, так как у них нет настраиваемых параметров.
        return obj instanceof GrayscaleScheme;
    }

    /**
     * Возвращает хэш-код для этой схемы.
     * @return Хэш-код, основанный на классе схемы.
     */
    @Override
    public int hashCode() {
        // Хэш-код одинаков для всех экземпляров этого класса.
        return GrayscaleScheme.class.hashCode();
    }
}