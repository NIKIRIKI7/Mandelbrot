// File: app/src/main/java/view/MenuBar.java
package view;

import model.ColorScheme;
import model.FractalState;
import model.GrayscaleScheme;
import model.NonlinearRGBScheme;
import services.FileService;
import viewmodel.FractalViewModel;
import services.AnimationService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CancellationException; // Для Animation

/**
 * Создает и управляет главным меню приложения для исследования фракталов.
 */
public class MenuBar extends JMenuBar {

    private final FractalViewModel viewModel;
    private final FileService fileService;
    private final FractalPanel fractalPanel;
    private final MainFrame ownerFrame; // Изменен тип на MainFrame для доступа к StatusBar
    private final AnimationService animationService;

    private JMenuItem undoMenuItem;
    private final List<ColorScheme> availableColorSchemes = List.of(
            new NonlinearRGBScheme(),
            new GrayscaleScheme()
    );

    /**
     * Конструирует MenuBar.
     * @param viewModel ViewModel приложения.
     * @param fileService Сервис для файловых операций.
     * @param fractalPanel Панель отрисовки фрактала.
     * @param ownerFrame Главное окно приложения {@link MainFrame}.
     */
    public MenuBar(FractalViewModel viewModel, FileService fileService, FractalPanel fractalPanel, MainFrame ownerFrame) { // <-- Тип ownerFrame изменен
        this.viewModel = viewModel;
        this.fileService = fileService;
        this.fractalPanel = fractalPanel;
        this.ownerFrame = ownerFrame;
        this.animationService = new AnimationService(); // Инициализация здесь

        createFileMenu();
        createEditMenu();
        createViewMenu();
        createAnimationMenu();

        // Добавление слушателя к ViewModel
        viewModel.addPropertyChangeListener(evt -> {
            if (FractalViewModel.PROPERTY_CAN_UNDO.equals(evt.getPropertyName())) {
                updateUndoState((Boolean) evt.getNewValue());
            } else if (FractalViewModel.PROPERTY_STATE.equals(evt.getPropertyName())) {
                updateColorSchemeSelection();
                // Обновляем статус после изменения состояния (например, после Undo)
                FractalState newState = (FractalState) evt.getNewValue();
                if (newState != null && ownerFrame.getStatusBar() != null && !fractalPanel.isRendering) { // Обновляем, если не идет рендер
                    ownerFrame.getStatusBar().setStatus(
                            String.format("Готово. Область: X=[%.4g, %.4g], Y=[%.4g, %.4g], Итер: %d",
                                    newState.getViewport().getMinX(), newState.getViewport().getMaxX(),
                                    newState.getViewport().getMinY(), newState.getViewport().getMaxY(),
                                    newState.getMaxIterations())
                    );
                }
            }
        });
        updateUndoState(viewModel.getUndoManager().canUndo());
        updateColorSchemeSelection();
    }

    private void createFileMenu() {
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem loadItem = new JMenuItem("Загрузить состояние...");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        loadItem.addActionListener(e -> loadFractal());
        fileMenu.add(loadItem);

        JMenuItem saveItem = new JMenuItem("Сохранить...");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        saveItem.addActionListener(e -> saveUsingFileChooser());
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        exitItem.addActionListener(e -> {
            ownerFrame.dispatchEvent(new java.awt.event.WindowEvent(ownerFrame, java.awt.event.WindowEvent.WINDOW_CLOSING));
        });
        fileMenu.add(exitItem);

        add(fileMenu);
    }

    private void saveUsingFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить файл");

        FileNameExtensionFilter fracFilter = new FileNameExtensionFilter("Файл состояния (*.frac)", "frac");
        FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("Изображение PNG (*.png)", "png");
        FileNameExtensionFilter jpgFilter = new FileNameExtensionFilter("Изображение JPEG (*.jpg)", "jpg");

