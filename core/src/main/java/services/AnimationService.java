// File: core/src/main/java/services/AnimationService.java
package services;

// Импорты могут понадобиться в будущем
// import model.FractalState;
// import java.io.File;
// import java.util.List;

/**
 * Сервис для создания анимации "экскурсии по фракталу".
 * Генерирует последовательность кадров (изображений или состояний)
 * между ключевыми точками, заданными пользователем, и может сохранять
 * результат в видеофайл (функциональность не реализована).
 *
 * <p>Текущая реализация является заглушкой.</p>
 */
public class AnimationService {

    /**
     * Конструктор по умолчанию.
     */
    public AnimationService() {
        // Конструктор пока пуст
        System.out.println("AnimationService initialized (Stub).");
    }

    /**
     * Метод для генерации и сохранения анимации (не реализован).
     *
     * @param keyFrames Список ключевых состояний FractalState.
     * @param frameRate Частота кадров в секунду.
     * @param durationSeconds Продолжительность сегмента между кадрами.
     * @param outputFile Файл для сохранения видео.
     * @throws UnsupportedOperationException Всегда, так как функциональность не реализована.
     */
    public void createAndSaveAnimation(/*List<FractalState> keyFrames, int frameRate, double durationSeconds, File outputFile*/) {
        // TODO: Реализовать логику интерполяции состояний (viewport, возможно итерации/цвета)
        // TODO: Реализовать рендеринг каждого кадра
        // TODO: Реализовать сборку кадров в видеофайл (используя внешние библиотеки, например, JCodec или Xuggler, что сложно)
        throw new UnsupportedOperationException("Анимация еще не реализована.");
    }

    // Другие вспомогательные методы могут быть добавлены здесь
}