// File: app/src/main/java/view/AnimationSettingsPanel.java
package view;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Панель для настройки общих параметров генерируемой анимации,
 * таких как длительность сегмента между ключевыми кадрами и
 * частота кадров в секунду (FPS) итогового видео.
 */
public class AnimationSettingsPanel extends JPanel {

    private final JSpinner durationSpinner;
    private final JSpinner fpsSpinner;

    /**
     * Создает панель настроек анимации.
     */
    public AnimationSettingsPanel() {
        // Используем BoxLayout для вертикального расположения или GridBagLayout для большего контроля
        //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); // Вертикальное расположение
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5)); // Горизонтальное расположение с отступами
        setBorder(new TitledBorder("Параметры анимации"));

        // Настройки спиннеров
        // Длительность: от 0.1 до 600 сек, шаг 0.5, значение по умолчанию 5.0
        durationSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 600.0, 0.5));
        JSpinner.NumberEditor durationEditor = new JSpinner.NumberEditor(durationSpinner, "0.0"); // Формат отображения
        durationSpinner.setEditor(durationEditor);
        durationSpinner.setToolTipText("Длительность перехода между двумя соседними ключевыми кадрами (в секундах)");

        // FPS: от 1 до 120, шаг 1, значение по умолчанию 30
        fpsSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 120, 1));
        fpsSpinner.setToolTipText("Количество кадров в секунду в итоговом видеофайле");

        // Добавление компонентов с метками
        add(new JLabel("Сегмент (сек):"));
        add(durationSpinner);
        add(Box.createHorizontalStrut(15)); // Промежуток между настройками
        add(new JLabel("FPS:"));
        add(fpsSpinner);

        // Выравнивание компонентов, если используется BoxLayout
        // durationSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        // fpsSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        // ((JLabel)getComponent(0)).setAlignmentX(Component.LEFT_ALIGNMENT);
        // ((JLabel)getComponent(2)).setAlignmentX(Component.LEFT_ALIGNMENT);

    }

    /**
     * Возвращает выбранное значение длительности одного сегмента анимации
     * (перехода между двумя ключевыми кадрами).
     *
     * @return Длительность сегмента в секундах (тип double).
     */
    public double getDurationPerSegment() {
        // JSpinner возвращает Object, нужно привести к Number и взять doubleValue
        return ((Number) durationSpinner.getValue()).doubleValue();
    }

    /**
     * Возвращает выбранное значение частоты кадров в секунду (FPS)
     * для генерируемого видеофайла.
     *
     * @return Частота кадров (тип int).
     */
    public int getFps() {
        return (Integer) fpsSpinner.getValue();
    }
}