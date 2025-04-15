// File: app/src/main/java/view/MenuBar.java
// ИЗМЕНЕНО: Импорты скорректированы
package view;

import model.ColorScheme; // core
import model.GrayscaleScheme; // core
import model.NonlinearRGBScheme; // core
import services.FileService; // core
import viewmodel.FractalViewModel; // core

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage; // Добавлен импорт
import java.io.File;
import java.io.IOException; // Добавлен импорт
import java.util.List;

/**
 * Creates the main menu bar for the application.
 */
public class MenuBar extends JMenuBar {

    private final FractalViewModel viewModel;
    private final FileService fileService;
    private final FractalPanel fractalPanel; // Нужен для получения текущего изображения
    private final JFrame ownerFrame; // Для диалоговых окон

    private JMenuItem undoMenuItem; // Для включения/выключения

    // Список доступных цветовых схем
    private final List<ColorScheme> availableColorSchemes = List.of(
            new NonlinearRGBScheme(),
            new GrayscaleScheme()
            // Добавьте сюда другие экземпляры ваших схем
    );

    /**
     * Constructs the MenuBar.
     *
     * @param viewModel    The application's ViewModel.
     * @param fileService  The service for file operations.
     * @param fractalPanel The panel displaying the fractal (needed for saving image).
     * @param ownerFrame   The main application frame (for dialogs).
     */
    public MenuBar(FractalViewModel viewModel, FileService fileService, FractalPanel fractalPanel, JFrame ownerFrame) {
        this.viewModel = viewModel;
        this.fileService = fileService;
        this.fractalPanel = fractalPanel;
        this.ownerFrame = ownerFrame;

        createFileMenu();
        createEditMenu();
        createViewMenu();
        // Добавьте другие меню (Help, etc.) по необходимости

        // Слушаем ViewModel для обновления состояния Undo
        viewModel.addPropertyChangeListener(evt -> {
            if (FractalViewModel.PROPERTY_CAN_UNDO.equals(evt.getPropertyName())) {
                updateUndoState((Boolean) evt.getNewValue());
            }
            // ИЗМЕНЕНО: Слушаем изменение состояния для обновления меню схем при загрузке
            else if (FractalViewModel.PROPERTY_STATE.equals(evt.getPropertyName())) {
                updateColorSchemeSelection();
            }
        });
        // Устанавливаем начальное состояние Undo
        updateUndoState(viewModel.getUndoManager().canUndo());
        updateColorSchemeSelection(); // Устанавливаем начальное состояние схем
    }

    private void createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        // --- Load ---
        JMenuItem loadItem = new JMenuItem("Load Fractal State...");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        loadItem.addActionListener(e -> loadFractal());
        fileMenu.add(loadItem);

        // --- Save As ---
        JMenu saveAsMenu = new JMenu("Save As...");

        JMenuItem saveFracItem = new JMenuItem("Fractal State (.frac)");
        saveFracItem.addActionListener(e -> saveFractal());
        saveAsMenu.add(saveFracItem);

        JMenuItem savePngItem = new JMenuItem("Image (.png)");
        savePngItem.addActionListener(e -> saveImage("PNG"));
        saveAsMenu.add(savePngItem);

        JMenuItem saveJpegItem = new JMenuItem("Image (.jpg)");
        saveJpegItem.addActionListener(e -> saveImage("JPEG"));
        saveAsMenu.add(saveJpegItem);

        fileMenu.add(saveAsMenu);
        fileMenu.addSeparator();

