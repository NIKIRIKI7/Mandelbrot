// File: core/src/main/java/model/NonlinearRGBScheme.java
package model;

import java.awt.Color;
import java.io.Serializable; // Импорт добавлен для полноты

/**
 * Пример нелинейной цветовой схемы RGB для визуализации фракталов.
 * Точки, принадлежащие множеству (достигшие максимального числа итераций), окрашиваются в черный цвет.
 * Точки вне множества окрашиваются в цвета, вычисляемые с помощью нелинейных функций
 * от нормализованного количества итераций. Формулы подобраны эмпирически для получения
 * определенного эстетического вида с переходами через синий, зеленый, желтый/оранжевый цвета.
 * Реализует интерфейс {@link ColorScheme} и {@link Serializable}.
 */
public class NonlinearRGBScheme implements ColorScheme {
    /**
     * Версия для сериализации. Важна для совместимости при сохранении/загрузке состояния.
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     * Возвращает цвет пикселя на основе количества итераций.
     * <ul>
     *     <li>Если {@code iterations >= maxIterations} (точка внутри множества) или {@code maxIterations <= 1} (некорректный предел), возвращает {@link Color#BLACK}.</li>
     *     <li>В противном случае, вычисляет RGB компоненты цвета. Используется нормализованное значение итераций
     *         {@code t = (double) iterations / maxIterations} в диапазоне [0, 1).
     *         Компоненты R, G, B вычисляются по нелинейным формулам, зависящим от {@code t},
     *         подобранным для создания плавных цветовых переходов.
     *         Например:
     *         <ul>
     *             <li>R: {@code 9 * (1-t) * t^3 * 255}</li>
     *             <li>G: {@code 15 * (1-t)^2 * t^2 * 255}</li>
     *             <li>B: {@code 8.5 * (1-t)^3 * t * 255}</li>
     *         </ul>
     *         Результаты ограничиваются диапазоном [0, 255].
     *     </li>
     * </ul>
     *
     * @param iterations    Количество итераций, выполненных для точки.
     * @param maxIterations Максимальное количество итераций, заданное для рендеринга.
     * @return Вычисленный цвет {@link Color} или черный.
     */
    @Override
    public Color getColor(int iterations, int maxIterations) {
        // Точка внутри множества или некорректный maxIterations
        if (iterations >= maxIterations || maxIterations <= 1) {
            return Color.BLACK;
        }

        // Нормализуем количество итераций к диапазону [0, 1)
        // t = 0 соответствует 0 итераций, t стремится к 1 при iterations -> maxIterations - 1
        // Используем maxIterations в знаменателе, так как итерации от 0 до maxIterations-1
        double t = (double) iterations / maxIterations;

        // Нелинейные функции для R, G, B.
        // Эти формулы выбраны эмпирически для получения определенного цветового градиента.
        // Можно экспериментировать с другими функциями (степенные, синусоидальные, логарифмические и т.д.).
        // Коэффициенты (9, 15, 8.5) и степени подобраны для управления яркостью и цветом.
        // Использование (1-t) обеспечивает затухание к черному при приближении к maxIterations.
        int r = (int) (9.0 * (1.0 - t) * t * t * t * 255.0);
        int g = (int) (15.0 * (1.0 - t) * (1.0 - t) * t * t * 255.0);
        int b = (int) (8.5 * (1.0 - t) * (1.0 - t) * (1.0 - t) * t * 255.0);

        // Ограничиваем значения диапазоном [0, 255], чтобы избежать выхода за пределы
        // из-за ошибок округления или особенностей формул.
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return new Color(r, g, b);
    }

     /**
     * {@inheritDoc}
     * Возвращает имя этой цветовой схемы для отображения в пользовательском интерфейсе.
     * @return Строка "Nonlinear RGB".
     */
     @Override
     public String getName() {
         return "Nonlinear RGB"; // Название схемы для UI
     }

     /**
      * Сравнивает эту цветовую схему с другим объектом.
      * Две {@code NonlinearRGBScheme} считаются равными, так как у них нет настраиваемых параметров.
      * @param obj Объект для сравнения.
      * @return {@code true}, если {@code obj} является экземпляром {@code NonlinearRGBScheme}, иначе {@code false}.
      */
     @Override
     public boolean equals(Object obj) {
         // Проверка на null и на совпадение класса
         if (this == obj) return true;
         return obj != null && getClass() == obj.getClass();
     }

     /**
      * Возвращает хэш-код для этой цветовой схемы.
      * Так как все экземпляры {@code NonlinearRGBScheme} эквивалентны,
      * хэш-код основан только на классе.
      * @return Хэш-код, соответствующий классу {@code NonlinearRGBScheme}.
      */
     @Override
     public int hashCode() {
         // Хэш-код одинаков для всех экземпляров этого класса.
         return NonlinearRGBScheme.class.hashCode();
     }
}