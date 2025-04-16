// File: core/src/main/java/math/FractalFunction.java
package math;

import utils.ComplexNumber;
import java.io.Serializable;

/**
 * Функциональный интерфейс для вычисления количества итераций для точки фрактала.
 * Позволяет абстрагироваться от конкретного типа фрактала (Мандельброт, Жюлиа и т.д.).
 * Должен быть Serializable для сохранения в FractalState.
 */
@FunctionalInterface
public interface FractalFunction extends Serializable {
    /**
     * Версия для сериализации.
     */
    long serialVersionUID = 1L;

    /**
     * Вычисляет количество итераций для заданной точки и параметра.
     *
     * @param z0            Начальная точка итерации (обычно точка комплексной плоскости для проверки).
     * @param c             Параметр функции (константа для множества Жюлиа,
     *                      или та же точка z0 для множества Мандельброта).
     * @param maxIterations Максимальное количество итераций.
     * @return Количество выполненных итераций до выхода за предел,
     *         или maxIterations, если точка принадлежит множеству.
     */
    int calculateIterations(ComplexNumber z0, ComplexNumber c, int maxIterations);

    /**
     * Возвращает имя функции фрактала (для отладки или UI).
     * @return Имя функции.
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}