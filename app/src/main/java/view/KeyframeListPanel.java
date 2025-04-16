// File: app/src/main/java/view/KeyframeListPanel.java
package view;

import model.Keyframe;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Панель для отображения списка ключевых кадров анимации и кнопок управления этим списком.
 * Позволяет добавлять, удалять и выбирать ключевые кадры. Уведомляет внешний
 * компонент об изменении выбора в списке.
 */
public class KeyframeListPanel extends JPanel {

    private final DefaultListModel<Keyframe> keyframeListModel;
    private final JList<Keyframe> keyframeList;
    private final JButton addButton;
    private final JButton removeButton;
    // TODO: Добавить кнопки Up/Down для изменения порядка кадров

    /**
     * Создает панель списка ключевых кадров.
     *
     * @param listModel Модель данных {@link DefaultListModel} для списка {@link Keyframe}.
     * @param addAction Слушатель {@link ActionListener} для кнопки "Добавить".
     * @param removeAction Слушатель {@link ActionListener} для кнопки "Удалить".
     * @param selectionConsumer Потребитель {@link Consumer}, вызываемый при изменении выбора
     *                          в списке. Передает выбранный индекс (или -1, если выбор снят).
     */
    public KeyframeListPanel(DefaultListModel<Keyframe> listModel,
                             ActionListener addAction,
                             ActionListener removeAction,
                             Consumer<Integer> selectionConsumer) {
        this.keyframeListModel = listModel;

        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Ключевые кадры"));

        // Список
        keyframeList = new JList<>(keyframeListModel);
        keyframeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyframeList.setBorder(BorderFactory.createEtchedBorder());
        // Используем cell renderer для отображения имени кадра (хотя toString() тоже работает)
        keyframeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Keyframe) {
                    label.setText(((Keyframe) value).getName());
                    label.setToolTipText(((Keyframe) value).getState().toString()); // Подсказка с полным состоянием
                }
                return label;
            }
        });
        keyframeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) { // Реагируем на окончание изменения выбора
                int selectedIndex = keyframeList.getSelectedIndex();
                updateRemoveButtonState(); // Обновляем активность кнопки "Удалить"
                if (selectionConsumer != null) {
                    selectionConsumer.accept(selectedIndex); // Уведомляем внешний компонент
                }
            }
        });

        // Кнопки
        addButton = new JButton("Добавить тек. вид");
        addButton.setToolTipText("Добавляет текущее состояние фрактала из главного окна как ключевой кадр");
        if (addAction != null) {
            addButton.addActionListener(addAction);
        }

        removeButton = new JButton("Удалить кадр");
        removeButton.setToolTipText("Удаляет выделенный ключевой кадр из списка");
        removeButton.setEnabled(false); // Изначально неактивна, пока ничего не выбрано
        if (removeAction != null) {
            removeButton.addActionListener(removeAction);
        }

        JPanel listButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        listButtonsPanel.add(addButton);
        listButtonsPanel.add(removeButton);
        // TODO: Добавить кнопки Up/Down на эту панель

        add(new JScrollPane(keyframeList), BorderLayout.CENTER);
        add(listButtonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Возвращает выбранный индекс в списке ключевых кадров.
     *
     * @return Выбранный индекс или -1, если ни один элемент не выбран.
     */
    public int getSelectedIndex() {
        return keyframeList.getSelectedIndex();
    }

    /**
     * Программно устанавливает выбранный индекс в списке.
     *
     * @param index Индекс элемента для выбора.
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < keyframeListModel.getSize()) {
            keyframeList.setSelectedIndex(index);
        } else {
            keyframeList.clearSelection();
        }
    }

    /**
     * Гарантирует, что элемент с указанным индексом будет видим (прокручивает список при необходимости).
     *
     * @param index Индекс элемента, который должен стать видимым.
     */
    public void ensureIndexIsVisible(int index) {
        if (index >= 0 && index < keyframeListModel.getSize()) {
            keyframeList.ensureIndexIsVisible(index);
        }
    }

    /**
     * Обновляет состояние активности кнопки "Удалить" в зависимости от наличия выбора в списке.
     */
    public void updateRemoveButtonState() {
        removeButton.setEnabled(keyframeList.getSelectedIndex() != -1);
    }
}