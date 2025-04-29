// File: core/src/main/java/viewmodel/FractalViewModel.java
package viewmodel;

import model.ColorScheme;
import model.FractalState;

import render.FractalRenderer;
import viewmodel.commands.Command;
import viewmodel.commands.PanCommand;
import viewmodel.commands.UndoManager;
import viewmodel.commands.ZoomCommand;
import utils.CoordinateConverter;
import utils.ComplexNumber;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * ViewModel (согласно паттерну MVVM) для управления состоянием и логикой
 * визуализации фрактала.
 * <p>
 * Обязанности:
 * <ul>
 *     <li>Хранит текущее состояние фрактала ({@link FractalState}).</li>
 *     <li>Предоставляет методы для изменения состояния (зум, панорамирование, смена схемы, итераций),
 *         используя паттерн Команда ({@link Command}) для поддержки отмены действий (Undo).</li>
 *     <li>Управляет историей команд через {@link UndoManager}.</li>
 *     <li>Уведомляет подписчиков (обычно View) об изменениях состояния через {@link PropertyChangeSupport}.</li>
 *     <li>Взаимодействует с {@link CoordinateConverter} для преобразования экранных координат в комплексные.</li>
 * </ul>
 * </p><p>
 * Не занимается непосредственно рендерингом (это задача {@link FractalRenderer}, который обычно
 * вызывается из View компонента {@link view.FractalPanel} при получении уведомления об изменении состояния).
 * </p>
 */
public class FractalViewModel {
    // Конструктор по умолчанию для совместимости с MainFrame
    public FractalViewModel() {
        this.currentState = new FractalState();
        this.undoManager = new UndoManager(100);
    }


    /**
     * Имя свойства для события изменения состояния фрактала ({@link FractalState}).
     * Слушатели могут отслеживать это свойство для обновления отображения.
     * Значение события: {@code (oldState, newState)}.
     */
    public static final String PROPERTY_STATE = "fractalState";
    /**
     * Имя свойства для события изменения возможности отмены последнего действия.
     * Используется для включения/выключения элементов UI (например, пункта меню "Undo").
     * Значение события: {@code (oldCanUndo, newCanUndo)} (тип Boolean).
     */
    public static final String PROPERTY_CAN_UNDO = "canUndo";

    /** Текущее неизменяемое состояние фрактала. */
    private FractalState currentState;
    /** Механизм поддержки слушателей PropertyChange для уведомления View. */
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    /** Менеджер для хранения истории команд и выполнения отмены (Undo). */
    private final UndoManager undoManager; // = new UndoManager(100); Инициализируется в конструкторе
    /** Ссылка на рендерер (может не использоваться напрямую, но передается по архитектуре). */


    /** Базовое количество итераций для расчета при зуме. */
    private static final int BASE_ITERATIONS_FOR_ZOOM = 50; // Было MIN_ITERATIONS
    /** Коэффициент, определяющий, насколько быстро растут итерации при зуме. */
    private static final double ITERATION_ZOOM_SENSITIVITY = 40.0; // Было ITERATION_ZOOM_FACTOR


    /**
     * Создает ViewModel с начальным состоянием фрактала по умолчанию
     * и заданным максимальным размером истории отмены.
     *
     * @param renderer Экземпляр {@link FractalRenderer}. Хотя ViewModel может не вызывать его методы
     *                 напрямую, он передается для соответствия архитектуре и потенциального использования.
     *                 Не может быть null.
     * @param undoHistorySize Максимальное количество шагов для отмены. Должно быть > 0.
     * @throws NullPointerException если {@code renderer} равен null.
     * @throws IllegalArgumentException если {@code undoHistorySize <= 0}.
     */
    public FractalViewModel(FractalRenderer renderer, int undoHistorySize) {
        // Параметр renderer используется только для совместимости с архитектурой
        Objects.requireNonNull(renderer, "Renderer не может быть null");
        if (undoHistorySize <= 0) {
             throw new IllegalArgumentException("Размер истории Undo должен быть положительным.");
        }
        this.undoManager = new UndoManager(undoHistorySize);
        // Используем статический фабричный метод для создания состояния Мандельброта по умолчанию
        this.currentState = FractalState.createDefault();
        // Уведомление о начальном состоянии не требуется здесь,
        // View (FractalPanel) запросит рендер при первом отображении/изменении размера.
    }

