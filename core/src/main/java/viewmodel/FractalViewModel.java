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
import model.Viewport; // Для DEFAULT_VIEWPORT


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

/**
 * ViewModel for the Mandelbrot fractal visualization.
 * Manages the FractalState, handles user actions via Commands,
 * and interacts with the Renderer to update the view.
 */
public class FractalViewModel {

    public static final String PROPERTY_STATE = "fractalState";
    public static final String PROPERTY_CAN_UNDO = "canUndo";

    private FractalState currentState;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final UndoManager undoManager = new UndoManager(100);
    private final FractalRenderer renderer; // Renderer теперь не используется напрямую для вызова render
    // Константы для динамических итераций
    private static final double DEFAULT_VIEWPORT_WIDTH = Viewport.DEFAULT_VIEWPORT.getWidth(); // Ширина viewport по умолчанию
    private static final double ITERATION_ZOOM_FACTOR = 30.0; // Коэффициент масштабирования итераций (подбирается экспериментально)
    private static final int MIN_ITERATIONS = 50; // Минимальное количество итераций

    /**
     * Constructs the ViewModel with a default state.
     * The renderer is now primarily used by the View (FractalPanel),
     * but might be needed here if ViewModel initiated rendering directly.
     *
     * @param renderer The renderer (potentially unused here directly, but required by architecture).
     */
    public FractalViewModel(FractalRenderer renderer) { // Renderer может и не понадобиться здесь
        this.renderer = Objects.requireNonNull(renderer, "Renderer cannot be null"); // Пока оставим
        // Используем статический фабричный метод, который уже создает состояние с MandelbrotFunction
        this.currentState = FractalState.createDefault();
        // Уведомление о начальном состоянии не обязательно здесь, панель запросит рендер при показе
        // support.firePropertyChange(PROPERTY_STATE, null, this.currentState);
   }
    /**
     * Gets the current fractal state.
     *
     * @return The current FractalState.
     */
    public FractalState getCurrentState() {
        return currentState;
    }

    /**
     * Gets the undo manager for testing or external use.
     *
     * @return The UndoManager instance.
     */
    public UndoManager getUndoManager() {
        return undoManager;
    }

    /**
     * Loads a new FractalState, typically from a file, clears the undo history,
     * and notifies listeners.
     *
     * @param newState The new state to set.
     */
    public void loadState(FractalState newState) {
        Objects.requireNonNull(newState, "New state cannot be null");
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        this.currentState = newState; // Прямое обновление состояния
        undoManager.clearHistory(); // Очистка истории для загруженного состояния

        support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
        // После очистки истории canUndo точно false (если только не было false до этого)
        boolean newCanUndo = false; // undoManager.canUndo() вернет false
        if (oldCanUndo != newCanUndo) {
             support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
        }
        // Рендер запустит панель при получении события PROPERTY_STATE
    }

