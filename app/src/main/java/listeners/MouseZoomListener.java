// File: app/src/main/java/listeners/MouseZoomListener.java
package listeners;

import viewmodel.FractalViewModel;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Обрабатывает масштабирование (зум) в выделенную прямоугольную область
 * с помощью перетаскивания левой кнопки мыши.
 * Также отвечает за отрисовку прямоугольника выделения на панели.
 * Реализует {@link MouseAdapter} и {@link MouseMotionListener}.
 */
public class MouseZoomListener extends MouseAdapter implements MouseMotionListener {

    /** ViewModel для доступа к состоянию и выполнения масштабирования. */
    private final FractalViewModel viewModel;
    /** Панель, на которой отслеживаются события и рисуется прямоугольник выделения. */
    private final JPanel panel;
    /** Начальная точка выделения при нажатии левой кнопки мыши. */
    private Point startPoint = null;
    /** Конечная точка выделения (текущая позиция мыши при перетаскивании). */
    private Point endPoint = null;
    /** Минимальный размер стороны прямоугольника в пикселях, чтобы зум сработал. */
    private static final int MIN_ZOOM_SIZE = 5;


    /**
     * Создает слушателя для масштабирования выделением.
     * @param viewModel ViewModel приложения. Не может быть null.
     * @param panel Панель, на которой будут отслеживаться события. Не может быть null.
     */
    public MouseZoomListener(FractalViewModel viewModel, JPanel panel) {
        this.viewModel = viewModel;
        this.panel = panel;
    }

    /**
     * Вызывается при нажатии кнопки мыши.
     * Если нажата левая кнопка, запоминает начальную точку выделения
     * и инициирует перерисовку панели (для возможного отображения начальной точки).
     * @param e Событие мыши.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        // Начинаем выделение левой кнопкой
        if (SwingUtilities.isLeftMouseButton(e)) {
            startPoint = e.getPoint();
            endPoint = startPoint; // Начинаем с точки
            panel.repaint(); // Перерисовать, чтобы показать начало (если нужно)
        }
    }

    /**
     * Вызывается при перетаскивании мыши с нажатой кнопкой.
     * Если нажата левая кнопка и выделение начато, обновляет конечную точку
     * и инициирует перерисовку панели для отображения прямоугольника выделения.
     * @param e Событие мыши.
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        // Обновляем конечную точку и перерисовываем прямоугольник выделения
        if (SwingUtilities.isLeftMouseButton(e) && startPoint != null) {
            endPoint = e.getPoint();
            panel.repaint(); // Перерисовать панель, чтобы показать прямоугольник
        }
    }

    /**
     * Вызывается при отпускании кнопки мыши.
     * Если была отпущена левая кнопка, выделение было начато и конечная точка установлена,
     * проверяет, имеет ли выделенный прямоугольник достаточный размер.
     * Если да, вызывает метод {@link FractalViewModel#zoomOnScreenRect(int, int, int, int, int, int)}
     * для выполнения масштабирования. Затем сбрасывает состояние выделения и инициирует
     * перерисовку для удаления прямоугольника.
     * @param e Событие мыши.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        // Завершаем выделение и зумируем, если прямоугольник валидный
        if (SwingUtilities.isLeftMouseButton(e) && startPoint != null && endPoint != null) {
            int x1 = startPoint.x;
            int y1 = startPoint.y;
            int x2 = endPoint.x;
            int y2 = endPoint.y;

            // Сбрасываем точки для следующего раза и для отрисовки
            startPoint = null;
            endPoint = null;
            panel.repaint(); // Убрать прямоугольник выделения

            // Проверяем минимальный размер
            if (Math.abs(x1 - x2) >= MIN_ZOOM_SIZE && Math.abs(y1 - y2) >= MIN_ZOOM_SIZE) {
                // Вызываем метод ViewModel для зума
                viewModel.zoomOnScreenRect(x1, y1, x2, y2, panel.getWidth(), panel.getHeight());
            }
        }
    }

    /**
     * Рисует прямоугольник выделения на предоставленном графическом контексте.
     * Вызывается из метода {@code paintComponent} панели {@link #panel}.
     * Рисует прямоугольник только если выделение активно ({@code startPoint} и {@code endPoint} не null).
     * Использует белую пунктирную линию.
     *
     * @param g Графический контекст панели для отрисовки.
     */
    public void drawSelectionRectangle(Graphics g) {
        if (startPoint != null && endPoint != null) {
            Graphics2D g2d = (Graphics2D) g.create(); // Создаем копию, чтобы не менять настройки Graphics
            try { // Используем try-finally для гарантированного dispose()
                g2d.setColor(Color.WHITE); // Цвет рамки
                // Используем XOR режим для инвертирования цветов под рамкой (не всегда хорошо работает)
                // g2d.setXORMode(Color.BLACK);

                // Устанавливаем штриховую линию
                g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                                              10.0f, new float[]{3.0f}, 0.0f));


                int x = Math.min(startPoint.x, endPoint.x);
                int y = Math.min(startPoint.y, endPoint.y);
                int width = Math.abs(startPoint.x - endPoint.x);
                int height = Math.abs(startPoint.y - endPoint.y);

                g2d.drawRect(x, y, width, height);
            } finally {
                 g2d.dispose(); // Освобождаем копию Graphics
            }
        }
    }

    /**
     * Метод интерфейса {@link MouseMotionListener}. Не используется в данной реализации.
     * @param e Событие мыши.
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        // Не используется для зума
    }
}