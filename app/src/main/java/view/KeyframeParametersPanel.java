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
 */
public class KeyframeParametersPanel extends JPanel {

    private final JFormattedTextField minXField, maxXField, minYField, maxYField, iterationsField;
    private final JButton applyFieldsButton;

    /**
     * Создает панель редактирования параметров.
     *
     * @param applyAction Слушатель {@link ActionListener} для кнопки "Применить к предпросмотру".
     *                    Вызывается, когда пользователь хочет применить значения из полей
     *                    к панели предпросмотра.
     */
    public KeyframeParametersPanel(ActionListener applyAction) {
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder("Параметры кадра"));

        // Настройка форматов для полей ввода
        NumberFormat doubleFormat = NumberFormat.getNumberInstance();
        doubleFormat.setGroupingUsed(false); // Без разделителей тысяч
        doubleFormat.setMaximumFractionDigits(17); // Максимальная точность для double
        doubleFormat.setMinimumFractionDigits(1); // По крайней мере одна цифра после запятой

        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        intFormat.setGroupingUsed(false);

        // Создание полей
        minXField = new JFormattedTextField(doubleFormat);
        maxXField = new JFormattedTextField(doubleFormat);
        minYField = new JFormattedTextField(doubleFormat);
        maxYField = new JFormattedTextField(doubleFormat);
        iterationsField = new JFormattedTextField(intFormat);
        iterationsField.setValue(100); // Значение по умолчанию

        // Установка предпочтительной ширины полей
        Dimension fieldDim = new Dimension(140, iterationsField.getPreferredSize().height);
        minXField.setPreferredSize(fieldDim);
        maxXField.setPreferredSize(fieldDim);
        minYField.setPreferredSize(fieldDim);
        maxYField.setPreferredSize(fieldDim);
        iterationsField.setPreferredSize(fieldDim);

        // Размещение компонентов с GridBagLayout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5); // Немного увеличены отступы
        gbc.anchor = GridBagConstraints.WEST;

        // Метки и поля
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Min X:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; add(minXField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Max X:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; add(maxXField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Min Y:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; add(minYField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; add(new JLabel("Max Y:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; add(maxYField, gbc);
        gbc.gridx = 0; gbc.gridy = 4; add(new JLabel("Итерации:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; add(iterationsField, gbc);

        // Кнопка "Применить"
        applyFieldsButton = new JButton("Применить к предпросмотру");
        applyFieldsButton.setToolTipText("Применяет значения из полей к панели предпросмотра");
        if (applyAction != null) {
            applyFieldsButton.addActionListener(applyAction);
        }
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; // Растягиваем на 2 колонки
        gbc.anchor = GridBagConstraints.CENTER; // Центрируем кнопку
        gbc.insets = new Insets(10, 5, 5, 5); // Увеличиваем верхний отступ
        add(applyFieldsButton, gbc);

        // Изначально поля и кнопка неактивны
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
        // Делаем поля активными, так как есть данные
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
     * состояние активности для кнопки "Применить".
     *
     * @param enabled {@code true}, чтобы разрешить редактирование и активировать кнопку,
     *                {@code false} - чтобы запретить и деактивировать.
     */
    public void setFieldsEnabled(boolean enabled) {
        minXField.setEditable(enabled);
        maxXField.setEditable(enabled);
        minYField.setEditable(enabled);
        maxYField.setEditable(enabled);
        iterationsField.setEditable(enabled);
        applyFieldsButton.setEnabled(enabled);
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
    public Object[] getValidatedParameters() throws ParseException, IllegalArgumentException {
        // Принудительно завершаем редактирование, чтобы получить актуальные значения
        commitEditIfValid(minXField, "Min X");
        commitEditIfValid(maxXField, "Max X");
        commitEditIfValid(minYField, "Min Y");
        commitEditIfValid(maxYField, "Max Y");
        commitEditIfValid(iterationsField, "Итерации");

        // Чтение значений (теперь они должны быть корректного типа Number)
        double minX = ((Number) Objects.requireNonNull(minXField.getValue(), "Поле Min X пусто")).doubleValue();
        double maxX = ((Number) Objects.requireNonNull(maxXField.getValue(), "Поле Max X пусто")).doubleValue();
        double minY = ((Number) Objects.requireNonNull(minYField.getValue(), "Поле Min Y пусто")).doubleValue();
        double maxY = ((Number) Objects.requireNonNull(maxYField.getValue(), "Поле Max Y пусто")).doubleValue();
        int iterations = ((Number) Objects.requireNonNull(iterationsField.getValue(), "Поле Итерации пусто")).intValue();

        // Логическая валидация
        if (minX >= maxX) {
            throw new IllegalArgumentException("Значение Min X должно быть строго меньше Max X.");
        }
        if (minY >= maxY) {
            throw new IllegalArgumentException("Значение Min Y должно быть строго меньше Max Y.");
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
            throw new ParseException("Некорректное значение в поле '" + fieldName + "'", 0);
        }
        field.commitEdit(); // Применяет введенное значение
    }
}