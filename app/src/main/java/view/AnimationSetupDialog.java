// File: app/src/main/java/view/AnimationSetupDialog.java
package view;

import math.FractalFunction;
import model.ColorScheme;
import model.FractalState;
import model.Keyframe;
import model.Viewport;
import services.AnimationService;
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Диалоговое окно для настройки параметров анимации фрактала.
 * Координирует работу дочерних панелей.
 */
public class AnimationSetupDialog extends JDialog {

    private final FractalViewModel mainViewModel;
    private final AnimationService animationService;
    private final DefaultListModel<Keyframe> keyframeListModel;

    private KeyframeListPanel keyframeListPanel;
    private KeyframePreviewPanel keyframePreviewPanel;
    private KeyframeParametersPanel keyframeParametersPanel;
    private AnimationSettingsPanel animationSettingsPanel;
    private GenerationControlPanel generationControlPanel;

    private SwingWorker<Void, String> animationWorker = null;

    /**
     * Создает диалог настройки анимации.
     * @param ownerFrame Родительское окно.
     * @param mainViewModel ViewModel основного окна.
     * @param animationService Сервис для генерации анимации.
     */
    public AnimationSetupDialog(JFrame ownerFrame, FractalViewModel mainViewModel, AnimationService animationService) {
        super(ownerFrame, "Редактор анимации фрактала", true);
        this.mainViewModel = mainViewModel;
        this.animationService = animationService;
        this.keyframeListModel = new DefaultListModel<>();

        createAndLayoutPanels();

        pack();
        setMinimumSize(new Dimension(850, 650));
        setLocationRelativeTo(ownerFrame);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
    }

