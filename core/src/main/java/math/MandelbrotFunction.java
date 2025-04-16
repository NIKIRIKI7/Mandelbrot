// File: core/src/main/java/math/MandelbrotFunction.java
package math;

import utils.ComplexNumber;

/**
 * Реализация интерфейса {@link FractalFunction} для расчета итераций
 * классического множества Мандельброта.
 * <p>
 * Множество Мандельброта определяется для точек {@code c} комплексной плоскости.
 * Для каждой точки {@code c} выполняется итерационная формула:
 * {@code z_{n+1} = z_n^2 + c}, начиная с {@code z_0 = 0}.
 * Точка {@code c} считается принадлежащей множеству, если последовательность {@code z_n}
 * остается ограниченной (обычно проверяется условие {@code |z_n|^2 <= 4})
 * в течение заданного максимального числа итераций.
 * </p>
 * <p>
 * Этот класс является неизменяемым и реализует {@link java.io.Serializable}.
 * </p>
 */
public class MandelbrotFunction implements FractalFunction {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     * <p>
     * Вычисляет количество итераций для точки {@code c} на комплексной плоскости
     * согласно алгоритму Мандельброта.
     * </p><p>
     * Итерации выполняются по формуле {@code z = z^2 + c}, начиная с {@code z = 0}.
     * Расчет останавливается, если {@code |z|^2 > 4} (точка "убежала") или
     * достигнуто максимальное количество итераций {@code maxIterations}.
     * </p><p>
     * Параметр {@code z0Ignored} из интерфейса {@link FractalFunction#calculateIterations(ComplexNumber, ComplexNumber, int)}
     * в данной реализации **игнорируется**, так как для Мандельброта начальное значение {@code z} всегда равно 0.
     * Параметр {@code c} интерфейса используется как точка комплексной плоскости, для которой производится расчет.
     * </p>
     *
     * @param z0Ignored     Начальное значение z (игнорируется).
     * @param c             Точка комплексной плоскости, для которой вычисляются итерации.
     * @param maxIterations Максимальное число итераций.
     * @return Количество выполненных итераций (от 0 до {@code maxIterations - 1}), если точка "убежала",
     *         или {@code maxIterations}, если точка считается принадлежащей множеству.
     */
    @Override
    public int calculateIterations(ComplexNumber z0Ignored, ComplexNumber c, int maxIterations) {
        // Для Мандельброта z всегда начинается с 0
        ComplexNumber z = new ComplexNumber(0, 0);
        // c - это точка, которую мы проверяем (передана как второй аргумент)

        for (int i = 0; i < maxIterations; ++i) {
            // Проверка выхода за радиус 2 (используем квадрат модуля |z|^2 > 4 для оптимизации)
            if (z.magnitudeSquared() > 4.0) {
                return i; // Точка вне множества, возвращаем число выполненных итераций
            }
            // Итерация: z = z^2 + c
            z = z.square().add(c);
        }
        // Если цикл завершился, значит точка не "убежала" за maxIterations шагов.
        // Считаем ее принадлежащей множеству (или находящейся очень близко к нему).
        return maxIterations;
    }

    /**
     * {@inheritDoc}
     * Возвращает имя функции для отображения.
     * @return Строка "Mandelbrot Set".
     */
    @Override
    public String getName() {
        return "Mandelbrot Set";
    }

     /**
      * Сравнивает эту функцию с другим объектом.
      * Все экземпляры {@code MandelbrotFunction} считаются эквивалентными,
      * так как у них нет изменяемого состояния.
      * @param obj Объект для сравнения.
      * @return {@code true}, если {@code obj} является экземпляром {@code MandelbrotFunction}, иначе {@code false}.
      */
     @Override
     public boolean equals(Object obj) {
         // Проверка на null и на совпадение класса
         if (this == obj) return true;
         return obj != null && getClass() == obj.getClass(); // Все экземпляры эквивалентны
     }

     /**
      * Возвращает хэш-код для этой функции.
      * Так как все экземпляры эквивалентны, хэш-код основан только на классе.
      * @return Хэш-код, соответствующий классу {@code MandelbrotFunction}.
      */
     @Override
     public int hashCode() {
         return MandelbrotFunction.class.hashCode();
     }
}