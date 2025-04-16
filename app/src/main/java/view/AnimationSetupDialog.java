// File: app/src/main/java/view/AnimationSetupDialog.java
package view;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.text.ParseException;

/**
 * Диалоговое окно для настройки параметров анимации фрактала.
 * Выступает координатором для дочерних панелей:
 * {@link KeyframeListPanel}, {@link KeyframePreviewPanel},
 * {@link KeyframeParametersPanel}, {@link AnimationSettingsPanel}, {@link GenerationControlPanel}.
 */
public class AnimationSetupDialog extends JDialog {

    private final FractalViewModel mainViewModel; // Для получения начального вида
    private final AnimationService animationService;

    // Модель данных списка (общая для списка и диалога)
    private final DefaultListModel<Keyframe> keyframeListModel;

    // Дочерние панели UI
    private KeyframeListPanel keyframeListPanel;
    private KeyframePreviewPanel keyframePreviewPanel;
    private KeyframeParametersPanel keyframeParametersPanel;
    private AnimationSettingsPanel animationSettingsPanel;
    private GenerationControlPanel generationControlPanel;

    // Фоновая задача для генерации
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
        setupInteractionLogic(); // Этот метод теперь пуст, логика в обработчиках

        pack();
        setMinimumSize(new Dimension(850, 650));
        setLocationRelativeTo(ownerFrame);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Ручная обработка закрытия

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleClose();
            }
        });
    }

    /**
     * Создает и размещает дочерние панели UI.
     */
    private void createAndLayoutPanels() {
        // 1. Создаем панели, передавая ссылки на методы-обработчики этого диалога
        keyframeListPanel = new KeyframeListPanel(
                keyframeListModel,
                this::addCurrentViewAsKeyframe,
                this::removeSelectedKeyframe,
                this::handleListSelectionChange // Передаем метод как Consumer<Integer>
        );

        keyframePreviewPanel = new KeyframePreviewPanel(
                this::loadSelectedToPreview,
                this::updateSelectedKeyframe
        );

        keyframeParametersPanel = new KeyframeParametersPanel(
                this::applyFieldsToPreview
        );

        animationSettingsPanel = new AnimationSettingsPanel();

        generationControlPanel = new GenerationControlPanel(
                this::startOrCancelAnimationGeneration
        );

        // 2. Размещаем панели в диалоге
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
        splitPane.setDividerLocation(280);
        splitPane.setResizeWeight(0.3);

        setLayout(new BorderLayout(10, 10));
        add(splitPane, BorderLayout.CENTER);
        add(generationControlPanel, BorderLayout.SOUTH);

        Container contentPane = getContentPane();
        if (contentPane instanceof JPanel) {
            ((JPanel) contentPane).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }
    }

    /**
     * Настраивает логику взаимодействия между панелями.
     * (В данной реализации основная логика встроена в обработчики событий).
     */
    private void setupInteractionLogic() {
        // Слушатели кнопок уже настроены при создании панелей.
        // Логика обновления UI при выборе элемента списка находится в handleListSelectionChange.
    }


    // --- Методы-обработчики событий от дочерних панелей ---

    /** Вызывается при изменении выбора в KeyframeListPanel. */
    private void handleListSelectionChange(int selectedIndex) {
        boolean selected = (selectedIndex != -1);
        keyframePreviewPanel.setControlButtonsEnabled(selected);
        keyframeParametersPanel.setFieldsEnabled(selected);
        // Кнопка "Удалить" обновляется внутри KeyframeListPanel

        if (selected) {
            Keyframe selectedKeyframe = keyframeListModel.getElementAt(selectedIndex);
            loadStateToPreviewAndFields(selectedKeyframe.getState()); // Обновляем предпросмотр и поля
            generationControlPanel.setStatus("Выбран кадр: " + selectedKeyframe.getName());
        } else {
            keyframePreviewPanel.loadState(null); // Очищаем предпросмотр
            keyframeParametersPanel.clearFields(); // Очищаем поля
            generationControlPanel.setStatus("Выберите кадр или добавьте новый.");
        }
    }

    /** Загружает состояние в предпросмотр и поля параметров. */
    private void loadStateToPreviewAndFields(FractalState state) {
        keyframePreviewPanel.loadState(state);
        keyframeParametersPanel.updateFields(state);
    }

    /** Добавляет текущий вид из главного окна как кадр. */
    private void addCurrentViewAsKeyframe(ActionEvent e) {
        FractalState currentState = mainViewModel.getCurrentState();
        if (currentState != null) {
            Keyframe newKeyframe = new Keyframe(currentState);
            keyframeListModel.addElement(newKeyframe);
            int newIndex = keyframeListModel.getSize() - 1;
            keyframeListPanel.setSelectedIndex(newIndex); // Выделит и вызовет handleListSelectionChange
            keyframeListPanel.ensureIndexIsVisible(newIndex);
            // Статус обновится в handleListSelectionChange
        } else {
            showError("Не удалось получить текущее состояние фрактала.");
        }
    }

    /** Удаляет выбранный кадр. */
    private void removeSelectedKeyframe(ActionEvent e) {
        int selectedIndex = keyframeListPanel.getSelectedIndex();
        if (selectedIndex != -1) {
            Keyframe removed = keyframeListModel.remove(selectedIndex);
            generationControlPanel.setStatus("Удален кадр: " + removed.getName());
            // handleListSelectionChange будет вызван автоматически после удаления
        } else {
            showWarning("Сначала выберите кадр для удаления.");
        }
    }

    /** Загружает выбранный кадр в предпросмотр. */
    private void loadSelectedToPreview(ActionEvent e) {
        int selectedIndex = keyframeListPanel.getSelectedIndex();
        if (selectedIndex != -1) {
            Keyframe selectedKeyframe = keyframeListModel.getElementAt(selectedIndex);
            loadStateToPreviewAndFields(selectedKeyframe.getState());
            generationControlPanel.setStatus("Состояние кадра '" + selectedKeyframe.getName() + "' загружено в предпросмотр.");
        } else {
            showWarning("Сначала выберите кадр для загрузки в предпросмотр.");
        }
    }

    /** Обновляет выбранный кадр состоянием из предпросмотра. */
    private void updateSelectedKeyframe(ActionEvent e) {
        int selectedIndex = keyframeListPanel.getSelectedIndex();
        if (selectedIndex != -1) {
            FractalState previewState = keyframePreviewPanel.getCurrentState();
            if (previewState != null) {
                String existingName = keyframeListModel.getElementAt(selectedIndex).getName();
                Keyframe updatedKeyframe = new Keyframe(previewState, existingName);
                keyframeListModel.set(selectedIndex, updatedKeyframe); // Заменяем
                keyframeParametersPanel.updateFields(previewState); // Обновляем поля согласно предпросмотру
                generationControlPanel.setStatus("Кадр '" + updatedKeyframe.getName() + "' обновлен.");
            } else {
                showError("Не удалось получить состояние из панели предпросмотра.");
            }
        } else {
            showWarning("Сначала выберите кадр для обновления.");
        }
    }

    /** Применяет значения из полей параметров к предпросмотру. */
    private void applyFieldsToPreview(ActionEvent e) {
        try {
            Object[] params = keyframeParametersPanel.getValidatedParameters();
            // params[0] - Viewport, params[1] - Integer iterations
            Viewport newViewport = (Viewport) params[0];
            int newIterations = (Integer) params[1];

            FractalState currentStateInPreview = keyframePreviewPanel.getCurrentState();
            // Если в предпросмотре ничего нет, берем дефолтные схему и функцию
            ColorScheme scheme = (currentStateInPreview != null) ? currentStateInPreview.getColorScheme() : FractalState.createDefault().getColorScheme();
            math.FractalFunction function = (currentStateInPreview != null) ? currentStateInPreview.getFractalFunction() : FractalState.createDefault().getFractalFunction();

            FractalState newState = new FractalState(newViewport, newIterations, scheme, function);
            keyframePreviewPanel.loadState(newState); // Загружаем в предпросмотр
            generationControlPanel.setStatus("Параметры из полей применены к предпросмотру.");

        } catch (ParseException | IllegalArgumentException | NullPointerException ex) {
            showError("Ошибка ввода параметров: " + ex.getMessage());
        }
    }

    /** Запускает или отменяет генерацию анимации. */
    private void startOrCancelAnimationGeneration(ActionEvent e) {
        if (animationWorker != null && !animationWorker.isDone()) {
            animationWorker.cancel(true);
            generationControlPanel.setStatus("Запрос на отмену генерации...");
            generationControlPanel.setGenerateButtonEnabled(false); // Блокируем на время отмены
            return;
        }

        if (keyframeListModel.getSize() < 2) {
            showError("Нужно как минимум два ключевых кадра для анимации.");
            return;
        }

        final List<Keyframe> keyframes = new ArrayList<>(keyframeListModel.getSize());
        for (int i = 0; i < keyframeListModel.getSize(); i++) keyframes.add(keyframeListModel.getElementAt(i));
        final double durationPerSegment = animationSettingsPanel.getDurationPerSegment();
        final int fps = animationSettingsPanel.getFps();

        JFileChooser fileChooser = new JFileChooser();
        // ... (настройка fileChooser, проверка перезаписи - без изменений) ...
        fileChooser.setDialogTitle("Сохранить видео анимации как...");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP4 Video File (*.mp4)", "mp4"));
        fileChooser.setSelectedFile(new File("fractal_animation.mp4"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            generationControlPanel.setStatus("Сохранение отменено.");
            return;
        }
        final File outputFile = ensureMp4Extension(fileChooser.getSelectedFile());

        if (outputFile.exists()) {
            int overwriteChoice = JOptionPane.showConfirmDialog(this,
                    "Файл '" + outputFile.getName() + "' уже существует.\nПерезаписать его?",
                    "Подтверждение перезаписи", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwriteChoice != JOptionPane.YES_OPTION) {
                generationControlPanel.setStatus("Сохранение отменено.");
                return;
            }
        }


        generationControlPanel.setProgress(0);
        generationControlPanel.setProgressVisible(true);
        generationControlPanel.setStatus("Подготовка к генерации...");
        generationControlPanel.setGenerateButtonText("Отмена");
        generationControlPanel.setGenerateButtonEnabled(true); // Кнопка "Отмена" активна

        // Блокируем UI на время генерации
        setUIEnabled(false);

        animationWorker = new SwingWorker<Void, String>() {
            private long startTime;
            @Override protected Void doInBackground() throws Exception { /* ... код без изменений ... */
                startTime = System.currentTimeMillis();
                animationService.createAndSaveAnimation(
                        keyframes, fps, durationPerSegment, outputFile,
                        progress -> publishProgress(progress), // Используем отдельный метод
                        message -> publish(message)
                );
                return null;
            }
            @Override protected void process(List<String> chunks) { /* ... код без изменений ... */
                if (!isCancelled() && !chunks.isEmpty()) {
                    generationControlPanel.setStatus(chunks.get(chunks.size() - 1));
                }
            }
            @Override protected void done() { /* ... код обработки done() с небольшими изменениями ... */
                try {
                    if (!isCancelled()) {
                        get();
                        long endTime = System.currentTimeMillis();
                        generationControlPanel.setStatus(String.format("Генерация завершена успешно! (%.1f сек)", (endTime - startTime) / 1000.0));
                        showInfo("Анимация успешно сохранена в файл:\n" + outputFile.getAbsolutePath(), "Генерация завершена");
                        generationControlPanel.setProgress(100);
                    } else {
                        generationControlPanel.setStatus("Генерация отменена пользователем.");
                        generationControlPanel.setProgress(0);
                        deleteOutputFileIfExists(outputFile);
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
                } catch (java.util.concurrent.CancellationException e) {
                    generationControlPanel.setStatus("Генерация отменена.");
                    generationControlPanel.setProgress(0);
                    deleteOutputFileIfExists(outputFile);
                } finally {
                    generationControlPanel.setGenerateButtonText("Генерировать видео...");
                    generationControlPanel.setGenerateButtonEnabled(true); // Всегда разблокируем
                    if (!generationControlPanel.getStatus().contains("успешно")) {
                        generationControlPanel.setProgressVisible(false);
                    }
                    setUIEnabled(true); // Разблокируем UI
                    animationWorker = null;
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
        keyframeListPanel.setEnabled(enabled); // Блокируем всю панель списка
        keyframePreviewPanel.setEnabled(enabled); // Блокируем панель предпросмотра
        keyframeParametersPanel.setEnabled(enabled); // Блокируем панель параметров
        animationSettingsPanel.setEnabled(enabled); // Блокируем панель настроек
        // Кнопка генерации управляется отдельно
    }


    /** Вспомогательный метод для удаления файла. */
    private void deleteOutputFileIfExists(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) { System.err.println("Не удалось удалить файл: " + file.getAbsolutePath()); }
            else { System.out.println("Файл удален: " + file.getAbsolutePath()); }
        }
    }

    /** Гарантирует расширение .mp4. */
    private File ensureMp4Extension(File file) {
        // ... (код без изменений) ...
        String path = file.getAbsolutePath();
        String lowerPath = path.toLowerCase();
        if (!lowerPath.endsWith(".mp4")) {
            int dotIndex = path.lastIndexOf('.');
            int slashIndex = path.lastIndexOf(File.separatorChar);
            if (dotIndex > slashIndex) path = path.substring(0, dotIndex);
            return new File(path + ".mp4");
        }
        return file;
    }

    /** Показывает диалоговое окно. */
    public void display() {
        // Сброс UI к начальному состоянию
        generationControlPanel.setStatus("Выберите кадр или добавьте новый.");
        generationControlPanel.setProgressVisible(false);
        generationControlPanel.setProgress(0);
        generationControlPanel.setGenerateButtonText("Генерировать видео...");
        generationControlPanel.setGenerateButtonEnabled(true);
        keyframePreviewPanel.setControlButtonsEnabled(false);
        keyframeParametersPanel.setFieldsEnabled(false);
        keyframeParametersPanel.clearFields();
        keyframeListModel.clear(); // Очищаем список кадров при каждом открытии
        keyframePreviewPanel.loadState(null); // Очищаем предпросмотр
        setUIEnabled(true); // Убедимся, что UI разблокирован
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
        keyframePreviewPanel.shutdownRenderer(); // Останавливаем рендерер предпросмотра
        dispose(); // Закрываем диалог
    }

    // Вспомогательные методы для сообщений
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE); }
    private void showWarning(String message) { JOptionPane.showMessageDialog(this, message, "Внимание", JOptionPane.WARNING_MESSAGE); }
    private void showInfo(String message, String title) { JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE); }
}