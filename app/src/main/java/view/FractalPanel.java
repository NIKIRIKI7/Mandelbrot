// File: app/src/main/java/view/FractalPanel.java
package view;

import listeners.MousePanListener;
import listeners.MouseZoomListener;
import model.ColorScheme; // Импорт нужен для передачи в JuliaSetWindow
import model.FractalState;
import render.FractalRenderer;
import utils.ComplexNumber; // Импорт нужен для координат Жюлиа
import utils.CoordinateConverter; // Импорт нужен для координат Жюлиа
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter; // <-- Импорт для листенера
import java.awt.event.MouseEvent;  // <-- Импорт для листенера
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * JPanel responsible for displaying the rendered fractal image.
 * Listens to ViewModel updates and handles mouse interactions (zoom, pan, Julia set invocation).
 */
public class FractalPanel extends JPanel implements PropertyChangeListener {

    private final FractalViewModel viewModel;
    private volatile BufferedImage fractalImage;
    private final FractalRenderer renderer;
    private final MouseZoomListener zoomListener;
    private final MousePanListener panListener;
    private volatile boolean isRendering = false;
    private volatile String statusMessage = "Initializing...";

    /**
     * Constructs the FractalPanel with a ViewModel and Renderer.
     * Initializes mouse listeners for zooming, panning, and opening Julia Set window on double-click.
     * Listens for component resize/show events and ViewModel state changes to trigger rendering.
     *
     * @param viewModel The application's ViewModel.
     * @param renderer  The fractal renderer.
     */
    public FractalPanel(FractalViewModel viewModel, FractalRenderer renderer) {
        this.viewModel = viewModel;
        this.renderer = renderer;
        this.fractalImage = null;

        this.viewModel.addPropertyChangeListener(this);
        setBackground(Color.BLACK);
        setOpaque(true);

        // Инициализация слушателей
        zoomListener = new MouseZoomListener(viewModel, this);
        panListener = new MousePanListener(viewModel, this);

        // Добавление слушателей событий мыши
        addMouseListener(zoomListener);
        addMouseMotionListener(zoomListener);
        // ИЗМЕНЕНО: Удалена строка addMouseWheelListener(zoomListener),
        // так как MouseZoomListener не реализует MouseWheelListener в предоставленной версии.
        // Если зум колесом нужен, MouseZoomListener должен быть доработан и строка возвращена.
        // addMouseWheelListener(zoomListener);
        addMouseListener(panListener);
        addMouseMotionListener(panListener);

        // Слушатель изменения размера и видимости компонента
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                 // Рендерим только если панель видима и имеет корректные размеры
                 if (isShowing() && getWidth() > 0 && getHeight() > 0) {
                    System.out.println("Panel resized. Triggering render.");
                    triggerRender();
                }
            }

