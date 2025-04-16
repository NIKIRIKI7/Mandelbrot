// File: core/src/main/java/math/MandelbrotFunction.java
package math;

import utils.ComplexNumber;

/**
 * Реализация FractalFunction для классического множества Мандельброта.
 * Формула итерации: z_{n+1} = z_n^2 + c, где z0 = 0, а c - точка плоскости.
 */
public class MandelbrotFunction implements FractalFunction {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /*
     * {@inheritDoc}
     * Для множества Мандельброта начальное значение z всегда 0,
     * а параметр c равен самой проверяемой точке (в имплементации используем параметр c как точку).
     * Параметр z0 из интерфейса здесь игнорируется, т.к. он всегда 0.
     */
    @Override
    public int calculateIterations(ComplexNumber z0Ignored, ComplexNumber c, int maxIterations) {
        // Для Мандельброта z всегда начинается с 0
        ComplexNumber z = new ComplexNumber(0, 0);
        // c - это точка, которую мы проверяем (передана как второй аргумент)

        for (int i = 0; i < maxIterations; ++i) {
            // Проверка выхода за радиус 2 (используем квадрат модуля для оптимизации)
            if (z.magnitudeSquared() > 4.0) {
                return i; // Точка вне множества
            }
            // Итерация: z = z^2 + c
            z = z.square().add(c);
        }
        // Если цикл завершился, точка считается принадлежащей множеству
        return maxIterations;
    }

    @Override
    public String getName() {
        return "Mandelbrot Set";
    }

     // equals и hashCode для Serializable не обязательны,
     // но могут быть полезны, если сравнивать функции
     @Override
     public boolean equals(Object obj) {
         return obj instanceof MandelbrotFunction; // Все экземпляры эквивалентны
     }

     @Override
     public int hashCode() {
         return MandelbrotFunction.class.hashCode();
     }
}