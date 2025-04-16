// File: core/src/main/java/utils/CoordinateConverter.java
package utils;

import model.Viewport;
// Предполагается, что класс из JAR находится в пакете com.yourgroup.converter
// и называется, например, ScreenComplexConverter
// import com.yourgroup.converter.ScreenComplexConverter; // Замените на ваш реальный класс

import java.awt.geom.Point2D; // Используем Point2D для точности с double

/**
 * Утилитарный класс для преобразования координат между экранными пикселями
 * и координатами комплексной плоскости на основе заданной области просмотра (Viewport)
 * и размеров экрана (панели отрисовки).
 * Этот класс может либо содержать логику преобразования сам, либо действовать
 * как мост к внешнему модулю конвертации координат (если он используется).
 */
public class CoordinateConverter {

    // Если используется внешний конвертер с состоянием:
    // private final ScreenComplexConverter internalConverter;

    // Приватный конструктор, чтобы предотвратить создание экземпляров утилитарного класса
    private CoordinateConverter() {
        throw new UnsupportedOperationException("Это утилитарный класс, его не нужно инстанциировать");
    }

    /**
     * Преобразует координаты экранного пикселя в комплексное число.
     *
     * @param screenX      X-координата на экране (пиксель).
     * @param screenY      Y-координата на экране (пиксель). В Swing 0 - верхний край.
     * @param screenWidth  Общая ширина области отрисовки (в пикселях).
     * @param screenHeight Общая высота области отрисовки (в пикселях).
     * @param viewport     Текущая область просмотра {@link Viewport}, определяющая регион комплексной плоскости.
     * @return Соответствующее {@link ComplexNumber}. Возвращает null, если входные данные некорректны
     *         (например, нулевая ширина/высота экрана или viewport == null).
     */
    public static ComplexNumber screenToComplex(int screenX, int screenY,
                                                int screenWidth, int screenHeight,
                                                Viewport viewport) {
        // Проверка валидности входных данных
        if (screenWidth <= 0 || screenHeight <= 0 || viewport == null) {
            System.err.printf("Ошибка конвертации: Некорректные входные данные (Ширина=%d, Высота=%d, Viewport=%s)%n",
                              screenWidth, screenHeight, viewport);
            return null;
        }

        // --- Логика преобразования ---
        // 1. Вычисляем относительное положение пикселя на экране в диапазоне [0, 1].
        //    Используем (screenWidth - 1) и (screenHeight - 1), так как пиксели нумеруются
        //    от 0 до width-1 и от 0 до height-1.
        //    proportionX = 0 соответствует левому краю, 1 - правому краю.
        //    proportionY = 0 соответствует верхнему краю, 1 - нижнему краю.
        double proportionX = (screenWidth == 1) ? 0.5 : (double) screenX / (screenWidth - 1);   // Избегаем деления на ноль, если ширина 1
        double proportionY = (screenHeight == 1) ? 0.5 : (double) screenY / (screenHeight - 1); // Избегаем деления на ноль, если высота 1

        // 2. Преобразуем относительные экранные координаты в координаты комплексной плоскости.
        //    Вещественная часть (real) линейно отображается из [0, 1] в [minX, maxX].
        double real = viewport.getMinX() + proportionX * viewport.getWidth();

        //    Мнимая часть (imaginary) линейно отображается из [0, 1] в [maxY, minY].
        //    Обратите внимание: proportionY = 0 (верх экрана) соответствует maxY (верх viewport),
        //    а proportionY = 1 (низ экрана) соответствует minY (низ viewport).
        //    Поэтому используем viewport.getMaxY() и вычитаем смещение.
        double imaginary = viewport.getMaxY() - proportionY * viewport.getHeight();

        return new ComplexNumber(real, imaginary);
        /* --- Пример использования внешнего класса из JAR ---
           try {
               // Адаптируйте вызов под API вашего конвертера
               Point2D.Double complexPoint = getInternalConverterInstance().pixelToComplex(
                   new Point(screenX, screenY),
                   new Dimension(screenWidth, screenHeight),
                   viewport.getMinX(), viewport.getMaxX(),
                   viewport.getMinY(), viewport.getMaxY()
               );
               return new ComplexNumber(complexPoint.x, complexPoint.y);
           } catch (Exception e) { // Обработайте специфичные исключения вашего конвертера
               System.err.println("Ошибка при конвертации координат через внешний модуль: " + e.getMessage());
               e.printStackTrace();
               return null;
           }
        */
    }