    private void createAndLayoutPanels() {
        // Создаем информационную панель с подсказками для работы (новый компонент)
        JPanel helpPanel = createHelpPanel();
        
        keyframeListPanel = new KeyframeListPanel(
                keyframeListModel,
                this::addCurrentViewAsKeyframe,
                this::removeSelectedKeyframe,
                this::handleListSelectionChange
        );
        // Улучшаем заголовок для большей понятности
        keyframeListPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Шаг 1: Добавьте ключевые кадры анимации"));

        keyframePreviewPanel = new KeyframePreviewPanel(
                this::handlePreviewViewportChange
        );
        // Улучшаем заголовок для большей понятности
        keyframePreviewPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Шаг 2: Просмотр и редактирование кадра"));

        keyframeParametersPanel = new KeyframeParametersPanel(
                this::handleParameterChangeFromFields // Передаем новый обработчик
        );
        // Улучшаем заголовок для большей понятности
        keyframeParametersPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Параметры кадра"));

        animationSettingsPanel = new AnimationSettingsPanel();
        // Улучшаем заголовок для большей понятности
        animationSettingsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Шаг 3: Настройте параметры анимации"));

        generationControlPanel = new GenerationControlPanel(
                this::startOrCancelAnimationGeneration
        );
        // Улучшаем заголовок для большей понятности
        generationControlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), 
                "Шаг 4: Сгенерируйте видео"));

        // --- Размещение панелей (улучшенная компоновка с подсказками) ---
        JPanel rightTopPanel = new JPanel(new BorderLayout(10, 10));
        rightTopPanel.add(keyframePreviewPanel, BorderLayout.CENTER);
        rightTopPanel.add(keyframeParametersPanel, BorderLayout.EAST);

        // Добавляем визуальный разделитель между шагами для большей ясности
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(rightTopPanel);
        rightPanel.add(Box.createVerticalStrut(15)); // Увеличен отступ для лучшей группировки
        rightPanel.add(createSeparator("Настройки генерации")); // Визуальный разделитель
        rightPanel.add(Box.createVerticalStrut(5));
        rightPanel.add(animationSettingsPanel);
        rightPanel.add(Box.createVerticalGlue());
        
        // Основной контент с разделением на левую и правую части
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, keyframeListPanel, rightPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.35);
        
        // Добавляем панель помощи сверху для общего понимания процесса
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(helpPanel, BorderLayout.NORTH);
        contentPanel.add(splitPane, BorderLayout.CENTER);
        contentPanel.add(generationControlPanel, BorderLayout.SOUTH);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
    }
    
    /**
     * Создает информационную панель с пошаговыми инструкциями для работы с редактором анимации.
     * Применяет паттерн Decorator для улучшения внешнего вида компонента.
     */
    private JPanel createHelpPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Как создать анимацию:"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Создаем многострочную метку с HTML-форматированием для пошаговой инструкции
        String helpText = "<html><body style='width: 500px'>"
                + "<b>1.</b> Добавьте ключевые кадры из основного окна или настройте их вручную.<br>"
                + "<b>2.</b> Упорядочите кадры с помощью кнопок «Вверх» и «Вниз» для создания последовательности.<br>"
                + "<b>3.</b> Для каждого кадра вы можете настроить параметры (масштаб, позиция, итерации, цвет).<br>"
                + "<b>4.</b> Укажите продолжительность перехода между кадрами и частоту кадров (FPS).<br>"
                + "<b>5.</b> Нажмите «Сгенерировать» для создания плавной анимации между ключевыми кадрами.<br><br>"
                + "<i>Подсказка:</i> Анимация создает плавные переходы между всеми ключевыми кадрами по порядку."
                + "</body></html>";
        
        JLabel helpLabel = new JLabel(helpText);
        
        // Добавляем кнопку-переключатель для показа/скрытия панели подсказок
        JToggleButton toggleHelpButton = new JToggleButton("Скрыть подсказки");
        toggleHelpButton.setSelected(true);
        toggleHelpButton.setToolTipText("Показать или скрыть панель с инструкциями по созданию анимации");
        toggleHelpButton.addActionListener(e -> {
            helpLabel.setVisible(toggleHelpButton.isSelected());
            toggleHelpButton.setText(toggleHelpButton.isSelected() ? "Скрыть подсказки" : "Показать подсказки");
            pack(); // Перерасчет размера диалога
        });
        
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        togglePanel.add(toggleHelpButton);
        
        panel.add(helpLabel);
        panel.add(togglePanel);
        
        return panel;
    }
    
    /**
     * Создает визуальный разделитель с текстовой меткой для группировки элементов интерфейса.
     * @param title текст заголовка разделителя
     * @return панель с разделителем
     */
    private JPanel createSeparator(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        panel.add(label, gbc);
        
        JSeparator separator = new JSeparator();
        gbc.insets = new Insets(2, 0, 5, 0);
        panel.add(separator, gbc);
        
        return panel;
    }

    // --- Обработчики событий ---

    private void handleListSelectionChange(int selectedIndex) {
        boolean selected = (selectedIndex != -1);
        keyframeParametersPanel.setFieldsEnabled(selected);
        // Кнопки Up/Down/Remove обновляются внутри KeyframeListPanel

        if (selected) {
            try {
                Keyframe selectedKeyframe = keyframeListModel.getElementAt(selectedIndex);
                // Проверим на null на всякий случай
                if (selectedKeyframe != null && selectedKeyframe.getState() != null) {
                    loadStateToPreviewAndFields(selectedKeyframe.getState());
                    generationControlPanel.setStatus("Выбран кадр: " + selectedKeyframe.getName());
                } else {
                    // Состояние невалидно, очищаем
                    keyframePreviewPanel.loadState(null);
                    keyframeParametersPanel.clearFields();
                    keyframeParametersPanel.setFieldsEnabled(false); // Деактивируем поля
                    generationControlPanel.setStatus("Ошибка: Некорректный выбранный кадр.");
                    showError("Выбранный кадр содержит некорректные данные.");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // Индекс стал невалидным (редкая ситуация)
                keyframePreviewPanel.loadState(null);
                keyframeParametersPanel.clearFields();
                keyframeParametersPanel.setFieldsEnabled(false);
                generationControlPanel.setStatus("Ошибка: Выбранный индекс недействителен.");
                keyframeListPanel.clearSelection();
            }
        } else {
            keyframePreviewPanel.loadState(null);
            keyframeParametersPanel.clearFields();
            generationControlPanel.setStatus("Выберите кадр или добавьте новый.");
        }
    }


    private void loadStateToPreviewAndFields(FractalState state) {
        keyframePreviewPanel.loadState(state);
        keyframeParametersPanel.updateFields(state);
    }

    /**
     * Добавляет новый ключевой кадр в список.
     * Если список пуст, состояние берется из основного окна.
     * Если в списке уже есть кадры, новый кадр копирует состояние последнего кадра.
     */
    private void addCurrentViewAsKeyframe(ActionEvent e) {
        FractalState stateForNewKeyframe;

        if (keyframeListModel.isEmpty()) {
            // Список пуст, берем состояние из основного окна
            stateForNewKeyframe = mainViewModel.getCurrentState();
            if (stateForNewKeyframe == null) {
                showError("Не удалось получить текущее состояние фрактала из главного окна.");
                return; // Не можем добавить кадр без состояния
            }
        } else {
            // Список не пуст, берем состояние последнего кадра
            try {
                Keyframe lastKeyframe = keyframeListModel.getElementAt(keyframeListModel.getSize() - 1);
                stateForNewKeyframe = lastKeyframe.getState();
                // Проверка на null, хотя getState не должен возвращать null по контракту Keyframe
                if (stateForNewKeyframe == null) {
                     showError("Ошибка: состояние последнего кадра некорректно.");
                     // В качестве запасного варианта можно взять дефолтное состояние
                     // stateForNewKeyframe = FractalState.createDefault();
                     // Или просто не добавлять кадр
                     return;
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                // Очень редкая ситуация, если список изменился между isEmpty и getElementAt
                showError("Внутренняя ошибка при получении последнего кадра.");
                return;
            }
        }

        // Создаем и добавляем новый кадр
        Keyframe newKeyframe = new Keyframe(stateForNewKeyframe);
        keyframeListModel.addElement(newKeyframe);
        int newIndex = keyframeListModel.getSize() - 1;
        keyframeListPanel.setSelectedIndex(newIndex); // Выделяем добавленный кадр
        keyframeListPanel.ensureIndexIsVisible(newIndex); // Прокручиваем к нему
        generationControlPanel.setStatus("Добавлен новый кадр: " + newKeyframe.getName());

        // Загружаем состояние нового кадра в панели редактирования
        loadStateToPreviewAndFields(stateForNewKeyframe);
    }

    /**
     * Удаляет выбранный ключевой кадр из списка.
     */
    private void removeSelectedKeyframe(ActionEvent e) {
        int selectedIndex = keyframeListPanel.getSelectedIndex();
        if (selectedIndex != -1) {
            try {
                Keyframe removed = keyframeListModel.remove(selectedIndex);
                generationControlPanel.setStatus("Удален кадр: " + removed.getName());
                // Выделение снимется автоматически, handleListSelectionChange будет вызван
            } catch (ArrayIndexOutOfBoundsException ex) {
                showError("Ошибка: Не удалось удалить кадр по выбранному индексу.");
                keyframeListPanel.clearSelection(); // Сбрасываем выбор
            }
        } else {
            showWarning("Сначала выберите кадр для удаления.");
        }
    }

    /**
     * Обработчик, вызываемый при изменении значения в любом поле KeyframeParametersPanel.
     * Обновляет предпросмотр в соответствии с введенными значениями.
     * Не изменяет сам ключевой кадр, только предпросмотр.
     */
    private void handleParameterChangeFromFields() {
        try {
            Object[] params = keyframeParametersPanel.getValidatedParameters();
            Viewport newViewport = (Viewport) params[0];
            int newIterations = (Integer) params[1];

            // Получаем текущие схему и функцию из предпросмотра, чтобы не сбрасывать их
            FractalState currentStateInPreview = keyframePreviewPanel.getCurrentState();
            ColorScheme scheme = (currentStateInPreview != null)
                    ? currentStateInPreview.getColorScheme()
                    : FractalState.createDefault().getColorScheme();
            FractalFunction function = (currentStateInPreview != null)
                    ? currentStateInPreview.getFractalFunction()
                    : FractalState.createDefault().getFractalFunction();

            // Создаем новое состояние только с измененным viewport и итерациями
            FractalState newStateForPreview = new FractalState(newViewport, newIterations, scheme, function);

            // Загружаем новое состояние в панель предпросмотра
            // Важно: это не меняет сам выбранный Keyframe в списке!
            keyframePreviewPanel.loadState(newStateForPreview);
            generationControlPanel.setStatus("Предпросмотр обновлен по значениям из полей.");

        } catch (ParseException | IllegalArgumentException | NullPointerException ex) {
            // Ошибку парсинга/валидации должен был показать сам KeyframeParametersPanel
            // Здесь можно просто обновить статус
            generationControlPanel.setStatus("Ошибка ввода в полях параметров.");
            // Можно добавить showWarning, если нужно более явное уведомление
            // showWarning("Некорректное значение в одном из полей параметров.");
        }
    }

    /**
     * Обрабатывает изменение Viewport в панели предпросмотра.
     * Обновляет состояние выбранного ключевого кадра в реальном времени.
     */
    private void handlePreviewViewportChange(Viewport newViewport) {
        if (keyframeListPanel.getSelectedIndex() != -1) {
            try {
                Keyframe selectedKeyframe = keyframeListModel.getElementAt(keyframeListPanel.getSelectedIndex());

                FractalState updatedState = selectedKeyframe.getState().withViewport(newViewport);
                keyframeParametersPanel.updateFields(updatedState);
                selectedKeyframe = new Keyframe(updatedState);
                keyframeListModel.set(keyframeListPanel.getSelectedIndex(), selectedKeyframe);
                keyframeListPanel.refreshList();
            } catch (ArrayIndexOutOfBoundsException ex) {
                showError("Ошибка: Выбранный кадр больше не доступен для обновления.");
                keyframeListPanel.clearSelection(); // Сбрасываем выбор
            }
        }
    }

    private void startOrCancelAnimationGeneration(ActionEvent e) {
        if (animationWorker != null && !animationWorker.isDone()) {
            animationWorker.cancel(true);
            generationControlPanel.setStatus("Запрос на отмену генерации...");
            generationControlPanel.setGenerateButtonEnabled(false);
            return;
        }

        if (keyframeListModel.getSize() < 2) {
            showError("Нужно как минимум два ключевых кадра для анимации.");
            return;
        }

        final List<Keyframe> keyframes = new ArrayList<>(keyframeListModel.getSize());
        for (int i = 0; i < keyframeListModel.getSize(); i++) {
            try {
                keyframes.add(keyframeListModel.getElementAt(i));
            } catch (ArrayIndexOutOfBoundsException ex) {
                showError("Ошибка чтения списка кадров. Попробуйте снова.");
                return;
            }
        }
        final double durationPerSegment = animationSettingsPanel.getDurationPerSegment();
        final int fps = animationSettingsPanel.getFps();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить видео анимации как...");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP4 Video File (*.mp4)", "mp4"));
        fileChooser.setSelectedFile(new File("fractal_animation.mp4"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            generationControlPanel.setStatus("Сохранение видео отменено.");
            return;
        }
        final File outputFile = ensureMp4Extension(fileChooser.getSelectedFile());

        if (outputFile.exists()) {
            int overwriteChoice = JOptionPane.showConfirmDialog(this,
                    "Файл '" + outputFile.getName() + "' уже существует.\nПерезаписать его?",
                    "Подтверждение перезаписи", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwriteChoice != JOptionPane.YES_OPTION) {
                generationControlPanel.setStatus("Сохранение видео отменено.");
                return;
            }
        }

        // --- Настройка UI перед запуском SwingWorker ---
        generationControlPanel.setProgress(0);
        generationControlPanel.setProgressVisible(true);
        generationControlPanel.setStatus("Подготовка к генерации...");
        generationControlPanel.setGenerateButtonText("Отмена");
        generationControlPanel.setGenerateButtonEnabled(true);
        setUIEnabled(false); // Блокируем UI

        // --- Создание и запуск SwingWorker ---
        animationWorker = new SwingWorker<Void, String>() {
            private long startTime;
            @Override
            protected Void doInBackground() throws Exception {
                startTime = System.currentTimeMillis();
                animationService.createAndSaveAnimation(
                        keyframes, fps, durationPerSegment, outputFile,
                        progress -> publishProgress(progress), // Колбэк прогресса
                        this::publish // Колбэк статуса
                );
                return null;
            }

            @Override
            protected void process(List<String> chunks) { // Обработка сообщений статуса в EDT
                // Скрываем текстовые обновления о рендеринге
                if (!chunks.isEmpty()) {
                    // Содержимое chunks игнорируем, только обновляем статус
                    // чтобы показать, что процесс идет, но без детальной информации
                    generationControlPanel.setStatus("Генерация анимации...");
                }
            }

            @Override
            protected void done() { // Выполняется в EDT после завершения/отмены/ошибки doInBackground
                try {
                    if (!isCancelled()) {
                        get(); // Проверяем наличие исключений в doInBackground
                        long endTime = System.currentTimeMillis();
                        generationControlPanel.setStatus(String.format(java.util.Locale.US,
                                "Генерация завершена успешно! (%.1f сек)", (endTime - startTime) / 1000.0));
                        generationControlPanel.setProgress(100);
                        showInfo("Анимация успешно сохранена в файл:\n" + outputFile.getAbsolutePath(), "Генерация завершена");
                    } else {
                        generationControlPanel.setStatus("Генерация отменена пользователем.");
                        generationControlPanel.setProgress(0);
                        deleteOutputFileIfExists(outputFile); // Удаляем недоделанный файл
                    }
                } catch (InterruptedException e) {
                    generationControlPanel.setStatus("Генерация прервана.");
                    generationControlPanel.setProgress(0);
                    deleteOutputFileIfExists(outputFile);
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = Objects.requireNonNullElse(e.getCause(), e);
                    generationControlPanel.setStatus("Ошибка генерации: " + cause.getMessage());
                    generationControlPanel.setProgress(0);
                    showError("Произошла ошибка во время генерации анимации:\n" + cause.getMessage());
                    cause.printStackTrace();
                    deleteOutputFileIfExists(outputFile);
                } catch (CancellationException e) { // Используем java.util.concurrent.CancellationException
                    generationControlPanel.setStatus("Генерация отменена.");
                    generationControlPanel.setProgress(0);
                    deleteOutputFileIfExists(outputFile);
                } finally {
                    generationControlPanel.setGenerateButtonText("Генерировать видео...");
                    generationControlPanel.setGenerateButtonEnabled(true);
                    // Скрываем прогресс только если не было успеха
                    if (!generationControlPanel.getStatus().contains("успешно")) {
                        generationControlPanel.setProgressVisible(false);
                    }
                    setUIEnabled(true); // Разблокируем UI
                    animationWorker = null; // Сбрасываем ссылку на worker
                }
            }

            // Метод для обновления прогресса в EDT
            private void publishProgress(double progress) {
                SwingUtilities.invokeLater(() -> {
                    if (!isCancelled()) {
                        // Обновляем индикатор прогресса
                        int progressPercent = (int) (progress * 100);
                        generationControlPanel.setProgress(progressPercent);
                        
                        // Обновляем статус в зависимости от этапа
                        String statusMsg = "Генерация анимации...";
                        if (progressPercent == 0) {
                            statusMsg = "Подготовка к рендерингу...";
                        } else if (progressPercent > 0 && progressPercent < 50) {
                            statusMsg = "Генерация кадров...";
                        } else if (progressPercent >= 50 && progressPercent < 90) {
                            statusMsg = "Создание видео...";
                        } else if (progressPercent >= 90 && progressPercent < 100) {
                            statusMsg = "Завершение создания анимации...";
                        }
                        generationControlPanel.setStatus(statusMsg);
                    }
                });
            }
        };
        animationWorker.execute();
    }

    /** Блокирует/разблокирует основные элементы управления UI во время генерации. */
    private void setUIEnabled(boolean enabled) {
        keyframeListPanel.setEnabled(enabled);
        keyframePreviewPanel.setEnabled(enabled);
        keyframeParametersPanel.setEnabled(enabled);
        animationSettingsPanel.setEnabled(enabled);
        // При включении восстанавливаем состояние кнопок в зависимости от выбора
        if (enabled) {
            int selectedIndex = keyframeListPanel.getSelectedIndex();
            keyframeParametersPanel.setFieldsEnabled(selectedIndex != -1);
            // Состояние кнопок Вверх/Вниз управляется внутри KeyframeListPanel
            keyframeListPanel.updateButtonStates();
        }
        // Кнопка генерации/отмены управляется отдельно
    }

    private void deleteOutputFileIfExists(File file) {
        if (file != null && file.exists()) {
            System.out.print("Попытка удаления файла " + file.getAbsolutePath() + "... ");
            if (!file.delete()) {
                System.err.println("Не удалось удалить.");
            } else {
                System.out.println("Удален.");
            }
        }
    }

    private File ensureMp4Extension(File file) {
        String path = file.getAbsolutePath();
        String lowerPath = path.toLowerCase();
        if (!lowerPath.endsWith(".mp4")) {
            int dotIndex = path.lastIndexOf('.');
            int slashIndex = path.lastIndexOf(File.separatorChar);
            if (dotIndex > slashIndex) {
                path = path.substring(0, dotIndex);
            }
            return new File(path + ".mp4");
        }
        return file;
    }

    /** Показывает диалоговое окно. */
    public void display() {
        // Сброс UI
        generationControlPanel.setStatus("Выберите кадр или добавьте новый.");
        generationControlPanel.setProgressVisible(false);
        generationControlPanel.setProgress(0);
        generationControlPanel.setGenerateButtonText("Генерировать видео...");
        generationControlPanel.setGenerateButtonEnabled(true);
        keyframeParametersPanel.setFieldsEnabled(false);
        keyframeParametersPanel.clearFields();
        keyframeListModel.clear();
        keyframePreviewPanel.loadState(null);
        setUIEnabled(true);
        setVisible(true);
    }

    /** Обрабатывает закрытие окна диалога. */
    private void handleClose() {
        if (animationWorker != null && !animationWorker.isDone()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Генерация анимации еще не завершена. Прервать и закрыть окно?",
                    "Подтверждение закрытия", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.NO_OPTION) return;
            animationWorker.cancel(true);
        }
        keyframePreviewPanel.shutdownRenderer();
        dispose();
    }

    // Вспомогательные методы для сообщений
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE); }
    private void showWarning(String message) { JOptionPane.showMessageDialog(this, message, "Внимание", JOptionPane.WARNING_MESSAGE); }
    private void showInfo(String message, String title) { JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE); }
}