// File: core/src/main/java/model/NonlinearRGBScheme.java
package model;

import java.awt.Color;
import java.util.Objects; // Импорт для hashCode

/**
 * Пример нелинейной цветовой схемы RGB.
 * Точки внутри множества черные. Точки снаружи переходят через различные цвета.
 * Формулы подобраны для получения эстетичного вида.
 */
public class NonlinearRGBScheme implements ColorScheme {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     * Возвращает черный цвет для точек внутри множества. Для точек снаружи
     * вычисляет RGB компоненты на основе нелинейных функций от нормализованного
     * количества итераций.
     *
     * @param iterations    Количество итераций.
     * @param maxIterations Максимальное количество итераций.
     * @return Цвет {@link Color}.
     */
    @Override
    public Color getColor(int iterations, int maxIterations) {
        // Точка внутри множества
        if (iterations >= maxIterations || maxIterations <= 1) { // Добавлена проверка maxIterations <= 1
            return Color.BLACK;
        }

        // Нормализуем количество итераций к диапазону [0, 1)
        // t = 0 соответствует 0 итераций, t стремится к 1 при iterations -> maxIterations - 1
        double t = (double) iterations / maxIterations;

        // Нелинейные функции для R, G, B.
        // Эти формулы выбраны эмпирически для получения определенного цветового градиента.
        // Можно экспериментировать с другими функциями (степенные, синусоидальные, логарифмические и т.д.).
        // Коэффициенты (9, 15, 8.5) и степени подобраны для управления яркостью и цветом.
        int r = (int) (9 * (1 - t) * t * t * t * 255);
        int g = (int) (15 * (1 - t) * (1 - t) * t * t * 255);
        int b = (int) (8.5 * (1 - t) * (1 - t) * (1 - t) * t * 255);

        // Ограничиваем значения диапазоном [0, 255], чтобы избежать выхода за пределы
        // из-за ошибок округления или особенностей формул.
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return new Color(r, g, b);
    }

     /**
     * {@inheritDoc}
     */
     @Override
     public String getName() {
         return "Nonlinear RGB"; // Название схемы для UI
     }

     /**
      * Сравнивает эту схему с другим объектом. Две NonlinearRGBScheme считаются равными.
      * @param obj Объект для сравнения.
      * @return true, если obj является экземпляром NonlinearRGBScheme, иначе false.
      */
     @Override
     public boolean equals(Object obj) {
         // Две схемы этого типа равны, так как у них нет настраиваемых параметров.
         return obj instanceof NonlinearRGBScheme;
     }

     /**
      * Возвращает хэш-код для этой схемы.
      * @return Хэш-код, основанный на классе схемы.
      */
     @Override
     public int hashCode() {
         // Хэш-код одинаков для всех экземпляров этого класса.
         return NonlinearRGBScheme.class.hashCode();
     }
}