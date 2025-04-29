// File: core/src/main/java/model/FractalState.java
package model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import math.FractalFunction; // <-- Добавлен импорт
import math.MandelbrotFunction; // <-- Добавлен импорт для дефолтной функции
import java.io.Serializable;
import java.util.Objects;

/**
 * Представляет полное, неизменяемое (immutable) состояние визуализации фрактала
 * в определенный момент времени. Инкапсулирует все параметры, необходимые
 * для рендеринга и описания текущего вида.
 * <p>
 * Включает:
 * <ul>
 *     <li>{@link Viewport}: Область комплексной плоскости для отображения.</li>
 *     <li>{@code maxIterations}: Максимальное количество итераций для расчета принадлежности точки множеству.</li>
 *     <li>{@link ColorScheme}: Цветовая схема для окрашивания точек в зависимости от числа итераций.</li>
 *     <li>{@link FractalFunction}: Функция, определяющая правило расчета итераций (например, для Мандельброта или Жюлиа).</li>
 * </ul>
 * Объекты этого класса предназначены для передачи между компонентами (ViewModel, View, Renderer)
 * и для сохранения/загрузки состояния приложения (так как реализует {@link Serializable}).
 */
public final class FractalState implements Serializable {
    // Конструктор по умолчанию для совместимости с ViewModel
    public FractalState() {
        this.viewport = new Viewport(); // Требуется дефолтный конструктор Viewport
        this.maxIterations = 1000;
        this.colorScheme = new model.NonlinearRGBScheme(); // или другая дефолтная схема
        this.fractalFunction = DEFAULT_FRACTAL_FUNCTION;
    }

    /**
     * Версия для сериализации. Увеличена до 2 из-за добавления поля {@code fractalFunction}.
     */
    private static final long serialVersionUID = 2L;

    private Viewport viewport;
    private int maxIterations;
    private ColorScheme colorScheme;
    private FractalFunction fractalFunction;

    private static final FractalFunction DEFAULT_FRACTAL_FUNCTION = new MandelbrotFunction();

    @JsonCreator
    public FractalState(
        @JsonProperty("viewport") Viewport viewport,
        @JsonProperty("maxIterations") int maxIterations,
        @JsonProperty("colorScheme") ColorScheme colorScheme,
        @JsonProperty("fractalFunction") FractalFunction fractalFunction
    ) {
        this.viewport = viewport;
        this.maxIterations = maxIterations;
        this.colorScheme = colorScheme;
        this.fractalFunction = fractalFunction;
    }

    public Viewport getViewport() { return viewport; }
    public void setViewport(Viewport viewport) { this.viewport = viewport; }

    public int getMaxIterations() { return maxIterations; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }

    public ColorScheme getColorScheme() { return colorScheme; }
    public void setColorScheme(ColorScheme colorScheme) { this.colorScheme = colorScheme; }

    public FractalFunction getFractalFunction() { return fractalFunction; }
    public void setFractalFunction(FractalFunction fractalFunction) { this.fractalFunction = fractalFunction; }

    // --- Методы для создания нового состояния с изменениями (immutable pattern) ---

    /**
     * Создает новый экземпляр {@code FractalState} с измененной областью просмотра.
     * Остальные параметры (итерации, схема, функция) копируются из текущего состояния.
     *
     * @param newViewport Новая область просмотра {@link Viewport}. Не может быть null.
     * @return Новый экземпляр {@code FractalState} с указанным {@code Viewport}.
     */
    public FractalState withViewport(Viewport newViewport) {
        return new FractalState(newViewport, this.maxIterations, this.colorScheme, this.fractalFunction);
    }

    /**
     * Создает новый экземпляр {@code FractalState} с измененным максимальным количеством итераций.
     * Остальные параметры (viewport, схема, функция) копируются из текущего состояния.
     *
     * @param newMaxIterations Новое максимальное количество итераций. Должно быть > 0.
     * @return Новый экземпляр {@code FractalState} с указанным количеством итераций.
     */
    public FractalState withMaxIterations(int newMaxIterations) {
        return new FractalState(this.viewport, newMaxIterations, this.colorScheme, this.fractalFunction);
    }

