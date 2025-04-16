// File: core/src/main/java/math/JuliaFunction.java
package math;

import utils.ComplexNumber;
import java.util.Objects;

/**
 * Реализация FractalFunction для множеств Жюлиа.
 * Формула итерации: z_{n+1} = z_n^2 + c, где z0 - точка плоскости, а c - константа.
 */
public class JuliaFunction implements FractalFunction {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Константный параметр 'c' для данного множества Жюлиа.
     */
    private final ComplexNumber c;

    /**
     * Создает функцию для множества Жюлиа с заданным параметром c.
     * @param c Комплексный параметр 'c'. Не может быть null.
     */
    public JuliaFunction(ComplexNumber c) {
        this.c = Objects.requireNonNull(c, "Параметр 'c' не может быть null");
    }

    /**
     * {@inheritDoc}
     * Для множества Жюлиа начальное значение z равно проверяемой точке (z0),
     * а параметр 'c' является константой, заданной при создании функции.
     */
    @Override
    public int calculateIterations(ComplexNumber z0, ComplexNumber cIgnored, int maxIterations) {
        // z начинается с точки z0
        ComplexNumber z = z0;
        // c - это константа, хранящаяся в поле this.c

        for (int i = 0; i < maxIterations; ++i) {
            if (z.magnitudeSquared() > 4.0) { // Порог обычно 4
                return i;
            }
            // Итерация: z = z^2 + this.c
            z = z.square().add(this.c);
        }
        return maxIterations;
    }

    @Override
    public String getName() {
        return "Julia Set (c=" + c.toString() + ")"; // Включаем параметр 'c' в имя
    }

    // Важно переопределить equals/hashCode, если состояния с разными
    // JuliaFunction должны считаться разными (например, для сохранения/загрузки)
    // Сравниваем по значению параметра 'c'.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JuliaFunction that = (JuliaFunction) o;
        return Objects.equals(c, that.c); // Сравнение по 'c'
    }

    @Override
    public int hashCode() {
        return Objects.hash(c); // Хэш-код на основе 'c'
    }
}