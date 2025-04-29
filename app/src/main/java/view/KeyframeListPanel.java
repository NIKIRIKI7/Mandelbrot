// File: app/src/main/java/view/KeyframeListPanel.java
package view;

import model.Keyframe;
import model.FractalState; // Добавлен импорт для formatStateTooltip
import model.Viewport;   // Добавлен импорт для formatStateTooltip


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * Панель для отображения и управления списком ключевых кадров анимации.
 * Позволяет добавлять, удалять, выбирать и изменять порядок кадров.
 */
public class KeyframeListPanel extends JPanel {

    private final DefaultListModel<Keyframe> keyframeListModel;
    private final JList<Keyframe> keyframeList;
    private final JButton addButton;
    private final JButton removeButton;
    private final JButton upButton;
    private final JButton downButton;
    private final Consumer<Integer> selectionConsumer;

    /**
     * Создает панель списка ключевых кадров.
     *
     * @param listModel Модель данных {@link DefaultListModel}.
     * @param addAction Слушатель для кнопки "Добавить".
     * @param removeAction Слушатель для кнопки "Удалить".
     * @param selectionConsumer Потребитель, вызываемый при изменении выбора в списке.
     */
    public KeyframeListPanel(DefaultListModel<Keyframe> listModel,
                             ActionListener addAction,
                             ActionListener removeAction,
                             Consumer<Integer> selectionConsumer) {
        this.keyframeListModel = listModel;
        this.selectionConsumer = selectionConsumer;

        setLayout(new BorderLayout(5, 5));
        setBorder(new TitledBorder("Ключевые кадры"));

        // --- Список ---
        keyframeList = new JList<>(keyframeListModel);
        keyframeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        keyframeList.setBorder(BorderFactory.createEtchedBorder());
        keyframeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Keyframe) {
                    label.setText(((Keyframe) value).getName());
                    // Тултип с полным состоянием
                    label.setToolTipText("<html><pre>" + formatStateTooltip(((Keyframe) value).getState()) + "</pre></html>");
                } else {
                    label.setToolTipText(null);
                }
                return label;
            }

            // Вспомогательный метод для форматирования тултипа состояния
            private String formatStateTooltip(FractalState state) {
                if (state == null) return "null";
                Viewport vp = state.getViewport();
                if (vp == null) return "Invalid Viewport"; // Доп. проверка
                String schemeName = (state.getColorScheme() != null) ? state.getColorScheme().getName() : "null";
                String functionName = (state.getFractalFunction() != null) ? state.getFractalFunction().getName() : "null";

                return String.format(java.util.Locale.US, // Использовать точку как разделитель
                        "Viewport:\n  X: [%.6g, %.6g]\n  Y: [%.6g, %.6g]\nIterations: %d\nScheme: %s\nFunction: %s",
                        vp.getMinX(), vp.getMaxX(), vp.getMinY(), vp.getMaxY(),
                        state.getMaxIterations(),
                        schemeName,
                        functionName
                );
            }
        });
        keyframeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = keyframeList.getSelectedIndex();
                updateButtonStates(); // Обновляем состояние всех кнопок
                if (this.selectionConsumer != null) {
                    this.selectionConsumer.accept(selectedIndex);
                }
            }
        });

        // --- Панель кнопок ---
        JPanel listButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3)); // Плотнее кнопки

        addButton = new JButton("Добавить вид");
        addButton.setToolTipText("Добавить текущее состояние фрактала из главного окна как новый ключевой кадр");
        if (addAction != null) addButton.addActionListener(addAction);

        removeButton = new JButton("Удалить");
        removeButton.setToolTipText("Удалить выделенный ключевой кадр");
        if (removeAction != null) removeButton.addActionListener(removeAction);

        upButton = new JButton("▲"); // Вверх
        upButton.setToolTipText("Переместить выделенный кадр вверх");
        upButton.setMargin(new Insets(2, 5, 2, 5));
        upButton.addActionListener(e -> moveSelectedItem(-1));

        downButton = new JButton("▼"); // Вниз
        downButton.setToolTipText("Переместить выделенный кадр вниз");
        downButton.setMargin(new Insets(2, 5, 2, 5));
        downButton.addActionListener(e -> moveSelectedItem(1));

        listButtonsPanel.add(addButton);
        listButtonsPanel.add(removeButton);
        listButtonsPanel.add(Box.createHorizontalStrut(10));
        listButtonsPanel.add(upButton);
        listButtonsPanel.add(downButton);

        add(new JScrollPane(keyframeList), BorderLayout.CENTER);
        add(listButtonsPanel, BorderLayout.SOUTH);

        updateButtonStates(); // Установка начального состояния кнопок
    }

    /**
     * Перемещает выделенный элемент в списке вверх или вниз.
     * @param direction -1 для перемещения вверх, +1 для перемещения вниз.
     */
    private void moveSelectedItem(int direction) {
        int index = keyframeList.getSelectedIndex();
        int size = keyframeListModel.getSize();

        if (index == -1) return; // Ничего не выбрано

        int targetIndex = index + direction;

        if (targetIndex < 0 || targetIndex >= size) {
            return; // Нельзя переместить за пределы списка
        }

        // Меняем местами элементы в модели
        Keyframe itemToMove = keyframeListModel.remove(index);
        keyframeListModel.add(targetIndex, itemToMove);

        // Восстанавливаем выделение на перемещенном элементе
        keyframeList.setSelectedIndex(targetIndex);
        keyframeList.ensureIndexIsVisible(targetIndex); // Прокручиваем к нему

        // Уведомляем слушателя об изменении выбора
        // Это также вызовет updateButtonStates() через слушателя списка
        if (selectionConsumer != null) {
            selectionConsumer.accept(targetIndex);
        } else {
            updateButtonStates();
        }
    }

    /**
     * Возвращает индекс выделенного элемента.
     * @return Выделенный индекс или -1.
     */
    public int getSelectedIndex() {
        return keyframeList.getSelectedIndex();
    }

    /**
     * Программно устанавливает выделение на элемент с указанным индексом.
     * @param index Индекс для выделения.
     */
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < keyframeListModel.getSize()) {
            keyframeList.setSelectedIndex(index);
        } else {
            keyframeList.clearSelection();
        }
        // Обновляем состояние кнопок при программной установке индекса
        updateButtonStates();
    }

    /**
     * Снимает выделение со всех элементов списка.
     */
    public void clearSelection() { 
        keyframeList.clearSelection();
        updateButtonStates(); // Обновляем кнопки после снятия выделения
    }

    /**
     * Обновляет отображение списка, чтобы отразить изменения в модели данных.
     * Гарантирует, что все изменения будут правильно отображены в UI.
     */
    public void refreshList() {
        int selectedIndex = keyframeList.getSelectedIndex();
        
        // Сохраняем все кадры во временном массиве
        Keyframe[] frames = new Keyframe[keyframeListModel.getSize()];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = keyframeListModel.getElementAt(i);
        }
        
        // Если список непуст, пересоздаём его, чтобы обновились все поля
        if (frames.length > 0) {
            SwingUtilities.invokeLater(() -> {
                keyframeListModel.clear();  // Очищаем модель
                for (Keyframe frame : frames) {
                    keyframeListModel.addElement(frame);  // Добавляем обратно все элементы
                }
                
                // Восстанавливаем выделение, если было
                if (selectedIndex >= 0 && selectedIndex < keyframeListModel.getSize()) {
                    keyframeList.setSelectedIndex(selectedIndex);
                    keyframeList.ensureIndexIsVisible(selectedIndex);
                }
            });
        } else {
            // Просто обновляем отображение, если список пуст
            keyframeList.repaint();
        }
    }

    /**
     * Гарантирует, что элемент с указанным индексом виден в области прокрутки.
     * @param index Индекс элемента.
     */
    public void ensureIndexIsVisible(int index) {
        if (index >= 0 && index < keyframeListModel.getSize()) {
            keyframeList.ensureIndexIsVisible(index);
        }
    }

    /**
     * Обновляет состояние активности кнопок ("Удалить", "Вверх", "Вниз")
     * в зависимости от наличия и положения выбора в списке.
     */
    public void updateButtonStates() {
        int selectedIndex = keyframeList.getSelectedIndex();
        int listSize = keyframeListModel.getSize();

        boolean isSelected = (selectedIndex != -1);
        removeButton.setEnabled(isSelected);
        upButton.setEnabled(isSelected && selectedIndex > 0); // Нельзя вверх, если выбран первый
        downButton.setEnabled(isSelected && selectedIndex < listSize - 1); // Нельзя вниз, если выбран последний
    }

    /**
     * Переопределяем setEnabled для блокировки всех интерактивных компонентов панели.
     * @param enabled {@code true} для включения, {@code false} для выключения.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        keyframeList.setEnabled(enabled);
        addButton.setEnabled(enabled);
        // Состояние остальных кнопок зависит от выбора, но их тоже надо выключать/включать
        if(enabled) {
            updateButtonStates(); // Восстанавливаем состояние в зависимости от выбора
        } else {
            removeButton.setEnabled(false);
            upButton.setEnabled(false);
            downButton.setEnabled(false);
        }
        // Скролл-панель тоже можно выключить
        Component scrollPane = getComponent(0); // Предполагаем, что это JScrollPane
        if (scrollPane instanceof JScrollPane) {
            scrollPane.setEnabled(enabled);
            // Сделать сам список внутри недоступным визуально (серый фон)
            keyframeList.setBackground(enabled ? UIManager.getColor("List.background") : UIManager.getColor("Panel.background"));
            keyframeList.setForeground(enabled ? UIManager.getColor("List.foreground") : UIManager.getColor("Label.disabledForeground"));
        }
    }
}