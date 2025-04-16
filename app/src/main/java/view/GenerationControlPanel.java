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
     * @param generateOrCancelAction Слушатель {@link ActionListener}, который будет вызываться
     *                               при нажатии кнопки "Генерировать/Отмена".
     */
    public GenerationControlPanel(ActionListener generateOrCancelAction) {
        setLayout(new BorderLayout(5, 5)); // Используем BorderLayout

        // Кнопка Запуска/Отмены
        generateButton = new JButton("Генерировать видео...");
        generateButton.setToolTipText("Запустить процесс генерации видео или отменить текущий процесс");
        if (generateOrCancelAction != null) {
            generateButton.addActionListener(generateOrCancelAction);
        }

        // Индикатор прогресса
        progressBar = new JProgressBar(0, 100); // Диапазон от 0 до 100 %
        progressBar.setStringPainted(true); // Отображать текст (например, "50%")
        progressBar.setVisible(false); // Изначально скрыт, пока генерация не начнется

        // Метка статуса
        statusLabel = new JLabel("Готово к настройке.");
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT); // Выравнивание текста по левому краю

        // Панель для статуса и прогресса (чтобы они были вместе)
        JPanel statusProgressPanel = new JPanel(new BorderLayout(5, 5));
        statusProgressPanel.add(statusLabel, BorderLayout.CENTER); // Статус занимает центр
        statusProgressPanel.add(progressBar, BorderLayout.SOUTH); // Прогресс-бар под статусом

        // Добавляем компоненты на основную панель
        add(generateButton, BorderLayout.NORTH); // Кнопка сверху
        add(statusProgressPanel, BorderLayout.CENTER); // Статус и прогресс в центре
    }

    /**
     * Обновляет текст в метке статуса. Потокобезопасно для вызова из любого потока.
     *
     * @param text Текст для отображения в статусной строке.
     */
    public void setStatus(String text) {
        // Обновляем метку в потоке диспетчеризации событий (EDT)
        if (SwingUtilities.isEventDispatchThread()) {
            statusLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }
    }

    /**
     * Возвращает текущий текст из метки статуса.
     *
     * @return Текст статуса.
     */
    public String getStatus() {
        return statusLabel.getText();
    }


    /**
     * Обновляет значение индикатора прогресса (от 0 до 100).
     * Потокобезопасно для вызова из любого потока.
     *
     * @param value Значение прогресса (0-100). Значения вне диапазона будут обрезаны.
     */
    public void setProgress(int value) {
        // Обновляем прогресс-бар в потоке EDT
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setValue(value);
        } else {
            SwingUtilities.invokeLater(() -> progressBar.setValue(value));
        }
    }

    /**
     * Устанавливает видимость индикатора прогресса.
     * Потокобезопасно для вызова из любого потока.
     *
     * @param visible {@code true}, чтобы показать индикатор, {@code false} - чтобы скрыть.
     */
    public void setProgressVisible(boolean visible) {
        // Обновляем видимость в потоке EDT
        if (SwingUtilities.isEventDispatchThread()) {
            progressBar.setVisible(visible);
        } else {
            SwingUtilities.invokeLater(() -> progressBar.setVisible(visible));
        }
    }

    /**
     * Устанавливает текст на кнопке генерации/отмены.
     * Потокобезопасно для вызова из любого потока.
     *
     * @param text Текст для кнопки (например, "Генерировать видео..." или "Отмена").
     */
    public void setGenerateButtonText(String text) {
        // Обновляем текст кнопки в потоке EDT
        if (SwingUtilities.isEventDispatchThread()) {
            generateButton.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> generateButton.setText(text));
        }
    }

    /**
     * Устанавливает состояние активности (enabled/disabled) для кнопки генерации/отмены.
     * Потокобезопасно для вызова из любого потока.
     *
     * @param enabled {@code true}, чтобы сделать кнопку активной, {@code false} - неактивной.
     */
    public void setGenerateButtonEnabled(boolean enabled) {
        // Обновляем состояние кнопки в потоке EDT
        if (SwingUtilities.isEventDispatchThread()) {
            generateButton.setEnabled(enabled);
        } else {
            SwingUtilities.invokeLater(() -> generateButton.setEnabled(enabled));
        }
    }
}