        fileChooser.addChoosableFileFilter(fracFilter);
        fileChooser.addChoosableFileFilter(pngFilter);
        fileChooser.addChoosableFileFilter(jpgFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(fracFilter); // По умолчанию

        int result = fileChooser.showSaveDialog(ownerFrame);
        StatusBar statusBar = ownerFrame.getStatusBar();

        if (result != JFileChooser.APPROVE_OPTION) {
            statusBar.setStatus("Сохранение отменено.");
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        String extension = "frac";
        String format = null;

        javax.swing.filechooser.FileFilter selectedFilter = fileChooser.getFileFilter();
        if (selectedFilter == pngFilter) {
            extension = "png";
            format = "PNG";
        } else if (selectedFilter == jpgFilter) {
            extension = "jpg";
            format = "JPEG";
        }

        // Добавляем расширение, если его нет
        if (!selectedFile.getName().toLowerCase().endsWith("." + extension)) {
            selectedFile = new File(selectedFile.getAbsolutePath() + "." + extension);
        }

        if (format == null) {
            // Сохраняем состояние
            statusBar.setStatus("Сохранение состояния в файл " + selectedFile.getName() + "...");
            try {
                fileService.saveFractalState(viewModel.getCurrentState(), selectedFile);
                statusBar.setStatus("Состояние успешно сохранено в '" + selectedFile.getName() + "'.");
            } catch (IOException ex) {
                showErrorDialog("Ошибка сохранения", "Не удалось сохранить состояние: " + ex.getMessage());
                statusBar.setStatus("Ошибка: " + ex.getMessage());
            }
        } else {
            // Сохраняем изображение
            BufferedImage image = fractalPanel.getCurrentImage();
            if (image == null) {
                showErrorDialog("Ошибка сохранения", "Нет изображения для сохранения.");
                statusBar.setStatus("Ошибка: Изображение не готово.");
                return;
            }
            statusBar.setStatus("Сохранение изображения в файл " + selectedFile.getName() + "...");
            try {
                fileService.saveImage(image, viewModel.getCurrentState(), selectedFile, format);
                statusBar.setStatus("Изображение успешно сохранено в '" + selectedFile.getName() + "'.");
            } catch (Exception ex) {
                showErrorDialog("Ошибка сохранения", "Не удалось сохранить изображение: " + ex.getMessage());
                statusBar.setStatus("Ошибка: " + ex.getMessage());
            }
        }
    }


    private void createEditMenu() {
        JMenu editMenu = new JMenu("Правка"); // Локализация
        editMenu.setMnemonic(KeyEvent.VK_E);

        undoMenuItem = new JMenuItem("Отменить"); // Локализация
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        undoMenuItem.addActionListener(e -> viewModel.undoLastAction());
        undoMenuItem.setEnabled(false);
        editMenu.add(undoMenuItem);

        add(editMenu);
    }

    private void createViewMenu() {
        JMenu viewMenu = new JMenu("Вид"); // Локализация
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JMenu colorSchemeMenu = new JMenu("Цветовая схема");
        ButtonGroup schemeGroup = new ButtonGroup();

        for (ColorScheme scheme : availableColorSchemes) {
            JRadioButtonMenuItem schemeItem = new JRadioButtonMenuItem(scheme.getName());
            schemeItem.addActionListener(e -> {
                if (schemeItem.isSelected()) {
                    viewModel.changeColorScheme(scheme);
                    ownerFrame.getStatusBar().setStatus("Цветовая схема изменена на: " + scheme.getName());
                }
            });
            schemeGroup.add(schemeItem);
            colorSchemeMenu.add(schemeItem);
        }
        viewMenu.add(colorSchemeMenu);

        JMenuItem iterationsItem = new JMenuItem("Задать макс. итераций...");
        iterationsItem.addActionListener(e -> changeMaxIterations());
        viewMenu.add(iterationsItem);

        add(viewMenu);
    }

    private void createAnimationMenu() {
        JMenu animationMenu = new JMenu("Анимация");
        animationMenu.setMnemonic(KeyEvent.VK_A);

        JMenuItem setupAnimationItem = new JMenuItem("Редактор анимации...");
        setupAnimationItem.addActionListener(e -> openAnimationSetupDialog());
        animationMenu.add(setupAnimationItem);

        add(animationMenu);
    }

    private void updateUndoState(boolean canUndo) {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(canUndo);
        }
    }

