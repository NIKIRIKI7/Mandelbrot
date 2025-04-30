package config;

import services.FileService;
import shortcuts.AppCommand;
import shortcuts.KeyboardShortcutManager;
import view.MainFrame;
import viewmodel.FractalViewModel;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * Конфигуратор горячих клавиш (паттерн Strategy).
 * Позволяет настраивать горячие клавиши и связанные с ними команды.
 */
public class ShortcutConfiguration {
    
    /**
     * Инициализирует стандартный набор горячих клавиш.
     * 
     * @param viewModel Модель представления фрактала
     * @param fileService Сервис для работы с файлами
     * @param mainFrame Главное окно приложения
     */
    public static void initializeDefaultShortcuts(
            FractalViewModel viewModel, 
            FileService fileService,
            MainFrame mainFrame) {
        
        KeyboardShortcutManager manager = KeyboardShortcutManager.getInstance();
        
        // Команда "Открыть" (Ctrl+O)
        manager.registerShortcut(KeyboardShortcutManager.SHORTCUT_OPEN, 
            new AppCommand() {
                @Override
                public String getName() {
                    return "Загрузить состояние...";
                }
                
                @Override
                public void execute() {
                    // Имитируем поведение метода loadFractal() из MenuBar
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Загрузить состояние фрактала");
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Файл состояния (*.frac)", "frac"));
                    
                    int result = fileChooser.showOpenDialog(mainFrame);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        try {
                            viewModel.loadState(fileService.loadFractalState(selectedFile));
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(mainFrame, 
                                    "Ошибка загрузки файла: " + ex.getMessage(),
                                    "Ошибка загрузки", 
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                
                @Override
                public boolean isEnabled() {
                    return true;
                }
            });
            
        // Команда "Сохранить" (Ctrl+S)
        manager.registerShortcut(KeyboardShortcutManager.SHORTCUT_SAVE, 
            new AppCommand() {
                @Override
                public String getName() {
                    return "Сохранить...";
                }
                
                @Override
                public void execute() {
                    // Имитируем поведение метода saveUsingFileChooser() из MenuBar
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Сохранить файл");
                    
                    javax.swing.filechooser.FileNameExtensionFilter fracFilter = 
                            new javax.swing.filechooser.FileNameExtensionFilter("Файл состояния (*.frac)", "frac");
                    fileChooser.addChoosableFileFilter(fracFilter);
                    fileChooser.setFileFilter(fracFilter);
                    
                    int result = fileChooser.showSaveDialog(mainFrame);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        // Добавляем расширение .frac, если отсутствует
                        if (!selectedFile.getName().toLowerCase().endsWith(".frac")) {
                            selectedFile = new File(selectedFile.getAbsolutePath() + ".frac");
                        }
                        
                        try {
                            fileService.saveFractalState(viewModel.getCurrentState(), selectedFile);
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Файл успешно сохранен.",
                                    "Сохранение",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Ошибка сохранения файла: " + ex.getMessage(),
                                    "Ошибка сохранения", 
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                
                @Override
                public boolean isEnabled() {
                    return true;
                }
            });
            
        // Команда "Отменить" (Ctrl+Z)
        manager.registerShortcut(KeyboardShortcutManager.SHORTCUT_UNDO, 
            new AppCommand() {
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
            });
            
        // Команда "Выход" (Ctrl+Q)
        manager.registerShortcut(KeyboardShortcutManager.SHORTCUT_EXIT, 
            new AppCommand() {
                @Override
                public String getName() {
                    return "Выход";
                }
                
                @Override
                public void execute() {
                    if (mainFrame != null) {
                        mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
                    }
                }
                
                @Override
                public boolean isEnabled() {
                    return true;
                }
            });
    }
}
