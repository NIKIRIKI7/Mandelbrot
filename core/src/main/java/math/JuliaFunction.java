// File: core/src/main/java/math/JuliaFunction.java
package math;

import utils.ComplexNumber;

import java.util.Objects;

/**
 * Реализация интерфейса {@link FractalFunction} для расчета итераций
 * множеств Жюлиа.
 * <p>
 * Множество Жюлиа {@code J(c)} определяется для фиксированного комплексного параметра {@code c}.
 * Принадлежность точки {@code z0} комплексной плоскости множеству Жюлиа {@code J(c)}
 * определяется поведением итерационной формулы:
 * {@code z_{n+1} = z_n^2 + c}, начиная с {@code z_0} (которое равно проверяемой точке).
 * Точка {@code z0} считается принадлежащей множеству {@code J(c)}, если последовательность {@code z_n}
 * остается ограниченной (обычно проверяется условие {@code |z_n|^2 <= Порог}, где порог часто равен 4)
 * в течение заданного максимального числа итераций.
 * </p><p>
 * В отличие от множества Мандельброта, здесь параметр {@code c} является константой для всего множества,
 * а начальное значение {@code z_0} меняется для каждой проверяемой точки.
 * </p><p>
 * Этот класс является неизменяемым и реализует {@link java.io.Serializable}.
 * </p>
 */
public class JuliaFunction implements FractalFunction {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Константный комплексный параметр 'c', определяющий конкретное множество Жюлиа.
     * Является частью состояния этой функции.
     */
    private final ComplexNumber c;

    /**
     * Создает функцию для расчета множества Жюлиа с заданным параметром {@code c}.
     * @param c Комплексный параметр 'c', определяющий множество. Не может быть null.
     * @throws NullPointerException если {@code c} равно null.
     */
    public JuliaFunction(ComplexNumber c) {
        this.c = Objects.requireNonNull(c, "Параметр 'c' не может быть null");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Вычисляет количество итераций для начальной точки {@code z0} на комплексной плоскости
     * для множества Жюлиа, определяемого константой {@link #c}.
     * </p><p>
     * Итерации выполняются по формуле {@code z = z^2 + this.c}, начиная с {@code z = z0}.
     * Расчет останавливается, если {@code |z|^2 > Порог} (например, 4.0) или
     * достигнуто максимальное количество итераций {@code maxIterations}.
     * </p><p>
     * Параметр {@code z0} из интерфейса {@link FractalFunction#calculateIterations(ComplexNumber, ComplexNumber, int)}
     * используется как начальная точка итераций.
     * Параметр {@code cIgnored} интерфейса **игнорируется**, так как константа {@code c}
     * хранится в поле {@link #c} этого объекта.
     * </p>
     *
     * @param z0            Начальная точка комплексной плоскости, для которой вычисляются итерации.
     * @param cIgnored      Параметр 'c' из интерфейса (игнорируется).
     * @param maxIterations Максимальное число итераций.
     * @return Количество выполненных итераций (от 0 до {@code maxIterations - 1}), если точка "убежала",
     *         или {@code maxIterations}, если точка считается принадлежащей множеству {@code J(c)}.
     */
    @Override
    public int calculateIterations(ComplexNumber z0, ComplexNumber cIgnored, int maxIterations) {
        // z начинается с точки z0 (первый аргумент)
        ComplexNumber z = z0;
        // c - это константа, хранящаяся в поле this.c

        // Порог выхода обычно 4.0 (|z|^2 > 4 => |z| > 2)
        final double escapeRadiusSquared = 4.0;

        for (int i = 0; i < maxIterations; ++i) {
            if (z.magnitudeSquared() > escapeRadiusSquared) {
                return i; // Точка вне множества
            }
            // Итерация: z = z^2 + this.c
            z = z.square().add(this.c);
        }
        // Точка не "убежала" за maxIterations шагов
        return maxIterations;
    }

    /**
     * {@inheritDoc}
     * Возвращает имя функции, включающее значение параметра {@code c}.
     * @return Строка вида "Julia Set (c=...)".
     */
    @Override
    public String getName() {
        // Включаем параметр 'c' в имя для информативности
        return "Julia Set (c=" + c.toString() + ")";
    }

    /**
     * Сравнивает эту функцию Жюлиа с другим объектом.
     * Две {@code JuliaFunction} считаются равными, если у них совпадает
     * константный параметр {@link #c}.
     * Это важно для корректного сравнения {@link FractalState}.
     *
     * @param o Объект для сравнения.
     * @return {@code true}, если {@code o} является {@code JuliaFunction} с тем же параметром {@code c}, иначе {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JuliaFunction that = (JuliaFunction) o;
        return Objects.equals(c, that.c); // Сравнение по значению параметра 'c'
    }

    /**
     * Возвращает хэш-код для этой функции Жюлиа.
     * Хэш-код зависит от константного параметра {@link #c}.
     * Реализация согласована с {@link #equals(Object)}.
     *
     * @return Хэш-код, основанный на параметре {@code c}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(c); // Хэш-код на основе 'c'
    }
}