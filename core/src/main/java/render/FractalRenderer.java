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
import java.util.concurrent.atomic.AtomicInteger; // <-- Добавлено для счетчика тайлов
import java.util.concurrent.CancellationException; // <-- Добавлено для обработки отмены



/**
 * Handles the multi-threaded rendering of the Mandelbrot fractal.
 */
public class FractalRenderer {

    private static final int TILE_SIZE = 32; // Размер тайла в пикселях
    private final ExecutorService executor;
    private final int numThreads;
    private final AtomicReference<RenderTask> currentRenderTask = new AtomicReference<>();

    public FractalRenderer() {
        this.numThreads = Runtime.getRuntime().availableProcessors();
        // Используем кэширующий пул или фиксированный, как раньше? Фиксированный проще для управления.
        this.executor = Executors.newFixedThreadPool(numThreads);
        System.out.println("Renderer инициализирован с " + numThreads + " потоками.");
    }

    /**
     * Асинхронно рендерит фрактал.
     * Отменяет предыдущую задачу перед запуском новой.
     *
     * @param state      Состояние фрактала для рендеринга.
     * @param width      Ширина целевого изображения.
     * @param height     Высота целевого изображения.
     * @param onComplete Колбэк при успешном завершении (вызывается в EDT).
     * @param onCancel   Колбэк при отмене задачи (вызывается в EDT).
     * @param progressUpdater Колбэк для обновления прогресса (вызывается после каждого тайла, можно из раб. потока).
     */
    public void render(FractalState state, int width, int height,
                       Consumer<BufferedImage> onComplete, Runnable onCancel, Runnable progressUpdater) { // <-- Добавлен progressUpdater
        if (width <= 0 || height <= 0) {
            System.err.println("Некорректные размеры для рендеринга: " + width + "x" + height);
            SwingUtilities.invokeLater(() -> onComplete.accept(null)); // Уведомляем о неудаче в EDT
            return;
        }

        // Создаем изображение здесь, чтобы оно было доступно задаче
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Создаем новую задачу рендеринга
        RenderTask newTask = new RenderTask(state, width, height, image, onComplete, onCancel, progressUpdater);

        // Получаем и отменяем предыдущую задачу атомарно
        RenderTask oldTask = currentRenderTask.getAndSet(newTask);
        if (oldTask != null) {
            System.out.println("Отмена предыдущей задачи рендеринга...");
            oldTask.cancel();
        }

        // Запускаем новую задачу
        newTask.startRendering(executor);
    }

