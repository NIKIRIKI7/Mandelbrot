// File: core/src/main/java/model/FractalState.java
package model;

// ColorScheme импортируется без model. префикса, т.к. находится в том же пакете
// import model.ColorScheme; - избыточно

import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет полное состояние представления фрактала в определенный момент времени.
 * Включает область просмотра (Viewport), параметры рендеринга (maxIterations) и цветовую схему.
 * Экземпляры этого класса являются неизменяемыми (immutable). Для модификации состояния
 * используются методы `with*`, которые возвращают новый экземпляр с измененными данными.
 */
public final class FractalState implements Serializable { // final для гарантии неизменяемости
    /**
     * Версия для сериализации.
     */
    private static final long serialVersionUID = 1L;

    /** Текущая область просмотра в комплексной плоскости. */
    private final Viewport viewport;
    /** Максимальное количество итераций для расчета принадлежности точки множеству. */
    private final int maxIterations;
    /** Цветовая схема, используемая для рендеринга. */
    private final ColorScheme colorScheme;
    // UndoManager здесь не хранится, он является частью логики ViewModel, а не состояния данных.

    /**
     * Создает новый экземпляр состояния фрактала.
     *
     * @param viewport      Текущая область просмотра. Не может быть null.
     * @param maxIterations Максимальное количество итераций. Должно быть положительным числом.
     * @param colorScheme   Цветовая схема для рендеринга. Не может быть null.
     * @throws NullPointerException если viewport или colorScheme равны null.
     * @throws IllegalArgumentException если maxIterations не положительное.
     */
    public FractalState(Viewport viewport, int maxIterations, ColorScheme colorScheme) {
        // Проверка аргументов на null с помощью Objects.requireNonNull
        this.viewport = Objects.requireNonNull(viewport, "Viewport не может быть null");
        this.colorScheme = Objects.requireNonNull(colorScheme, "Цветовая схема не может быть null");

        // Проверка корректности maxIterations
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("Максимальное количество итераций должно быть положительным");
        }
        this.maxIterations = maxIterations;
    }

    // --- Геттеры ---

    /**
     * Возвращает текущую область просмотра.
     * @return {@link Viewport}.
     */
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * Возвращает максимальное количество итераций.
     * @return Максимальное количество итераций.
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Возвращает активную цветовую схему.
     * @return {@link ColorScheme}.
     */
    public ColorScheme getColorScheme() {
        return colorScheme;
    }

     // --- Методы для создания нового состояния с изменениями (паттерн copy-on-write) ---

    /**
     * Создает новый экземпляр FractalState с указанной областью просмотра.
     * Остальные параметры (maxIterations, colorScheme) остаются прежними.
     *
     * @param newViewport Новая область просмотра {@link Viewport}.
     * @return Новый экземпляр {@link FractalState}.
     */
    public FractalState withViewport(Viewport newViewport) {
        return new FractalState(newViewport, this.maxIterations, this.colorScheme);
    }

    /**
     * Создает новый экземпляр FractalState с указанным максимальным количеством итераций.
     * Остальные параметры (viewport, colorScheme) остаются прежними.
     *
     * @param newMaxIterations Новое максимальное количество итераций.
     * @return Новый экземпляр {@link FractalState}.
     */
    public FractalState withMaxIterations(int newMaxIterations) {
        // Проверка корректности нового значения может быть добавлена здесь или в конструкторе
        return new FractalState(this.viewport, newMaxIterations, this.colorScheme);
    }

    /**
     * Создает новый экземпляр FractalState с указанной цветовой схемой.
     * Остальные параметры (viewport, maxIterations) остаются прежними.
     *
     * @param newColorScheme Новая цветовая схема {@link ColorScheme}.
     * @return Новый экземпляр {@link FractalState}.
     */
    public FractalState withColorScheme(ColorScheme newColorScheme) {
        return new FractalState(this.viewport, this.maxIterations, newColorScheme);
    }

    // --- Стандартные методы Object ---

    /**
     * Сравнивает это состояние с другим объектом на равенство.
     * Два состояния равны, если равны их viewport, maxIterations и классы их цветовых схем.
     * Сравнение схем по классу используется, так как конкретные экземпляры схем
     * без параметров (как GrayscaleScheme) считаются взаимозаменяемыми.
     * Если бы схемы имели параметры, потребовалось бы переопределить equals/hashCode в них.
     *
     * @param o Объект для сравнения.
     * @return true, если объекты равны, иначе false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        // Проверка на null и совпадение классов
        if (o == null || getClass() != o.getClass()) return false;
        FractalState that = (FractalState) o;
        // Сравнение полей
        return maxIterations == that.maxIterations &&
               Objects.equals(viewport, that.viewport) &&
               // Сравниваем классы цветовых схем, а не сами экземпляры
               Objects.equals(colorScheme.getClass(), that.colorScheme.getClass());
    }

    /**
     * Вычисляет хэш-код для этого состояния.
     * Основан на хэш-кодах viewport, maxIterations и класса цветовой схемы.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        // Хэш-код вычисляется на основе тех же полей, что и в equals
        return Objects.hash(viewport, maxIterations, colorScheme.getClass());
    }

    /**
     * Возвращает строковое представление состояния.
     * @return Строковое представление.
     */
    @Override
    public String toString() {
        return "FractalState{" +
               "viewport=" + viewport + // Используется toString() из Viewport
               ", maxIterations=" + maxIterations +
               ", colorScheme=" + colorScheme.getName() + // Используем имя схемы
               '}';
    }

    // --- Статический фабричный метод ---

    /**
     * Создает начальное состояние фрактала по умолчанию.
     * Использует {@link Viewport#DEFAULT_VIEWPORT}, 100 итераций и {@link NonlinearRGBScheme}.
     *
     * @return Новый экземпляр {@link FractalState} с настройками по умолчанию.
     */
    public static FractalState createDefault() {
        // Устанавливаем разумные начальные значения
        return new FractalState(Viewport.DEFAULT_VIEWPORT, 100, new NonlinearRGBScheme());
    }
}