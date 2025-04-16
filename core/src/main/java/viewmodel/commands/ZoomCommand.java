// File: core/src/main/java/viewmodel/commands/ZoomCommand.java
package viewmodel.commands;

import model.FractalState;
import model.Viewport;
import viewmodel.FractalViewModel; // Добавлен импорт для доступа к константам (если они там)
                                  // Лучше передавать константы или использовать отдельный класс
import java.util.Objects;

/**
 * Команда для выполнения операции масштабирования (зума).
 * При выполнении также динамически изменяет максимальное количество итераций
 * в зависимости от степени приближения.
 */
public class ZoomCommand implements Command {

    private final FractalViewModel viewModel;
    private final double targetMinX;
    private final double targetMaxX;
    private final double targetMinY;
    private final double targetMaxY;
    private final double targetAspectRatio;

    // Поля для отмены
    private Viewport previousViewport;
    private int previousMaxIterations; // <-- Добавлено поле для итераций
    private boolean executed = false;

    // Получаем константы (можно передать через конструктор или получить из ViewModel)
    // Для простоты пока захардкодим здесь, но лучше вынести
    private static final double DEFAULT_VIEWPORT_WIDTH = 3.0; // (-2.0, 1.0) -> width = 3.0
    private static final double ITERATION_ZOOM_FACTOR = 40.0; // Экспериментальный коэффициент
    private static final int MIN_ITERATIONS = 50;

    /**
     * Создает команду масштабирования.
     *
     * @param viewModel         ViewModel, состояние которой будет изменяться.
     * @param x1                Одна из вещественных координат целевой области.
     * @param x2                Другая вещественная координата целевой области.
     * @param y1                Одна из мнимых координат целевой области.
     * @param y2                Другая мнимая координата целевой области.
     * @param targetAspectRatio Желаемое соотношение сторон нового Viewport.
     */
    public ZoomCommand(FractalViewModel viewModel, double x1, double x2, double y1, double y2, double targetAspectRatio) {
        this.viewModel = Objects.requireNonNull(viewModel, "ViewModel не может быть null");
        this.targetMinX = Math.min(x1, x2);
        this.targetMaxX = Math.max(x1, x2);
        this.targetMinY = Math.min(y1, y2);
        this.targetMaxY = Math.max(y1, y2);
        this.targetAspectRatio = targetAspectRatio;
        this.previousViewport = null;
        this.previousMaxIterations = 0; // Инициализируем
    }

    @Override
    public boolean execute() {
        if (executed) {
            System.err.println("ZoomCommand: Попытка повторного выполнения команды.");
            return false;
        }

        FractalState currentState = viewModel.getCurrentState();
        this.previousViewport = currentState.getViewport();
        this.previousMaxIterations = currentState.getMaxIterations(); // <-- Сохраняем итерации

        // Вычисляем новый viewport
        Viewport newViewport = previousViewport.zoom(targetMinX, targetMaxX, targetMinY, targetMaxY, targetAspectRatio);

        if (newViewport.equals(previousViewport)) {
            System.out.println("ZoomCommand: Viewport не изменился после зума.");
            this.previousViewport = null;
            this.previousMaxIterations = 0;
            return false;
        }

        // Вычисляем новое количество итераций
        int newMaxIterations = calculateNewIterations(previousViewport, newViewport, previousMaxIterations);

        // Создаем новое состояние с новым viewport И новыми итерациями
        FractalState nextState = currentState
            .withViewport(newViewport)
            .withMaxIterations(newMaxIterations); // <-- Устанавливаем новые итерации

        // Обновляем состояние в ViewModel (ViewModel сам вызовет оповещение)
        viewModel.updateStateFromCommand(nextState);
        executed = true;
        System.out.printf("ZoomCommand: Выполнено. Viewport: %s, Iterations: %d%n", newViewport, newMaxIterations);
        return true;
    }
    

    @Override
    public boolean undo() {
        if (!executed || previousViewport == null) {
            System.err.println("ZoomCommand: Попытка отмены невыполненной или уже отмененной команды.");
            return false;
        }
        FractalState currentState = viewModel.getCurrentState();

        // Создаем восстановленное состояние с предыдущим viewport И предыдущими итерациями
        FractalState restoredState = currentState
            .withViewport(previousViewport)
            .withMaxIterations(previousMaxIterations); // <-- Восстанавливаем итерации

        viewModel.updateStateFromCommand(restoredState);

        this.previousViewport = null;
        this.previousMaxIterations = 0;
        executed = false;
        System.out.printf("ZoomCommand: Отменено. Viewport: %s, Iterations: %d%n",
            restoredState.getViewport(), restoredState.getMaxIterations());
        return true;
    }

    /**
     * Рассчитывает новое количество итераций на основе изменения масштаба.
     *
     * @param oldViewport      Предыдущий Viewport.
     * @param newViewport      Новый Viewport.
     * @param currentIterations Текущее количество итераций.
     * @return Рассчитанное новое количество итераций.
     */
    private int calculateNewIterations(Viewport oldViewport, Viewport newViewport, int currentIterations) {
         // Используем ширину для определения масштаба
         double oldWidth = oldViewport.getWidth();
         double newWidth = newViewport.getWidth();

         if (oldWidth <= 0 || newWidth <= 0) {
             return currentIterations; // Избегаем деления на ноль или логарифма от некорректного значения
         }

         // Коэффициент масштабирования (во сколько раз уменьшилась ширина)
         double zoomFactor = oldWidth / newWidth;

         if (zoomFactor <= 0) {
              return currentIterations; // Логарифм не определен для <= 0
         }

         // Простое логарифмическое увеличение.
         // Math.log() - натуральный логарифм.
         // Добавляем прирост к ТЕКУЩИМ итерациям, а не к базовым.
         // Это позволяет итерациям накапливаться при последовательных зумах.
         int deltaIterations = (int) (Math.log(zoomFactor) * ITERATION_ZOOM_FACTOR);

         // Если deltaIterations отрицательный (отдаление), итерации могут уменьшиться
         int calculatedIterations = currentIterations + deltaIterations;

         // Ограничиваем минимальным значением
         return Math.max(MIN_ITERATIONS, calculatedIterations);
    }
}