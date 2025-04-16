// File: core/src/main/java/model/FractalState.java
package model;

import math.FractalFunction; // <-- Добавлен импорт
import math.MandelbrotFunction; // <-- Добавлен импорт для дефолтной функции
import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет полное состояние представления фрактала в определенный момент времени.
 * Включает область просмотра (Viewport), параметры рендеринга (maxIterations),
 * цветовую схему и функцию расчета фрактала.
 * Экземпляры этого класса являются неизменяемыми (immutable).
 */
public final class FractalState implements Serializable {
    private static final long serialVersionUID = 2L; // <-- Увеличена версия из-за добавления поля

    private final Viewport viewport;
    private final int maxIterations;
    private final ColorScheme colorScheme;
    private final FractalFunction fractalFunction; // <-- Новое поле

    // Константа для дефолтной функции
    private static final FractalFunction DEFAULT_FRACTAL_FUNCTION = new MandelbrotFunction();

    /**
     * Создает новый экземпляр состояния фрактала.
     *
     * @param viewport        Текущая область просмотра. Не может быть null.
     * @param maxIterations   Максимальное количество итераций. Должно быть положительным числом.
     * @param colorScheme     Цветовая схема для рендеринга. Не может быть null.
     * @param fractalFunction Функция расчета фрактала. Не может быть null.
     * @throws NullPointerException если viewport, colorScheme или fractalFunction равны null.
     * @throws IllegalArgumentException если maxIterations не положительное.
     */
    public FractalState(Viewport viewport, int maxIterations, ColorScheme colorScheme, FractalFunction fractalFunction) {
        this.viewport = Objects.requireNonNull(viewport, "Viewport не может быть null");
        this.colorScheme = Objects.requireNonNull(colorScheme, "Цветовая схема не может быть null");
        this.fractalFunction = Objects.requireNonNull(fractalFunction, "Функция фрактала не может быть null"); // <-- Проверка нового поля

        if (maxIterations <= 0) {
            throw new IllegalArgumentException("Максимальное количество итераций должно быть положительным");
        }
        this.maxIterations = maxIterations;
    }

    // --- Геттеры ---

    public Viewport getViewport() {
        return viewport;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    /**
     * Возвращает активную функцию расчета фрактала.
     * @return {@link FractalFunction}.
     */
    public FractalFunction getFractalFunction() { // <-- Геттер для нового поля
        return fractalFunction;
    }

    // --- Методы для создания нового состояния с изменениями ---

    public FractalState withViewport(Viewport newViewport) {
        // Передаем текущую fractalFunction в новый state
        return new FractalState(newViewport, this.maxIterations, this.colorScheme, this.fractalFunction);
    }

    public FractalState withMaxIterations(int newMaxIterations) {
        // Передаем текущую fractalFunction в новый state
        return new FractalState(this.viewport, newMaxIterations, this.colorScheme, this.fractalFunction);
    }

    public FractalState withColorScheme(ColorScheme newColorScheme) {
        // Передаем текущую fractalFunction в новый state
        return new FractalState(this.viewport, this.maxIterations, newColorScheme, this.fractalFunction);
    }

    /**
     * Создает новый экземпляр FractalState с указанной функцией расчета фрактала.
     * Остальные параметры остаются прежними.
     *
     * @param newFractalFunction Новая функция расчета {@link FractalFunction}.
     * @return Новый экземпляр {@link FractalState}.
     */
    public FractalState withFractalFunction(FractalFunction newFractalFunction) { // <-- Метод для изменения функции
        return new FractalState(this.viewport, this.maxIterations, this.colorScheme, newFractalFunction);
    }


    // --- Стандартные методы Object ---

    /**
     * Сравнивает это состояние с другим объектом на равенство.
     * Два состояния равны, если равны их viewport, maxIterations,
     * классы их цветовых схем и классы их функций фрактала.
     *
     * @param o Объект для сравнения.
     * @return true, если объекты равны, иначе false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FractalState that = (FractalState) o;
        return maxIterations == that.maxIterations &&
               Objects.equals(viewport, that.viewport) &&
               Objects.equals(colorScheme.getClass(), that.colorScheme.getClass()) &&
               Objects.equals(fractalFunction.getClass(), that.fractalFunction.getClass()); // <-- Сравнение по классу функции
               // Если функции имеют параметры (как Julia), нужно сравнивать и их,
               // переопределив equals/hashCode в конкретных реализациях FractalFunction.
               // Для простоты пока сравниваем только классы.
    }

    /**
     * Вычисляет хэш-код для этого состояния.
     * Основан на хэш-кодах viewport, maxIterations, класса цветовой схемы и класса функции фрактала.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        // Включаем класс функции в хэш-код
        return Objects.hash(viewport, maxIterations, colorScheme.getClass(), fractalFunction.getClass());
    }

    @Override
    public String toString() {
        return "FractalState{" +
               "viewport=" + viewport +
               ", maxIterations=" + maxIterations +
               ", colorScheme=" + colorScheme.getName() +
               ", fractalFunction=" + fractalFunction.getName() + // <-- Добавлено имя функции
               '}';
    }

    // --- Статический фабричный метод ---

    /**
     * Создает начальное состояние фрактала по умолчанию.
     * Использует {@link Viewport#DEFAULT_VIEWPORT}, 100 итераций, {@link NonlinearRGBScheme}
     * и {@link MandelbrotFunction}.
     *
     * @return Новый экземпляр {@link FractalState} с настройками по умолчанию.
     */
    public static FractalState createDefault() {
        return new FractalState(
            Viewport.DEFAULT_VIEWPORT,
            100, // Начальное количество итераций
            new NonlinearRGBScheme(),
            DEFAULT_FRACTAL_FUNCTION // Используем функцию Мандельброта по умолчанию
        );
    }
}