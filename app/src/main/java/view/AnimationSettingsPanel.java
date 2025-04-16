// File: app/src/main/java/view/AnimationSettingsPanel.java
package view;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Панель для настройки общих параметров генерируемой анимации,
 * таких как длительность сегмента и частота кадров (FPS).
 */
public class AnimationSettingsPanel extends JPanel {

    private final JSpinner durationSpinner;
    private final JSpinner fpsSpinner;

    /**
     * Создает панель настроек анимации.
     */
    public AnimationSettingsPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(new TitledBorder("Параметры анимации"));

        // Длительность сегмента
        durationSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 600.0, 0.5));
        JSpinner.NumberEditor durationEditor = new JSpinner.NumberEditor(durationSpinner, "0.0");
        durationSpinner.setEditor(durationEditor);
        durationSpinner.setToolTipText("Длительность перехода между двумя соседними ключевыми кадрами (в секундах)");

        // FPS
        fpsSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 120, 1));
        fpsSpinner.setToolTipText("Частота кадров в секунду (FPS) в итоговом видеофайле");

        // Добавление компонентов
        add(new JLabel("Сегмент (сек):"));
        add(durationSpinner);
        add(Box.createHorizontalStrut(15));
        add(new JLabel("FPS:"));
        add(fpsSpinner);
    }

    /**
     * Возвращает выбранную длительность сегмента анимации (в секундах).
     * @return Длительность сегмента (double).
     */
    public double getDurationPerSegment() {
        return ((Number) durationSpinner.getValue()).doubleValue();
    }

    /**
     * Возвращает выбранную частоту кадров (FPS).
     * @return Частота кадров (int).
     */
    public int getFps() {
        return (Integer) fpsSpinner.getValue();
    }

    /**
     * Переопределяем setEnabled для блокировки всех интерактивных компонентов панели.
     * @param enabled {@code true} для включения, {@code false} для выключения.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        durationSpinner.setEnabled(enabled);
        fpsSpinner.setEnabled(enabled);
        // Метки тоже можно сделать серыми
        for (Component comp : getComponents()) {
            if (comp instanceof JLabel) {
                comp.setEnabled(enabled);
            }
        }
    }
}