    /**
     * Альтернативный конструктор с размером истории отмены по умолчанию (100 шагов).
     * @param renderer Экземпляр {@link FractalRenderer}. Не может быть null.
     */
     public FractalViewModel(FractalRenderer renderer) {
         this(renderer, 100); // Используем значение по умолчанию 100
     }


    /**
     * Возвращает текущее состояние фрактала.
     *
     * @return Неизменяемый объект {@link FractalState}, представляющий текущий вид.
     */
    public FractalState getCurrentState() {
        return currentState;
    }

    /**
     * Возвращает менеджер отмены {@link UndoManager}.
     * Может использоваться View для проверки возможности отмены ({@code undoManager.canUndo()})
     * или для тестирования.
     *
     * @return Экземпляр {@link UndoManager}.
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Загружает новое состояние фрактала ({@code newState}).
     * Обычно используется при загрузке состояния из файла.
     * <p>
     * При загрузке нового состояния происходит следующее:
     * <ul>
     *     <li>Текущее состояние заменяется на {@code newState}.</li>
     *     <li>История отмены (Undo) полностью очищается.</li>
     *     <li>Генерируются события {@link #PROPERTY_STATE} и {@link #PROPERTY_CAN_UNDO}
     *         для уведомления подписчиков (View).</li>
     * </ul>
     *
     * @param newState Новое состояние {@link FractalState} для установки. Не может быть null.
     * @throws NullPointerException если {@code newState} равно null.
     */
    public void loadState(FractalState newState) {
        Objects.requireNonNull(newState, "Новое состояние не может быть null");
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        // Проверяем, действительно ли состояние отличается (избегаем лишних действий)
        if (!Objects.equals(oldState, newState)) {
            this.currentState = newState; // Прямое обновление состояния
            undoManager.clearHistory(); // Очистка истории для загруженного состояния

            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            // После очистки истории canUndo точно станет false
            boolean newCanUndo = false; // undoManager.canUndo() вернет false
            if (oldCanUndo != newCanUndo) { // Уведомляем, только если изменилось
                 support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
            }
            System.out.println("Состояние загружено: " + newState);
            // Рендер запустит панель при получении события PROPERTY_STATE
        } else {
             System.out.println("Загрузка состояния пропущена: новое состояние идентично текущему.");
        }
    }

    /**
     * Выполняет переданную команду {@link Command}.
     * <p>
     * Процесс выполнения:
     * <ul>
     *     <li>Вызывается метод {@code command.execute()}.</li>
     *     <li>Если {@code execute()} вернул {@code true} (команда успешно выполнена и изменила состояние):
     *         <ul>
     *             <li>Команда добавляется в историю {@link UndoManager}.</li>
     *             <li>Генерируются события {@link #PROPERTY_STATE} (с oldState и обновленным currentState)
     *                 и {@link #PROPERTY_CAN_UNDO} (если доступность отмены изменилась)
     *                 для уведомления View.</li>
     *         </ul>
     *     </li>
     *     <li>Если {@code execute()} вернул {@code false}, никаких изменений и уведомлений не происходит.</li>
     * </ul>
     * @param command Команда для выполнения. Не должна быть null.
     */
    private void executeCommand(Command command) {
        Objects.requireNonNull(command, "Команда не может быть null");
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        // Команда сама обновит состояние через viewModel.updateStateFromCommand() внутри execute()
        if (command.execute()) {
            // Состояние УЖЕ обновлено командой, теперь нужно добавить в историю и оповестить
            undoManager.addCommand(command);

            // Сравниваем состояния ДО и ПОСЛЕ выполнения команды
            // Если команда ничего не изменила, событие не генерируем
            if (!Objects.equals(oldState, this.currentState)) {
                 support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            } else {
                // Если состояние не изменилось, но команда сказала "true" - странная ситуация
                System.err.println("Предупреждение: Команда " + command.getClass().getSimpleName()
                                   + " вернула true, но состояние ViewModel не изменилось.");
                // Возможно, команду все равно стоит добавить в историю, если она что-то сделала, но не состояние?
                // Пока оставляем как есть - добавляем в историю, но не шлем PROPERTY_STATE.
            }


            boolean newCanUndo = undoManager.canUndo();
            if (oldCanUndo != newCanUndo) { // Уведомляем, только если изменилось
                support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
            }
            // Рендер запустит панель при получении события PROPERTY_STATE (если оно было сгенерировано)
        }
    }

