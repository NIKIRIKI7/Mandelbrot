package render;

import model.ColorScheme;
import model.FractalState;
import model.Viewport;
import utils.ComplexNumber;
import utils.CoordinateConverter;
import math.FractalFunction; // <-- Добавлен импорт


import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.*; // Для SwingUtilities



/**
 * Handles the multi-threaded rendering of the Mandelbrot fractal.
 */
public class FractalRenderer {

    private static final int TILE_SIZE = 32; // Размер тайла в пикселях
    private final ExecutorService executor;
    private final int numThreads;
    private final AtomicReference<RenderTask> currentRenderTask = new AtomicReference<>();

    /**
     * Constructs a new FractalRenderer with a thread pool based on available processors.
     */
    public FractalRenderer() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Renderer initialized with " + numThreads + " threads.");
    }

    /**
     * Asynchronously renders the fractal based on the given state and dimensions.
     * Cancels any previous render task before starting a new one.
     *
     * @param state      The FractalState defining the view and parameters.
     * @param width      The target image width.
     * @param height     The target image height.
     * @param onComplete Callback to accept the rendered image upon completion (called on EDT).
     * @param onCancel   Callback called if the task is cancelled (called on EDT).
     */
    public void render(FractalState state, int width, int height,
                       Consumer<BufferedImage> onComplete, Runnable onCancel) {
        if (width <= 0 || height <= 0) {
            System.err.println("Invalid render dimensions: " + width + "x" + height);
            SwingUtilities.invokeLater(() -> onComplete.accept(null));
            return;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        RenderTask newTask = new RenderTask(state, width, height, image, onComplete, onCancel);
        RenderTask oldTask = currentRenderTask.getAndSet(newTask);

        if (oldTask != null) {
            oldTask.cancel();
        }

        newTask.startRendering(executor);
    }

    /**
     * Shuts down the thread pool gracefully. Blocks until all tasks are terminated.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Renderer shut down.");
    }

    private static class RenderTask {
        private final FractalState state;
        private final int width;
        private final int height;
        private final BufferedImage image;
        private final Consumer<BufferedImage> onComplete;
        private final Runnable onCancel;
        private volatile boolean cancelled = false;
        private final List<Future<?>> futures = new ArrayList<>(); // Список задач для отмены

        RenderTask(FractalState state, int width, int height, BufferedImage image,
                   Consumer<BufferedImage> onComplete, Runnable onCancel) {
            this.state = state;
            this.width = width;
            this.height = height;
            this.image = image;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
        }

        /**
         * Cancels the rendering task and interrupts all running tile tasks.
         */
        void cancel() {
            cancelled = true;
            for (Future<?> future : futures) {
                future.cancel(true); // Прерываем потоки
            }
            System.out.println("Render task cancelled.");
            SwingUtilities.invokeLater(onCancel);
        }

        /**
         * Starts the rendering process using the provided ExecutorService.
         *
         * @param executor The thread pool to use for rendering.
         */
        void startRendering(ExecutorService executor) {
            List<Tile> tiles = TileCalculator.calculateTiles(width, height, TILE_SIZE);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, width, height);
            g.dispose();

            executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();

                    for (Tile tile : tiles) {
                        if (cancelled) break;

                        Future<?> future = executor.submit(() -> {
                            if (!cancelled) {
                                renderTile(tile, state, width, height, image);
                            }
                        });
                        futures.add(future);
                    }

                    for (Future<?> future : futures) {
                        if (cancelled) break;
                        try {
                            future.get();
                        } catch (Exception e) {
                            if (!cancelled) {
                                System.err.println("Error rendering tile: " + e.getMessage());
                            }
                        }
                    }

                    if (!cancelled) {
                        long endTime = System.currentTimeMillis();
                        System.out.printf("Rendering finished in %d ms%n", (endTime - startTime));
                        SwingUtilities.invokeLater(() -> onComplete.accept(image));
                    }

                } catch (Exception e) {
                    System.err.println("Rendering failed: " + e.getMessage());
                    if (!cancelled) {
                        SwingUtilities.invokeLater(() -> onComplete.accept(null));
                    }
                }
            });
        }

        /**
         * Renders a single tile of the fractal image.
         *
         * @param tile        The tile to render.
         * @param state       The fractal state.
         * @param imageWidth  The total image width.
         * @param imageHeight The total image height.
         * @param targetImage The image to render into.
         */
        private static void renderTile(Tile tile, FractalState state, int imageWidth, int imageHeight, BufferedImage targetImage) {
            Viewport viewport = state.getViewport();
            int maxIterations = state.getMaxIterations();
            ColorScheme colorScheme = state.getColorScheme();

            for (int y = tile.startY; y < tile.startY + tile.height; ++y) {
                if (Thread.currentThread().isInterrupted()) return;
                for (int x = tile.startX; x < tile.startX + tile.width; ++x) {
                    ComplexNumber c = CoordinateConverter.screenToComplex(x, y, imageWidth, imageHeight, viewport);
                    if (c == null) continue;

                    int iterations = calculateMandelbrotIterations(c, maxIterations);
                    Color color = colorScheme.getColor(iterations, maxIterations);

                    if (x >= 0 && x < targetImage.getWidth() && y >= 0 && y < targetImage.getHeight()) {
                        targetImage.setRGB(x, y, color.getRGB());
                    }
                }
            }
        }

        /**
         * Calculates iterations for a point in the Mandelbrot set.
         *
         * @param c             The complex number to test.
         * @param maxIterations The maximum iterations to perform.
         * @return The number of iterations before escaping, or maxIterations if inside.
         */
        private static int calculateMandelbrotIterations(ComplexNumber c, int maxIterations) {
            ComplexNumber z = new ComplexNumber(0, 0);
            for (int i = 0; i < maxIterations; ++i) {
                if (z.magnitudeSquared() > 4.0) return i;
                z = z.square().add(c);
            }
            return maxIterations;
        }
    }
}