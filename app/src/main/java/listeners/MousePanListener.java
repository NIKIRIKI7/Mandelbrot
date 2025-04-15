// File: app/src/main/java/listeners/MousePanListener.java
// ИЗМЕНЕНО: Импорты скорректированы
package listeners;

import viewmodel.FractalViewModel; // core

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Handles panning the fractal view using the right mouse button drag.
 */
public class MousePanListener extends MouseAdapter implements MouseMotionListener {

    private final FractalViewModel viewModel;
    private final JPanel panel; // Панель, на которой слушаем события
    private Point lastPoint = null; // Последняя точка при перетаскивании
    private Cursor originalCursor; // Исходный курсор панели

    /**
     * Constructs a MousePanListener.
     * @param viewModel The application's ViewModel.
     * @param panel The panel to listen on.
     */
    public MousePanListener(FractalViewModel viewModel, JPanel panel) {
        this.viewModel = viewModel;
        this.panel = panel;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Начинаем панорамирование только правой кнопкой (или средней, если хотим)
        if (SwingUtilities.isRightMouseButton(e)) {
            // ИЛИ: if (e.getButton() == MouseEvent.BUTTON3) { // Более явно
            lastPoint = e.getPoint();
            originalCursor = panel.getCursor(); // Сохраняем текущий курсор
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            e.consume(); // Поглощаем событие, чтобы оно не обрабатывалось другими слушателями (например, Zoom)
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // Продолжаем панорамирование, если нажата правая кнопка и есть начальная точка
        if (SwingUtilities.isRightMouseButton(e) && lastPoint != null) {
            Point currentPoint = e.getPoint();
            int deltaX = currentPoint.x - lastPoint.x;
            int deltaY = currentPoint.y - lastPoint.y;

            if (deltaX != 0 || deltaY != 0) {
                if (panel.getWidth() > 0 && panel.getHeight() > 0) {
                    // Вызываем метод ViewModel для панорамирования
                    viewModel.panOnScreenDelta(deltaX, deltaY, panel.getWidth(), panel.getHeight());
                }
                // Обновляем lastPoint для следующего шага перетаскивания
                lastPoint = currentPoint;
            }
            e.consume(); // Поглощаем событие
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // Завершаем панорамирование при отпускании правой кнопки
        if (e.getButton() == MouseEvent.BUTTON3) { // Проверяем явно правую кнопку
            if (lastPoint != null) {
                panel.setCursor(originalCursor); // Восстанавливаем исходный курсор
                lastPoint = null; // Сбрасываем состояние
                originalCursor = null;
            }
            e.consume(); // Поглощаем событие
        }
    }

    // mouseMoved не нужен для панорамирования перетаскиванием
    @Override
    public void mouseMoved(MouseEvent e) { }
}