    /**
     * Создает новый экземпляр {@code FractalState} с измененной цветовой схемой.
     * Остальные параметры (viewport, итерации, функция) копируются из текущего состояния.
     *
     * @param newColorScheme Новая цветовая схема {@link ColorScheme}. Не может быть null.
     * @return Новый экземпляр {@code FractalState} с указанной цветовой схемой.
     */
    public FractalState withColorScheme(ColorScheme newColorScheme) {
        return new FractalState(this.viewport, this.maxIterations, newColorScheme, this.fractalFunction);
    }

    /**
     * Создает новый экземпляр {@code FractalState} с измененной функцией расчета фрактала.
     * Остальные параметры (viewport, итерации, схема) копируются из текущего состояния.
     *
     * @param newFractalFunction Новая функция расчета {@link FractalFunction}. Не может быть null.
     * @return Новый экземпляр {@code FractalState} с указанной функцией фрактала.
     */
    public FractalState withFractalFunction(FractalFunction newFractalFunction) {
        return new FractalState(this.viewport, this.maxIterations, this.colorScheme, newFractalFunction);
    }


    // --- Стандартные методы Object ---

    /**
     * Сравнивает это состояние фрактала с другим объектом на равенство.
     * Два состояния считаются равными, если у них совпадают:
     * <ul>
     *     <li>Область просмотра ({@code viewport})</li>
     *     <li>Максимальное количество итераций ({@code maxIterations})</li>
     *     <li>Класс цветовой схемы ({@code colorScheme.getClass()})</li>
     *     <li>Функция фрактала ({@code fractalFunction}) - сравнение через {@code equals} функции</li>
     * </ul>
     * Сравнение цветовой схемы и функции по классу/equals важно для корректной работы Undo/Redo
     * и избежания лишних перерисовок, если меняется только экземпляр, но не суть схемы/функции.
     * Для функций с параметрами (как {@link math.JuliaFunction}) их {@code equals} должен сравнивать эти параметры.
     *
     * @param o Объект для сравнения.
     * @return {@code true}, если объекты семантически равны, иначе {@code false}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FractalState that = (FractalState) o;
        // Сравниваем все поля, включая саму функцию через equals
        return maxIterations == that.maxIterations &&
               Objects.equals(viewport, that.viewport) &&
               Objects.equals(colorScheme, that.colorScheme) && // Сравнение через equals схемы
               Objects.equals(fractalFunction, that.fractalFunction); // Сравнение через equals функции
    }

    /**
     * Вычисляет хэш-код для этого состояния фрактала.
     * Хэш-код зависит от {@code viewport}, {@code maxIterations}, {@code colorScheme} и {@code fractalFunction}.
     * Реализация должна быть согласована с {@link #equals(Object)}.
     *
     * @return Хэш-код объекта.
     */
    @Override
    public int hashCode() {
        // Включаем все поля, используемые в equals
        return Objects.hash(viewport, maxIterations, colorScheme, fractalFunction);
    }

    /**
     * Возвращает строковое представление состояния фрактала.
     * Полезно для логирования и отладки. Включает основные параметры.
     *
     * @return Строка, описывающая состояние.
     */
    @Override
    public String toString() {
        return "FractalState{" +
               "viewport=" + viewport +
               ", maxIterations=" + maxIterations +
               ", colorScheme=" + (colorScheme != null ? colorScheme.getName() : "null") + // Проверка на null для getName()
               ", fractalFunction=" + (fractalFunction != null ? fractalFunction.getName() : "null") + // Проверка на null для getName()
               '}';
    }

    // --- Статический фабричный метод ---

    /**
     * Создает начальное (дефолтное) состояние фрактала.
     * Использует стандартную область просмотра {@link Viewport#DEFAULT_VIEWPORT},
     * начальное количество итераций (100), стандартную цветовую схему
     * ({@link NonlinearRGBScheme}) и функцию множества Мандельброта
     * ({@link MandelbrotFunction}).
     *
     * @return Новый экземпляр {@link FractalState} с настройками по умолчанию.
     */
    public static FractalState createDefault() {
        return new FractalState(
            Viewport.DEFAULT_VIEWPORT,
            100, // Начальное количество итераций
            new NonlinearRGBScheme(), // Можно выбрать другую схему по умолчанию
            DEFAULT_FRACTAL_FUNCTION // Используем функцию Мандельброта по умолчанию
        );
    }
}