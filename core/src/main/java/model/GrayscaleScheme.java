// File: core/src/main/java/model/GrayscaleScheme.java
package model;

import java.awt.Color;
import java.io.Serializable; // Импорт добавлен для полноты

/**
 * Простая цветовая схема в оттенках серого для визуализации фракталов.
 * Точки, принадлежащие множеству (достигшие максимального числа итераций), окрашиваются в черный цвет.
 * Точки вне множества окрашиваются в градации серого: от белого (для точек, "убежавших" быстро,
 * т.е. с малым числом итераций) до почти черного (для точек, "убежавших" медленно,
 * т.е. с числом итераций, близким к максимальному).
 * Реализует интерфейс {@link ColorScheme} и {@link Serializable}.
 */
public class GrayscaleScheme implements ColorScheme {
    /**
     * Версия для сериализации. Важна для совместимости при сохранении/загрузке состояния.
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     * Возвращает цвет пикселя на основе количества итераций.
     * <ul>
     *     <li>Если {@code iterations >= maxIterations} (точка внутри множества) или {@code maxIterations <= 1} (некорректный предел), возвращает {@link Color#BLACK}.</li>
     *     <li>В противном случае, вычисляет оттенок серого. Используется линейная интерполяция:
     *         значение серого = 255 * (1 - (iterations / (maxIterations - 1))).
     *         Это обеспечивает плавный переход от белого (iterations=0) к темно-серому (iterations=maxIterations-1).</li>
     * </ul>
     *
     * @param iterations    Количество итераций, выполненных для точки.
     * @param maxIterations Максимальное количество итераций, заданное для рендеринга.
     * @return Вычисленный цвет {@link Color} в оттенках серого или черный.
     */
    @Override
    public Color getColor(int iterations, int maxIterations) {
        // Точка внутри множества или некорректный maxIterations
        if (iterations >= maxIterations || maxIterations <= 1) {
            return Color.BLACK;
        }

        // Линейная интерполяция от белого (мало итераций) к почти черному (много итераций).
        // Используем (maxIterations - 1.0) в знаменателе для корректной обработки диапазона [0, maxIterations-1].
        // Используем double для точности вычислений.
        double ratio = (double) iterations / (maxIterations - 1.0);

        // Инвертируем отношение (1.0 - ratio), чтобы 0 итераций давал белый цвет (255),
        // а maxIterations - 1 давал почти черный (близкий к 0).
        // Умножаем на 255 и преобразуем в int.
        int gray = (int) (255.0 * (1.0 - ratio));

        // Ограничиваем значения диапазоном [0, 255] на случай ошибок округления или граничных значений.
        gray = Math.min(255, Math.max(0, gray));

        return new Color(gray, gray, gray);
    }

    /**
     * {@inheritDoc}
     * Возвращает имя этой цветовой схемы для отображения в пользовательском интерфейсе.
     * @return Строка "Grayscale".
     */
    @Override
    public String getName() {
        return "Grayscale"; // Название схемы для UI
    }

    /**
     * Сравнивает эту цветовую схему с другим объектом.
     * Две {@code GrayscaleScheme} считаются равными, так как у них нет настраиваемых параметров.
     * @param obj Объект для сравнения.
     * @return {@code true}, если {@code obj} является экземпляром {@code GrayscaleScheme}, иначе {@code false}.
     */
    @Override
    public boolean equals(Object obj) {
        // Проверка на null и на совпадение класса
        if (this == obj) return true; // Оптимизация: если это тот же объект
        return obj != null && getClass() == obj.getClass(); // Достаточно сравнить классы
    }

    /**
     * Возвращает хэш-код для этой цветовой схемы.
     * Так как все экземпляры {@code GrayscaleScheme} эквивалентны,
     * хэш-код основан только на классе.
     * @return Хэш-код, соответствующий классу {@code GrayscaleScheme}.
     */
    @Override
    public int hashCode() {
        // Хэш-код одинаков для всех экземпляров этого класса.
        return GrayscaleScheme.class.hashCode();
    }
}