    // --- Обработчики действий меню с интеграцией StatusBar ---

    private void loadFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Загрузить состояние фрактала");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Файлы состояния (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(ownerFrame);
        StatusBar statusBar = ownerFrame.getStatusBar(); // Получаем StatusBar

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            statusBar.setStatus("Загрузка состояния из файла " + selectedFile.getName() + "...");
            try {
                viewModel.loadState(fileService.loadFractalState(selectedFile));
                // Сообщение об успехе теперь в обработчике PROPERTY_STATE ViewModel
                statusBar.setStatus("Состояние '" + selectedFile.getName() + "' загружено.");
            } catch (Exception ex) {
                String errorMsg = "Ошибка загрузки состояния: " + ex.getMessage();
                showErrorDialog("Ошибка загрузки", errorMsg); // Диалог для файловых ошибок
                statusBar.setStatus("Ошибка: " + errorMsg); // Статус
                ex.printStackTrace();
            }
        } else {
            statusBar.setStatus("Загрузка состояния отменена.");
        }
    }

    private void saveFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить состояние фрактала как");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Файлы состояния (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setSelectedFile(new File("fractal_state.frac"));

        int result = fileChooser.showSaveDialog(ownerFrame);
        StatusBar statusBar = ownerFrame.getStatusBar();

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            statusBar.setStatus("Сохранение состояния в файл " + selectedFile.getName() + "...");
            try {
                fileService.saveFractalState(viewModel.getCurrentState(), selectedFile);
                statusBar.setStatus("Состояние успешно сохранено в '" + selectedFile.getName() + "'.");
            } catch (IOException ex) {
                String errorMsg = "Ошибка сохранения состояния: " + ex.getMessage();
                showErrorDialog("Ошибка сохранения", errorMsg); // Диалог для файловых ошибок
                statusBar.setStatus("Ошибка: " + errorMsg); // Статус
                ex.printStackTrace();
            }
        } else {
            statusBar.setStatus("Сохранение состояния отменено.");
        }
    }

    private void saveImage(String format) {
        BufferedImage imageToSave = fractalPanel.getCurrentImage();
        StatusBar statusBar = ownerFrame.getStatusBar();

        if (imageToSave == null) {
            String msg = "Нет готового изображения для сохранения. Подождите завершения рендеринга.";
            JOptionPane.showMessageDialog(ownerFrame, msg, "Ошибка сохранения", JOptionPane.WARNING_MESSAGE);
            statusBar.setStatus("Ошибка: " + msg);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        String extension = format.toLowerCase().equals("jpeg") ? "jpg" : format.toLowerCase();
        String description = format.toUpperCase() + " изображение (*." + extension + ")";
        fileChooser.setDialogTitle("Сохранить изображение как " + format.toUpperCase());
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setSelectedFile(new File("fractal_image." + extension));

        int result = fileChooser.showSaveDialog(ownerFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            statusBar.setStatus("Сохранение изображения в файл " + selectedFile.getName() + "...");
            try {
                fileService.saveImage(imageToSave, viewModel.getCurrentState(), selectedFile, format);
                statusBar.setStatus("Изображение успешно сохранено в '" + selectedFile.getName() + "'.");
            } catch (Exception ex) {
                String errorMsg = "Ошибка сохранения изображения: " + ex.getMessage();
                showErrorDialog("Ошибка сохранения", errorMsg); // Диалог для файловых ошибок
                statusBar.setStatus("Ошибка: " + errorMsg); // Статус
                ex.printStackTrace();
            }
        } else {
            statusBar.setStatus("Сохранение изображения отменено.");
        }
    }

    private void changeMaxIterations() {
        StatusBar statusBar = ownerFrame.getStatusBar();
        String currentIterationsStr = Integer.toString(viewModel.getCurrentState().getMaxIterations());
        String input = (String) JOptionPane.showInputDialog(
                ownerFrame,
                "Введите максимальное количество итераций:",
                "Задать макс. итераций",
                JOptionPane.PLAIN_MESSAGE, null, null, currentIterationsStr
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                int newIterations = Integer.parseInt(input.trim());
                if (newIterations > 0) {
                    viewModel.changeMaxIterations(newIterations);
                    // Сообщение об успехе будет в обработчике PROPERTY_STATE
                    // statusBar.setStatus("Максимальное количество итераций изменено на: " + newIterations);
                } else {
                    String errorMsg = "Итерации должны быть положительным числом.";
                    showErrorDialog("Некорректный ввод", errorMsg);
                    statusBar.setStatus("Ошибка: " + errorMsg);
                }
            } catch (NumberFormatException ex) {
                String errorMsg = "Некорректный формат числа.";
                showErrorDialog("Некорректный ввод", errorMsg);
                statusBar.setStatus("Ошибка: " + errorMsg);
            }
        } else if (input != null) { // Ввод был, но пустой
            statusBar.setStatus("Ввод максимального количества итераций отменен (пустое значение).");
        } else { // Нажат Cancel
            statusBar.setStatus("Ввод максимального количества итераций отменен.");
        }
    }

    private void updateColorSchemeSelection() {
        if (viewModel == null || viewModel.getCurrentState() == null) return;
        ColorScheme currentScheme = viewModel.getCurrentState().getColorScheme();
        if (currentScheme == null) return;

        try {
            // Индексы могут меняться, ищем по имени
            JMenu viewMenu = null;
            for(int i=0; i < getMenuCount(); ++i){
                if("Вид".equals(getMenu(i).getText())) { // Ищем по локализованному имени
                    viewMenu = getMenu(i);
                    break;
                }
            }

            if (viewMenu != null) {
                JMenu colorSchemeMenu = null;
                for (int i = 0; i < viewMenu.getItemCount(); i++) {
                    JMenuItem item = viewMenu.getItem(i);
                    if (item instanceof JMenu && "Цветовая схема".equals(item.getText())) { // Локализованное имя
                        colorSchemeMenu = (JMenu) item;
                        break;
                    }
                }

                if (colorSchemeMenu != null) {
                    for (int i = 0; i < colorSchemeMenu.getItemCount(); i++) {
                        JMenuItem item = colorSchemeMenu.getItem(i);
                        if (item instanceof JRadioButtonMenuItem) {
                            JRadioButtonMenuItem radioItem = (JRadioButtonMenuItem) item;
                            String itemName = radioItem.getText();
                            // Сверяем имя из меню с именами доступных схем и с классом текущей схемы
                            boolean shouldBeSelected = availableColorSchemes.stream()
                                    .anyMatch(scheme -> scheme.getName().equals(itemName) &&
                                            scheme.getClass().equals(currentScheme.getClass()));

                            if (radioItem.isSelected() != shouldBeSelected) {
                                radioItem.setSelected(shouldBeSelected);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обновления выбора цветовой схемы в меню: " + e.getMessage());
            e.printStackTrace(); // Продолжаем работу
        }
    }


    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(ownerFrame, message, title, JOptionPane.ERROR_MESSAGE);
    }


    private void openAnimationSetupDialog() {
        // Проверяем, не идет ли основной рендеринг
        if (fractalPanel.isRendering) {
            ownerFrame.getStatusBar().setStatus("Дождитесь завершения рендеринга перед открытием редактора анимации.");
            JOptionPane.showMessageDialog(ownerFrame,
                    "Пожалуйста, дождитесь завершения текущего рендеринга фрактала.",
                    "Рендеринг активен", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Создаем и отображаем диалог
        AnimationSetupDialog dialog = new AnimationSetupDialog(ownerFrame, viewModel, animationService);
        dialog.display();
        // StatusBar главного окна обновляем после закрытия диалога (если нужно)
        // ownerFrame.getStatusBar().setStatus("Редактор анимации закрыт.");
    }
}