// File: core/src/main/java/utils/CoordinateConverter.java
package utils;

import model.Viewport;
import java.awt.geom.Point2D; // Используем Point2D для точности с double

/**
 * Утилитарный класс для преобразования координат между системой пиксельных
 * координат экрана (или панели отрисовки) и системой координат комплексной плоскости.
 * Преобразования выполняются на основе заданной области просмотра {@link Viewport}
 * и размеров области отрисовки (ширина и высота в пикселях).
 * <p>
 * Класс содержит статические методы и не предназначен для инстанцирования.
 * Предполагается, что ось Y экранных координат направлена вниз (0 находится вверху),
 * а ось Y (мнимая ось) комплексной плоскости направлена вверх.
 * </p>
 * <p>
 * Этот класс реализует логику преобразования напрямую. В архитектуре упоминалась
 * возможность интеграции с внешним модулем (JAR), но текущий код содержит
 * собственную реализацию.
 * </p>
 */
public final class CoordinateConverter { // final, т.к. утилитарный класс со статическими методами

    // Приватный конструктор, чтобы предотвратить создание экземпляров утилитарного класса
    private CoordinateConverter() {
        throw new UnsupportedOperationException("Это утилитарный класс, его не нужно инстанциировать");
    }

    /**
     * Преобразует координаты экранного пикселя в соответствующее комплексное число.
     *
     * @param screenX      X-координата пикселя на экране (от 0 до {@code screenWidth - 1}).
     * @param screenY      Y-координата пикселя на экране (от 0 до {@code screenHeight - 1}). В Swing 0 - верхний край.
     * @param screenWidth  Общая ширина области отрисовки (в пикселях). Должна быть > 0.
     * @param screenHeight Общая высота области отрисовки (в пикселях). Должна быть > 0.
     * @param viewport     Текущая область просмотра {@link Viewport}, определяющая отображаемый
     *                     регион комплексной плоскости. Не может быть null.
     * @return Соответствующее {@link ComplexNumber} или {@code null}, если входные данные некорректны
     *         (нулевая или отрицательная ширина/высота, {@code viewport == null}).
     */
    public static ComplexNumber screenToComplex(int screenX, int screenY,
                                                int screenWidth, int screenHeight,
                                                Viewport viewport) {
        // Проверка валидности входных данных
        if (screenWidth <= 0 || screenHeight <= 0 || viewport == null) {
            System.err.printf("Ошибка конвертации screenToComplex: Некорректные входные данные (Ширина=%d, Высота=%d, Viewport=%s)%n",
                              screenWidth, screenHeight, viewport);
            return null;
        }

        // --- Логика преобразования ---
        // 1. Вычисляем относительное положение пикселя на экране в диапазоне [0, 1].
        //    Используем (screenWidth - 1.0) и (screenHeight - 1.0), так как пиксели нумеруются
        //    от 0 до width-1 и от 0 до height-1. Деление на (width-1) дает пропорцию.
        //    Используем double для точности.
        //    proportionX = 0 соответствует левому краю (screenX=0), 1 - правому краю (screenX=width-1).
        //    proportionY = 0 соответствует верхнему краю (screenY=0), 1 - нижнему краю (screenY=height-1).
        double proportionX = (screenWidth == 1) ? 0.5 : (double) screenX / (screenWidth - 1.0);   // Избегаем деления на ноль, если ширина 1
        double proportionY = (screenHeight == 1) ? 0.5 : (double) screenY / (screenHeight - 1.0); // Избегаем деления на ноль, если высота 1

        // 2. Преобразуем относительные экранные координаты в координаты комплексной плоскости.
        //    Вещественная часть (real) линейно отображается из [0, 1] в [minX, maxX].
        //    real = minX + proportionX * (maxX - minX)
        double real = viewport.getMinX() + proportionX * viewport.getWidth();

        //    Мнимая часть (imaginary) линейно отображается из [0, 1] в [maxY, minY].
        //    Обратите внимание на инверсию оси Y: proportionY = 0 (верх экрана) соответствует maxY (верх viewport),
        //    а proportionY = 1 (низ экрана) соответствует minY (низ viewport).
        //    imaginary = maxY - proportionY * (maxY - minY)
        double imaginary = viewport.getMaxY() - proportionY * viewport.getHeight();

        return new ComplexNumber(real, imaginary);
    }

