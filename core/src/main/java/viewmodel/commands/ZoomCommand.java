// File: core/src/main/java/viewmodel/commands/ZoomCommand.java
package viewmodel.commands;

import model.FractalState;
import model.Viewport;
import viewmodel.FractalViewModel; // Добавлен импорт для доступа к ViewModel

import java.util.Objects;

/**
 * Реализация интерфейса {@link Command}, отвечающая за операцию
 * масштабирования (зума) области просмотра фрактала ({@link Viewport}).
 * <p>
 * При выполнении ({@link #execute()}) команда:
 * <ol>
 *     <li>Вычисляет новый {@code Viewport}, соответствующий запрошенной области
 *         (с коррекцией соотношения сторон).</li>
 *     <li>Динамически рассчитывает новое максимальное количество итераций
 *         на основе степени приближения (чем ближе зум, тем больше итераций).</li>
 *     <li>Обновляет состояние в {@link FractalViewModel}, устанавливая новый
 *         {@code Viewport} и новое количество итераций.</li>
 *     <li>Сохраняет предыдущий {@code Viewport} и предыдущее количество итераций для возможности отмены.</li>
 * </ol>
 * При отмене ({@link #undo()}) команда восстанавливает предыдущий {@code Viewport}
 * и предыдущее количество итераций в {@link FractalViewModel}.
 * </p>
 */
public class ZoomCommand implements Command {

    /** Ссылка на ViewModel для доступа к текущему состоянию и его обновления. */
    private final FractalViewModel viewModel;
    /** Минимальная вещественная координата целевой области зума. */
    private final double targetMinX;
    /** Максимальная вещественная координата целевой области зума. */
    private final double targetMaxX;
    /** Минимальная мнимая координата целевой области зума. */
    private final double targetMinY;
    /** Максимальная мнимая координата целевой области зума. */
    private final double targetMaxY;
    /** Целевое соотношение сторон (ширина/высота) для нового Viewport. */
    private final double targetAspectRatio;

    // Поля для сохранения состояния перед выполнением команды (для undo)
    /** Viewport, который был активен до выполнения этой команды. */
    private Viewport previousViewport;
    /** Количество итераций, которое было активно до выполнения этой команды. */
    private int previousMaxIterations;
    /** Флаг, указывающий, была ли команда уже выполнена хотя бы один раз. */
    private boolean executed = false;

    // Параметры для расчета динамических итераций
    /** Базовое количество итераций, к которому добавляется дельта при зуме. */
    private final int baseIterations;
    /** Коэффициент, влияющий на скорость роста итераций при зуме. */
    private final double iterationZoomSensitivity;


    /**
     * Создает команду масштабирования (зума).
     *
     * @param viewModel         ViewModel, состояние которой будет изменяться. Не может быть null.
     * @param x1                Одна из вещественных координат целевой области масштабирования.
     * @param x2                Другая вещественная координата целевой области масштабирования.
     * @param y1                Одна из мнимых координат целевой области масштабирования.
     * @param y2                Другая мнимая координата целевой области масштабирования.
     * @param targetAspectRatio Желаемое соотношение сторон нового Viewport (обычно соответствует панели). Должно быть > 0.
     * @param baseIterations    Базовое/минимальное количество итераций для расчета при зуме. Должно быть > 0.
     * @param iterationZoomSensitivity Коэффициент чувствительности роста итераций (больше значение - быстрее растут).
     * @throws NullPointerException если {@code viewModel} равен null.
     * @throws IllegalArgumentException если {@code targetAspectRatio <= 0} или {@code baseIterations <= 0}.
     */
    public ZoomCommand(FractalViewModel viewModel,
                       double x1, double x2, double y1, double y2,
                       double targetAspectRatio,
                       int baseIterations, double iterationZoomSensitivity) {
        this.viewModel = Objects.requireNonNull(viewModel, "ViewModel не может быть null");
        // Упорядочиваем координаты сразу
        this.targetMinX = Math.min(x1, x2);
        this.targetMaxX = Math.max(x1, x2);
        this.targetMinY = Math.min(y1, y2);
        this.targetMaxY = Math.max(y1, y2);

        if (targetAspectRatio <= 0) {
            throw new IllegalArgumentException("Целевое соотношение сторон должно быть положительным.");
        }
        this.targetAspectRatio = targetAspectRatio;

         if (baseIterations <= 0) {
            throw new IllegalArgumentException("Базовое количество итераций должно быть положительным.");
        }
        this.baseIterations = baseIterations;
        this.iterationZoomSensitivity = iterationZoomSensitivity;

        // Инициализируем поля для undo
        this.previousViewport = null;
        this.previousMaxIterations = 0;
    }

