package viewmodel;

import model.ColorScheme;
import model.FractalState;
import model.Viewport;
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
    private final FractalRenderer renderer;

    /**
     * Constructs the ViewModel with a default state and renderer.
     *
     * @param renderer The renderer to use for fractal generation.
     */
    public FractalViewModel(FractalRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "Renderer cannot be null");
        this.currentState = FractalState.createDefault();
        support.firePropertyChange(PROPERTY_STATE, null, this.currentState); // Начальное уведомление
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
     * Loads a new FractalState, typically from a file, and clears the undo history.
     *
     * @param newState The new state to set.
     */
    public void loadState(FractalState newState) {
        Objects.requireNonNull(newState, "New state cannot be null");
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        this.currentState = newState;
        undoManager.clearHistory();

        support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
        support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, false);
        triggerRender();
    }

    /**
     * Executes a command, updates the state, and adds it to the undo history.
     *
     * @param command The command to execute.
     */
    private void executeCommand(Command command) {
        FractalState oldState = this.currentState;
        boolean oldCanUndo = undoManager.canUndo();

        if (command.execute()) {
            undoManager.addCommand(command);
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            boolean newCanUndo = undoManager.canUndo();
            if (oldCanUndo != newCanUndo) {
                support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
            }
            triggerRender();
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
     * Undoes the last executed command.
     */
    public void undoLastAction() {
        boolean oldCanUndo = undoManager.canUndo();
        FractalState oldState = this.currentState;

        if (undoManager.undo()) {
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            boolean newCanUndo = undoManager.canUndo();
            if (oldCanUndo != newCanUndo) {
                support.firePropertyChange(PROPERTY_CAN_UNDO, oldCanUndo, newCanUndo);
            }
            triggerRender();
        }
    }

    /**
     * Changes the current color scheme and triggers a re-render.
     *
     * @param newScheme The new color scheme to apply.
     */
    public void changeColorScheme(ColorScheme newScheme) {
        Objects.requireNonNull(newScheme, "Color scheme cannot be null");
        FractalState oldState = this.currentState;
        if (!oldState.getColorScheme().getClass().equals(newScheme.getClass())) {
            this.currentState = oldState.withColorScheme(newScheme);
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            triggerRender();
        }
    }

    /**
     * Changes the maximum number of iterations and triggers a re-render.
     *
     * @param newMaxIterations The new maximum iteration count (must be positive).
     */
    public void changeMaxIterations(int newMaxIterations) {
        if (newMaxIterations <= 0) {
            System.err.println("Max iterations must be positive.");
            return;
        }
        FractalState oldState = this.currentState;
        if (oldState.getMaxIterations() != newMaxIterations) {
            this.currentState = oldState.withMaxIterations(newMaxIterations);
            support.firePropertyChange(PROPERTY_STATE, oldState, this.currentState);
            triggerRender();
        }
    }

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
     * Updates the current state from a command execution.
     * Should only be called by Command implementations.
     *
     * @param newState The new state to set.
     */
    public void updateStateFromCommand(FractalState newState) {
        this.currentState = newState;
    }
}