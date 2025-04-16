// File: app/src/main/java/view/GenerationControlPanel.java
package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Панель, содержащая элементы управления процессом генерации анимации:
 * кнопку запуска/отмены, индикатор прогресса и метку для отображения статуса.
 */
public class GenerationControlPanel extends JPanel {

    private final JButton generateButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;

    /**
     * Создает панель управления генерацией анимации.
     *
     * @param generateOrCancelAction Слушатель для кнопки "Генерировать/Отмена".
     */
    public GenerationControlPanel(ActionListener generateOrCancelAction) {
        setLayout(new BorderLayout(5, 5));

        // Кнопка
        generateButton = new JButton("Генерировать видео...");
        generateButton.setToolTipText("Запустить процесс генерации видео или отменить текущий процесс");
        if (generateOrCancelAction != null) {
            generateButton.addActionListener(generateOrCancelAction);
        }

        // Индикатор прогресса
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false); // Изначально скрыт

        // Метка статуса
        statusLabel = new JLabel("Готово к настройке.");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Небольшие отступы

        // Панель для статуса и прогресса
        JPanel statusProgressPanel = new JPanel(new BorderLayout(5, 0)); // Меньший отступ
        statusProgressPanel.add(statusLabel, BorderLayout.CENTER);
        statusProgressPanel.add(progressBar, BorderLayout.EAST); // Поместим прогресс бар справа от статуса

        // Добавляем компоненты
        add(generateButton, BorderLayout.NORTH);
        add(statusProgressPanel, BorderLayout.CENTER); // Статус и прогресс вместе в центре
        setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0)); // Отступ сверху
    }

    /**
     * Обновляет текст в метке статуса. Потокобезопасно.
     * @param text Текст статуса.
     */
    public void setStatus(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }
    }

    /**
     * Возвращает текущий текст статуса.
     * @return Текст статуса.
     */
    public String getStatus() {
        return statusLabel.getText();
    }


    /**
     * Обновляет значение индикатора прогресса (0-100). Потокобезопасно.
     * @param value Значение прогресса.
     */
    public void setProgress(int value) {
        final int clampedValue = Math.max(0, Math.min(100, value));
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setValue(clampedValue);
        } else {
            SwingUtilities.invokeLater(() -> progressBar.setValue(clampedValue));
        }
    }

    /**
     * Устанавливает видимость индикатора прогресса. Потокобезопасно.
     * @param visible {@code true} для показа, {@code false} для скрытия.
     */
    public void setProgressVisible(boolean visible) {
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setVisible(visible);
        } else {
            SwingUtilities.invokeLater(() -> progressBar.setVisible(visible));
        }
    }

    /**
     * Устанавливает текст на кнопке генерации/отмены. Потокобезопасно.
     * @param text Текст для кнопки.
     */
    public void setGenerateButtonText(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            generateButton.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> generateButton.setText(text));
        }
    }

    /**
     * Устанавливает состояние активности кнопки генерации/отмены. Потокобезопасно.
     * @param enabled {@code true} для активации, {@code false} для деактивации.
     */
    public void setGenerateButtonEnabled(boolean enabled) {
        if (SwingUtilities.isEventDispatchThread()) {
            generateButton.setEnabled(enabled);
        } else {
            SwingUtilities.invokeLater(() -> generateButton.setEnabled(enabled));
        }
    }
}