    /**
     * Выполняет команду масштабирования (зума).
     * <p>
     * Сохраняет текущий {@code Viewport} и {@code maxIterations} для отмены.
     * Вычисляет новый {@code Viewport} с помощью {@link Viewport#zoom(double, double, double, double, double)}.
     * Вычисляет новое количество итераций с помощью {@link #calculateNewIterations(Viewport, Viewport, int)}.
     * Создает новый {@link FractalState} с этими значениями и обновляет состояние в {@link #viewModel}.
     * </p>
     * @return {@code true}, если команда была успешно выполнена (еще не выполнялась или была отменена,
     *         и новый Viewport отличается от предыдущего), {@code false} в противном случае.
     */
    @Override
    public boolean execute() {
        if (executed) {
            System.err.println("ZoomCommand: Попытка повторного выполнения команды без предварительной отмены.");
            return false;
        }

        FractalState currentState = viewModel.getCurrentState();
        // Сохраняем состояние *перед* выполнением для возможности отмены
        this.previousViewport = currentState.getViewport();
        this.previousMaxIterations = currentState.getMaxIterations();

        // 1. Вычисляем новый viewport с коррекцией аспекта
        Viewport newViewport = previousViewport.zoom(targetMinX, targetMaxX, targetMinY, targetMaxY, targetAspectRatio);

        // Проверяем, изменился ли viewport после зума (может не измениться при ошибках или малом выделении)
        if (Objects.equals(newViewport, previousViewport)) {
            System.out.println("ZoomCommand: Viewport не изменился после зума. Выполнение пропущено.");
            // Сбрасываем сохраненное состояние, так как команда не была реально выполнена
            this.previousViewport = null;
            this.previousMaxIterations = 0;
            return false; // Изменений нет
        }

        // 2. Вычисляем новое количество итераций на основе изменения масштаба
        int newMaxIterations = calculateNewIterations(previousViewport, newViewport, this.previousMaxIterations);

        // 3. Создаем новое состояние с новым viewport И новыми итерациями
        FractalState nextState = currentState
            .withViewport(newViewport)
            .withMaxIterations(newMaxIterations); // Устанавливаем рассчитанные итерации

        // 4. Обновляем состояние в ViewModel
        viewModel.updateStateFromCommand(nextState);
        executed = true; // Помечаем команду как выполненную
        System.out.printf("ZoomCommand: Выполнено. Viewport: %s, Iterations: %d%n", newViewport, newMaxIterations);
        return true;
    }


