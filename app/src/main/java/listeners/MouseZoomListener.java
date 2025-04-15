// File: app/src/main/java/listeners/MouseZoomListener.java
// ИЗМЕНЕНО: Пакет и импорт FractalViewModel исправлены
package listeners; // <-- ИСПРАВЛЕНО: Пакет изменен на 'listeners'

import viewmodel.FractalViewModel; // <-- ИСПРАВЛЕНО: Импорт скорректирован

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Handles zooming into a selected rectangular region using the left mouse button drag.
 * Draws the selection rectangle on the panel.
 */
public class MouseZoomListener extends MouseAdapter implements MouseMotionListener {

    private final FractalViewModel viewModel;
    private final JPanel panel; // Панель, на которой рисуем и слушаем
    private Point startPoint = null; // Начало выделения
    private Point endPoint = null;   // Конец выделения (текущая позиция мыши при драге)
    private static final int MIN_ZOOM_SIZE = 5; // Минимальный размер прямоугольника для зума (пиксели)


    public MouseZoomListener(FractalViewModel viewModel, JPanel panel) {
        this.viewModel = viewModel;
        this.panel = panel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Начинаем выделение левой кнопкой
        if (SwingUtilities.isLeftMouseButton(e)) {
            startPoint = e.getPoint();
            endPoint = startPoint; // Начинаем с точки
            panel.repaint(); // Перерисовать, чтобы показать начало (если нужно)
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Обновляем конечную точку и перерисовываем прямоугольник выделения
        if (SwingUtilities.isLeftMouseButton(e) && startPoint != null) {
            endPoint = e.getPoint();
            panel.repaint(); // Перерисовать панель, чтобы показать прямоугольник
        }
    }

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
     * Рисует прямоугольник выделения на Graphics объекте панели.
     * Вызывается из метода paintComponent панели.
     *
     * @param g Graphics context.
     */
    public void drawSelectionRectangle(Graphics g) {
        if (startPoint != null && endPoint != null) {
            Graphics2D g2d = (Graphics2D) g.create(); // Создаем копию, чтобы не менять настройки Graphics
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
            g2d.dispose(); // Освобождаем копию Graphics
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Не используется для зума
    }
}