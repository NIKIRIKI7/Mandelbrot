// File: app/src/main/java/view/MenuBar.java
package view;

import model.ColorScheme;
import model.GrayscaleScheme;
import model.NonlinearRGBScheme;
import services.FileService;
import viewmodel.FractalViewModel;
import services.AnimationService; // <-- Добавить импорт


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Создает и управляет главным меню приложения для исследования фракталов.
 * Предоставляет доступ к функциям загрузки/сохранения состояния, сохранения изображений,
 * отмены действий, выбора цветовой схемы и настройки итераций.
 */
public class MenuBar extends JMenuBar {

    /** ViewModel для доступа к состоянию фрактала и выполнения действий. */
    private final FractalViewModel viewModel;
    /** Сервис для операций сохранения и загрузки файлов. */
    private final FileService fileService;
    /** Панель, отображающая фрактал (необходима для получения текущего изображения при сохранении). */
    private final FractalPanel fractalPanel;
    /** Родительское окно JFrame (используется для отображения диалоговых окон). */
    private final JFrame ownerFrame;

    private final AnimationService animationService;


    /** Пункт меню "Undo", который включается/выключается в зависимости от доступности отмены. */
    private JMenuItem undoMenuItem;
    /** Список доступных цветовых схем для выбора пользователем. */
    private final List<ColorScheme> availableColorSchemes = List.of(
            new NonlinearRGBScheme(),
            new GrayscaleScheme()
            // При необходимости добавьте сюда другие экземпляры ваших схем
    );

    /**
     * Конструирует MenuBar и инициализирует все его элементы.
     *
     * @param viewModel    ViewModel приложения. Не может быть null.
     * @param fileService  Сервис для файловых операций. Не может быть null.
     * @param fractalPanel Панель отрисовки фрактала (для сохранения изображения). Не может быть null.
     * @param ownerFrame   Главное окно приложения (для диалоговых окон). Не может быть null.
     */
    public MenuBar(FractalViewModel viewModel, FileService fileService, FractalPanel fractalPanel, JFrame ownerFrame) {
        this.viewModel = viewModel;
        this.fileService = fileService;
        this.fractalPanel = fractalPanel;
        this.ownerFrame = ownerFrame;
        this.animationService = new AnimationService();

        // Создание основных меню
        createFileMenu();
        createEditMenu();
        createViewMenu();
        createAnimationMenu(); // <-- Вызвать создание нового меню

        // Добавьте другие меню (Help, etc.) по необходимости

        // Добавление слушателя к ViewModel для отслеживания изменений
        viewModel.addPropertyChangeListener(evt -> {
            // Обновление доступности пункта "Undo"
            if (FractalViewModel.PROPERTY_CAN_UNDO.equals(evt.getPropertyName())) {
                updateUndoState((Boolean) evt.getNewValue());
            }
            // Обновление выбора цветовой схемы при изменении состояния (например, после загрузки)
            else if (FractalViewModel.PROPERTY_STATE.equals(evt.getPropertyName())) {
                updateColorSchemeSelection();
            }
        });
        // Установка начального состояния доступности "Undo"
        updateUndoState(viewModel.getUndoManager().canUndo());
        // Установка начального выбора в меню цветовых схем
        updateColorSchemeSelection();
    }

