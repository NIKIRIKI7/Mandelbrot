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
 * Обрабатывает панорамирование (перемещение) вида фрактала с помощью
 * перетаскивания правой кнопки мыши.
 * Реализует {@link MouseAdapter} и {@link MouseMotionListener}.
 */
public class MousePanListener extends MouseAdapter {

    /** ViewModel для доступа к состоянию и выполнения панорамирования. */
    private final FractalViewModel viewModel;
    /** Панель, на которой отслеживаются события мыши. */
    private final JPanel panel;
    /** Последняя зафиксированная точка мыши во время перетаскивания. */
    private Point lastPoint = null;
    /** Исходный курсор панели до начала панорамирования. */
    private Cursor originalCursor; // Исходный курсор панели

    /**
     * Создает слушателя для панорамирования.
     * @param viewModel ViewModel приложения. Не может быть null.
     * @param panel Панель, на которой будут отслеживаться события. Не может быть null.
     */
    public MousePanListener(FractalViewModel viewModel, JPanel panel) {
        this.viewModel = viewModel;
        this.panel = panel;
    }

    /**
     * Вызывается при нажатии кнопки мыши.
     * Если нажата правая кнопка, запоминает начальную точку,
     * сохраняет текущий курсор и устанавливает курсор перемещения.
     * @param e Событие мыши.
     */
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

    /**
     * Вызывается при перетаскивании мыши с нажатой кнопкой.
     * Если нажата правая кнопка и панорамирование начато, вычисляет смещение (deltaX, deltaY)
     * и вызывает метод {@link FractalViewModel#panOnScreenDelta(int, int, int, int)} для обновления вида.
     * @param e Событие мыши.
     */
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

    /**
     * Вызывается при отпускании кнопки мыши.
     * Если была отпущена правая кнопка и выполнялось панорамирование,
     * восстанавливает исходный курсор и сбрасывает состояние панорамирования.
     * @param e Событие мыши.
     */
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

    /**
     * Метод интерфейса {@link MouseMotionListener}. Не используется в данной реализации.
     * @param e Событие мыши.
     */
    @Override
    public void mouseMoved(MouseEvent e) { }
}