// File: app/src/main/java/view/StatusBar.java
package view;

import javax.swing.*;
import java.awt.*;

/**
 * Панель строки состояния для отображения текстовых сообщений и индикатора прогресса.
 * Потокобезопасна для вызовов методов обновления из других потоков.
 */
public class StatusBar extends JPanel {

    private final JLabel statusLabel;
    private final JProgressBar progressBar;

    /**
     * Создает новую строку состояния.
     */
    public StatusBar() {
        super(new BorderLayout(5, 0)); // Отступ между компонентами
        setBorder(BorderFactory.createLoweredBevelBorder()); // Граница для визуального отделения

        statusLabel = new JLabel("Готово");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Отступы внутри метки
        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(150, progressBar.getPreferredSize().height)); // Фикс. ширина
        progressBar.setStringPainted(true);
        progressBar.setVisible(false); // Изначально скрыт

        add(statusLabel, BorderLayout.CENTER);
        add(progressBar, BorderLayout.EAST);
    }

    /**
     * Устанавливает текстовое сообщение в строке состояния.
     * Безопасно для вызова из любого потока.
     *
     * @param text Текст для отображения.
     */
    public void setStatus(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }
    }

    /**
     * Устанавливает значение индикатора прогресса.
     * Безопасно для вызова из любого потока.
     *
     * @param value Значение прогресса (0-100).
     */
    public void setProgress(int value) {
        final int clampedValue = Math.max(0, Math.min(100, value)); // Ограничиваем 0-100
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setValue(clampedValue);
        } else {
            SwingUtilities.invokeLater(() -> progressBar.setValue(clampedValue));
        }
    }

    /**
     * Устанавливает видимость индикатора прогресса.
     * Безопасно для вызова из любого потока.
     *
     * @param visible {@code true}, чтобы показать индикатор, {@code false} - чтобы скрыть.
     */
    public void setProgressVisible(boolean visible) {
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setVisible(visible);
        } else {
            SwingUtilities.invokeLater(() -> progressBar.setVisible(visible));
        }
    }
}