// File: core/src/main/java/viewmodel/commands/Command.java
package viewmodel.commands;

/**
 * Интерфейс для команд, изменяющих состояние фрактала ({@link model.FractalState}).
 * Поддерживает выполнение (execute) и отмену (undo) операции.
 * Используется для реализации паттерна Command и механизма Undo/Redo.
 */
public interface Command {
    /**
     * Выполняет команду, изменяя состояние, управляемое {@link viewmodel.FractalViewModel}.
     * @return true, если состояние было успешно изменено, false в противном случае
     *         (например, если команда не может быть выполнена в текущем состоянии или уже была выполнена).
     */
    boolean execute();

    /**
     * Отменяет действие команды, восстанавливая предыдущее состояние.
     * @return true, если отмена прошла успешно, false в противном случае
     *         (например, если команду не выполняли или отмена невозможна).
     */
    boolean undo();
}