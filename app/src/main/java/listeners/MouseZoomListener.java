// File: app/src/main/java/listeners/MouseZoomListener.java
package listeners;

import viewmodel.FractalViewModel;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * Обрабатывает масштабирование (зум) в выделенную область левой кнопкой мыши.
 * Рисует рамку и полупрозрачную заливку для выделения.
 */
public class MouseZoomListener extends MouseAdapter {

    private final FractalViewModel viewModel;
    private final JPanel panel;
    private Point startPoint = null;
    private Point endPoint = null;
    private static final int MIN_ZOOM_SIZE = 5;
    // Цвет для заливки области выделения
    private static final Color SELECTION_FILL_COLOR = new Color(100, 150, 255, 60); // Полупрозрачный синий
    // Цвет для рамки выделения
    private static final Color SELECTION_BORDER_COLOR = Color.WHITE;
    // Стиль линии для рамки
    private static final Stroke SELECTION_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
            10.0f, new float[]{3.0f}, 0.0f); // Пунктир

    public MouseZoomListener(FractalViewModel viewModel, JPanel panel) {
        this.viewModel = viewModel;
        this.panel = panel;
    }

    // mousePressed, mouseDragged, mouseReleased остаются без изменений

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            startPoint = e.getPoint();
            endPoint = startPoint;
            panel.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && startPoint != null) {
            endPoint = e.getPoint();
            panel.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && startPoint != null && endPoint != null) {
            int x1 = startPoint.x;
            int y1 = startPoint.y;
            int x2 = endPoint.x;
            int y2 = endPoint.y;

            // Сохраняем точки перед сбросом для отрисовки (если нужно)
            startPoint = null;
            endPoint = null;
            panel.repaint(); // Убрать прямоугольник выделения

            if (Math.abs(x1 - x2) >= MIN_ZOOM_SIZE && Math.abs(y1 - y2) >= MIN_ZOOM_SIZE) {
                viewModel.zoomOnScreenRect(x1, y1, x2, y2, panel.getWidth(), panel.getHeight());
            }
        }
    }


    /**
     * Рисует прямоугольник выделения (рамку и заливку) на графическом контексте.
     * Вызывается из метода {@code paintComponent} панели.
     *
     * @param g Графический контекст панели для отрисовки.
     */
    public void drawSelectionRectangle(Graphics g) {
        // Используем локальные копии точек на случай, если они сбросятся в null в другом потоке
        Point p1 = startPoint;
        Point p2 = endPoint;

        if (p1 != null && p2 != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                int x = Math.min(p1.x, p2.x);
                int y = Math.min(p1.y, p2.y);
                int width = Math.abs(p1.x - p2.x);
                int height = Math.abs(p1.y - p2.y);

                // Рисуем полупрозрачную заливку
                g2d.setColor(SELECTION_FILL_COLOR);
                g2d.fillRect(x, y, width, height);

                // Рисуем рамку поверх заливки
                g2d.setColor(SELECTION_BORDER_COLOR);
                g2d.setStroke(SELECTION_STROKE);
                g2d.drawRect(x, y, width, height);

            } finally {
                g2d.dispose();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) { } // Не используется
}