    /**
     * Executes a command, updates the state, adds it to the undo history,
     * and notifies listeners.
     *
     * @param command The command to execute.
     */
    private void executeCommand(Command command) {
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        // Команда сама обновит состояние через viewModel.updateStateFromCommand()
        if (command.execute()) {
            undoManager.addCommand(command);
            // Состояние УЖЕ обновлено командой, теперь нужно оповестить слушателей
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);

            boolean newCanUndo = undoManager.canUndo();
            if (oldCanUndo != newCanUndo) {
                support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
            }
            // Рендер запустит панель при получении события PROPERTY_STATE
        }
    }

    /**
     * Zooms into a rectangular region defined by screen coordinates.
     *
     * @param startX      Start X pixel coordinate.
     * @param startY      Start Y pixel coordinate.
     * @param endX        End X pixel coordinate.
     * @param endY        End Y pixel coordinate.
     * @param panelWidth  Current width of the drawing panel.
     * @param panelHeight Current height of the drawing panel.
     */
    public void zoomOnScreenRect(int startX, int startY, int endX, int endY, int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) return;

        ComplexNumber c1 = CoordinateConverter.screenToComplex(startX, startY, panelWidth, panelHeight, currentState.getViewport());
        ComplexNumber c2 = CoordinateConverter.screenToComplex(endX, endY, panelWidth, panelHeight, currentState.getViewport());

        if (c1 == null || c2 == null) return;

        double targetAspectRatio = (double) panelWidth / panelHeight;
        Command zoomCommand = new ZoomCommand(this, c1.getReal(), c2.getReal(), c1.getImaginary(), c2.getImaginary(), targetAspectRatio);
        executeCommand(zoomCommand);
    }

    /**
     * Pans the view by a delta in screen coordinates.
     *
     * @param deltaX      Change in X pixel coordinate.
     * @param deltaY      Change in Y pixel coordinate.
     * @param panelWidth  Current width of the drawing panel.
     * @param panelHeight Current height of the drawing panel.
     */
    public void panOnScreenDelta(int deltaX, int deltaY, int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0 || (deltaX == 0 && deltaY == 0)) return;

        double complexDeltaPerPixelX = currentState.getViewport().getWidth() / (panelWidth - 1);
        double complexDeltaPerPixelY = currentState.getViewport().getHeight() / (panelHeight - 1);

        double complexDeltaX = -deltaX * complexDeltaPerPixelX;
        double complexDeltaY = deltaY * complexDeltaPerPixelY;

        Command panCommand = new PanCommand(this, complexDeltaX, complexDeltaY);
        executeCommand(panCommand);
    }

    /**
     * Undoes the last executed command and notifies listeners.
     */
    public void undoLastAction() {
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        // undo() внутри себя вызовет viewModel.updateStateFromCommand()
        if (undoManager.undo()) {
             // Состояние УЖЕ обновлено через undo(), оповещаем слушателей
             support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);

             boolean newCanUndo = undoManager.canUndo();
             if (oldCanUndo != newCanUndo) {
                 support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
             }
             // Рендер запустит панель при получении события PROPERTY_STATE
        }
    }


    /**
     * Changes the current color scheme and notifies listeners.
     * Does NOT add an undo step for simplicity, but could be refactored into a command.
     *
     * @param newScheme The new color scheme to apply.
     */
    public void changeColorScheme(ColorScheme newScheme) {
        Objects.requireNonNull(newScheme, "Color scheme cannot be null");
        FractalState oldState = this.currentState;
        // Сравниваем классы, чтобы не обновлять, если схема того же типа уже установлена
        if (!oldState.getColorScheme().getClass().equals(newScheme.getClass())) {
            // Используем with-метод для создания нового состояния
            this.currentState = oldState.withColorScheme(newScheme);
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
             // Рендер запустит панель при получении события PROPERTY_STATE
        }
    }


    /**
     * Changes the maximum number of iterations and notifies listeners.
     * Does NOT add an undo step for simplicity, but could be refactored into a command.
     *
     * @param newMaxIterations The new maximum iteration count (must be positive).
     */
    public void changeMaxIterations(int newMaxIterations) {
        if (newMaxIterations <= 0) {
            System.err.println("Max iterations must be positive.");
            return; // Или бросить исключение / показать диалог
        }
        FractalState oldState = this.currentState;
        if (oldState.getMaxIterations() != newMaxIterations) {
             // Используем with-метод для создания нового состояния
            this.currentState = oldState.withMaxIterations(newMaxIterations);
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
             // Рендер запустит панель при получении события PROPERTY_STATE
        }
    }

    // public public void updateStateFromCommand(FractalState newState) {
    //     this.currentState = newState;
    // }

    /**
     * Triggers rendering of the current state using the FractalRenderer.
     */
    private void triggerRender() {
        // Здесь должен быть вызов рендерера с передачей текущего состояния
        // Размеры изображения будут переданы из FractalPanel
        System.out.println("Triggering render for state: " + currentState);
        // Реальная реализация будет в FractalPanel, здесь только логика уведомления
    }

    /**
     * Adds a listener for property change events.
     *
     * @param listener The listener to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * Removes a listener for property change events.
     *
     * @param listener The listener to remove.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     * Updates the current state from a command execution or load.
     * Fires property change event.
     *
     * @param newState The new state to set.
     * @param oldState The previous state (for event firing).
     */
    public void updateState(FractalState newState, FractalState oldState) {
        Objects.requireNonNull(newState, "New state cannot be null");
        this.currentState = newState;
        support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
        // Обновление canUndo должно происходить там, где меняется undoManager
    }

    public void updateStateFromCommand(FractalState nextState) {
        this.currentState = nextState;
    }
}