    /**
     * Выполняет масштабирование (зум) в прямоугольную область,
     * заданную координатами углов на экране (в пикселях).
     * Создает и выполняет {@link ZoomCommand}.
     *
     * @param startX      X-координата одного угла выделения (пиксель).
     * @param startY      Y-координата одного угла выделения (пиксель).
     * @param endX        X-координата противоположного угла выделения (пиксель).
     * @param endY        Y-координата противоположного угла выделения (пиксель).
     * @param panelWidth  Текущая ширина панели отрисовки (для конвертации координат и аспекта). Должна быть > 0.
     * @param panelHeight Текущая высота панели отрисовки (для конвертации координат и аспекта). Должна быть > 0.
     */
    public void zoomOnScreenRect(int startX, int startY, int endX, int endY, int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) {
            System.err.println("zoomOnScreenRect вызван с некорректными размерами панели.");
            return;
        }

        // Конвертируем экранные координаты углов в комплексные числа
        ComplexNumber c1 = CoordinateConverter.screenToComplex(startX, startY, panelWidth, panelHeight, currentState.getViewport());
        ComplexNumber c2 = CoordinateConverter.screenToComplex(endX, endY, panelWidth, panelHeight, currentState.getViewport());

        if (c1 == null || c2 == null) {
            System.err.println("Не удалось сконвертировать экранные координаты для зума.");
            return; // Ошибка конвертации
        }

        // Вычисляем целевое соотношение сторон на основе размеров панели
        double targetAspectRatio = (double) panelWidth / panelHeight;

