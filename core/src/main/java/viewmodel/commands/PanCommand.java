// File: core/src/main/java/viewmodel/commands/PanCommand.java
package viewmodel.commands;

import model.FractalState;
import model.Viewport;
import viewmodel.FractalViewModel;
import java.util.Objects;

/**
 * Команда для выполнения операции панорамирования (сдвига) области просмотра фрактала.
 * Изменяет {@link Viewport} в {@link FractalState}.
 */
public class PanCommand implements Command {

    /** Ссылка на ViewModel для доступа к текущему состоянию и его обновления. */
    private final FractalViewModel viewModel;
    /** Величина сдвига по оси X (вещественной оси) в координатах комплексной плоскости. */
    private final double deltaX;
    /** Величина сдвига по оси Y (мнимой оси) в координатах комплексной плоскости. */
    private final double deltaY;

    /** Флаг, указывающий, была ли команда уже выполнена. Используется для предотвращения повторного выполнения. */
    private boolean executed = false;
    // Сохранять предыдущее состояние (previousViewport) здесь не нужно,
    // так как undo() вычисляет его обратным сдвигом от текущего состояния.

    /**
     * Создает команду панорамирования.
     *
     * @param viewModel ViewModel, состояние которой будет изменяться.
     * @param deltaX    Сдвиг по оси X (вещественной) в комплексных координатах.
     * @param deltaY    Сдвиг по оси Y (мнимой) в комплексных координатах.
     */
    public PanCommand(FractalViewModel viewModel, double deltaX, double deltaY) {
        this.viewModel = Objects.requireNonNull(viewModel, "ViewModel не может быть null");
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    /**
     * Выполняет панорамирование.
     * Получает текущее состояние из ViewModel, вычисляет новый Viewport,
     * сдвинутый на deltaX и deltaY, и обновляет состояние в ViewModel.
     *
     * @return true, если панорамирование было выполнено и состояние изменилось, иначе false.
     */
    @Override
    public boolean execute() {
         // Команду можно выполнить только один раз перед отменой
         if (executed) {
             System.err.println("PanCommand: Попытка повторного выполнения команды.");
             return false;
         }

         FractalState currentState = viewModel.getCurrentState();
         Viewport currentViewport = currentState.getViewport();

         // Вычисляем новый viewport путем сдвига текущего
         Viewport newViewport = currentViewport.pan(deltaX, deltaY);

         // Проверяем, изменился ли viewport (сдвиг мог быть нулевым)
         if (newViewport.equals(currentViewport)) {
              System.out.println("PanCommand: Сдвиг не изменил Viewport.");
              return false; // Состояние не изменилось
         }

         // Создаем новое состояние с обновленным viewport
         FractalState nextState = currentState.withViewport(newViewport);
         // Обновляем состояние в ViewModel (это НЕ вызывает PropertyChange event,
         // это делает executeCommand во ViewModel после успешного execute)
         viewModel.updateStateFromCommand(nextState);
         executed = true; // Помечаем команду как выполненную
         System.out.println("PanCommand: Выполнено. Новый viewport: " + newViewport);
         return true;
    }

    /**
     * Отменяет панорамирование.
     * Получает текущее состояние из ViewModel, вычисляет предыдущий Viewport,
     * применяя обратный сдвиг (-deltaX, -deltaY), и обновляет состояние в ViewModel.
     *
     * @return true, если отмена была выполнена, иначе false (если команда не была выполнена ранее).
     */
    @Override
    public boolean undo() {
         // Отменить можно только выполненную команду
         if (!executed) {
             System.err.println("PanCommand: Попытка отмены невыполненной команды.");
             return false;
         }

         FractalState currentState = viewModel.getCurrentState();
         Viewport currentViewport = currentState.getViewport();

         // Вычисляем предыдущий viewport, применяя обратный сдвиг
         Viewport previousViewport = currentViewport.pan(-deltaX, -deltaY);

         // Создаем восстановленное состояние
         FractalState restoredState = currentState.withViewport(previousViewport);
         // Обновляем состояние в ViewModel
         viewModel.updateStateFromCommand(restoredState);
         executed = false; // Сбрасываем флаг, позволяя выполнить команду снова (если потребуется Redo)
                           // Для простого Undo этого достаточно.
         System.out.println("PanCommand: Отменено. Восстановленный viewport: " + previousViewport);
         return true;
    }
}