// File: app/src/main/java/view/KeyframeParametersPanel.java
package view;

import model.FractalState;
import model.Viewport;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;

/**
 * Панель для отображения и ручного редактирования числовых параметров
 * (координаты Viewport, количество итераций) выбранного ключевого кадра.
 * Изменения в полях теперь должны обрабатываться внешним слушателем (например, focus lost).
 */
public class KeyframeParametersPanel extends JPanel {

    private final JFormattedTextField minXField, maxXField, minYField, maxYField, iterationsField;
    private Runnable parametersChangeListener; // Слушатель для уведомления об изменениях в полях

    /**
     * Создает панель редактирования параметров.
     *
     * @param parametersChangeListener Слушатель, который будет уведомлен при изменении значений в полях.
     */
    public KeyframeParametersPanel(Runnable parametersChangeListener) {
        this.parametersChangeListener = parametersChangeListener;
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

        // Добавляем слушателей потери фокуса для уведомления об изменениях
        addFocusLostListenerToField(minXField);
        addFocusLostListenerToField(maxXField);
        addFocusLostListenerToField(minYField);
        addFocusLostListenerToField(maxYField);
        addFocusLostListenerToField(iterationsField);

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

        // Изначально поля неактивны
        setFieldsEnabled(false);
    }

    /**
     * Добавляет слушателя потери фокуса к полю, который вызывает parametersChangeListener.
     */
    private void addFocusLostListenerToField(JFormattedTextField field) {
        field.addPropertyChangeListener("value", evt -> {
            // Проверяем, активно ли поле, чтобы избежать срабатывания при программном изменении
            if (field.isEditable() && parametersChangeListener != null) {
                parametersChangeListener.run();
            }
        });
    }

    /**
     * Обновляет значения в полях ввода на основе данных из {@link FractalState}.
     * Не вызывает слушателя изменений.
     *
     * @param state Состояние фрактала, параметры которого нужно отобразить.
     *              Если null, поля будут очищены.
     */
    public void updateFields(FractalState state) {
        // Временно удаляем слушателя, чтобы программное обновление не триггерило событие
        Runnable originalListener = this.parametersChangeListener;
        this.parametersChangeListener = null;
        try {
            if (state == null) {
                clearFields();
                setFieldsEnabled(false); // Делаем неактивными при очистке
                return;
            }
            Viewport vp = state.getViewport();
            minXField.setValue(vp.getMinX());
            maxXField.setValue(vp.getMaxX());
            minYField.setValue(vp.getMinY());
            maxYField.setValue(vp.getMaxY());
            iterationsField.setValue(state.getMaxIterations());
            setFieldsEnabled(true);
        } finally {
            // Возвращаем слушателя обратно
            this.parametersChangeListener = originalListener;
        }
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
     * Устанавливает состояние редактируемости для всех полей ввода.
     *
     * @param enabled {@code true}, чтобы разрешить редактирование,
     *                {@code false} - чтобы запретить.
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

        // Метки тоже делаем серыми при выключении
        for (Component comp : getComponents()) {
            if (comp instanceof JLabel) {
                comp.setEnabled(enabled);
            }
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
     * Принудительно вызывает commitEdit для поля, если оно редактируется.
     * Выбрасывает ParseException с указанием имени поля при ошибке.
     */
    private void commitEditIfValid(JFormattedTextField field, String fieldName) throws ParseException {
        if (field.isEditValid()) {
            try {
                field.commitEdit();
            } catch (ParseException e) {
                throw new ParseException("Некорректное значение в поле '" + fieldName + "': " + e.getMessage(), e.getErrorOffset());
            }
        } else {
            // Это может произойти, если значение было введено, но фокус не терялся
            // и значение не валидно для формата.
            // Можно попробовать получить текст и парсить вручную, но commitEdit должен был это сделать.
            // Пока считаем это ошибкой формата.
            throw new ParseException("Некорректный формат значения в поле '" + fieldName + "'.", 0);
        }
    }

    /**
     * Устанавливает слушателя, который будет вызываться при изменении значения в любом поле.
     * @param listener Слушатель
     */
    public void setParametersChangeListener(Runnable listener) {
        this.parametersChangeListener = listener;
    }
}