     /**
      * Преобразует комплексное число в координаты экранного пикселя.
      *
      * @param complex      Комплексное число {@link ComplexNumber} для преобразования.
      * @param screenWidth  Общая ширина области отрисовки (в пикселях).
      * @param screenHeight Общая высота области отрисовки (в пикселях).
      * @param viewport     Текущая область просмотра {@link Viewport}.
      * @return {@link Point2D.Double}, представляющий экранные координаты (x, y).
      *         Возвращает null, если входные данные некорректны.
      *         Возвращает координаты центра экрана, если Viewport вырожденный (нулевая ширина или высота).
      *         Возвращает Point2D.Double для сохранения точности; округление до int - задача вызывающего кода (View).
      */
     public static Point2D.Double complexToScreen(ComplexNumber complex,
                                                  int screenWidth, int screenHeight,
                                                  Viewport viewport) {
         // Проверка валидности входных данных
         if (screenWidth <= 0 || screenHeight <= 0 || viewport == null || complex == null) {
             System.err.printf("Ошибка конвертации: Некорректные входные данные (Complex=%s, Ширина=%d, Высота=%d, Viewport=%s)%n",
                               complex, screenWidth, screenHeight, viewport);
             return null;
         }

         double viewportWidth = viewport.getWidth();
         double viewportHeight = viewport.getHeight();

         // Проверка на вырожденный Viewport
         if (viewportWidth == 0 || viewportHeight == 0) {
              System.err.println("Ошибка конвертации: Viewport имеет нулевую ширину или высоту.");
              // Не можем корректно преобразовать. Вернем центр экрана как запасной вариант.
              return new Point2D.Double(screenWidth / 2.0, screenHeight / 2.0);
              // Альтернативно: return null;
         }

         // --- Логика преобразования ---
         // 1. Вычисляем относительное положение комплексного числа внутри Viewport в диапазоне [0, 1].
         //    proportionX = 0 соответствует minX, 1 - maxX.
         //    proportionY = 0 соответствует maxY, 1 - minY (из-за инверсии оси Y).
         double proportionX = (complex.getReal() - viewport.getMinX()) / viewportWidth;
         double proportionY = (viewport.getMaxY() - complex.getImaginary()) / viewportHeight; // Инверсия Y

         // 2. Преобразуем относительные координаты Viewport в абсолютные экранные координаты.
         //    Умножаем на (screenWidth - 1) и (screenHeight - 1) для получения пиксельных координат [0, width-1] / [0, height-1].
         double screenX = proportionX * (screenWidth - 1);
         double screenY = proportionY * (screenHeight - 1);

         return new Point2D.Double(screenX, screenY);

         /* --- Пример использования внешнего класса из JAR ---
            try {
                Point screenPoint = getInternalConverterInstance().complexToPixel(
                    new Point2D.Double(complex.getReal(), complex.getImaginary()),
                    new Dimension(screenWidth, screenHeight),
                    viewport.getMinX(), viewport.getMaxX(),
                    viewport.getMinY(), viewport.getMaxY()
                );
                // Преобразуем int Point в Point2D.Double, если нужно сохранить точность
                return new Point2D.Double(screenPoint.x, screenPoint.y);
            } catch (Exception e) {
                System.err.println("Ошибка при конвертации координат через внешний модуль: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
         */
    }

    /* // Пример получения экземпляра внешнего конвертера (если он не статический)
    private static ScreenComplexConverter getInternalConverterInstance() {
        // Реализуйте логику получения/создания экземпляра вашего конвертера
        // Например, Singleton или простое создание new ScreenComplexConverter()
        if (internalConverter == null) {
            // internalConverter = new ScreenComplexConverter(); // Пример
        }
        return internalConverter;
    }
    */
}