    /**
     * Создает меню "File" со стандартными опциями: Load, Save As, Exit.
     */
    private void createFileMenu() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F); // Горячая клавиша Alt+F

        // --- Пункт "Load Fractal State..." ---
        JMenuItem loadItem = new JMenuItem("Load Fractal State...");
        loadItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK)); // Ctrl+O
        loadItem.addActionListener(e -> loadFractal());
        fileMenu.add(loadItem);

        // --- Подменю "Save As..." ---
        JMenu saveAsMenu = new JMenu("Save As...");

        // Сохранить состояние (.frac)
        JMenuItem saveFracItem = new JMenuItem("Fractal State (.frac)");
        saveFracItem.addActionListener(e -> saveFractal());
        saveAsMenu.add(saveFracItem);

        // Сохранить изображение (.png)
        JMenuItem savePngItem = new JMenuItem("Image (.png)");
        savePngItem.addActionListener(e -> saveImage("PNG"));
        saveAsMenu.add(savePngItem);

        // Сохранить изображение (.jpg)
        JMenuItem saveJpegItem = new JMenuItem("Image (.jpg)");
        saveJpegItem.addActionListener(e -> saveImage("JPEG"));
        saveAsMenu.add(saveJpegItem);

        fileMenu.add(saveAsMenu);
        fileMenu.addSeparator(); // Разделитель

        // --- Пункт "Exit" ---
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic(KeyEvent.VK_X); // Горячая клавиша X (внутри меню File)
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK)); // Ctrl+Q для выхода
        exitItem.addActionListener(e -> {
            // Инициируем стандартное событие закрытия окна, чтобы сработал WindowListener в MainFrame
            ownerFrame.dispatchEvent(new java.awt.event.WindowEvent(ownerFrame, java.awt.event.WindowEvent.WINDOW_CLOSING));
        });
        fileMenu.add(exitItem);

        add(fileMenu); // Добавляем меню "File" в строку меню
    }

    /**
     * Создает меню "Edit" с опцией "Undo".
     */
    private void createEditMenu() {
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E); // Alt+E

        // --- Пункт "Undo" ---
        undoMenuItem = new JMenuItem("Undo");
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK)); // Ctrl+Z
        undoMenuItem.addActionListener(e -> viewModel.undoLastAction());
        undoMenuItem.setEnabled(false); // Изначально отмена недоступна
        editMenu.add(undoMenuItem);

        add(editMenu); // Добавляем меню "Edit" в строку меню
    }

    /**
     * Создает меню "View" с опциями выбора цветовой схемы и установки максимального числа итераций.
     */
    private void createViewMenu() {
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V); // Alt+V

        // --- Подменю "Color Scheme" ---
        JMenu colorSchemeMenu = new JMenu("Color Scheme");
        ButtonGroup schemeGroup = new ButtonGroup(); // Гарантирует выбор только одной опции

        // Динамическое создание пунктов меню для каждой доступной цветовой схемы
        for (ColorScheme scheme : availableColorSchemes) {
            JRadioButtonMenuItem schemeItem = new JRadioButtonMenuItem(scheme.getName());
            // Начальное выделение устанавливается в updateColorSchemeSelection()
            schemeItem.addActionListener(e -> {
                // Если пункт выбран пользователем
                if (schemeItem.isSelected()) {
                    // Устанавливаем выбранную схему через ViewModel
                    viewModel.changeColorScheme(scheme);
                }
            });
            schemeGroup.add(schemeItem); // Добавляем в группу для эксклюзивного выбора
            colorSchemeMenu.add(schemeItem);
        }
        viewMenu.add(colorSchemeMenu);

        // --- Пункт "Set Max Iterations..." ---
        JMenuItem iterationsItem = new JMenuItem("Set Max Iterations...");
        iterationsItem.addActionListener(e -> changeMaxIterations());
        viewMenu.add(iterationsItem);

        add(viewMenu); // Добавляем меню "View" в строку меню
    }

    /**
     * Обновляет состояние (включен/выключен) пункта меню "Undo".
     * Вызывается при изменении свойства {@code FractalViewModel.PROPERTY_CAN_UNDO}.
     *
     * @param canUndo true, если операция отмены доступна, иначе false.
     */
    private void updateUndoState(boolean canUndo) {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(canUndo);
        }
    }

    // --- Обработчики действий меню ---

    /**
     * Обрабатывает действие "Load Fractal State...".
     * Открывает диалоговое окно выбора файла, загружает состояние фрактала (.frac)
     * с помощью {@link FileService} и обновляет {@link FractalViewModel}.
     */
    private void loadFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Fractal State");
        // Фильтр для отображения только .frac файлов
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fractal State Files (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false); // Не показывать "All Files"

        int result = fileChooser.showOpenDialog(ownerFrame); // Показываем диалог
        if (result == JFileChooser.APPROVE_OPTION) { // Если пользователь выбрал файл
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Загружаем состояние через ViewModel. ViewModel уведомит панель о необходимости перерисовки.
                viewModel.loadState(fileService.loadFractalState(selectedFile));
                // Обновление выбора цветовой схемы произойдет автоматически через слушатель свойства PROPERTY_STATE.
            } catch (Exception ex) {
                showErrorDialog("Load Error", "Error loading fractal state: " + ex.getMessage());
                ex.printStackTrace(); // Логирование ошибки для отладки
            }
        }
    }

    /**
     * Обрабатывает действие "Save Fractal State (.frac)".
     * Открывает диалоговое окно сохранения файла, получает текущее состояние из {@link FractalViewModel}
     * и сохраняет его в файл .frac с помощью {@link FileService}.
     */
    private void saveFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Fractal State As");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Fractal State Files (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        // Предлагаем имя файла по умолчанию
        fileChooser.setSelectedFile(new File("mandelbrot_state.frac"));

        int result = fileChooser.showSaveDialog(ownerFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Сохраняем текущее состояние ViewModel в выбранный файл
                fileService.saveFractalState(viewModel.getCurrentState(), selectedFile);
            } catch (IOException ex) {
                showErrorDialog("Save Error", "Error saving fractal state: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Обрабатывает действие "Save Image (.png)" или "Save Image (.jpg)".
     * Получает текущее отрисованное изображение из {@link FractalPanel},
     * открывает диалоговое окно сохранения файла и сохраняет изображение
     * в выбранном формате (PNG или JPEG) с помощью {@link FileService}.
     * Добавляет подпись с координатами на изображение.
     *
     * @param format Строка формата ("PNG" или "JPEG", регистронезависимо).
     */
    private void saveImage(String format) {
        // Получаем текущее отрисованное изображение из панели
        BufferedImage imageToSave = fractalPanel.getCurrentImage();

        // Проверяем, доступно ли изображение для сохранения
        if (imageToSave == null) {
            JOptionPane.showMessageDialog(ownerFrame,
                    "No fractal image available to save yet. Please wait for rendering to complete.",
                    "Save Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Image As " + format.toUpperCase());
        // Определяем расширение и описание файла для фильтра
        String extension = format.toLowerCase().equals("jpeg") ? "jpg" : format.toLowerCase();
        String description = format.toUpperCase() + " Image Files (*." + extension + ")";
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        fileChooser.setAcceptAllFileFilterUsed(false);
        // Предлагаем имя файла по умолчанию
        fileChooser.setSelectedFile(new File("mandelbrot_image." + extension));

        int result = fileChooser.showSaveDialog(ownerFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Передаем изображение, актуальное состояние ViewModel (для координат в подписи) и формат
                fileService.saveImage(imageToSave, viewModel.getCurrentState(), selectedFile, format);
            } catch (Exception ex) {
                showErrorDialog("Save Error", "Error saving image: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Обрабатывает действие "Set Max Iterations...".
     * Показывает диалоговое окно для ввода нового значения максимального числа итераций.
     * Если введено корректное положительное число, обновляет значение через {@link FractalViewModel}.
     */
    private void changeMaxIterations() {
        // Получаем текущее значение для отображения в диалоге
        String currentIterationsStr = Integer.toString(viewModel.getCurrentState().getMaxIterations());
        // Отображаем диалог ввода
        String input = (String) JOptionPane.showInputDialog(
                ownerFrame, // Центрировать относительно главного окна
                "Enter maximum iterations:",
                "Set Max Iterations",
                JOptionPane.PLAIN_MESSAGE,
                null, // Иконка по умолчанию
                null, // Нет списка для выбора
                currentIterationsStr // Начальное значение в поле ввода
        );

        // Если пользователь ввел значение и не нажал "Cancel"
        if (input != null && !input.trim().isEmpty()) {
            try {
                int newIterations = Integer.parseInt(input.trim());
                // Проверяем, что значение положительное
                if (newIterations > 0) {
                    // Обновляем значение через ViewModel
                    viewModel.changeMaxIterations(newIterations);
                } else {
                    showErrorDialog("Invalid Input", "Iterations must be a positive integer.");
                }
            } catch (NumberFormatException ex) {
                // Обработка ошибки, если введено не число
                showErrorDialog("Invalid Input", "Invalid number format.");
            }
        }
    }

    /**
     * Обновляет выбор радио-кнопки в меню "Color Scheme"
     * в соответствии с текущей цветовой схемой, установленной в {@link FractalViewModel}.
     * Вызывается при инициализации и при изменении состояния ({@code FractalViewModel.PROPERTY_STATE}).
     */
    private void updateColorSchemeSelection() {
        // Проверка на случай, если метод вызван до полной инициализации
        if (viewModel == null || viewModel.getCurrentState() == null) return;

        ColorScheme currentScheme = viewModel.getCurrentState().getColorScheme();
        if (currentScheme == null) return; // Дополнительная защита

        try {
            // Получаем меню "View" (предполагая, что оно третье по счету, индекс 2)
            JMenu viewMenu = getMenu(2);
            if (viewMenu != null) {
                // Ищем подменю "Color Scheme" по тексту, а не по индексу (более надежно)
                JMenu colorSchemeMenu = null;
                for (int i = 0; i < viewMenu.getItemCount(); i++) {
                    JMenuItem item = viewMenu.getItem(i);
                    // Проверяем, что это меню и текст совпадает
                    if (item instanceof JMenu && "Color Scheme".equals(item.getText())) {
                        colorSchemeMenu = (JMenu) item;
                        break;
                    }
                }

                if (colorSchemeMenu != null) {
                    // Проходим по всем пунктам подменю цветовых схем
                    for (int i = 0; i < colorSchemeMenu.getItemCount(); i++) {
                        JMenuItem item = colorSchemeMenu.getItem(i);
                        // Убеждаемся, что это радио-кнопка
                        if (item instanceof JRadioButtonMenuItem) {
                            JRadioButtonMenuItem radioItem = (JRadioButtonMenuItem) item;
                            String itemName = radioItem.getText(); // Имя схемы из пункта меню

                            // Проверяем, соответствует ли имя пункта меню и класс текущей активной схеме
                            boolean shouldBeSelected = availableColorSchemes.stream()
                                    .anyMatch(scheme -> scheme.getName().equals(itemName) &&
                                            scheme.getClass().equals(currentScheme.getClass()));

                            // Устанавливаем состояние selected, только если оно отличается от текущего,
                            // чтобы избежать рекурсивного вызова ActionListener
                            if (radioItem.isSelected() != shouldBeSelected) {
                                radioItem.setSelected(shouldBeSelected);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Логгируем ошибку, но не прерываем работу приложения
            System.err.println("Error updating color scheme menu selection: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Вспомогательный метод для отображения диалогового окна с сообщением об ошибке.
     *
     * @param title   Заголовок окна ошибки.
     * @param message Текст сообщения об ошибке.
     */
    private void showErrorDialog(String title, String message) {
        JOptionPane.showMessageDialog(ownerFrame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Создает меню "Animation" с опцией настройки анимации.
     */
    private void createAnimationMenu() {
        JMenu animationMenu = new JMenu("Animation");
        animationMenu.setMnemonic(KeyEvent.VK_A); // Alt+A

        JMenuItem setupAnimationItem = new JMenuItem("Setup Animation...");
        setupAnimationItem.addActionListener(e -> openAnimationSetupDialog());
        animationMenu.add(setupAnimationItem);

        add(animationMenu); // Добавляем меню в строку меню
    }

    /**
     * Открывает диалоговое окно настройки анимации.
     */
    private void openAnimationSetupDialog() {
        // Создаем и отображаем диалог
        AnimationSetupDialog dialog = new AnimationSetupDialog(ownerFrame, viewModel, animationService);
        dialog.display(); // Показываем диалог
    }
}