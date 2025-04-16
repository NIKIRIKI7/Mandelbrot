// File: core/src/main/java/utils/ComplexNumber.java
package utils;

import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет комплексное число вида {@code z = real + i * imaginary}.
 * Является неизменяемым (immutable) классом: все арифметические операции
 * возвращают новый экземпляр {@code ComplexNumber}, не изменяя текущий.
 * <p>
 * Реализует {@link Serializable} для возможности сохранения в составе {@link model.FractalState}.
 * </p>
 */
public final class ComplexNumber implements Serializable {
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /** Вещественная часть комплексного числа (Re(z)). */
    private final double real;
    /** Мнимая часть комплексного числа (Im(z)). */
    private final double imaginary;

    /**
     * Константа, представляющая комплексное число 0 (0 + 0i).
     * Может быть полезна в некоторых вычислениях (например, начальное z для Мандельброта).
     */
    public static final ComplexNumber ZERO = new ComplexNumber(0.0, 0.0);


    /**
     * Создает новый экземпляр комплексного числа с заданными вещественной и мнимой частями.
     *
     * @param real      Вещественная часть.
     * @param imaginary Мнимая часть.
     */
    public ComplexNumber(double real, double imaginary) {
        this.real = real;
        this.imaginary = imaginary;
    }

    // --- Геттеры ---

    /**
     * Возвращает вещественную часть комплексного числа.
     * @return Значение поля {@code real}.
     */
    public double getReal() {
        return real;
    }

    /**
     * Возвращает мнимую часть комплексного числа.
     * @return Значение поля {@code imaginary}.
     */
    public double getImaginary() {
        return imaginary;
    }

    // --- Арифметические операции (возвращают новый экземпляр) ---

    /**
     * Вычисляет квадрат модуля (абсолютного значения) комплексного числа:
     * {@code |z|^2 = real^2 + imaginary^2}.
     * <p>
     * Этот метод часто используется вместо вычисления самого модуля {@code sqrt(magnitudeSquared())}
     * в итеративных алгоритмах (как расчет фракталов) для оптимизации,
     * так как операция извлечения квадратного корня является относительно дорогой.
     * Сравнение квадрата модуля с квадратом радиуса (например, {@code |z|^2 > 4})
     * эквивалентно сравнению модуля с радиусом ({@code |z| > 2}).
     * </p>
     *
     * @return Квадрат модуля комплексного числа.
     */
    public double magnitudeSquared() {
        return real * real + imaginary * imaginary;
    }

    /**
     * Вычисляет модуль (абсолютное значение) комплексного числа:
     * {@code |z| = sqrt(real^2 + imaginary^2)}.
     * @return Модуль комплексного числа.
     * @see #magnitudeSquared()
     */
    public double magnitude() {
        return Math.sqrt(magnitudeSquared());
    }


    /**
     * Складывает это комплексное число с другим комплексным числом.
     * Выполняет операцию {@code (this + other)}.
     *
     * @param other Комплексное число для сложения. Не должно быть null.
     * @return Новый экземпляр {@link ComplexNumber}, представляющий сумму {@code (this.real + other.real) + i * (this.imaginary + other.imaginary)}.
     * @throws NullPointerException если {@code other} равно null.
     */
    public ComplexNumber add(ComplexNumber other) {
        Objects.requireNonNull(other, "Other complex number cannot be null for addition");
        return new ComplexNumber(this.real + other.real, this.imaginary + other.imaginary);
    }

    /**
     * Умножает это комплексное число на другое комплексное число.
     * Выполняет операцию {@code (this * other)}.
     * Формула умножения: {@code (a + bi) * (c + di) = (ac - bd) + i * (ad + bc)}.
     *
     * @param other Комплексное число для умножения. Не должно быть null.
     * @return Новый экземпляр {@link ComplexNumber}, представляющий произведение.
     * @throws NullPointerException если {@code other} равно null.
     */
    public ComplexNumber multiply(ComplexNumber other) {
        Objects.requireNonNull(other, "Other complex number cannot be null for multiplication");
        double resultReal = this.real * other.real - this.imaginary * other.imaginary;
        double resultImaginary = this.real * other.imaginary + this.imaginary * other.real;
        return new ComplexNumber(resultReal, resultImaginary);
    }

    /**
     * Возводит это комплексное число в квадрат.
     * Выполняет операцию {@code (this * this)}.
     * Является более эффективной версией вызова {@code this.multiply(this)}.
     * Формула возведения в квадрат: {@code (a + bi)^2 = (a^2 - b^2) + i * (2ab)}.
     *
     * @return Новый экземпляр {@link ComplexNumber}, представляющий квадрат исходного числа.
     */
    public ComplexNumber square() {
        double resultReal = real * real - imaginary * imaginary;
        double resultImaginary = 2 * real * imaginary;
        return new ComplexNumber(resultReal, resultImaginary);
    }

    // --- Стандартные методы Object ---

    /**
     * Сравнивает это комплексное число с другим объектом на равенство.
     * Два комплексных числа считаются равными, если равны их вещественные
     * и мнимые части соответственно.
     * Использует {@link Double#compare(double, double)} для корректного сравнения
     * значений типа double, включая {@code NaN} и {@code +/-Infinity}.
     *
     * @param o Объект для сравнения.
     * @return {@code true}, если {@code o} является {@code ComplexNumber} с теми же
     *         вещественной и мнимой частями, иначе {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplexNumber that = (ComplexNumber) o;
        // Сравнение double через Double.compare для корректной обработки NaN и т.п.
        return Double.compare(that.real, real) == 0 &&
               Double.compare(that.imaginary, imaginary) == 0;
    }

    /**
     * Вычисляет хэш-код для этого комплексного числа.
     * Хэш-код зависит от вещественной и мнимой частей.
     * Реализация согласована с {@link #equals(Object)}.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        // Используем стандартный Objects.hash для генерации хэш-кода на основе полей
        return Objects.hash(real, imaginary);
    }

    /**
     * Возвращает строковое представление комплексного числа.
     * Формат: "(real ± |imaginary|i)". Использует {@code %g} для компактного
     * представления чисел double и показывает знак мнимой части.
     * Примеры: "(1.0 + 2.5i)", "(0.0 - 1.0i)", "(3.14 + 0i)".
     *
     * @return Строковое представление комплексного числа.
     */
    @Override
    public String toString() {
        // Форматируем для читаемого вывода
        if (imaginary == 0.0) {
            return String.format("(%g)", real); // Только вещественная часть
        } else if (real == 0.0) {
             return String.format("(%gi)", imaginary); // Только мнимая часть
        } else {
            return String.format("(%g %s %gi)", real, (imaginary >= 0 ? "+" : "-"), Math.abs(imaginary));
        }
    }
}