        // Создаем команду зума, передавая ViewModel, комплексные координаты углов и аспект
        Command zoomCommand = new ZoomCommand(this,
                                              c1.getReal(), c2.getReal(),
                                              c1.getImaginary(), c2.getImaginary(),
                                              targetAspectRatio,
                                              BASE_ITERATIONS_FOR_ZOOM,
                                              ITERATION_ZOOM_SENSITIVITY);
        executeCommand(zoomCommand);
    }

    /**
     * Выполняет панорамирование (сдвиг) вида на заданное смещение в экранных координатах (пикселях).
     * Создает и выполняет {@link PanCommand}.
     *
     * @param deltaX      Смещение по оси X в пикселях. Положительное значение - сдвиг содержимого влево (вид смещается вправо).
     * @param deltaY      Смещение по оси Y в пикселях. Положительное значение - сдвиг содержимого вверх (вид смещается вниз).
     * @param panelWidth  Текущая ширина панели отрисовки (для расчета шага). Должна быть > 0.
     * @param panelHeight Текущая высота панели отрисовки (для расчета шага). Должна быть > 0.
     */
    public void panOnScreenDelta(int deltaX, int deltaY, int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) {
             System.err.println("panOnScreenDelta вызван с некорректными размерами панели.");
             return;
        }
        if (deltaX == 0 && deltaY == 0) {
            return; // Нет смещения
        }

        // Рассчитываем, какому изменению комплексных координат соответствует сдвиг на один пиксель
        double complexDeltaPerPixelX = currentState.getViewport().getWidth() / (panelWidth > 1 ? (panelWidth - 1.0) : 1.0);
        double complexDeltaPerPixelY = currentState.getViewport().getHeight() / (panelHeight > 1 ? (panelHeight - 1.0) : 1.0);

        // Вычисляем общее смещение в комплексных координатах
        // Знак (-) для deltaX, так как движение мыши вправо (увеличение screenX) должно сдвигать viewport влево (уменьшать minX/maxX)
        // Знак (+) для deltaY, так как движение мыши вниз (увеличение screenY) должно сдвигать viewport вверх (увеличивать minY/maxY) из-за инверсии оси Y экрана.
        double complexDeltaX = -deltaX * complexDeltaPerPixelX;
        double complexDeltaY = deltaY * complexDeltaPerPixelY;

        // Создаем и выполняем команду панорамирования
        Command panCommand = new PanCommand(this, complexDeltaX, complexDeltaY);
        executeCommand(panCommand);
    }

    /**
     * Отменяет последнее выполненное действие (команду).
     * Извлекает последнюю команду из {@link UndoManager} и вызывает ее метод {@code undo()}.
     * Если отмена прошла успешно, генерируются события {@link #PROPERTY_STATE} и
     * {@link #PROPERTY_CAN_UNDO} для уведомления View.
     */
    public void undoLastAction() {
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        // undo() внутри себя вызовет viewModel.updateStateFromCommand() через команду
        if (undoManager.undo()) {
             // Состояние УЖЕ обновлено через undo(), оповещаем слушателей
             if (!Objects.equals(oldState, this.currentState)) { // Уведомляем только если состояние изменилось
                 support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
             } else {
                 System.err.println("Предупреждение: UndoManager.undo() вернул true, но состояние ViewModel не изменилось.");
             }

             boolean newCanUndo = undoManager.canUndo();
             if (oldCanUndo != newCanUndo) { // Уведомляем, только если изменилось
                 support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
             }
             // Рендер запустит панель при получении события PROPERTY_STATE (если оно было)
        }
    }


    /**
     * Изменяет текущую цветовую схему {@link ColorScheme}.
     * Это действие *не* добавляется в историю отмены (Undo) для простоты.
     * Если бы требовалась отмена смены схемы, это следовало бы реализовать
     * через отдельную команду {@code ChangeColorSchemeCommand}.
     * <p>
     * Если новая схема отличается от текущей, состояние обновляется и
     * генерируется событие {@link #PROPERTY_STATE}.
     * </p>
     *
     * @param newScheme Новая цветовая схема для применения. Не может быть null.
     * @throws NullPointerException если {@code newScheme} равно null.
     */
    public void changeColorScheme(ColorScheme newScheme) {
        Objects.requireNonNull(newScheme, "Цветовая схема не может быть null");
        FractalState oldState = this.currentState;

        // Сравниваем схемы через equals, чтобы избежать обновления, если схема та же
        if (!Objects.equals(oldState.getColorScheme(), newScheme)) {
            // Используем with-метод для создания нового immutable состояния
            FractalState newState = oldState.withColorScheme(newScheme);
            // Напрямую обновляем состояние (без Undo)
            this.currentState = newState;
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            System.out.println("Цветовая схема изменена на: " + newScheme.getName());
             // Рендер запустит панель при получении события PROPERTY_STATE
        } else {
             System.out.println("Смена цветовой схемы пропущена: новая схема идентична текущей.");
        }
    }


    /**
     * Изменяет максимальное количество итераций для расчета фрактала.
     * Это действие *не* добавляется в историю отмены (Undo) для простоты.
     * Как и смена схемы, могло бы быть реализовано через команду.
     * <p>
     * Если новое количество итераций отличается от текущего и положительно,
     * состояние обновляется и генерируется событие {@link #PROPERTY_STATE}.
     * </p>
     *
     * @param newMaxIterations Новое максимальное количество итераций. Должно быть > 0.
     */
    public void changeMaxIterations(int newMaxIterations) {
        if (newMaxIterations <= 0) {
            System.err.println("Максимальное количество итераций должно быть положительным.");
            // Можно бросить исключение или показать диалог ошибки во View
            // throw new IllegalArgumentException("Max iterations must be positive.");
            return;
        }
        FractalState oldState = this.currentState;
        if (oldState.getMaxIterations() != newMaxIterations) {
             // Используем with-метод для создания нового immutable состояния
            FractalState newState = oldState.withMaxIterations(newMaxIterations);
            // Напрямую обновляем состояние (без Undo)
            this.currentState = newState;
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            System.out.println("Макс. итераций изменено на: " + newMaxIterations);
             // Рендер запустит панель при получении события PROPERTY_STATE
        } else {
             System.out.println("Смена макс. итераций пропущена: новое значение идентично текущему.");
        }
    }


    /**
     * Добавляет слушателя для отслеживания изменений свойств ViewModel
     * ({@link #PROPERTY_STATE} и {@link #PROPERTY_CAN_UNDO}).
     *
     * @param listener Слушатель {@link PropertyChangeListener} для добавления.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * Удаляет ранее добавленного слушателя изменений свойств.
     *
     * @param listener Слушатель {@link PropertyChangeListener} для удаления.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     * Внутренний метод для обновления текущего состояния {@link #currentState}.
     * Используется командами ({@link Command#execute()}, {@link Command#undo()})
     * для установки нового или восстановленного состояния.
     * <p>
     * <strong>Важно:</strong> Этот метод сам по себе *не* генерирует события
     * {@code PropertyChange}. Генерация событий происходит в методах
     * {@link #executeCommand(Command)}, {@link #undoLastAction()}, {@link #loadState(FractalState)}
     * после успешного изменения состояния.
     * </p>
     *
     * @param newState Новое состояние {@link FractalState} для установки. Не должно быть null.
     */
    public void updateStateFromCommand(FractalState newState) {
        Objects.requireNonNull(newState, "Новое состояние от команды не может быть null");
        this.currentState = newState;
        // PropertyChange event НЕ генерируется здесь, а в вызывающем методе (executeCommand/undoLastAction)
    }
}