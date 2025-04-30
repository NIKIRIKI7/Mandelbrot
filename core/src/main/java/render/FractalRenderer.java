package render;

import math.FractalFunction;
import model.ColorScheme;
import model.FractalState;
import model.Viewport;
import utils.ComplexNumber;
import utils.CoordinateConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Обеспечивает многопоточный рендеринг фрактала Мандельброта.
 * 
 * Архитектура многопоточности в данной реализации:
 * 
 * 1. Принцип тайлинга (разделения на плитки):
 *    - Большое изображение разбивается на маленькие прямоугольные участки (тайлы) размером TILE_SIZE x TILE_SIZE пикселей
 *    - Каждый тайл обрабатывается отдельным потоком, что позволяет распараллелить вычисления
 *    - TILE_SIZE (32x32) выбран как компромисс между эффективностью распараллеливания и накладными расходами на создание задач
 *      (слишком маленькие тайлы = больше накладных расходов, слишком большие = менее эффективное использование ядер)
 * 
 * 2. Пул потоков (ExecutorService):
 *    - Создается фиксированный пул потоков с количеством, равным числу доступных процессоров
 *    - Фиксированный пул защищает от перегрузки системы, так как количество одновременно работающих потоков ограничено
 *    - Каждый тайл отправляется на обработку в пул как отдельная задача (Runnable)
 * 
 * 3. Атомарные ссылки и переменные:
 *    - AtomicReference<RenderTask> обеспечивает потокобезопасную замену текущей задачи рендеринга
 *    - AtomicInteger используется для подсчета завершенных тайлов без блокировок
 * 
 * 4. Локальные буферы для тайлов:
 *    - Каждый поток рендерит свой тайл в локальный буфер, минимизируя конфликты доступа к памяти
 *    - Только финальное копирование в общее изображение требует синхронизации
 * 
 * 5. Система отмены задач:
 *    - Механизм отмены текущего рендеринга при запуске нового
 *    - Отслеживание Future для каждого тайла, позволяющее отменить отдельные задачи
 *    - Volatile флаг cancelled для быстрых проверок состояния отмены
 * 
 * 6. Обратные вызовы (callbacks):
 *    - onComplete - вызывается в EDT при успешном завершении рендеринга
 *    - onCancel - вызывается в EDT при отмене задачи
 *    - progressUpdater - вызывается после завершения каждого тайла для обновления UI
 *
 * ==== ЦВЕТОВАЯ СХЕМА ====
 * 
 * Процесс рендеринга фрактала с применением цветовой схемы состоит из следующих этапов:
 * 
 * 1. ВЫЧИСЛЕНИЕ ИТЕРАЦИЙ:
 *    - Для каждой точки изображения вычисляются её координаты в комплексной плоскости
 *    - Применяется итерационная формула z = z² + c (или другая для разных типов фракталов)
 *    - Подсчитывается количество итераций, за которое последовательность "убегает" за предел
 *    - Если точка не "убегает" за maxIterations шагов, считается принадлежащей множеству
 * 
 * 2. ПРЕОБРАЗОВАНИЕ ИТЕРАЦИЙ В ЦВЕТ (ColorScheme):
 *    - Интерфейс ColorScheme определяет стратегию отображения итераций в цвет
 *    - Разные реализации (GrayscaleScheme, NonlinearRGBScheme) создают разные визуальные эффекты
 *    - Метод getColor(iterations, maxIterations) преобразует математические значения в RGB
 * 
 * 3. ОТРИСОВКА С ИСПОЛЬЗОВАНИЕМ ТАЙЛИНГА:
 *    - Изображение разбивается на тайлы для параллельной обработки
 *    - Каждый тайл обрабатывается в отдельном потоке
 *    - Для каждого пикселя тайла вычисляются итерации и применяется цветовая схема
 *    - Готовые тайлы объединяются в итоговое изображение
 *
 * Архитектура ColorScheme реализует паттерн "Стратегия", позволяя легко заменять алгоритмы
 * визуализации без изменения кода рендеринга. Благодаря этому можно получать различные
 * визуальные представления одного и того же математического объекта.
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
     * 
     * Класс RenderTask играет ключевую роль в архитектуре многопоточного рендеринга:
     * 
     * 1. Инкапсуляция состояния:
     *    - Хранит всю информацию, необходимую для одного цикла рендеринга: состояние фрактала,
     *      размеры изображения, буфер изображения и колбэки
     *    - Это позволяет изолировать каждый запуск рендеринга, делая его атомарной операцией
     * 
     * 2. Управление жизненным циклом:
     *    - Отвечает за создание, запуск, отмену и завершение всего процесса рендеринга
     *    - Управляет отдельными подзадачами рендеринга для каждого тайла
     * 
     * 3. Механизм отмены и обработки ошибок:
     *    - Содержит volatile флаг cancelled для сигнализации отмены во все потоки рендеринга
     *    - Хранит список всех Future для возможности явной отмены задач
     *    - Обрабатывает исключения, прерывания и другие ошибки рендеринга
     * 
     * 4. Координация уведомлений и UI:
     *    - Вызывает колбэки прогресса, завершения и отмены в нужных потоках (EDT для UI-колбэков)
     *    - Обеспечивает обновление прогресса после завершения каждого тайла
     * 
     * 5. Преимущества выделения отдельного класса:
     *    - Возможность атомарно заменить одну задачу рендеринга на другую (через AtomicReference)
     *    - Чистый и модульный дизайн, изолирующий логику рендеринга
     *    - Упрощение управления ресурсами и очистки памяти
     *
     * Таким образом, RenderTask – это центральный компонент архитектуры, который координирует весь процесс рендеринга,
     * обеспечивая изоляцию состояния и корректную обработку многопоточности.
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
         * 
         * === ПРОЦЕСС ПРИМЕНЕНИЯ ЦВЕТОВОЙ СХЕМЫ ПРИ ОТРИСОВКЕ ===
         * 
         * Процесс применения цветовой схемы включает несколько шагов:
         * 
         * 1. Получение цветовой схемы из состояния фрактала (state.getColorScheme())
         *    - Цветовая схема - это объект, реализующий интерфейс ColorScheme
         *    - Каждая реализация (GrayscaleScheme, NonlinearRGBScheme) использует свой алгоритм
         * 
         * 2. Расчет математических характеристик точки:
         *    - Для каждого пикселя рассчитываются координаты в комплексной плоскости
         *    - Вычисляется число итераций через fractalFunction.calculateIterations()
         *    - Число итераций - ключевая характеристика, определяющая поведение точки во фрактале
         * 
         * 3. Преобразование итераций в цвет:
         *    - Вызывается метод colorScheme.getColor(iterations, maxIterations)
         *    - Алгоритм преобразования зависит от конкретной реализации схемы:
         *      a) GrayscaleScheme: линейная интерполяция между белым и черным
         *         - Точки внутри множества = черные
         *         - Точки вне множества: чем меньше итераций, тем ближе к белому
         *      b) NonlinearRGBScheme: нелинейные формулы для компонентов R, G, B
         *         - Использует специальные формулы для создания эстетичных градиентов
         *         - R: 9 * (1-t) * t^3 * 255
         *         - G: 15 * (1-t)^2 * t^2 * 255
         *         - B: 8.5 * (1-t)^3 * t * 255
         *         - где t = iterations / maxIterations (нормализованное значение)
         * 
         * 4. Установка цвета пикселя:
         *    - Полученный цвет (java.awt.Color) преобразуется в RGB
         *    - Устанавливается в соответствующий пиксель изображения
         * 
         * Такой подход позволяет отделить логику визуализации от математических вычислений,
         * обеспечивая гибкость и расширяемость системы. Добавление новой цветовой схемы
         * не требует изменения кода рендеринга.
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
                    // fractalFunction выполняет математические итерации z = z² + c 
                    // и возвращает число шагов, за которое точка "убегает" за предел
                    int iterations = fractalFunction.calculateIterations(pointCoords, pointCoords, maxIterations);
                    
                    // Получаем цвет и записываем в локальный буфер
                    // Здесь происходит ключевое преобразование математической величины (iterations) в визуальную (цвет)
                    // Различные реализации ColorScheme дают разные визуальные эффекты для одних и тех же значений:
                    // - GrayscaleScheme: преобразует в оттенки серого (меньше итераций = белее, больше = чернее)
                    // - NonlinearRGBScheme: создает цветовые градиенты через нелинейные функции
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