     /**
      * Преобразует комплексное число в координаты экранного пикселя.
      *
      * @param complex      Комплексное число {@link ComplexNumber} для преобразования. Не может быть null.
      * @param screenWidth  Общая ширина области отрисовки (в пикселях). Должна быть > 0.
      * @param screenHeight Общая высота области отрисовки (в пикселях). Должна быть > 0.
      * @param viewport     Текущая область просмотра {@link Viewport}. Не может быть null.
      * @return Объект {@link Point2D.Double}, представляющий экранные координаты (x, y),
      *         соответствующие комплексному числу. Возвращает {@code null}, если входные данные некорректны.
      *         Если {@code Viewport} имеет нулевую ширину или высоту, возвращает координаты центра экрана
      *         {@code (screenWidth / 2.0, screenHeight / 2.0)}.
      *         Возвращается {@code Point2D.Double} для сохранения точности; вызывающий код
      *         при необходимости должен выполнить округление до {@code int}.
      */
     public static Point2D.Double complexToScreen(ComplexNumber complex,
                                                  int screenWidth, int screenHeight,
                                                  Viewport viewport) {
         // Проверка валидности входных данных
         if (screenWidth <= 0 || screenHeight <= 0 || viewport == null || complex == null) {
             System.err.printf("Ошибка конвертации complexToScreen: Некорректные входные данные (Complex=%s, Ширина=%d, Высота=%d, Viewport=%s)%n",
                               complex, screenWidth, screenHeight, viewport);
             return null;
         }

         double viewportWidth = viewport.getWidth();
         double viewportHeight = viewport.getHeight();

         // Проверка на вырожденный Viewport (нулевая ширина или высота)
         // Используем небольшой допуск EPSILON для сравнения double с нулем
         final double EPSILON = 1e-9;
         if (viewportWidth < EPSILON || viewportHeight < EPSILON) {
              System.err.println("Ошибка конвертации complexToScreen: Viewport имеет нулевую ширину или высоту.");
              // Не можем корректно преобразовать. Вернем центр экрана как запасной вариант.
              return new Point2D.Double(screenWidth / 2.0, screenHeight / 2.0);
              // Альтернативно: можно вернуть null или бросить исключение
              // return null;
              // throw new IllegalArgumentException("Viewport has zero width or height");
         }

         // --- Логика преобразования ---
         // 1. Вычисляем относительное положение комплексного числа внутри Viewport в диапазоне [0, 1].
         //    proportionX = (real - minX) / (maxX - minX)
         //    proportionY = (imaginary - minY) / (maxY - minY) -- это для стандартной оси Y
         //    Из-за инверсии оси Y экрана, нам нужно отображение из [maxY, minY] в [0, 1].
         //    proportionY_inverted = (maxY - imaginary) / (maxY - minY)
         double proportionX = (complex.getReal() - viewport.getMinX()) / viewportWidth;
         double proportionY = (viewport.getMaxY() - complex.getImaginary()) / viewportHeight; // Инверсия Y

         // 2. Преобразуем относительные координаты Viewport в абсолютные экранные координаты.
         //    Умножаем на (screenWidth - 1.0) и (screenHeight - 1.0) для получения пиксельных координат [0, width-1] / [0, height-1].
         double screenX = proportionX * (screenWidth - 1.0);
         double screenY = proportionY * (screenHeight - 1.0);

         // Опционально: можно ограничить координаты границами экрана, если complex выходит за пределы viewport
         // screenX = Math.max(0, Math.min(screenWidth - 1, screenX));
         // screenY = Math.max(0, Math.min(screenHeight - 1, screenY));

         return new Point2D.Double(screenX, screenY);
    }
}