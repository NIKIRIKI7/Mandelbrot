package view;

import listeners.MousePanListener;
import listeners.MouseZoomListener;
import model.FractalState;
import render.FractalRenderer;
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * JPanel responsible for displaying the rendered fractal image.
 * Listens to ViewModel updates and handles mouse interactions.
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

        zoomListener = new MouseZoomListener(viewModel, this);
        panListener = new MousePanListener(viewModel, this);
        addMouseListener(zoomListener);
        addMouseMotionListener(zoomListener);
        addMouseWheelListener(zoomListener);
        addMouseListener(panListener);
        addMouseMotionListener(panListener);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (getWidth() > 0 && getHeight() > 0) {
                    System.out.println("Panel resized. Triggering render.");
                    triggerRender();
                }
            }
        });
    }

    /**
     * Triggers rendering of the fractal based on the current state and panel size.
     */
    public void triggerRender() {
        int width = getWidth();
        int height = getHeight();
        if (!isShowing() || width <= 0 || height <= 0 || renderer == null) {
            System.out.println("Skipping render: Size " + width + "x" + height + ", Showing: " + isShowing());
            statusMessage = (width <= 0 || height <= 0) ? "Panel size invalid." : "Panel not ready.";
            isRendering = false;
            repaint();
            return;
        }

        System.out.println("Triggering render for size: " + width + "x" + height);
        isRendering = true;
        statusMessage = "Rendering...";
        repaint();

        FractalState currentState = viewModel.getCurrentState();
        if (currentState == null) {
            System.err.println("Cannot render: Current state is null.");
            statusMessage = "Error: State is null.";
            isRendering = false;
            repaint();
            return;
        }

        renderer.render(currentState, width, height,
                newImage -> {
                    isRendering = false;
                    if (newImage != null) {
                        this.fractalImage = newImage;
                        statusMessage = String.format("Ready. Viewport: X=[%.4g, %.4g], Y=[%.4g, %.4g], Iter: %d",
                                currentState.getViewport().getMinX(), currentState.getViewport().getMaxX(),
                                currentState.getViewport().getMinY(), currentState.getViewport().getMaxY(),
                                currentState.getMaxIterations());
                    } else {
                        statusMessage = "Error during rendering.";
                    }
                    repaint();
                },
                () -> {
                    isRendering = false;
                    FractalState cancelledState = viewModel.getCurrentState();
                    statusMessage = String.format("Cancelled. Viewport: X=[%.4g, %.4g], Y=[%.4g, %.4g], Iter: %d",
                            cancelledState.getViewport().getMinX(), cancelledState.getViewport().getMaxX(),
                            cancelledState.getViewport().getMinY(), cancelledState.getViewport().getMaxY(),
                            cancelledState.getMaxIterations());
                    repaint();
                }
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage currentImage = this.fractalImage;
        boolean renderingNow = this.isRendering;
        String currentStatus = this.statusMessage;

        if (currentImage != null) {
            g.drawImage(currentImage, 0, 0, this);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            FontMetrics fm = g.getFontMetrics();
            String message = currentStatus != null ? currentStatus : "Status unavailable";
            int stringWidth = fm.stringWidth(message);
            int stringAscent = fm.getAscent();
            int x = (getWidth() - stringWidth) / 2;
            int y = (getHeight() + stringAscent) / 2 - fm.getDescent();
            g.drawString(message, x, y);
        }

        if (renderingNow) {
            drawLoadingIndicator(g);
        }

        zoomListener.drawSelectionRectangle(g);
    }

    /**
     * Draws a loading indicator during rendering.
     *
     * @param g The Graphics context to draw on.
     */
    private void drawLoadingIndicator(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, getHeight() / 2 - 20, getWidth(), 40);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        String loadingText = "Rendering...";
        int stringWidth = fm.stringWidth(loadingText);
        int stringAscent = fm.getAscent();
        int x = (getWidth() - stringWidth) / 2;
        int y = (getHeight() + stringAscent) / 2 - fm.getDescent();
        g.drawString(loadingText, x, y);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (FractalViewModel.PROPERTY_STATE.equals(evt.getPropertyName())) {
            System.out.println("FractalPanel received state update. Triggering render.");
            triggerRender();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(800, 600);
    }

    /**
     * Gets the currently displayed fractal image.
     *
     * @return The current BufferedImage, or null if not yet rendered.
     */
    public BufferedImage getCurrentImage() {
        return this.fractalImage;
    }
}