    public void shutdown() {
        // Отменяем текущую задачу, если она есть
        RenderTask task = currentRenderTask.get();
        if(task != null) {
            task.cancel();
        }

        // Завершаем работу пула потоков
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) { // Ждем немного
                executor.shutdownNow();
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) // Еще ждем
                    System.err.println("Пул потоков рендерера не завершился корректно.");
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Рендерер остановлен.");
    }

    /**
     * Внутренний класс, представляющий одну задачу рендеринга.
     */
    private static class RenderTask {
        private final FractalState state;
        private final int width;
        private final int height;
        private final BufferedImage image;
        private final Consumer<BufferedImage> onComplete;
        private final Runnable onCancel;
        private final Runnable progressUpdater; // <-- Добавлен колбэк прогресса
        private volatile boolean cancelled = false;
        private final List<Future<?>> futures = new ArrayList<>(); // Для возможности отмены отдельных тайлов

        RenderTask(FractalState state, int width, int height, BufferedImage image,
                   Consumer<BufferedImage> onComplete, Runnable onCancel, Runnable progressUpdater) {
            this.state = state;
            this.width = width;
            this.height = height;
            this.image = image;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
            this.progressUpdater = progressUpdater; // <-- Сохраняем колбэк
        }

        /**
         * Отменяет задачу рендеринга.
         */
        void cancel() {
            // Устанавливаем флаг отмены ДО отмены Future
            if (cancelled) return; // Уже отменено
            cancelled = true;
            System.out.println("Запрос на отмену RenderTask...");
            // Отменяем все запущенные или ожидающие Future
            synchronized (futures) { // Синхронизация на случай добавления Future в другом потоке (хотя здесь это маловероятно)
                for (Future<?> future : futures) {
                    future.cancel(true); // true - прерывать поток, если он выполняется
                }
                futures.clear(); // Очищаем список, больше не нужен
            }
            // Вызываем onCancel в EDT ПОСЛЕ установки флага и попытки отмены
            SwingUtilities.invokeLater(onCancel);
        }

        /**
         * Запускает рендеринг тайлов в пуле потоков.
         * @param executor Пул потоков для выполнения задач.
         */
        void startRendering(ExecutorService executor) {
            List<Tile> tiles = TileCalculator.calculateTiles(width, height, TILE_SIZE);
            if (tiles.isEmpty()) {
                System.err.println("Нет тайлов для рендеринга (некорректные размеры?).");
                SwingUtilities.invokeLater(() -> onComplete.accept(image)); // Завершаем с пустым изображением
                return;
            }

            // Заполняем фон серым (быстрее, чем рендерить все пиксели)
            Graphics2D g = image.createGraphics();
            g.setColor(Color.DARK_GRAY); // Цвет фона во время рендеринга
            g.fillRect(0, 0, width, height);
            g.dispose();

            AtomicInteger completedTiles = new AtomicInteger(0); // Счетчик завершенных тайлов

            // Создаем главную задачу, которая запускает рендеринг тайлов
            Future<?> mainRenderFuture = executor.submit(() -> {
                long startTime = System.currentTimeMillis();
                List<Future<?>> tileFutures = new ArrayList<>(tiles.size()); // Локальный список для этой задачи

                try {
                    // Отправляем все задачи на рендеринг тайлов
                    for (Tile tile : tiles) {
                        if (cancelled) throw new CancellationException("Задача отменена перед рендерингом тайла");

                        Future<?> future = executor.submit(() -> {
                            if (Thread.currentThread().isInterrupted() || cancelled) {
                                //System.out.println("Рендеринг тайла пропущен из-за отмены/прерывания");
                                return; // Не рендерим, если отменили или прервали
                            }
                            renderTile(tile, state, width, height, image);
                        });
                        tileFutures.add(future);
                        // Добавляем в общий список для внешней отмены (на всякий случай, если cancel() вызовется во время этого цикла)
                        synchronized(futures) {
                            if(cancelled) throw new CancellationException("Задача отменена во время добавления тайлов");
                            futures.add(future);
                        }
                    }

                    // Ждем завершения всех тайлов
                    for (Future<?> future : tileFutures) {
                        if (cancelled) throw new CancellationException("Задача отменена во время ожидания тайлов");
                        try {
                            future.get(); // Ждем завершения одного тайла
                            // Вызываем progressUpdater ПОСЛЕ успешного завершения тайла
                            if (!cancelled) { // Дополнительная проверка перед вызовом
                                progressUpdater.run();
                            }
                            completedTiles.incrementAndGet(); // Увеличиваем счетчик
                        } catch (CancellationException e) {
                            // Если тайл отменили, просто пропускаем его и идем дальше (или прерываем всю задачу?)
                            System.out.println("Рендеринг тайла отменен.");
                            // Если отменили один тайл, скорее всего, отменена вся задача.
                            if(!cancelled) cancel(); // Отменяем всю задачу, если еще не отменена
                            throw e; // Перебрасываем, чтобы прервать ожидание остальных
                        } catch (InterruptedException e) {
                            System.err.println("Поток рендеринга тайла прерван.");
                            if(!cancelled) cancel(); // Отменяем всю задачу
                            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
                            throw new CancellationException("Задача прервана"); // Используем CancellationException для единообразия
                        } catch (Exception e) {
                            // Ошибка при рендеринге тайла
                            System.err.println("Ошибка рендеринга тайла: " + e.getMessage());
                            e.printStackTrace(); // Логируем для отладки
                            if(!cancelled) cancel(); // Считаем это фатальной ошибкой для всей задачи
                            throw new RuntimeException("Ошибка рендеринга тайла", e); // Перебрасываем
                        }
                    }

                    // Если дошли сюда и не было отмены - рендеринг успешен
                    if (!cancelled) {
                        long endTime = System.currentTimeMillis();
                        System.out.printf("Рендеринг завершен за %d мс (%d тайлов)%n", (endTime - startTime), completedTiles.get());
                        SwingUtilities.invokeLater(() -> {
                            // Финальная проверка на отмену перед вызовом onComplete
                            if (!cancelled) {
                                onComplete.accept(image);
                            } else {
                                System.out.println("Финальный onComplete пропущен из-за отмены.");
                            }
                        });
                    }

                } catch (CancellationException e){
                    // Эта ветка ловит отмену, инициированную изнутри цикла или извне
                    System.out.println("Основная задача рендеринга отменена.");
                    // onCancel уже должен был быть вызван методом cancel()
                } catch (Exception e) {
                    // Ловим другие ошибки основной задачи
                    System.err.println("Ошибка в основной задаче рендеринга: " + e.getMessage());
                    e.printStackTrace();
                    if (!cancelled) { // Если не было явной отмены, вызываем onCancel
                        cancel(); // Устанавливаем флаг и вызываем onCancel
                    }
                    // Не вызываем onComplete(null) здесь, т.к. cancel() вызовет onCancel
                } finally {
                    // Очистка списка futures после завершения/отмены
                    synchronized (futures) {
                        futures.clear();
                    }
                }
            });
            // Добавляем основную задачу в список (хотя ее отмена не так важна, как отмена тайлов)
            synchronized (futures) {
                if(!cancelled) futures.add(mainRenderFuture);
            }
        }

        /**
         * Рендерит тайл в локальный буфер, а затем копирует в целевое изображение.
         * Этот метод оптимизирован для снижения конфликтов по памяти и кэшу между потоками.
         */
        private static void renderTile(Tile tile, FractalState state, int imageWidth, int imageHeight, BufferedImage targetImage) {
            // Получаем данные для рендеринга
            Viewport viewport = state.getViewport();
            int maxIterations = state.getMaxIterations();
            ColorScheme colorScheme = state.getColorScheme();
            FractalFunction fractalFunction = state.getFractalFunction();
            
            // Создаем локальный буфер для этого тайла
            BufferedImage tileBuffer = new BufferedImage(tile.width, tile.height, BufferedImage.TYPE_INT_RGB);
            
            // Рендерим в локальный буфер
            for (int localY = 0; localY < tile.height; ++localY) {
                if (Thread.currentThread().isInterrupted()) return; // Быстрая проверка на прерывание
                
                // Рассчитываем глобальные координаты для преобразования
                int globalY = tile.startY + localY;
                
                for (int localX = 0; localX < tile.width; ++localX) {
                    int globalX = tile.startX + localX;
                    
                    // Преобразуем координаты экрана в комплексные
                    ComplexNumber pointCoords = CoordinateConverter.screenToComplex(
                            globalX, globalY, imageWidth, imageHeight, viewport);
                    if (pointCoords == null) continue;

                    // Рассчитываем итерации для точки
                    int iterations = fractalFunction.calculateIterations(pointCoords, pointCoords, maxIterations);
                    
                    // Получаем цвет и записываем в локальный буфер
                    Color color = colorScheme.getColor(iterations, maxIterations);
                    tileBuffer.setRGB(localX, localY, color.getRGB());
                }
            }
            
            // Проверяем на прерывание перед финальной копией
            if (Thread.currentThread().isInterrupted()) return;
            
            // Копируем данные в целевое изображение
            // Синхронизируем доступ к целевому изображению
            synchronized(targetImage) {
                Graphics2D g2d = targetImage.createGraphics();
                try {
                    g2d.drawImage(tileBuffer, tile.startX, tile.startY, null);
                } finally {
                    g2d.dispose(); // Освобождаем ресурсы
                }
            }
        }
    }
}