// File: core/src/main/java/viewmodel/commands/UndoManager.java
package viewmodel.commands;

// Command импортируется без viewmodel.commands префикса, т.к. находится в том же пакете
// import viewmodel.commands.Command; - избыточно

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Управляет историей выполненных команд ({@link Command}) для реализации функциональности отмены (Undo).
 * Хранит ограниченное количество последних выполненных команд.
 * Использует {@link Deque} (реализация {@link ArrayDeque}) в качестве стека LIFO (Last-In, First-Out).
 */
public class UndoManager {
    /**
     * Стек для хранения истории команд. Последняя выполненная команда находится наверху стека.
     */
    private final Deque<Command> history;
    /**
     * Максимальный размер истории команд. При превышении самые старые команды удаляются.
     */
    private final int maxHistorySize;

    /**
     * Создает UndoManager с заданным максимальным размером истории.
     * @param maxHistorySize Максимальное количество команд для хранения. Должно быть положительным.
     * @throws IllegalArgumentException если maxHistorySize не положительное.
     */
    public UndoManager(int maxHistorySize) {
        if (maxHistorySize <= 0) {
            throw new IllegalArgumentException("Максимальный размер истории должен быть положительным.");
        }
        this.maxHistorySize = maxHistorySize;
        // Используем ArrayDeque как эффективную реализацию стека/очереди
        this.history = new ArrayDeque<>(maxHistorySize);
    }

    /**
     * Добавляет выполненную команду в историю (на вершину стека).
     * Если история заполнена (достигнут {@code maxHistorySize}), самая старая команда
     * (находящаяся в "дне" стека) удаляется перед добавлением новой.
     *
     * @param command Команда для добавления. Не должна быть null.
     * @throws NullPointerException если command равна null.
     */
    public void addCommand(Command command) {
        Objects.requireNonNull(command, "Команда не может быть null");
        // Проверяем, не переполнен ли стек
        if (history.size() >= maxHistorySize) {
            history.removeLast(); // Удаляем самый старый элемент (из конца Deque при использовании как стек)
        }
        history.push(command); // Добавляем новый элемент в начало (вершину стека)
    }

    /**
     * Проверяет, возможна ли операция отмены (Undo).
     * @return true, если в истории есть хотя бы одна команда для отмены (стек не пуст).
     */
    public boolean canUndo() {
        return !history.isEmpty();
    }

    /**
     * Отменяет последнюю выполненную команду.
     * Извлекает команду с вершины стека и вызывает ее метод {@link Command#undo()}.
     *
     * @return true, если команда была успешно отменена, false, если история пуста.
     */
    public boolean undo() {
        if (!canUndo()) {
            return false; // Нечего отменять
        }
        Command lastCommand = history.pop(); // Снимаем команду с вершины стека
        // Выполняем отмену действия этой команды
        return lastCommand.undo();
    }

    /**
     * Очищает всю историю команд. Операция Undo становится невозможной.
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * Возвращает текущее количество команд в истории.
     * (Может быть полезно для отладки или отображения)
     * @return Количество команд в истории.
     */
    public int getHistorySize() {
        return history.size();
    }
}