        // --- Exit ---
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK)); // Ctrl+Q для выхода
        exitItem.addActionListener(e -> {
            // ИЗМЕНЕНО: Инициируем закрытие окна штатным образом, чтобы сработал WindowListener
            ownerFrame.dispatchEvent(new java.awt.event.WindowEvent(ownerFrame, java.awt.event.WindowEvent.WINDOW_CLOSING));
            // System.exit(0); // Простой выход заменен
        });
        fileMenu.add(exitItem);

        add(fileMenu);
    }

    private void createEditMenu() {
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        // --- Undo ---
        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        undoMenuItem.addActionListener(e -> viewModel.undoLastAction());
        undoMenuItem.setEnabled(false); // Изначально недоступно
        editMenu.add(undoMenuItem);

        add(editMenu);
    }

    private void createViewMenu() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        // --- Color Scheme ---
        JMenu colorSchemeMenu = new JMenu("Color Scheme");
        ButtonGroup schemeGroup = new ButtonGroup(); // Чтобы только одна схема была выбрана

        // ИЗМЕНЕНО: Получаем схему при создании меню, но выделение обновится в updateColorSchemeSelection()
        // ColorScheme currentScheme = viewModel.getCurrentState().getColorScheme();

        for (ColorScheme scheme : availableColorSchemes) {
            JRadioButtonMenuItem schemeItem = new JRadioButtonMenuItem(scheme.getName());
            // schemeItem.setSelected(currentScheme.getClass().equals(scheme.getClass())); // Начальное выделение убрано, делается в update
            schemeItem.addActionListener(e -> {
                if (schemeItem.isSelected()) {
                    viewModel.changeColorScheme(scheme);
                }
            });
            schemeGroup.add(schemeItem);
            colorSchemeMenu.add(schemeItem);
        }
        viewMenu.add(colorSchemeMenu);

        // --- Max Iterations ---
        JMenuItem iterationsItem = new JMenuItem("Set Max Iterations...");
        iterationsItem.addActionListener(e -> changeMaxIterations());
        viewMenu.add(iterationsItem);

        add(viewMenu);
    }

    /**
     * Updates the enabled state of the Undo menu item.
     * @param canUndo true if undo is possible, false otherwise.
     */
    private void updateUndoState(boolean canUndo) {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(canUndo);
        }
    }

    // --- Action Handlers ---

    private void loadFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Fractal State");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fractal State Files (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(ownerFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // ИЗМЕНЕНО: loadState теперь не вызывает render, панель сама среагирует на PROPERTY_STATE
                viewModel.loadState(fileService.loadFractalState(selectedFile));
                // Обновление выбора цветовой схемы произойдет через слушатель PROPERTY_STATE
            } catch (Exception ex) {
                showErrorDialog("Load Error", "Error loading fractal state: " + ex.getMessage());
                ex.printStackTrace(); // Логирование для отладки
            }
        }
    }

    private void saveFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Fractal State As");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fractal State Files (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        // Предложить имя файла по умолчанию
        fileChooser.setSelectedFile(new File("mandelbrot_state.frac"));

        int result = fileChooser.showSaveDialog(ownerFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                fileService.saveFractalState(viewModel.getCurrentState(), selectedFile);
            } catch (IOException ex) {
                showErrorDialog("Save Error", "Error saving fractal state: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void saveImage(String format) {
        // Получаем текущее изображение из панели
        // ИЗМЕНЕНО: Используем геттер для получения изображения
        BufferedImage imageToSave = fractalPanel.getCurrentImage();

        if (imageToSave == null) {
            JOptionPane.showMessageDialog(ownerFrame,
                    "No fractal image available to save yet. Please wait for rendering to complete.",
                    "Save Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image As " + format.toUpperCase());
        String extension = format.toLowerCase().equals("jpeg") ? "jpg" : format.toLowerCase();
        String description = format.toUpperCase() + " Image Files (*." + extension + ")";
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setSelectedFile(new File("mandelbrot_image." + extension));

        int result = fileChooser.showSaveDialog(ownerFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Передаем актуальное состояние viewModel для координат в подписи
                fileService.saveImage(imageToSave, viewModel.getCurrentState(), selectedFile, format);
            } catch (Exception ex) {
                showErrorDialog("Save Error", "Error saving image: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void changeMaxIterations() {
        String currentIterationsStr = Integer.toString(viewModel.getCurrentState().getMaxIterations());
        // ИЗМЕНЕНО: Указываем ownerFrame для центрирования диалога
        String input = (String) JOptionPane.showInputDialog(
                ownerFrame, // Вот здесь
                "Enter maximum iterations:",
                "Set Max Iterations",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                currentIterationsStr
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                int newIterations = Integer.parseInt(input.trim());
                if (newIterations > 0) {
                    // ИЗМЕНЕНО: Метод ViewModel больше не вызывает render напрямую
                    viewModel.changeMaxIterations(newIterations);
                } else {
                    showErrorDialog("Invalid Input", "Iterations must be a positive integer.");
                }
            } catch (NumberFormatException ex) {
                showErrorDialog("Invalid Input", "Invalid number format.");
            }
        }
    }

    /**
     * Updates the radio button selection in the Color Scheme menu
     * based on the current ViewModel state.
     */
    private void updateColorSchemeSelection() {
        // ИЗМЕНЕНО: Полностью переписан для большей надежности
        if (viewModel == null || viewModel.getCurrentState() == null) return; // Защита

        ColorScheme currentScheme = viewModel.getCurrentState().getColorScheme();
        if (currentScheme == null) return; // Защита

        try {
            JMenu viewMenu = getMenu(2); // Меню View (индекс 2)
            if (viewMenu != null) {
                // Ищем подменю "Color Scheme" по тексту (более надежно чем по индексу)
                JMenu colorSchemeMenu = null;
                for (int i = 0; i < viewMenu.getItemCount(); i++) {
                    JMenuItem item = viewMenu.getItem(i);
                    if (item instanceof JMenu && "Color Scheme".equals(item.getText())) {
                        colorSchemeMenu = (JMenu) item;
                        break;
                    }
                }

                if (colorSchemeMenu != null) {
                    for (int i = 0; i < colorSchemeMenu.getItemCount(); i++) {
                        JMenuItem item = colorSchemeMenu.getItem(i);
                        if (item instanceof JRadioButtonMenuItem) {
                            JRadioButtonMenuItem radioItem = (JRadioButtonMenuItem) item;
                            // Ищем соответствующую схему в availableColorSchemes по имени пункта меню
                            String itemName = radioItem.getText();
                            boolean shouldBeSelected = availableColorSchemes.stream()
                                    .anyMatch(scheme -> scheme.getName().equals(itemName) &&
                                            scheme.getClass().equals(currentScheme.getClass()));
                            // Вызываем setSelected только если состояние нужно изменить,
                            // чтобы не вызывать лишний раз ActionListener
                            if (radioItem.isSelected() != shouldBeSelected) {
                                radioItem.setSelected(shouldBeSelected);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Логгирование ошибки обновления меню, но не прерываем работу приложения
            System.err.println("Error updating color scheme menu selection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper method to show error messages.
     * @param title The dialog title.
     * @param message The error message.
     */
    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(ownerFrame, message, title, JOptionPane.ERROR_MESSAGE);
    }
}