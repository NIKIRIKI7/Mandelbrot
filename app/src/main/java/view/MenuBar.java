// File: app/src/main/java/view/MenuBar.java
package view;

import model.ColorScheme;
import model.FractalState;
import model.GrayscaleScheme;
import model.NonlinearRGBScheme;
import services.AnimationService;
import services.FileService;
import shortcuts.KeyboardShortcutManager;
import viewmodel.FractalViewModel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Создает и управляет главным меню приложения для исследования фракталов.
 */
public class MenuBar extends JMenuBar {

    private final FractalViewModel viewModel;
    private final FileService fileService;
    private final FractalPanel fractalPanel;
    private final StatusBar statusBar;
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
     * @param statusBar Строка состояния.
     */
    public MenuBar(FractalViewModel viewModel, FileService fileService, FractalPanel fractalPanel, StatusBar statusBar) {
        this.viewModel = viewModel;
        this.fileService = fileService;
        this.fractalPanel = fractalPanel;
        this.statusBar = statusBar;
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
                if (newState != null && statusBar != null && !fractalPanel.isRendering) { // Обновляем, если не идет рендер
                    statusBar.setStatus(
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

    /**
     * Создает меню Файл с применением паттерна Command для горячих клавиш.
     */
    private void createFileMenu() {
        JMenu fileMenu = new JMenu("Файл");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        // Настраиваем команды для пунктов меню
        JMenuItem loadItem = new JMenuItem();
        KeyboardShortcutManager.getInstance().registerShortcut(
            KeyboardShortcutManager.SHORTCUT_OPEN, 
            createLoadCommand());
        KeyboardShortcutManager.getInstance().bindMenuItemToShortcut(
            loadItem, KeyboardShortcutManager.SHORTCUT_OPEN);
        fileMenu.add(loadItem);

        JMenuItem saveItem = new JMenuItem();
        KeyboardShortcutManager.getInstance().registerShortcut(
            KeyboardShortcutManager.SHORTCUT_SAVE, 
            createSaveCommand());
        KeyboardShortcutManager.getInstance().bindMenuItemToShortcut(
            saveItem, KeyboardShortcutManager.SHORTCUT_SAVE);
        fileMenu.add(saveItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem();
        KeyboardShortcutManager.getInstance().registerShortcut(
            KeyboardShortcutManager.SHORTCUT_EXIT, 
            createExitCommand());
        KeyboardShortcutManager.getInstance().bindMenuItemToShortcut(
            exitItem, KeyboardShortcutManager.SHORTCUT_EXIT);
        fileMenu.add(exitItem);

        add(fileMenu);
    }
    
    /**
     * Создает команду загрузки файла.
     */
    private shortcuts.AppCommand createLoadCommand() {
        return new shortcuts.AppCommand() {
            @Override
            public String getName() {
                return "Загрузить состояние...";
            }
            
            @Override
            public void execute() {
                loadFractal();
            }
            
            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }
    
    /**
     * Создает команду сохранения файла (паттерн Command).
     */
    private shortcuts.AppCommand createSaveCommand() {
        return new shortcuts.AppCommand() {
            @Override
            public String getName() {
                return "Сохранить...";
            }
            
            @Override
            public void execute() {
                saveUsingFileChooser();
            }
            
            @Override
            public boolean isEnabled() {
                return true;
            }
        };
    }
    
    /**
     * Создает команду выхода из приложения (паттерн Command).
     */
    private shortcuts.AppCommand createExitCommand() {
        return new shortcuts.AppCommand() {
            @Override
            public String getName() {
                return "Выход";
            }
            
            @Override
            public void execute() {
                Window window = SwingUtilities.getWindowAncestor(MenuBar.this);
                if (window != null) {
                    window.dispatchEvent(new java.awt.event.WindowEvent(window, java.awt.event.WindowEvent.WINDOW_CLOSING));
                }
            }
            
            @Override
            public boolean isEnabled() {
                return true;
            }
        };
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

        int result = fileChooser.showSaveDialog(this);
        

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
        JMenu editMenu = new JMenu("Правка");
        editMenu.setMnemonic(KeyEvent.VK_E);

        undoMenuItem = new JMenuItem();
        // Регистрируем команду Undo
        KeyboardShortcutManager.getInstance().registerShortcut(
            KeyboardShortcutManager.SHORTCUT_UNDO, 
            createUndoCommand());
        KeyboardShortcutManager.getInstance().bindMenuItemToShortcut(
            undoMenuItem, KeyboardShortcutManager.SHORTCUT_UNDO);
        editMenu.add(undoMenuItem);

        add(editMenu);
    }
    
    /**
     * Создает команду отмены действия (паттерн Command).
     * 
     * Реализует интерфейс AppCommand, который является ключевым компонентом паттерна Command:
     * 1. getName() - возвращает отображаемое название команды для элемента меню
     * 2. execute() - выполняет саму команду отмены через ViewModel
     * 3. isEnabled() - определяет, доступна ли команда в данный момент,
     *    проверяя наличие действий для отмены в UndoManager
     * 
     * Эта анонимная реализация AppCommand используется как для пункта меню,
     * так и для горячей клавиши, что обеспечивает единую логику выполнения
     * команды отмены независимо от способа её вызова.
     */
    private shortcuts.AppCommand createUndoCommand() {
        return new shortcuts.AppCommand() {
            @Override
            public String getName() {
                return "Отменить";
            }
            
            @Override
            public void execute() {
                viewModel.undoLastAction();
            }
            
            @Override
            public boolean isEnabled() {
                return viewModel.getUndoManager().canUndo();
            }
        };
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
                    statusBar.setStatus("Цветовая схема изменена на: " + scheme.getName());
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

    /**
     * Обновляет состояние пункта меню Undo.
     * @param canUndo Можно ли выполнить отмену действия
     */
    private void updateUndoState(boolean canUndo) {
        undoMenuItem.setEnabled(canUndo);
        // Также обновляем состояние всех команд горячих клавиш
        updateShortcutStates();
    }
    
    /**
     * Обновляет состояние всех команд горячих клавиш.
     * Вызывается после изменений, которые могут повлиять на доступность команд.
     */
    private void updateShortcutStates() {
        SwingUtilities.invokeLater(() -> 
            KeyboardShortcutManager.getInstance().updateMenuItemStates(this));
    }

    // --- Обработчики действий меню с интеграцией StatusBar ---

    private void loadFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Загрузить состояние фрактала");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Файлы состояния (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        int result = fileChooser.showOpenDialog(this);
         // Получаем StatusBar

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

    /**
     * Сохраняет текущее состояние фрактала в файл с расширением .frac
     * Позволяет выбрать файл через JFileChooser и обновляет строку состояния
     * после завершения операции или в случае ошибки.
     * 
     * Аннотация указывает, что метод фактически используется через обработчики событий.
     */
    @SuppressWarnings("unused")
    private void saveFractal() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить состояние фрактала как");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Файлы состояния (*.frac)", "frac"));
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setSelectedFile(new File("fractal_state.frac"));

        int result = fileChooser.showSaveDialog(this);

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

    /**
     * Сохраняет текущее изображение фрактала в файл в заданном формате.
     * Выполняет проверку наличия готового изображения, позволяет выбрать
     * файл через JFileChooser и обновляет строку состояния.
     * 
     * Аннотация указывает, что метод фактически используется через обработчики событий.
     * 
     * @param format Формат сохранения (например, "png", "jpeg")
     */
    @SuppressWarnings("unused")
    private void saveImage(String format) {
        BufferedImage imageToSave = fractalPanel.getCurrentImage();
        
        if (imageToSave == null) {
            String msg = "Нет готового изображения для сохранения. Подождите завершения рендеринга.";
            JOptionPane.showMessageDialog(this, msg, "Ошибка сохранения", JOptionPane.WARNING_MESSAGE);
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

        int result = fileChooser.showSaveDialog(this);
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
        
        String currentIterationsStr = Integer.toString(viewModel.getCurrentState().getMaxIterations());
        String input = (String) JOptionPane.showInputDialog(
            this,
            "Введите максимальное количество итераций:",
            "Задать макс. итераций",
            JOptionPane.QUESTION_MESSAGE,
            null,
            null,
            currentIterationsStr
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
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }


    private void openAnimationSetupDialog() {
        // Проверяем, не идет ли основной рендеринг
        if (fractalPanel.isRendering) {
            statusBar.setStatus("Дождитесь завершения рендеринга перед открытием редактора анимации.");
            JOptionPane.showMessageDialog(this,
                    "Пожалуйста, дождитесь завершения текущего рендеринга фрактала.",
                    "Рендеринг активен", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Создаем и отображаем диалог
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
AnimationSetupDialog dialog = new AnimationSetupDialog(owner, viewModel, animationService);
        dialog.display();
        // StatusBar главного окна обновляем после закрытия диалога (если нужно)
        // statusBar.setStatus("Редактор анимации закрыт.");
    }
}