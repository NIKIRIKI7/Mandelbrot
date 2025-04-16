// File: core/src/main/java/viewmodel/commands/UndoManager.java
package viewmodel.commands;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Менеджер истории команд, реализующий функциональность отмены (Undo).
 * Использует паттерн Команда ({@link Command}).
 * <p>
 * Хранит последовательность выполненных команд в стеке (LIFO - Last-In, First-Out).
 * Позволяет добавлять команды в историю, проверять возможность отмены,
 * выполнять отмену последней команды и очищать историю.
 * </p><p>
 * Имеет ограничение на максимальное количество команд в истории
 * ({@code maxHistorySize}). При добавлении новой команды сверх лимита,
 * самая старая команда автоматически удаляется.
 * </p><p>
 * В качестве стека используется {@link ArrayDeque}, как эффективная реализация {@link Deque}.
 * </p>
 */
public class UndoManager {
    /**
     * Стек для хранения истории выполненных команд.
     * Последняя выполненная команда находится на вершине стека (добавляется через {@code push}).
     * Операция отмены (Undo) извлекает команду с вершины (через {@code pop}).
     */
    private final Deque<Command> history;
    /**
     * Максимальный размер истории команд. Определяет, сколько шагов отмены доступно.
     */
    private final int maxHistorySize;

    /**
     * Создает {@code UndoManager} с заданным максимальным размером истории отмены.
     *
     * @param maxHistorySize Максимальное количество команд, которое будет храниться в истории.
     *                       Должно быть положительным числом (> 0).
     * @throws IllegalArgumentException если {@code maxHistorySize} не является положительным.
     */
    public UndoManager(int maxHistorySize) {
        if (maxHistorySize <= 0) {
            throw new IllegalArgumentException("Максимальный размер истории должен быть положительным.");
        }
        this.maxHistorySize = maxHistorySize;
        // Используем ArrayDeque как эффективную реализацию стека/очереди
        this.history = new ArrayDeque<>(maxHistorySize); // Начальная емкость для оптимизации
    }

    /**
     * Добавляет выполненную команду в историю (на вершину стека).
     * <p>
     * Если текущий размер истории равен или превышает {@link #maxHistorySize},
     * самая старая команда (находящаяся в "хвосте" {@link Deque} при использовании как стека)
     * удаляется перед добавлением новой команды {@code command}.
     * </p>
     *
     * @param command Команда, которая была успешно выполнена и должна быть добавлена
     *                в историю для возможной отмены. Не должна быть null.
     * @throws NullPointerException если {@code command} равна null.
     */
    public void addCommand(Command command) {
        Objects.requireNonNull(command, "Команда для добавления в UndoManager не может быть null");
        // Проверяем, не достигнут ли лимит истории
        if (history.size() >= maxHistorySize) {
            history.removeLast(); // Удаляем самый старый элемент (из "дна" стека/конца Deque)
        }
        history.push(command); // Добавляем новый элемент на "вершину" стека (в начало Deque)
    }

    /**
     * Проверяет, возможна ли в данный момент операция отмены (Undo).
     * Отмена возможна, если история команд не пуста.
     *
     * @return {@code true}, если в истории есть хотя бы одна команда для отмены
     *         ({@code history.size() > 0}), {@code false} в противном случае.
     */
    public boolean canUndo() {
        return !history.isEmpty();
    }

    /**
     * Отменяет последнюю выполненную команду.
     * <p>
     * Если история не пуста ({@code canUndo() == true}):
     * <ol>
     *     <li>Извлекает последнюю команду с вершины стека ({@code history.pop()}).</li>
     *     <li>Вызывает метод {@link Command#undo()} для извлеченной команды.</li>
     * </ol>
     * </p>
     *
     * @return {@code true}, если команда была успешно извлечена и ее метод {@code undo()}
     *         был вызван (возвращаемое значение {@code command.undo()} игнорируется здесь,
     *         предполагается, что оно обработано внутри команды или ViewModel);
     *         {@code false}, если история была пуста и отменять было нечего.
     */
    public boolean undo() {
        if (!canUndo()) {
            System.out.println("UndoManager: Нет действий для отмены.");
            return false; // Нечего отменять
        }
        Command lastCommand = history.pop(); // Снимаем команду с вершины стека
        System.out.println("UndoManager: Отмена команды " + lastCommand.getClass().getSimpleName());
        // Выполняем отмену действия этой команды.
        // Возвращаемое значение lastCommand.undo() здесь не используется,
        // т.к. ViewModel должен обработать изменение состояния независимо.
        boolean undoSuccess = lastCommand.undo();
        if (!undoSuccess) {
             // Логируем, если сама команда сообщила о неудаче отмены
             System.err.println("UndoManager: Команда " + lastCommand.getClass().getSimpleName() + " сообщила о неудаче операции undo().");
             // Возможно, стоит вернуть команду обратно в стек? Зависит от логики. Пока просто логируем.
        }
        return true; // Возвращаем true, так как команда была извлечена и undo вызван.
    }

    /**
     * Полностью очищает историю команд.
     * После вызова этого метода операция Undo ({@link #undo()}) будет невозможна
     * до добавления новых команд. Используется, например, при загрузке нового состояния.
     */
    public void clearHistory() {
        System.out.println("UndoManager: История очищена.");
        history.clear();
    }

    /**
     * Возвращает текущее количество команд, хранящихся в истории отмены.
     * Может быть полезно для отладки или отображения информации пользователю.
     *
     * @return Количество команд в истории (от 0 до {@link #maxHistorySize}).
     */
    public int getHistorySize() {
        return history.size();
    }
}