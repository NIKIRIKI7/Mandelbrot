// File: core/src/main/java/utils/ComplexNumber.java
package utils;

import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет комплексное число вида z = real + i * imaginary.
 * Экземпляры этого класса являются неизменяемыми (immutable).
 */
public final class ComplexNumber implements Serializable { // final для неизменяемости
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /** Вещественная часть комплексного числа. */
    private final double real;
    /** Мнимая часть комплексного числа. */
    private final double imaginary;

    /**
     * Создает новый экземпляр комплексного числа.
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
     * Возвращает вещественную часть числа.
     * @return Вещественная часть.
     */
    public double getReal() {
        return real;
    }

    /**
     * Возвращает мнимую часть числа.
     * @return Мнимая часть.
     */
    public double getImaginary() {
        return imaginary;
    }

    // --- Арифметические операции (возвращают новый экземпляр) ---

    /**
     * Вычисляет квадрат модуля комплексного числа: |z|^2 = real^2 + imaginary^2.
     * Используется вместо вычисления модуля {@code Math.sqrt(magnitudeSquared())}
     * для оптимизации в итеративных вычислениях (например, в алгоритме Мандельброта),
     * так как сравнение квадрата модуля с константой (например, 4) эквивалентно
     * сравнению самого модуля с корнем из этой константы (например, 2).
     *
     * @return Квадрат модуля числа.
     */
    public double magnitudeSquared() {
        return real * real + imaginary * imaginary;
    }

    /**
     * Складывает это комплексное число с другим. (this + other)
     *
     * @param other Комплексное число для сложения. Не должно быть null.
     * @return Новый экземпляр {@link ComplexNumber}, представляющий сумму.
     */
    public ComplexNumber add(ComplexNumber other) {
        // Objects.requireNonNull(other, "Other complex number cannot be null"); // Опционально, для защиты
        return new ComplexNumber(this.real + other.real, this.imaginary + other.imaginary);
    }

    /**
     * Умножает это комплексное число на другое. (this * other)
     * Формула: (a + bi) * (c + di) = (ac - bd) + (ad + bc)i
     *
     * @param other Комплексное число для умножения. Не должно быть null.
     * @return Новый экземпляр {@link ComplexNumber}, представляющий произведение.
     */
    public ComplexNumber multiply(ComplexNumber other) {
        // Objects.requireNonNull(other, "Other complex number cannot be null"); // Опционально
        double resultReal = this.real * other.real - this.imaginary * other.imaginary;
        double resultImaginary = this.real * other.imaginary + this.imaginary * other.real;
        return new ComplexNumber(resultReal, resultImaginary);
    }

    /**
     * Возводит это комплексное число в квадрат (z * z).
     * Является оптимизированной версией вызова {@code multiply(this)}.
     * Формула: (a + bi)^2 = (a^2 - b^2) + (2ab)i
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
     * Два комплексных числа равны, если равны их вещественные и мнимые части.
     * Использует {@link Double#compare(double, double)} для корректной обработки NaN и +/- Infinity.
     *
     * @param o Объект для сравнения.
     * @return true, если объекты равны, иначе false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComplexNumber that = (ComplexNumber) o;
        // Сравнение double через Double.compare
        return Double.compare(that.real, real) == 0 &&
               Double.compare(that.imaginary, imaginary) == 0;
    }

    /**
     * Вычисляет хэш-код для этого комплексного числа.
     * Основан на хэш-кодах вещественной и мнимой частей.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        return Objects.hash(real, imaginary);
    }

    /**
     * Возвращает строковое представление комплексного числа в формате "(real +/- imaginary i)".
     *
     * @return Строковое представление.
     */
    @Override
    public String toString() {
        // Форматируем для читаемого вывода, например, (1.0 + 2.5i) или (0.0 - 1.0i)
        return String.format("(%g %s %gi)", real, (imaginary >= 0 ? "+" : "-"), Math.abs(imaginary));
    }
}