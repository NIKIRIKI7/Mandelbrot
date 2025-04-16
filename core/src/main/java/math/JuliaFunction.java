// File: core/src/main/java/math/JuliaFunction.java
package math;

import utils.ComplexNumber;
import java.util.Objects;
// Serializable уже есть в FractalFunction, но явно указать не помешает

/**
 * Реализация {@link FractalFunction} для множества Жюлиа.
 * Формула итерации: z_{n+1} = z_n^2 + c, где 'c' - константа для данного
 * множества Жюлиа, а z0 - начальная точка на комплексной плоскости (обычно
 * соответствующая пикселю на экране).
 * Этот класс является неизменяемым (immutable) и потокобезопасным.
 */
public final class JuliaFunction implements FractalFunction { // Объявлен final для гарантии неизменяемости

    /**
     * Версия для сериализации. Уникальна для этого класса.
     * Изменена для отражения потенциальных изменений в классе (например, добавление поля 'c').
     */
    private static final long serialVersionUID = 2L; // Отличается от MandelbrotFunction

    /**
     * Константный параметр 'c', определяющий конкретное множество Жюлиа.
     * Этот параметр остается неизменным в процессе вычисления итераций
     * для разных точек z0.
     */
    private final ComplexNumber c;

    /**
     * Создает экземпляр функции для множества Жюлиа с заданным параметром 'c'.
     *
     * @param c Константное комплексное число, определяющее форму множества Жюлиа.
     *          Не должно быть null.
     * @throws NullPointerException если параметр c равен null.
     */
    public JuliaFunction(ComplexNumber c) {
        this.c = Objects.requireNonNull(c, "Параметр 'c' для множества Жюлиа не может быть null");
    }

    /**
     * {@inheritDoc}
     * <p>
     * Вычисляет количество итераций по формуле z_{n+1} = z_n^2 + c.
     * </p><p>
     * <b>Важно:</b> В этой реализации параметр {@code cParam} из аргументов метода
     * <i>игнорируется</i>. Вместо него используется константа {@code this.c},
     * которая была передана в конструктор {@link JuliaFunction}.
     * Параметр {@code z0} используется как начальное значение итерации z.
     * </p>
     *
     * @param z0            Начальная точка итерации (комплексное число для проверки). Не должно быть null.
     * @param cParam        <i>Игнорируется в этой реализации.</i> Используется {@code this.c}.
     * @param maxIterations Максимальное количество итераций. Должно быть положительным.
     * @return Количество итераций до выхода за радиус 2, или maxIterations, если точка внутри.
     * @throws NullPointerException если z0 равен null.
     */
    @Override
    public int calculateIterations(ComplexNumber z0, ComplexNumber cParam, int maxIterations) {
        Objects.requireNonNull(z0, "Начальная точка z0 не может быть null");
        // maxIterations > 0 проверяется в FractalState или в вызывающем коде (например, Renderer)

        // Начальное значение z берем из аргумента z0
        ComplexNumber z = z0;
        // Константу c берем из поля класса (this.c), игнорируя cParam

        for (int i = 0; i < maxIterations; ++i) {
            // Проверка выхода за радиус 2 (используем квадрат модуля для оптимизации)
            // 4.0 - константа, соответствующая радиусу 2
            if (z.magnitudeSquared() > 4.0) {
                return i; // Точка вне множества
            }
            // Итерация: z = z^2 + c (используем this.c)
            z = z.square().add(this.c);
        }
        // Если цикл завершился, точка считается принадлежащей множеству
        return maxIterations;
    }

    /**
     * {@inheritDoc}
     * Возвращает имя "Julia Set".
     * @return Строка "Julia Set".
     */
    @Override
    public String getName() {
        // Можно добавить значение c в имя для отладки, но согласно ТЗ лучше оставить общим
        // return "Julia Set (c=" + c + ")";
        return "Julia Set";
    }

    /**
     * Возвращает константный параметр 'c', определяющий это множество Жюлиа.
     * @return Неизменяемое комплексное число 'c'.
     */
    public ComplexNumber getC() {
        return c; // Возвращаем ссылку на неизменяемый объект
    }

    /**
     * Сравнивает эту функцию Жюлиа с другим объектом.
     * Две функции {@code JuliaFunction} равны, если равны их параметры 'c'.
     *
     * @param o Объект для сравнения.
     * @return {@code true}, если {@code o} является {@code JuliaFunction} с тем же параметром 'c', иначе {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Проверка instanceof предпочтительнее getClass() != o.getClass() для совместимости с подклассами,
        // но так как класс final, можно использовать и getClass().
        if (o == null || getClass() != o.getClass()) return false;
        JuliaFunction that = (JuliaFunction) o;
        // Сравнение по значению параметра 'c' с использованием Objects.equals для null-безопасности
        // (хотя конструктор не допускает null, это хорошая практика)
        return Objects.equals(c, that.c);
    }

    /**
     * Вычисляет хэш-код для этой функции Жюлиа.
     * Основан на хэш-коде параметра 'c'.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        // Хэш-код зависит только от параметра 'c'
        return Objects.hash(c);
    }

    /**
     * Возвращает строковое представление функции Жюлиа, включая параметр 'c'.
     * @return Строковое представление.
     */
    @Override
    public String toString() {
        // Используем имя класса и значение поля 'c' для информативности
        return "JuliaFunction{" +
                "c=" + c +
                '}';
    }
}