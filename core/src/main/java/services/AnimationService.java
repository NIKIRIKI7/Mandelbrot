// File: core/src/main/java/services/AnimationService.java
package services;

import math.FractalFunction;
import model.ColorScheme;
import model.FractalState;
import model.Keyframe;
import model.Viewport;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import utils.ComplexNumber;
import utils.Converter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Сервис для создания и сохранения анимированной "экскурсии по фракталу".
 * Генерирует последовательность кадров путем интерполяции между заданными
 * ключевыми кадрами {@link Keyframe} и кодирует их в видеофайл формата MP4
 * с использованием библиотеки JCodec.
 */
public class AnimationService {

    // Размер рендерируемых кадров для анимации (можно сделать настраиваемым)
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 600;
    // Размер тайла для рендеринга кадров (влияет на отзывчивость прогресса)
    private static final int RENDER_TILE_SIZE = 64;


    public AnimationService() {
        System.out.println("AnimationService initialized.");
    }

    /**
     * Создает и сохраняет анимацию фрактала в видеофайл MP4.
     * Выполняет интерполяцию между ключевыми кадрами, рендерит каждый промежуточный кадр
     * и кодирует последовательность кадров в видео. Операция выполняется синхронно
     * в вызывающем потоке (предполагается, что это фоновый поток, например, SwingWorker).
     *
     * @param keyframes Список ключевых кадров {@link Keyframe}. Должен содержать минимум 2 кадра.
     * @param fps Частота кадров в секунду (Frames Per Second) для итогового видео.
     * @param durationPerSegment Продолжительность анимационного сегмента *между* двумя
     *                           последовательными ключевыми кадрами (в секундах).
     * @param outputFile Файл для сохранения видео (должен иметь расширение .mp4).
     * @param progressConsumer Callback для уведомления о прогрессе генерации (принимает значение от 0.0 до 1.0).
     * @param statusConsumer Callback для отправки текстовых сообщений о статусе.
     * @throws IOException Если возникает ошибка при рендеринге или записи видеофайла.
     * @throws IllegalArgumentException Если список keyframes содержит менее 2 кадров, или параметры некорректны.
     * @throws InterruptedException Если поток выполнения был прерван во время рендеринга.
     */
    public void createAndSaveAnimation(List<Keyframe> keyframes,
                                       int fps,
                                       double durationPerSegment,
                                       File outputFile,
                                       Consumer<Double> progressConsumer,
                                       Consumer<String> statusConsumer)
            throws IOException, IllegalArgumentException, InterruptedException {

        // --- Валидация входных данных ---
        if (keyframes == null || keyframes.size() < 2) {
            throw new IllegalArgumentException("Для анимации требуется как минимум 2 ключевых кадра.");
        }
        if (fps <= 0) {
            throw new IllegalArgumentException("Частота кадров (FPS) должна быть положительной.");
        }
        if (durationPerSegment <= 0) {
            throw new IllegalArgumentException("Длительность сегмента должна быть положительной.");
        }
        Objects.requireNonNull(outputFile, "Выходной файл не может быть null.");
        Objects.requireNonNull(progressConsumer, "Callback прогресса не может быть null.");
        Objects.requireNonNull(statusConsumer, "Callback статуса не может быть null.");

        statusConsumer.accept("Инициализация кодера видео...");
        AWTSequenceEncoder encoder = null; // Объявляем до try-блока для использования в catch/finally
        SeekableByteChannel channel = null; // Объявляем канал

        try {
            // --- Подготовка к кодированию ---
            // Создаем канал для записи в файл
            channel = org.jcodec.common.io.NIOUtils.writableChannel(outputFile); // Используем полное имя или импортируем NIOUtils
            // Создаем кодер JCodec, передавая канал и FPS
            Rational rationalFps = Rational.R(fps, 1); // fps/1
            encoder = new AWTSequenceEncoder(channel, rationalFps);

            int totalSegments = keyframes.size() - 1;
            int framesPerSegment = Math.max(1, (int) Math.round(fps * durationPerSegment));
            int totalFrames = totalSegments * framesPerSegment;
            int framesRendered = 0;

            statusConsumer.accept(String.format("Начало генерации %d кадров (%d сегментов по %d кадров)...", totalFrames, totalSegments, framesPerSegment));
            progressConsumer.accept(0.0);

            // --- Цикл по сегментам анимации ---
            for (int i = 0; i < totalSegments; i++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Генерация анимации прервана.");

                Keyframe startKeyframe = keyframes.get(i);
                Keyframe endKeyframe = keyframes.get(i + 1);
                statusConsumer.accept(String.format("Генерация сегмента %d/%d (кадры %d-%d)...",
                        i + 1, totalSegments, framesRendered + 1, framesRendered + framesPerSegment));

                List<FractalState> intermediateStates = interpolateStates(
                        startKeyframe.getState(),
                        endKeyframe.getState(),
                        framesPerSegment);

                // --- Рендеринг и кодирование кадров сегмента ---
                for (int j = 0; j < intermediateStates.size(); j++) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Генерация анимации прервана.");

                    FractalState frameState = intermediateStates.get(j);
                    int currentFrameNumber = framesRendered + j + 1;
                    statusConsumer.accept(String.format("Рендеринг кадра %d/%d...", currentFrameNumber, totalFrames));

                    BufferedImage frameImage = renderSingleFrame(frameState, FRAME_WIDTH, FRAME_HEIGHT);

                    statusConsumer.accept(String.format("Кодирование кадра %d/%d...", currentFrameNumber, totalFrames));
                    encoder.encodeImage(frameImage); // Используем созданный encoder

                    framesRendered++;
                    progressConsumer.accept((double) framesRendered / totalFrames);
                }
            }

            // --- Завершение кодирования ---
            statusConsumer.accept("Завершение записи видеофайла...");
            encoder.finish(); // Завершаем работу энкодера (записывает заголовки и т.д.)
            encoder = null; // Обнуляем ссылку после успешного завершения
            statusConsumer.accept("Видеофайл успешно сохранен: " + outputFile.getName());
            progressConsumer.accept(1.0);

        } catch (IOException e) {
            statusConsumer.accept("Ошибка ввода/вывода при записи видео: " + e.getMessage());
            // Не нужно удалять файл здесь, пусть это решает вызывающий код или пользователь
            throw e;
        } catch (InterruptedException e) {
            statusConsumer.accept("Генерация анимации прервана.");
            // Не нужно удалять файл здесь
            throw e;
        } finally {
            // Закрываем энкодер и канал в блоке finally для гарантии
            if (encoder != null) {
                try {
                    statusConsumer.accept("Попытка аварийного завершения кодера...");
                    encoder.finish(); // Попытка завершить, если еще не завершено
                } catch (Exception ex) {
                    System.err.println("Ошибка при аварийном завершении энкодера: " + ex.getMessage());
                }
            }
            if (channel != null) {
                try {
                    channel.close(); // Закрываем канал
                } catch (IOException ex) {
                    System.err.println("Ошибка при закрытии канала файла: " + ex.getMessage());
                }
            }
            // Если генерация была прервана или неудачна, файл может остаться неполным.
            // Удаление файла лучше оставить на усмотрение пользователя или вызывающего кода.
        }
    }

    // Методы interpolateStates, renderSingleFrame, renderTile остаются без изменений

    /**
     * Выполняет интерполяцию между двумя состояниями фрактала для создания
     * последовательности промежуточных состояний.
     * Интерполирует центр Viewport линейно, а масштаб (ширину/высоту) логарифмически.
     * Количество итераций интерполируется линейно.
     * Цветовая схема и функция фрактала берутся из начального состояния.
     *
     * @param state1 Начальное состояние.
     * @param state2 Конечное состояние.
     * @param numFrames Количество промежуточных кадров, включая начальный и конечный (должно быть >= 2).
     * @return Список {@link FractalState}, представляющий интерполированные кадры.
     */
    private List<FractalState> interpolateStates(FractalState state1, FractalState state2, int numFrames) {
        if (numFrames < 2) {
            return List.of(state1); // Невозможно интерполировать меньше 2 кадров
        }

        List<FractalState> interpolated = new ArrayList<>(numFrames);
        Viewport vp1 = state1.getViewport();
        Viewport vp2 = state2.getViewport();
        int iter1 = state1.getMaxIterations();
        int iter2 = state2.getMaxIterations();
        ColorScheme colorScheme = state1.getColorScheme(); // Берем из первого кадра
        FractalFunction function = state1.getFractalFunction(); // Берем из первого кадра
        // Используем аспект первого кадра как целевой для всех промежуточных
        double aspectRatio = vp1.getAspectRatio();
        // Обработка случая NaN или 0 для аспекта
        if (Double.isNaN(aspectRatio) || aspectRatio <= 0) {
            // Пытаемся взять аспект второго кадра
            aspectRatio = vp2.getAspectRatio();
            if (Double.isNaN(aspectRatio) || aspectRatio <= 0) {
                // Если и он некорректный, используем 4:3 или 16:9 как запасной вариант
                aspectRatio = (double) FRAME_WIDTH / FRAME_HEIGHT; // Используем аспект кадра по умолчанию
                System.err.println("Предупреждение: Не удалось определить корректное соотношение сторон из ключевых кадров, используется " + aspectRatio);
            }
        }


        // Параметры начального Viewport
        double startCenterX = (vp1.getMinX() + vp1.getMaxX()) / 2.0;
        double startCenterY = (vp1.getMinY() + vp1.getMaxY()) / 2.0;
        double startWidth = vp1.getWidth();
        // Обработка нулевой ширины для логарифма
        double startLogWidth = (startWidth > 1e-9) ? Math.log(startWidth) : -Double.MAX_VALUE;

        // Параметры конечного Viewport
        double endCenterX = (vp2.getMinX() + vp2.getMaxX()) / 2.0;
        double endCenterY = (vp2.getMinY() + vp2.getMaxY()) / 2.0;
        double endWidth = vp2.getWidth();
        // Обработка нулевой ширины для логарифма
        double endLogWidth = (endWidth > 1e-9) ? Math.log(endWidth) : -Double.MAX_VALUE;


        for (int i = 0; i < numFrames; i++) {
            // Фактор интерполяции от 0.0 до 1.0
            double t = (numFrames <= 1) ? 0.0 : (double) i / (numFrames - 1);

            // Линейная интерполяция центра
            double currentCenterX = startCenterX * (1.0 - t) + endCenterX * t;
            double currentCenterY = startCenterY * (1.0 - t) + endCenterY * t;

            // Логарифмическая интерполяция масштаба (ширины)
            double currentLogWidth = startLogWidth * (1.0 - t) + endLogWidth * t;
            double currentWidth = Math.exp(currentLogWidth);
            // Проверка на случай очень маленькой ширины после exp()
            currentWidth = Math.max(1e-9, currentWidth); // Минимальная ширина

            double currentHeight = currentWidth / aspectRatio; // Рассчитываем высоту по аспекту

            // Вычисляем границы нового Viewport
            double currentMinX = currentCenterX - currentWidth / 2.0;
            double currentMaxX = currentCenterX + currentWidth / 2.0;
            double currentMinY = currentCenterY - currentHeight / 2.0;
            double currentMaxY = currentCenterY + currentHeight / 2.0;
            Viewport currentViewport = new Viewport(currentMinX, currentMaxX, currentMinY, currentMaxY);

            // Линейная интерполяция итераций
            int currentIterations = (int) Math.round(iter1 * (1.0 - t) + iter2 * t);
            currentIterations = Math.max(1, currentIterations); // Итераций должно быть > 0

            // Создаем промежуточное состояние
            interpolated.add(new FractalState(currentViewport, currentIterations, colorScheme, function));
        }

        return interpolated;
    }


    /**
     * Синхронно рендерит один кадр (FractalState) в BufferedImage.
     * Эта реализация копирует логику рендеринга тайлов из FractalRenderer,
     * но выполняет ее в текущем (фоновом) потоке без использования внешнего ExecutorService.
     *
     * @param state Состояние фрактала для рендеринга.
     * @param width Ширина изображения кадра.
     * @param height Высота изображения кадра.
     * @return Отрендеренное изображение BufferedImage.
     * @throws InterruptedException если поток был прерван во время рендеринга.
     */
    private BufferedImage renderSingleFrame(FractalState state, int width, int height) throws InterruptedException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Ширина и высота кадра должны быть положительными.");
        }
        // Используем TYPE_INT_RGB, так как большинство видеокодеков плохо работают с альфа-каналом
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // --- Логика рендеринга ---
        Viewport viewport = state.getViewport();
        int maxIterations = state.getMaxIterations();
        ColorScheme colorScheme = state.getColorScheme();
        FractalFunction fractalFunction = state.getFractalFunction();

        // Проверка на валидность Viewport перед рендерингом
        if (viewport.getWidth() <= 0 || viewport.getHeight() <= 0) {
            System.err.println("Предупреждение: Рендеринг кадра с вырожденным Viewport: " + viewport);
            // Заполняем черным или серым цветом
            Graphics2D g = image.createGraphics();
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, width, height);
            g.dispose();
            return image;
        }


        List<render.Tile> tiles = render.TileCalculator.calculateTiles(width, height, RENDER_TILE_SIZE);

        for (render.Tile tile : tiles) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Рендеринг кадра прерван.");
            }
            renderTile(tile, viewport, maxIterations, colorScheme, fractalFunction, width, height, image);
        }

        return image;
    }

    /**
     * Рендерит один тайл изображения фрактала.
     */
    private void renderTile(render.Tile tile, Viewport viewport, int maxIterations,
                            ColorScheme colorScheme, FractalFunction fractalFunction,
                            int imageWidth, int imageHeight, BufferedImage targetImage) throws InterruptedException {

        for (int y = tile.getStartY(); y < tile.getStartY() + tile.getHeight(); ++y) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Рендеринг тайла прерван.");

            for (int x = tile.getStartX(); x < tile.getStartX() + tile.getWidth(); ++x) {
                ComplexNumber pointCoords = Converter.screenToComplex(x, y, imageWidth, imageHeight, viewport);
                if (pointCoords == null) continue;

                int iterations = fractalFunction.calculateIterations(pointCoords, pointCoords, maxIterations);
                Color color = colorScheme.getColor(iterations, maxIterations);

                if (x >= 0 && x < imageWidth && y >= 0 && y < imageHeight) {
                    targetImage.setRGB(x, y, color.getRGB());
                }
            }
        }
    }
}