// File: core/src/main/java/viewmodel/commands/PanCommand.java
package viewmodel.commands;

import model.FractalState;
import model.Viewport;
import viewmodel.FractalViewModel;
import java.util.Objects;

/**
 * Реализация интерфейса {@link Command}, отвечающая за операцию
 * панорамирования (сдвига) области просмотра фрактала ({@link Viewport}).
 * <p>
 * При выполнении ({@link #execute()}) команда вычисляет новый {@code Viewport},
 * сдвинутый на заданные {@code deltaX} и {@code deltaY} относительно текущего,
 * и обновляет состояние в {@link FractalViewModel}.
 * </p><p>
 * При отмене ({@link #undo()}) команда вычисляет предыдущий {@code Viewport},
 * применяя обратный сдвиг ({@code -deltaX}, {@code -deltaY}) к текущему,
 * и восстанавливает это состояние в {@link FractalViewModel}.
 * </p>
 */
public class PanCommand implements Command {

    /** Ссылка на ViewModel для доступа к текущему состоянию и его обновления. */
    private final FractalViewModel viewModel;
    /** Величина сдвига по оси X (вещественной оси) в координатах комплексной плоскости. */
    private final double deltaX;
    /** Величина сдвига по оси Y (мнимой оси) в координатах комплексной плоскости. */
    private final double deltaY;

    /** Флаг, указывающий, была ли команда уже выполнена хотя бы один раз. Используется для логики undo. */
    private boolean executed = false;
    // Сохранять предыдущее состояние (previousViewport) здесь не нужно,
    // так как undo() вычисляет его обратным сдвигом от *текущего на момент undo* состояния.

    /**
     * Создает команду панорамирования.
     *
     * @param viewModel ViewModel, состояние которой будет изменяться. Не может быть null.
     * @param deltaX    Сдвиг по оси X (вещественной) в комплексных координатах.
     * @param deltaY    Сдвиг по оси Y (мнимой) в комплексных координатах.
     * @throws NullPointerException если {@code viewModel} равен null.
     */
    public PanCommand(FractalViewModel viewModel, double deltaX, double deltaY) {
        this.viewModel = Objects.requireNonNull(viewModel, "ViewModel не может быть null");
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    /**
     * Выполняет команду панорамирования.
     * <p>
     * Получает текущее состояние из {@link #viewModel}, вычисляет новый {@link Viewport},
     * сдвинутый на {@link #deltaX} и {@link #deltaY} с помощью метода {@link Viewport#pan(double, double)}.
     * Затем создает новый {@link FractalState} с этим {@code Viewport} и обновляет
     * состояние в {@code viewModel} через {@link FractalViewModel#updateStateFromCommand(FractalState)}.
     * </p>
     *
     * @return {@code true}, если панорамирование было выполнено успешно (т.е. команда еще не была выполнена
     *         после создания или последней отмены, и сдвиг был ненулевым, что привело к изменению Viewport),
     *         {@code false} в противном случае (например, повторное выполнение без отмены, нулевой сдвиг).
     */
    @Override
    public boolean execute() {
         // Команду можно выполнить только один раз между отменами
         if (executed) {
             System.err.println("PanCommand: Попытка повторного выполнения команды без предварительной отмены.");
             return false;
         }
         // Проверяем, есть ли смысл выполнять сдвиг
         if (deltaX == 0.0 && deltaY == 0.0) {
             System.out.println("PanCommand: Нулевой сдвиг, выполнение пропущено.");
             // Не помечаем как executed, так как ничего не сделано
             return false;
         }


         FractalState currentState = viewModel.getCurrentState();
         Viewport currentViewport = currentState.getViewport();

         // Вычисляем новый viewport путем сдвига текущего
         Viewport newViewport = currentViewport.pan(deltaX, deltaY);

         // Хотя проверка на нулевой сдвиг была выше, убедимся, что viewport реально изменился
         // (на случай крайне малых delta или особенностей реализации Viewport.pan/equals)
         if (Objects.equals(newViewport, currentViewport)) {
              System.out.println("PanCommand: Сдвиг не изменил Viewport (возможно, из-за точности double).");
              // Не помечаем как executed, так как состояние не изменилось
              return false;
         }

         // Создаем новое состояние с обновленным viewport
         FractalState nextState = currentState.withViewport(newViewport);
         // Обновляем состояние в ViewModel. ViewModel сам позаботится об уведомлениях позже.
         viewModel.updateStateFromCommand(nextState);
         executed = true; // Помечаем команду как выполненную
         System.out.println("PanCommand: Выполнено. Новый viewport: " + newViewport);
         return true;
    }

    /**
     * Отменяет действие команды панорамирования.
     * <p>
     * Получает текущее состояние из {@link #viewModel}, вычисляет предыдущий {@link Viewport},
     * применяя обратный сдвиг ({@code -deltaX}, {@code -deltaY}) к *текущему* {@code Viewport}
     * с помощью {@link Viewport#pan(double, double)}.
     * Затем создает новый {@link FractalState} с восстановленным {@code Viewport} и обновляет
     * состояние в {@code viewModel} через {@link FractalViewModel#updateStateFromCommand(FractalState)}.
     * </p>
     *
     * @return {@code true}, если отмена была выполнена успешно (т.е. команда была ранее выполнена),
     *         {@code false} в противном случае (попытка отменить невыполненную команду).
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

         // Вычисляем предыдущий viewport, применяя обратный сдвиг к ТЕКУЩЕМУ viewport
         Viewport previousViewport = currentViewport.pan(-deltaX, -deltaY);

         // Создаем восстановленное состояние
         FractalState restoredState = currentState.withViewport(previousViewport);
         // Обновляем состояние в ViewModel. ViewModel сам позаботится об уведомлениях позже.
         viewModel.updateStateFromCommand(restoredState);
         executed = false; // Сбрасываем флаг, позволяя выполнить команду снова (если потребуется Redo)
                           // Для простого Undo/Execute цикла этого достаточно.
         System.out.println("PanCommand: Отменено. Восстановленный viewport: " + previousViewport);
         return true;
    }
}