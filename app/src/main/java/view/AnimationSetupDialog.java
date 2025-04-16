// File: app/src/main/java/view/AnimationSetupDialog.java
package view;

import model.ColorScheme;
import model.FractalState;
import model.Keyframe;
import model.Viewport;
import services.AnimationService;
import viewmodel.FractalViewModel;
import math.FractalFunction;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.text.ParseException;
import java.util.concurrent.CancellationException; // Добавлен импорт

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
        keyframeListPanel = new KeyframeListPanel(
                keyframeListModel,
                this::addCurrentViewAsKeyframe,
                this::removeSelectedKeyframe,
                this::handleListSelectionChange
        );

        keyframePreviewPanel = new KeyframePreviewPanel(
                this::loadSelectedToPreview,
                this::updateSelectedKeyframeFromPreview // Переименован для ясности
        );

        keyframeParametersPanel = new KeyframeParametersPanel(
                this::applyFieldsToPreview,
                this::applyFieldsToSelectedKeyframe // <-- Передаем новый обработчик
        );

        animationSettingsPanel = new AnimationSettingsPanel();

        generationControlPanel = new GenerationControlPanel(
                this::startOrCancelAnimationGeneration
        );

        // --- Размещение панелей (без изменений) ---
        JPanel rightTopPanel = new JPanel(new BorderLayout(10, 10));
        rightTopPanel.add(keyframePreviewPanel, BorderLayout.CENTER);
        rightTopPanel.add(keyframeParametersPanel, BorderLayout.EAST);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(rightTopPanel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(animationSettingsPanel);
        rightPanel.add(Box.createVerticalGlue());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, keyframeListPanel, rightPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.35);

        setLayout(new BorderLayout(10, 10));
        add(splitPane, BorderLayout.CENTER);
        add(generationControlPanel, BorderLayout.SOUTH);

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    // --- Обработчики событий ---

    private void handleListSelectionChange(int selectedIndex) {
        boolean selected = (selectedIndex != -1);
        keyframePreviewPanel.setControlButtonsEnabled(selected);
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

    private void addCurrentViewAsKeyframe(ActionEvent e) {
        FractalState currentState = mainViewModel.getCurrentState();
        if (currentState != null) {
            Keyframe newKeyframe = new Keyframe(currentState);
            keyframeListModel.addElement(newKeyframe);
            int newIndex = keyframeListModel.getSize() - 1;
            keyframeListPanel.setSelectedIndex(newIndex);
            keyframeListPanel.ensureIndexIsVisible(newIndex);
        } else {
            showError("Не удалось получить текущее состояние фрактала из главного окна.");
        }
    }

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

    private void loadSelectedToPreview(ActionEvent e) {
        int selectedIndex = keyframeListPanel.getSelectedIndex();
        if (selectedIndex != -1) {
            try {
                Keyframe selectedKeyframe = keyframeListModel.getElementAt(selectedIndex);
                if (selectedKeyframe != null && selectedKeyframe.getState() != null) {
                    loadStateToPreviewAndFields(selectedKeyframe.getState());
                    generationControlPanel.setStatus("Состояние кадра '" + selectedKeyframe.getName() + "' загружено в предпросмотр.");
                } else {
                    showError("Выбранный кадр содержит некорректные данные.");
                    generationControlPanel.setStatus("Ошибка: Некорректный выбранный кадр.");
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                showError("Ошибка: Выбранный кадр больше не доступен.");
                keyframeListPanel.clearSelection(); // Сбрасываем выбор
            }
        } else {
            showWarning("Сначала выберите кадр для загрузки в предпросмотр.");
        }
    }

    /** Обновляет выбранный кадр состоянием из предпросмотра. */
    private void updateSelectedKeyframeFromPreview(ActionEvent e) {
        int selectedIndex = keyframeListPanel.getSelectedIndex();
        if (selectedIndex != -1) {
            try {
                FractalState previewState = keyframePreviewPanel.getCurrentState();
                if (previewState != null) {
                    Keyframe selectedKeyframe = keyframeListModel.getElementAt(selectedIndex); // Может кинуть IndexOutOfBounds
                    String existingName = selectedKeyframe.getName();
                    Keyframe updatedKeyframe = new Keyframe(previewState, existingName);
                    keyframeListModel.set(selectedIndex, updatedKeyframe);
                    keyframeParametersPanel.updateFields(previewState);
                    generationControlPanel.setStatus("Кадр '" + updatedKeyframe.getName() + "' обновлен из предпросмотра.");
                } else {
                    showError("Не удалось получить состояние из панели предпросмотра.");
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                showError("Ошибка: Выбранный кадр больше не доступен для обновления.");
                keyframeListPanel.clearSelection(); // Сбрасываем выбор
            }
        } else {
            showWarning("Сначала выберите кадр для обновления.");
        }
    }

    /** Применяет значения из полей параметров к предпросмотру. */
    private void applyFieldsToPreview(ActionEvent e) {
        try {
            Object[] params = keyframeParametersPanel.getValidatedParameters();
            Viewport newViewport = (Viewport) params[0];
            int newIterations = (Integer) params[1];

            FractalState currentStateInPreview = keyframePreviewPanel.getCurrentState();
            ColorScheme scheme = (currentStateInPreview != null)
                    ? currentStateInPreview.getColorScheme()
                    : FractalState.createDefault().getColorScheme();
            FractalFunction function = (currentStateInPreview != null)
                    ? currentStateInPreview.getFractalFunction()
                    : FractalState.createDefault().getFractalFunction();

            FractalState newState = new FractalState(newViewport, newIterations, scheme, function);
            keyframePreviewPanel.loadState(newState);
            generationControlPanel.setStatus("Параметры из полей применены к предпросмотру.");

        } catch (ParseException | IllegalArgumentException | NullPointerException ex) {
            showError("Ошибка ввода параметров: " + ex.getMessage());
        }
    }

    /** Применяет значения из полей параметров напрямую к выбранному ключевому кадру. */
    private void applyFieldsToSelectedKeyframe(ActionEvent e) {
        int selectedIndex = keyframeListPanel.getSelectedIndex();
        if (selectedIndex == -1) {
            showWarning("Сначала выберите кадр в списке, к которому нужно применить параметры.");
            return;
        }

        try {
            // 1. Получаем параметры из полей
            Object[] params = keyframeParametersPanel.getValidatedParameters();
            Viewport newViewport = (Viewport) params[0];
            int newIterations = (Integer) params[1];

            // 2. Получаем текущий выбранный кадр (может быть IndexOutOfBounds)
            Keyframe selectedKeyframe = keyframeListModel.getElementAt(selectedIndex);
            FractalState currentKeyState = selectedKeyframe.getState();

            // Проверка на null для безопасности
            if (currentKeyState == null || currentKeyState.getColorScheme() == null || currentKeyState.getFractalFunction() == null) {
                showError("Ошибка: Состояние выбранного кадра некорректно.");
                return;
            }

            // 3. Создаем новое состояние
            FractalState newStateForKeyframe = new FractalState(
                    newViewport,
                    newIterations,
                    currentKeyState.getColorScheme(), // Схема из кадра
                    currentKeyState.getFractalFunction() // Функция из кадра
            );

            // 4. Создаем новый объект Keyframe
            Keyframe updatedKeyframe = new Keyframe(newStateForKeyframe, selectedKeyframe.getName());

            // 5. Заменяем старый кадр на новый в модели списка
            keyframeListModel.set(selectedIndex, updatedKeyframe);

            // 6. Обновляем UI
            generationControlPanel.setStatus("Параметры применены к кадру '" + updatedKeyframe.getName() + "'.");
            // Поля параметров уже показывают введенные значения.
            // Предпросмотр не меняем, так как применили напрямую к кадру.

        } catch (ParseException | IllegalArgumentException | NullPointerException ex) {
            showError("Ошибка ввода параметров: " + ex.getMessage());
        } catch (ArrayIndexOutOfBoundsException ex) {
            // Это может произойти, если selectedIndex стал невалидным между проверкой и использованием
            showError("Ошибка: Выбранный кадр больше не доступен.");
            keyframeListPanel.clearSelection(); // <-- Исправлено: вызываем метод у KeyframeListPanel
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
                if (!isCancelled() && !chunks.isEmpty()) {
                    generationControlPanel.setStatus(chunks.get(chunks.size() - 1));
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
                        generationControlPanel.setProgress((int) (progress * 100));
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
            keyframePreviewPanel.setControlButtonsEnabled(selectedIndex != -1);
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
        keyframePreviewPanel.setControlButtonsEnabled(false);
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