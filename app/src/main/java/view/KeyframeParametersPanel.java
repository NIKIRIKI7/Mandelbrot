// File: app/src/main/java/view/KeyframeParametersPanel.java
package view;

import model.FractalState;
import model.Viewport;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;

/**
 * Панель для отображения и ручного редактирования числовых параметров
 * (координаты Viewport, количество итераций) выбранного ключевого кадра.
 * Позволяет применить изменения к предпросмотру или напрямую к выбранному кадру.
 */
public class KeyframeParametersPanel extends JPanel {

    private final JFormattedTextField minXField, maxXField, minYField, maxYField, iterationsField;
    private final JButton applyToPreviewButton; // Старая кнопка
    private final JButton applyToKeyframeButton; // Новая кнопка

    /**
     * Создает панель редактирования параметров.
     *
     * @param applyToPreviewAction Слушатель для кнопки "Применить к предпросмотру".
     * @param applyToKeyframeAction Слушатель для кнопки "Применить к кадру". // <-- Новый параметр
     */
    public KeyframeParametersPanel(ActionListener applyToPreviewAction, ActionListener applyToKeyframeAction) { // <-- Обновлен конструктор
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Параметры кадра"));

        // Настройка форматов (без изменений)
        NumberFormat doubleFormat = NumberFormat.getNumberInstance(java.util.Locale.US);
        doubleFormat.setGroupingUsed(false);
        doubleFormat.setMaximumFractionDigits(17);
        doubleFormat.setMinimumFractionDigits(1);
        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        intFormat.setGroupingUsed(false);

        // Создание полей (без изменений)
        minXField = new JFormattedTextField(doubleFormat);
        maxXField = new JFormattedTextField(doubleFormat);
        minYField = new JFormattedTextField(doubleFormat);
        maxYField = new JFormattedTextField(doubleFormat);
        iterationsField = new JFormattedTextField(intFormat);
        iterationsField.setValue(100);
        Dimension fieldDim = new Dimension(140, iterationsField.getPreferredSize().height);
        minXField.setPreferredSize(fieldDim);
        maxXField.setPreferredSize(fieldDim);
        minYField.setPreferredSize(fieldDim);
        maxYField.setPreferredSize(fieldDim);
        iterationsField.setPreferredSize(fieldDim);

        // Размещение компонентов с GridBagLayout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Метки и поля (без изменений)
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Мин X:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; add(minXField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Макс X:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; add(maxXField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Мин Y:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; add(minYField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; add(new JLabel("Макс Y:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; add(maxYField, gbc);
        gbc.gridx = 0; gbc.gridy = 4; add(new JLabel("Итерации:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; add(iterationsField, gbc);

        // --- Панель для кнопок ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0)); // Горизонтальная панель для кнопок

        // Кнопка "Применить к предпросмотру"
        applyToPreviewButton = new JButton("К предпросмотру"); // Сокращенное название
        applyToPreviewButton.setToolTipText("Обновить панель предпросмотра значениями, введенными в этих полях");
        if (applyToPreviewAction != null) {
            applyToPreviewButton.addActionListener(applyToPreviewAction);
        }
        buttonPanel.add(applyToPreviewButton); // Добавляем на панель кнопок

        // Кнопка "Применить к кадру" <-- Новая кнопка
        applyToKeyframeButton = new JButton("К кадру"); // Сокращенное название
        applyToKeyframeButton.setToolTipText("Применить введенные значения напрямую к выделенному ключевому кадру в списке");
        if (applyToKeyframeAction != null) {
            applyToKeyframeButton.addActionListener(applyToKeyframeAction);
        }
        buttonPanel.add(applyToKeyframeButton); // Добавляем на панель кнопок

        // Добавляем панель кнопок под полями
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; // Растягиваем на 2 колонки
        gbc.anchor = GridBagConstraints.CENTER; // Центрируем панель кнопок
        gbc.fill = GridBagConstraints.HORIZONTAL; // Растягиваем панель кнопок
        gbc.insets = new Insets(10, 5, 5, 5); // Увеличиваем верхний отступ
        add(buttonPanel, gbc);

        // Изначально поля и кнопки неактивны
        setFieldsEnabled(false);
    }

    /**
     * Обновляет значения в полях ввода на основе данных из {@link FractalState}.
     *
     * @param state Состояние фрактала, параметры которого нужно отобразить.
     *              Если null, поля будут очищены.
     */
    public void updateFields(FractalState state) {
        if (state == null) {
            clearFields();
            setFieldsEnabled(false); // Делаем неактивными при очистке
            return;
        }
        Viewport vp = state.getViewport();
        // Устанавливаем значения, не вызывая события редактирования
        minXField.setValue(vp.getMinX());
        maxXField.setValue(vp.getMaxX());
        minYField.setValue(vp.getMinY());
        maxYField.setValue(vp.getMaxY());
        iterationsField.setValue(state.getMaxIterations());
        // Делаем поля и кнопки активными, так как есть данные
        setFieldsEnabled(true);
    }

    /**
     * Очищает все поля ввода.
     */
    public void clearFields() {
        minXField.setValue(null);
        maxXField.setValue(null);
        minYField.setValue(null);
        maxYField.setValue(null);
        iterationsField.setValue(null);
    }

    /**
     * Устанавливает состояние редактируемости для всех полей ввода и
     * состояние активности для кнопок "Применить".
     *
     * @param enabled {@code true}, чтобы разрешить редактирование и активировать кнопки,
     *                {@code false} - чтобы запретить и деактивировать.
     */
    public void setFieldsEnabled(boolean enabled) {
        // Управляем редактируемостью и enabled состоянием полей
        minXField.setEditable(enabled);
        maxXField.setEditable(enabled);
        minYField.setEditable(enabled);
        maxYField.setEditable(enabled);
        iterationsField.setEditable(enabled);
        minXField.setEnabled(enabled);
        maxXField.setEnabled(enabled);
        minYField.setEnabled(enabled);
        maxYField.setEnabled(enabled);
        iterationsField.setEnabled(enabled);

        // Управляем активностью кнопок
        applyToPreviewButton.setEnabled(enabled);
        applyToKeyframeButton.setEnabled(enabled); // <-- Управляем новой кнопкой

        // Метки тоже делаем серыми при выключении
        for (Component comp : getComponents()) {
            if (comp instanceof JLabel) {
                comp.setEnabled(enabled);
            }
            // Панель с кнопками не трогаем, только сами кнопки
        }
    }

    /**
     * Пытается прочитать и валидировать значения из полей ввода.
     *
     * @return Массив {@code Object[]}, где:
     *         <ul>
     *             <li>{@code [0]} - новый объект {@link Viewport}, созданный из полей X и Y.</li>
     *             <li>{@code [1]} - новое количество итераций ({@code Integer}).</li>
     *         </ul>
     * @throws ParseException Если не удалось распарсить значение в одном из полей.
     * @throws IllegalArgumentException Если введенные значения некорректны (например, minX >= maxX, итерации <= 0).
     * @throws NullPointerException Если значение одного из полей равно null после попытки commitEdit.
     */
    public Object[] getValidatedParameters() throws ParseException, IllegalArgumentException, NullPointerException {
        // Принудительно завершаем редактирование, чтобы получить актуальные значения
        commitEditIfValid(minXField, "Мин X");
        commitEditIfValid(maxXField, "Макс X");
        commitEditIfValid(minYField, "Мин Y");
        commitEditIfValid(maxYField, "Макс Y");
        commitEditIfValid(iterationsField, "Итерации");

        // Чтение значений (теперь они должны быть корректного типа Number)
        double minX = ((Number) Objects.requireNonNull(minXField.getValue(), "Поле Мин X пусто")).doubleValue();
        double maxX = ((Number) Objects.requireNonNull(maxXField.getValue(), "Поле Макс X пусто")).doubleValue();
        double minY = ((Number) Objects.requireNonNull(minYField.getValue(), "Поле Мин Y пусто")).doubleValue();
        double maxY = ((Number) Objects.requireNonNull(maxYField.getValue(), "Поле Макс Y пусто")).doubleValue();
        int iterations = ((Number) Objects.requireNonNull(iterationsField.getValue(), "Поле Итерации пусто")).intValue();

        // Логическая валидация
        if (minX >= maxX) {
            throw new IllegalArgumentException("Значение Мин X должно быть строго меньше Макс X.");
        }
        if (minY >= maxY) {
            throw new IllegalArgumentException("Значение Мин Y должно быть строго меньше Макс Y.");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("Количество итераций должно быть положительным числом (> 0).");
        }

        // Создание и возврат результатов
        Viewport viewport = new Viewport(minX, maxX, minY, maxY);
        return new Object[]{viewport, iterations};
    }

    /**
     * Вспомогательный метод для принудительного завершения редактирования поля.
     * @param field Поле для коммита.
     * @param fieldName Имя поля для сообщения об ошибке.
     * @throws ParseException если значение поля невалидно.
     */
    private void commitEditIfValid(JFormattedTextField field, String fieldName) throws ParseException {
        if (!field.isEditValid()) {
            // Попытка получить текст для более информативной ошибки
            String textValue = field.getText();
            throw new ParseException("Некорректное значение '" + textValue + "' в поле '" + fieldName + "'", 0);
        }
        field.commitEdit(); // Применяет введенное значение
    }

    /**
     * Переопределяем setEnabled для блокировки всех интерактивных компонентов панели.
     * @param enabled {@code true} для включения, {@code false} для выключения.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        // Вызываем наш же метод, который управляет полями и кнопками
        setFieldsEnabled(enabled);
    }
}