// File: core/src/main/java/viewmodel/commands/Command.java
package viewmodel.commands;

/**
 * Интерфейс, представляющий собой команду в рамках паттерна "Команда".
 * Команды инкапсулируют запросы на выполнение действий, обычно связанных
 * с изменением состояния приложения (в данном случае, состояния фрактала
 * {@link model.FractalState}, управляемого {@link viewmodel.FractalViewModel}).
 * <p>
 * Ключевой особенностью является поддержка отмены действия через метод {@link #undo()}.
 * Это позволяет реализовать функциональность Undo/Redo с помощью менеджера команд,
 * такого как {@link UndoManager}.
 * </p>
 */
public interface Command {
    /**
     * Выполняет инкапсулированное действие команды.
     * Как правило, это приводит к изменению состояния в {@link viewmodel.FractalViewModel}.
     * Реализация должна быть идемпотентной в том смысле, что повторный вызов
     * без предварительного вызова {@link #undo()} не должен приводить к
     * повторному эффекту (обычно реализуется через внутренний флаг).
     *
     * @return {@code true}, если команда была успешно выполнена и состояние (потенциально)
     *         изменилось; {@code false}, если выполнение не удалось или было пропущено
     *         (например, команда уже была выполнена, или условия для выполнения не соблюдены).
     */
    boolean execute();

    /**
     * Отменяет действие, ранее выполненное методом {@link #execute()}.
     * Восстанавливает состояние приложения к тому виду, который был до выполнения команды.
     * Реализация также должна быть идемпотентной: повторный вызов {@code undo()}
     * без промежуточного {@code execute()} не должен иметь эффекта.
     *
     * @return {@code true}, если отмена прошла успешно и состояние было восстановлено;
     *         {@code false}, если отмена невозможна (например, команда не была выполнена
     *         или уже была отменена).
     */
    boolean undo();
}