    /**
     * Отменяет действие команды масштабирования.
     * <p>
     * Восстанавливает {@code Viewport} и {@code maxIterations}, которые были сохранены
     * при последнем вызове {@link #execute()}, и обновляет состояние в {@link #viewModel}.
     * </p>
     * @return {@code true}, если отмена прошла успешно (команда была ранее выполнена),
     *         {@code false} в противном случае.
     */
    @Override
    public boolean undo() {
        if (!executed) {
            System.err.println("ZoomCommand: Попытка отмены невыполненной команды.");
            return false;
        }
        // Проверяем, было ли сохранено предыдущее состояние (дополнительная защита)
        if (previousViewport == null) {
             System.err.println("ZoomCommand: Ошибка отмены - предыдущее состояние не было сохранено.");
             executed = false; // Сбрасываем флаг в любом случае
             return false;
        }

        FractalState currentState = viewModel.getCurrentState();

        // Создаем восстановленное состояние с предыдущим viewport И предыдущими итерациями
        FractalState restoredState = currentState
            .withViewport(previousViewport)
            .withMaxIterations(previousMaxIterations); // Восстанавливаем предыдущие итерации

        // Обновляем состояние в ViewModel
        viewModel.updateStateFromCommand(restoredState);

        // Сбрасываем сохраненное состояние и флаг
        this.previousViewport = null; // Очищаем, чтобы избежать утечек и повторной отмены
        this.previousMaxIterations = 0;
        executed = false;
        System.out.printf("ZoomCommand: Отменено. Viewport: %s, Iterations: %d%n",
            restoredState.getViewport(), restoredState.getMaxIterations());
        return true;
    }

    /**
     * Рассчитывает новое рекомендуемое количество итераций на основе изменения масштаба (зума).
     * Идея в том, чтобы увеличивать детализацию (количество итераций) при приближении
     * для лучшего рассмотрения сложных структур фрактала.
     * Использует логарифмическую зависимость от коэффициента масштабирования.
     *
     * @param oldViewport      Viewport *до* выполнения зума.
     * @param newViewport      Viewport *после* выполнения зума.
     * @param currentIterations Текущее количество итераций (до расчета новых). Не используется напрямую в формуле,
     *                         но может использоваться для альтернативных расчетов (например, относительное увеличение).
     *                         Текущая формула использует {@link #baseIterations}.
     * @return Рассчитанное новое количество итераций, но не менее {@link #baseIterations}.
     */
    private int calculateNewIterations(Viewport oldViewport, Viewport newViewport, int currentIterations) {
         // Используем ширину для определения масштаба. Можно использовать и высоту или площадь.
         double oldWidth = oldViewport.getWidth();
         double newWidth = newViewport.getWidth();

         // Избегаем деления на ноль или некорректных значений
         final double EPSILON = 1e-9;
         if (oldWidth < EPSILON || newWidth < EPSILON) {
             System.err.println("Предупреждение: Расчет новых итераций пропущен из-за нулевой ширины Viewport.");
             return Math.max(this.baseIterations, currentIterations); // Возвращаем большее из базового и текущего
         }

         // Коэффициент масштабирования (во сколько раз уменьшилась ширина/увеличился зум)
         double zoomLevelFactor = oldWidth / newWidth;

         // Если произошло отдаление (zoomLevelFactor < 1), итерации не уменьшаем ниже базовых.
         // Логарифм от числа < 1 отрицателен.
         if (zoomLevelFactor <= 0) { // Логарифм не определен для <= 0
              System.err.println("Предупреждение: Некорректный zoomLevelFactor <= 0 для расчета итераций.");
              return Math.max(this.baseIterations, currentIterations);
         }

         // Формула: Новые Итерации = БазовыеИтерации + log(КоэффициентЗума) * Чувствительность
         // Math.log() - натуральный логарифм.
         // Добавляем прирост к БАЗОВЫМ итерациям.
         int calculatedIterations = this.baseIterations + (int) (Math.log(zoomLevelFactor) * this.iterationZoomSensitivity);

         // Ограничиваем минимальным значением (baseIterations) и, возможно, максимальным (если нужно)
         return Math.max(this.baseIterations, calculatedIterations);

         // Альтернативная формула: относительное увеличение от текущих итераций
         /*
         int deltaIterations = (int) (Math.log(zoomLevelFactor) * this.iterationZoomSensitivity);
         int calculatedIterations = currentIterations + deltaIterations; // Добавляем к текущим
         return Math.max(this.baseIterations, calculatedIterations); // Ограничиваем базовыми снизу
         */
    }
}