            // Также триггерим рендер при первом показе панели (после изменения размера)
            @Override
            public void componentShown(ComponentEvent e) {
                 if (getWidth() > 0 && getHeight() > 0) {
                   System.out.println("Panel shown. Triggering render.");
                   triggerRender();
               }
            }
       });

       // --- Добавляем слушатель для окна Жюлиа по двойному клику ---
       /**
        * Mouse listener to detect double-clicks and open the corresponding Julia Set window.
        */
       addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Проверяем двойной клик левой кнопкой мыши
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    System.out.println("Double-click detected at: " + e.getPoint());
                    openJuliaSetWindow(e.getPoint());
                }
            }
        });
       // -------------------------------------------------------------
    }

    /**
     * Opens the Julia Set window corresponding to the complex number
     * at the clicked screen coordinates.
     * Retrieves current color scheme and iteration count from the ViewModel
     * to initialize the Julia Set view.
     *
     * @param screenPoint The Point on the panel where the double-click occurred.
     */
    private void openJuliaSetWindow(Point screenPoint) {
        int w = getWidth();
        int h = getHeight();
        FractalState currentState = viewModel.getCurrentState(); // Get current state

        // Basic validation
        if (w <= 0 || h <= 0 || currentState == null) {
            System.err.println("Cannot open Julia Set: Panel size or state invalid.");
            JOptionPane.showMessageDialog(this,
                "Cannot open Julia Set window.\nPanel size or fractal state is invalid.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Convert screen coordinates to complex number 'c'
        ComplexNumber c = CoordinateConverter.screenToComplex(
            screenPoint.x, screenPoint.y, w, h, currentState.getViewport()
        );

        if (c == null) {
            System.err.println("Cannot open Julia Set: Failed to convert screen coordinates.");
             JOptionPane.showMessageDialog(this,
                "Cannot open Julia Set window.\nFailed to determine complex coordinates for the clicked point.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get current settings to pass to the Julia window
        ColorScheme currentScheme = currentState.getColorScheme();
        int currentIterations = currentState.getMaxIterations();

        // Find the parent JFrame
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (owner == null) {
             System.err.println("Cannot determine owner frame for Julia Set window. It will be centered on screen.");
             // ИЗМЕНЕНО: Устанавливаем owner в null вместо создания нового JFrame.
             // setLocationRelativeTo(null) корректно обработает этот случай.
             owner = null;
        }

        // Create and display the Julia window on the Event Dispatch Thread
        final JFrame finalOwner = owner; // Final variable for use in lambda
        SwingUtilities.invokeLater(() -> {
            JuliaSetWindow juliaWindow = new JuliaSetWindow(finalOwner, c, currentScheme, currentIterations);
            juliaWindow.display(); // Show the window
        });
    }


    /**
     * Triggers asynchronous rendering of the fractal based on the current ViewModel state
     * and the panel's current dimensions. Cancels any ongoing render task first.
     * Updates the status message and requests a repaint to show the loading indicator.
     */
    public void triggerRender() {
        int width = getWidth();
        int height = getHeight();

        // Skip rendering if panel is not ready or dimensions are invalid
        if (!isShowing() || width <= 0 || height <= 0 || renderer == null) {
            System.out.println("Skipping render: Size " + width + "x" + height + ", Showing: " + isShowing());
            statusMessage = (width <= 0 || height <= 0) ? "Panel size invalid." : "Panel not ready.";
            isRendering = false;
            repaint(); // Repaint to show the status message
            return;
        }

        System.out.println("Triggering render for size: " + width + "x" + height);
        isRendering = true;
        statusMessage = "Rendering...";
        repaint(); // Show "Rendering..." message and potentially loading indicator

        FractalState currentState = viewModel.getCurrentState();
        if (currentState == null) {
            System.err.println("Cannot render: Current state is null.");
            statusMessage = "Error: State is null.";
            isRendering = false;
            repaint();
            return;
        }

        // Call the renderer asynchronously
        renderer.render(currentState, width, height,
                // onComplete callback
                newImage -> {
                    isRendering = false; // Mark rendering as finished
                    if (newImage != null) {
                        this.fractalImage = newImage; // Update the image
                        // Update status with viewport details
                        FractalState completedState = viewModel.getCurrentState(); // Get state again in case it changed slightly
                        statusMessage = String.format("Ready. Viewport: X=[%.4g, %.4g], Y=[%.4g, %.4g], Iter: %d",
                                completedState.getViewport().getMinX(), completedState.getViewport().getMaxX(),
                                completedState.getViewport().getMinY(), completedState.getViewport().getMaxY(),
                                completedState.getMaxIterations());
                    } else {
                        // Handle rendering error
                        statusMessage = "Error during rendering.";
                        this.fractalImage = null; // Ensure no old image is shown
                    }
                    repaint(); // Redraw the panel with the new image or error status
                },
                // onCancel callback
                () -> {
                    isRendering = false; // Mark rendering as finished (cancelled)
                    FractalState cancelledState = viewModel.getCurrentState(); // Get state at cancellation time
                    statusMessage = String.format("Cancelled. Viewport: X=[%.4g, %.4g], Y=[%.4g, %.4g], Iter: %d",
                            cancelledState.getViewport().getMinX(), cancelledState.getViewport().getMaxX(),
                            cancelledState.getViewport().getMinY(), cancelledState.getViewport().getMaxY(),
                            cancelledState.getMaxIterations());
                    // Do not clear the image, leave the partially rendered or previous one
                    repaint(); // Update status message
                }
        );
    }

    /**
     * Paints the component. Draws the background, the fractal image (or status message),
     * the loading indicator if rendering, and the zoom selection rectangle.
     *
     * @param g The Graphics context to paint on.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Paint background

        // Get current state atomically
        BufferedImage currentImage = this.fractalImage;
        boolean renderingNow = this.isRendering;
        String currentStatus = this.statusMessage;

        // Draw the fractal image or a status message if image is null
        if (currentImage != null) {
            g.drawImage(currentImage, 0, 0, this);
        } else {
            // Draw status message centered if no image
            g.setColor(Color.DARK_GRAY); // Use a less bright color for background text
            g.fillRect(0,0, getWidth(), getHeight()); // Fill background to obscure artifacts
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            FontMetrics fm = g.getFontMetrics();
            String message = currentStatus != null ? currentStatus : "Status unavailable";
            int stringWidth = fm.stringWidth(message);
            int stringAscent = fm.getAscent();
            int x = (getWidth() - stringWidth) / 2;
            int y = (getHeight() + stringAscent) / 2 - fm.getDescent(); // Center vertically
            g.drawString(message, x, y);
        }

        // Draw loading indicator overlay if rendering is in progress
        if (renderingNow) {
            drawLoadingIndicator(g);
        }

        // Draw the zoom selection rectangle (if user is dragging)
        zoomListener.drawSelectionRectangle(g);
    }

    /**
     * Draws a semi-transparent overlay with "Rendering..." text
     * to indicate background processing.
     *
     * @param g The Graphics context to draw on.
     */
    private void drawLoadingIndicator(Graphics g) {
        // Semi-transparent black overlay
        g.setColor(new Color(0, 0, 0, 150)); // Black with alpha
        g.fillRect(0, getHeight() / 2 - 20, getWidth(), 40); // Centered vertically

        // White text on the overlay
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String loadingText = "Rendering...";
        int stringWidth = fm.stringWidth(loadingText);
        int stringAscent = fm.getAscent();
        // Center text horizontally and vertically within the overlay rect
        int x = (getWidth() - stringWidth) / 2;
        int y = (getHeight() + stringAscent) / 2 - fm.getDescent();
        g.drawString(loadingText, x, y);
    }

    /**
     * Listens for property changes from the ViewModel.
     * Specifically triggers a re-render when the {@code FractalViewModel.PROPERTY_STATE} changes.
     *
     * @param evt The PropertyChangeEvent object.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Check if the fractal state property has changed
        if (FractalViewModel.PROPERTY_STATE.equals(evt.getPropertyName())) {
            System.out.println("FractalPanel received state update. Triggering render.");
            // Ensure rendering happens on the EDT if state change wasn't triggered by user interaction
            // Though triggerRender itself is safe, direct calls following state changes might benefit
             if (SwingUtilities.isEventDispatchThread()) {
                 triggerRender();
             } else {
                 SwingUtilities.invokeLater(this::triggerRender);
             }
        }
    }

    /**
     * Specifies the preferred size for this panel.
     * Used by layout managers.
     *
     * @return The preferred dimension (800x600).
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }

    /**
     * Gets the currently displayed fractal image.
     * This can be used, for example, for saving the image.
     * Returns a volatile read of the image buffer.
     *
     * @return The current BufferedImage, or null if not yet rendered or if rendering failed.
     */
    public BufferedImage getCurrentImage() {
        return this.fractalImage; // Return the volatile reference
    }
}