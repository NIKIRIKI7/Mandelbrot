// File: core/src/main/java/viewmodel/commands/ZoomCommand.java
package viewmodel.commands;

import model.FractalState;
import model.Viewport;
import viewmodel.FractalViewModel;
import java.util.Objects;

/**
 * Команда для выполнения операции масштабирования (зума) области просмотра фрактала
 * к новому прямоугольному региону. Учитывает соотношение сторон для корректного отображения.
 * Изменяет {@link Viewport} в {@link FractalState}.
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
    /** Целевое соотношение сторон (ширина/высота), которое должен иметь новый Viewport. */
    private final double targetAspectRatio;

    /** Предыдущее состояние Viewport до выполнения команды. Сохраняется для возможности отмены (Undo). */
    private Viewport previousViewport;
    /** Флаг, была ли команда выполнена. */
    private boolean executed = false; // Добавлен флаг executed

    /**
     * Создает команду масштабирования.
     * Координаты (x1, y1) и (x2, y2) задают диагональ целевого прямоугольника
     * в координатах комплексной плоскости.
     *
     * @param viewModel         ViewModel, состояние которой будет изменяться.
     * @param x1                Одна из вещественных координат целевой области.
     * @param x2                Другая вещественная координата целевой области.
     * @param y1                Одна из мнимых координат целевой области.
     * @param y2                Другая мнимая координата целевой области.
     * @param targetAspectRatio Желаемое соотношение сторон нового Viewport (обычно равно
     *                          соотношению сторон панели отрисовки).
     */
    public ZoomCommand(FractalViewModel viewModel, double x1, double x2, double y1, double y2, double targetAspectRatio) {
        this.viewModel = Objects.requireNonNull(viewModel, "ViewModel не может быть null");
        // Определяем min/max координаты целевой области
        this.targetMinX = Math.min(x1, x2);
        this.targetMaxX = Math.max(x1, x2);
        this.targetMinY = Math.min(y1, y2);
        this.targetMaxY = Math.max(y1, y2);
        this.targetAspectRatio = targetAspectRatio;
        this.previousViewport = null; // Инициализируем null, будет установлено при execute()
    }

    /**
     * Выполняет масштабирование.
     * Сохраняет текущий Viewport для возможности отмены.
     * Вычисляет новый Viewport на основе целевых координат и соотношения сторон,
     * используя метод {@link Viewport#zoom(double, double, double, double, double)}.
     * Обновляет состояние в ViewModel.
     *
     * @return true, если масштабирование было выполнено и состояние изменилось, иначе false.
     */
    @Override
    public boolean execute() {
        // Команду можно выполнить только один раз перед отменой
        if (executed) {
            System.err.println("ZoomCommand: Попытка повторного выполнения команды.");
            return false;
        }

        FractalState currentState = viewModel.getCurrentState();
        // Сохраняем текущий viewport перед изменением для возможности отмены
        this.previousViewport = currentState.getViewport();

        // Вычисляем новый viewport с помощью метода zoom, который учтет соотношение сторон
        Viewport newViewport = previousViewport.zoom(targetMinX, targetMaxX, targetMinY, targetMaxY, targetAspectRatio);

        // Проверяем, изменился ли viewport реально (зум мог быть слишком мал или координаты совпадали)
        if (newViewport.equals(previousViewport)) {
            System.out.println("ZoomCommand: Viewport не изменился после зума.");
            this.previousViewport = null; // Сбрасываем, так как состояние не изменилось
            return false; // Состояние не изменилось
        }

         // Создаем новое состояние с новым viewport
        FractalState nextState = currentState.withViewport(newViewport);
        // Обновляем состояние в ViewModel
        viewModel.updateStateFromCommand(nextState);
        executed = true; // Помечаем как выполненную
        System.out.println("ZoomCommand: Выполнено. Новый viewport: " + newViewport);
        return true;
    }

    /**
     * Отменяет масштабирование.
     * Восстанавливает {@code previousViewport}, сохраненный при выполнении команды,
     * и обновляет состояние в ViewModel.
     *
     * @return true, если отмена была выполнена, иначе false (если команда не была выполнена или уже отменена).
     */
    @Override
    public boolean undo() {
        // Отменить можно только выполненную команду, у которой сохранен previousViewport
        if (!executed || previousViewport == null) {
             System.err.println("ZoomCommand: Попытка отмены невыполненной или уже отмененной команды.");
             return false;
        }
        FractalState currentState = viewModel.getCurrentState();
        // Создаем восстановленное состояние с предыдущим viewport
        FractalState restoredState = currentState.withViewport(previousViewport);
        // Обновляем состояние в ViewModel
        viewModel.updateStateFromCommand(restoredState);

        // Очищаем previousViewport и сбрасываем флаг, т.к. отмена выполнена
        this.previousViewport = null;
        executed = false;
        System.out.println("ZoomCommand: Отменено. Восстановлен viewport: " + restoredState.getViewport());
        return true;
    }
}